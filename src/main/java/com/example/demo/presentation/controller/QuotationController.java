package com.example.demo.presentation.controller;

import com.example.demo.application.port.in.AddWatermarkUseCase;
import com.example.demo.application.port.in.DecryptPdfUseCase;
import com.example.demo.application.port.in.EncryptPdfUseCase;
import com.example.demo.application.port.in.GenerateQuotationPdfUseCase;
import com.example.demo.application.port.in.GenerateTemplateQuotationPdfUseCase;
import com.example.demo.application.port.in.ParseQuotationPdfUseCase;
import com.example.demo.application.port.in.SignPdfUseCase;
import com.example.demo.application.shared.command.AddWatermarkCommand;
import com.example.demo.application.shared.command.DecryptPdfCommand;
import com.example.demo.application.shared.command.EncryptPdfCommand;
import com.example.demo.application.shared.command.ParseQuotationPdfCommand;
import com.example.demo.application.shared.command.SignPdfCommand;
import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.query.GetQuotationQuery;
import com.example.demo.presentation.assembler.QuotationResourceAssembler;
import com.example.demo.presentation.resource.in.GetQuotationPdfResource;
import com.example.demo.presentation.resource.out.ErrorOccurredResource;
import com.example.demo.presentation.resource.out.QuotationParsedResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 報價單 PDF 生成與下載 REST 控制器 (Presentation Layer - Inbound Adapter)
 * <p>
 * 提供報價單生成下載與上傳解析對外 HTTP API 介面：
 * 1. 產出與下載：客戶端僅需傳入單號或識別碼 ID，搭配 {@code @Validated} 進行參數檢核，使用 OpenPDF 或 BIRT 範本生成 PDF。
 * 2. 上傳與解析：客戶端上傳產出的 PDF 檔案，調用 {@link ParseQuotationPdfUseCase} 透過 PDFBox 解析結構化資訊並回傳前端。
 * 3. 浮水印蓋印：調用 {@link AddWatermarkUseCase} 於每一頁疊加半透明防偽字樣。
 * 4. 安全加解密與簽章：調用 {@link EncryptPdfUseCase}、{@link DecryptPdfUseCase} 與 {@link SignPdfUseCase} 進行密碼權限控管與 PKCS#7 數位簽署。
 * 遵循六角形架構與防腐層規範。
 * </p>
 */
@Tag(name = "報價單 PDF 生成、安全防護與數位簽章 API", description = "提供基於 OpenPDF (2.0.4) 與 BIRT 報表範本之報價單 PDF 生成下載、PDF 防偽浮水印蓋印、安全加密解密、PKCS#7 數位簽章，以及基於 Apache PDFBox (3.0.8) 之 PDF 上傳內容抽取與欄位解析服務")
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/quotations")
@RequiredArgsConstructor
public class QuotationController {

	/**
	 * 自定義 HTTP 回應標頭名稱 (Custom HTTP Response Header)
	 * <p>
	 * 用於在回應標頭中明確標註當前回傳之 PDF 檔案所採用的底層渲染或安全處理引擎，
	 * 提供執行期架構透明化、APM 效能監控、前端識別與自動化整合測試斷言。
	 * </p>
	 */
	public static final String HEADER_PDF_ENGINE = "X-Pdf-Engine";

	/**
	 * OpenPDF 程式化像素級高精確排版引擎標識
	 * <p>
	 * 用於標註透過 OpenPDF 2.0.4 純 Java 繪製之報價單文件（包含雙層防孤兒排版與單次防偽浮水印）。
	 * 對應端點：{@code GET /api/v1/quotations/openpdf}。
	 * </p>
	 */
	public static final String PDF_ENGINE_OPENPDF = "OpenPDF";

	/**
	 * Eclipse BIRT 視覺化報表設計範本渲染引擎標識
	 * <p>
	 * 用於標註透過 Eclipse BIRT Runtime 4.20.0 配合 XML 報表設計範本 ({@code .rptdesign}) 動態渲染之報價單文件。
	 * 對應端點：{@code GET /api/v1/quotations/template} 或 {@code GET /api/v1/quotations/birt}。
	 * </p>
	 */
	public static final String PDF_ENGINE_TEMPLATE = "BIRT (Template)";

	/**
	 * OpenPDF 通用防偽浮水印後製蓋印標識
	 * <p>
	 * 用於標註透過 OpenPDF {@code PdfStamper} 於既有 PDF 各頁面疊加傾斜 45 度半透明防偽字樣之處理結果。
	 * 對應端點：{@code POST /api/v1/quotations/watermark}。
	 * </p>
	 */
	public static final String PDF_ENGINE_WATERMARK = "OpenPDF (Watermark)";

	/**
	 * OpenPDF 輕量級安全加密防護標識 (備用)
	 * <p>
	 * 用於標註透過 OpenPDF 執行之 128-bit 權限控管與密碼加解密處理結果。
	 * </p>
	 */
	public static final String PDF_ENGINE_SECURITY = "OpenPDF (Security)";

	/**
	 * OpenPDF 標準數位簽章標識 (備用)
	 * <p>
	 * 用於標註透過 OpenPDF 執行之標準 PKCS#7 數位簽署處理結果。
	 * </p>
	 */
	public static final String PDF_ENGINE_SIGNATURE = "OpenPDF (Digital Signature)";

	/**
	 * Apache PDFBox 最高安全規格 256-bit AES 加解密防護引擎標識
	 * <p>
	 * 用於標註透過 Apache PDFBox 3.x 執行之軍規級 256-bit AES 加密、8 項細粒度權限矩陣控管或無損密碼解密還原。
	 * 對應端點：{@code POST /api/v1/quotations/encrypt} 與 {@code POST /api/v1/quotations/decrypt}。
	 * </p>
	 */
	public static final String PDF_ENGINE_PDFBOX_SECURITY = "PDFBox (Security)";

	/**
	 * Apache PDFBox 增量儲存不可否認性數位簽章引擎標識
	 * <p>
	 * 用於標註透過 Apache PDFBox 3.x 結合 BouncyCastle 執行之增量儲存 ({@code doc.saveIncremental}) PKCS#7 / PAdES
	 * 數位簽署，支援單據範本感知專屬規範與雙線防偽鋼印客製化外觀。
	 * 對應端點：{@code POST /api/v1/quotations/sign}。
	 * </p>
	 */
	public static final String PDF_ENGINE_PDFBOX_SIGNATURE = "PDFBox (Digital Signature)";


	private final GenerateQuotationPdfUseCase generateQuotationPdfUseCase;
	private final GenerateTemplateQuotationPdfUseCase generateTemplateQuotationPdfUseCase;
	private final ParseQuotationPdfUseCase parseQuotationPdfUseCase;
	private final AddWatermarkUseCase addWatermarkUseCase;
	private final EncryptPdfUseCase encryptPdfUseCase;
	private final DecryptPdfUseCase decryptPdfUseCase;
	private final SignPdfUseCase signPdfUseCase;
	private final QuotationResourceAssembler quotationResourceAssembler;

	/**
	 * 取得報價單 PDF 檔案 (使用 OpenPDF 引擎生成)
	 * <p>
	 * 單一接口收斂：客戶端僅需傳入 ID (Request Data)，若未提供 ID 則自動套用預設範本 ID。
	 * 預設以附件方式回傳 (Content-Disposition: attachment)，使 Swagger UI 與瀏覽器可直接下載檔案。
	 * 回傳標頭明確標註 {@code X-Pdf-Engine: OpenPDF}。
	 * </p>
	 *
	 * @param id       報價單識別碼或單號 (可選，未傳入時使用預設範本單號，格式限制為半形英數字、減號與底線)
	 * @param download 是否以附件直接下載檔案 (預設為 true，Swagger UI 會出現「Download file」按鈕；若為 false 則為 inline 瀏覽器預覽)
	 * @return PDF 二進位串流，包含 X-Pdf-Engine: OpenPDF 標頭
	 */
	@Operation(
			summary = "取得報價單 PDF 檔案 (使用 OpenPDF 引擎)",
			description = "單一接口收斂：客戶端傳入報價單識別碼或單號 ID，由應用層調用 MockServicePort 取得範本資料，"
					+ "並使用 OpenPDF (3.0.5) 排版引擎產出包含公司標頭、客戶交易網格、物料明細、稅額小計與雙方簽核欄位的標準 A4 PDF 檔案串流。"
					+ "預設為 attachment 模式，支援在 Swagger UI 介面上直接點擊「Download file」下載保存檔案；若未傳入 ID 則自動套用預設範本單號。"
	)
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "成功產出報價單 PDF 二進位位元組串流 (包含 X-Pdf-Engine: OpenPDF 標頭與 Download file 下載支援)",
					content = @Content(
							mediaType = MediaType.APPLICATION_PDF_VALUE,
							schema = @Schema(type = "string", format = "binary", description = "PDF 檔案二進位位元組陣列")
					),
					headers = {
							@Header(
									name = "Content-Disposition",
									description = "指示瀏覽器下載檔案與檔名 (例如 attachment; filename=\"QT-20261023-001_openpdf.pdf\")",
									schema = @Schema(type = "string")
							),
							@Header(
									name = "X-Pdf-Engine",
									description = "底層使用的 PDF 渲染引擎名稱 (固定為 OpenPDF)",
									schema = @Schema(type = "string")
							)
					}
			),
			@ApiResponse(
					responseCode = "400",
					description = "請求參數驗證失敗 (例如 ID 長度超過 50 個字元或包含非允許字元)",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorOccurredResource.class)
					)
			),
			@ApiResponse(
					responseCode = "500",
					description = "伺服器內部錯誤或 PDF 渲染失敗",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorOccurredResource.class)
					)
			)
	})
	@GetMapping(value = "/openpdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> getQuotationPdf(
			@Parameter(
					name = "id",
					description = "報價單識別碼或單號 (可選，未傳入時使用預設單號 QT-20261023-001。僅允許半形英數字、減號與底線，長度不可超過 50)",
					example = "QT-20261023-001"
			)
			@RequestParam(name = "id", required = false)
			@Size(max = 50, message = "報價單 ID 長度不可超過 50 個字元")
			@Pattern(regexp = "^[A-Za-z0-9_-]{0,50}$", message = "報價單 ID 格式不符 (僅允許半形英數字、減號與底線)")
			String id,

			@Parameter(
					name = "watermark",
					description = "可選的防偽浮水印文字 (例如：機密草稿、正式報價、內部審閱)",
					example = "機密草稿"
			)
			@RequestParam(name = "watermark", required = false)
			@Size(max = 30, message = "浮水印文字長度不可超過 30 個字元")
			String watermark,

			@Parameter(
					name = "download",
					description = "是否以附件方式下載檔案 (預設為 true，回傳 attachment 標頭並於 Swagger UI 呈現「Download file」按鈕；若為 false 則為 inline 預覽)",
					example = "true"
			)
			@RequestParam(name = "download", required = false, defaultValue = "true")
			boolean download) {

		log.info("【API】收到取得報價單 PDF 請求: id={}, watermark={}, download={}, [引擎: {}]", id, watermark, download, PDF_ENGINE_OPENPDF);

		// 1. 封裝表現層 Request Data
		GetQuotationPdfResource resource = new GetQuotationPdfResource(id, watermark);

		// 2. 防腐層組裝為應用層 Query
		GetQuotationQuery query = quotationResourceAssembler.toQuery(resource);

		// 3. 調用 UseCase 產出二進位 PDF
		byte[] pdfBytes = generateQuotationPdfUseCase.generateQuotationPdf(query);

		if (pdfBytes == null || pdfBytes.length == 0) {
			log.error("產出報價單 PDF 失敗: id={}", id);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

		String fileName = (query.id() != null && !query.id().trim().isEmpty() ? query.id() : "quotation") + "_openpdf.pdf";
		return buildPdfResponse(pdfBytes, fileName, download, PDF_ENGINE_OPENPDF);
	}

	/**
	 * 取得報價單 PDF 檔案 (使用範本 / BIRT 引擎生成)
	 * <p>
	 * 單一接口收斂：客戶端僅需傳入 ID (Request Data)，若未提供 ID 則自動套用預設範本 ID。
	 * 使用 Eclipse BIRT 報表設計範本 (bom_quotation_template.rptdesign) 排版產出。
	 * 預設以附件方式回傳 (Content-Disposition: attachment)，使 Swagger UI 與瀏覽器可直接下載檔案。
	 * 回傳標頭明確標註 {@code X-Pdf-Engine: BIRT (Template)}。
	 * </p>
	 *
	 * @param id       報價單識別碼或單號 (可選，未傳入時使用預設範本單號，格式限制為半形英數字、減號與底線)
	 * @param download 是否以附件直接下載檔案 (預設為 true，Swagger UI 會出現「Download file」按鈕；若為 false 則為 inline 瀏覽器預覽)
	 * @return PDF 二進位串流，包含 X-Pdf-Engine: BIRT (Template) 標頭
	 */
	@Operation(
			summary = "取得報價單 PDF 檔案 (使用範本 / BIRT 引擎)",
			description = "單一接口收斂：客戶端傳入報價單識別碼或單號 ID，由應用層調用 MockServicePort 取得資料，"
					+ "並透過 BIRT 報表設計範本 (bom_quotation_template.rptdesign) 渲染引擎產出包含公司標頭、客戶交易網格、物料明細、稅額小計與雙方簽核欄位的標準 A4 PDF 檔案串流。"
					+ "預設為 attachment 模式，支援在 Swagger UI 介面上直接點擊「Download file」下載保存檔案；若未傳入 ID 則自動套用預設範本單號。"
	)
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "成功使用範本產出報價單 PDF 二進位位元組串流 (包含 X-Pdf-Engine: BIRT (Template) 標頭與 Download file 下載支援)",
					content = @Content(
							mediaType = MediaType.APPLICATION_PDF_VALUE,
							schema = @Schema(type = "string", format = "binary", description = "PDF 檔案二進位位元組陣列")
					),
					headers = {
							@Header(
									name = "Content-Disposition",
									description = "指示瀏覽器下載檔案與檔名 (例如 attachment; filename=\"QT-20261023-001_template.pdf\")",
									schema = @Schema(type = "string")
							),
							@Header(
									name = "X-Pdf-Engine",
									description = "底層使用的 PDF 渲染引擎名稱 (固定為 BIRT (Template))",
									schema = @Schema(type = "string")
							)
					}
			),
			@ApiResponse(
					responseCode = "400",
					description = "請求參數驗證失敗 (例如 ID 長度超過 50 個字元或包含非允許字元)",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorOccurredResource.class)
					)
			),
			@ApiResponse(
					responseCode = "500",
					description = "伺服器內部錯誤或 PDF 範本渲染失敗",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorOccurredResource.class)
					)
			)
	})
	@GetMapping(value = {"/template", "/birt"}, produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> getTemplateQuotationPdf(
			@Parameter(
					name = "id",
					description = "報價單識別碼或單號 (可選，未傳入時使用預設單號 QT-20261023-001。僅允許半形英數字、減號與底線，長度不可超過 50)",
					example = "QT-20261023-001"
			)
			@RequestParam(name = "id", required = false)
			@Size(max = 50, message = "報價單 ID 長度不可超過 50 個字元")
			@Pattern(regexp = "^[A-Za-z0-9_-]{0,50}$", message = "報價單 ID 格式不符 (僅允許半形英數字、減號與底線)")
			String id,

			@Parameter(
					name = "watermark",
					description = "可選的防偽浮水印文字 (例如：機密草稿、正式報價、內部審閱)",
					example = "機密草稿"
			)
			@RequestParam(name = "watermark", required = false)
			@Size(max = 30, message = "浮水印文字長度不可超過 30 個字元")
			String watermark,

			@Parameter(
					name = "download",
					description = "是否以附件方式下載檔案 (預設為 true，回傳 attachment 標頭並於 Swagger UI 呈現「Download file」按鈕；若為 false 則為 inline 預覽)",
					example = "true"
			)
			@RequestParam(name = "download", required = false, defaultValue = "true")
			boolean download) {

		log.info("【API】收到取得報價單 PDF 請求 (使用範本): id={}, watermark={}, download={}, [引擎: {}]", id, watermark, download, PDF_ENGINE_TEMPLATE);

		// 1. 封裝表現層 Request Data
		GetQuotationPdfResource resource = new GetQuotationPdfResource(id, watermark);

		// 2. 防腐層組裝為應用層 Query
		GetQuotationQuery query = quotationResourceAssembler.toQuery(resource);

		// 3. 調用 UseCase 使用範本產出二進位 PDF
		byte[] pdfBytes = generateTemplateQuotationPdfUseCase.generateTemplateQuotationPdf(query);

		if (pdfBytes == null || pdfBytes.length == 0) {
			log.error("使用範本產出報價單 PDF 失敗: id={}", id);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

		String fileName = (query.id() != null && !query.id().trim().isEmpty() ? query.id() : "quotation") + "_template.pdf";
		return buildPdfResponse(pdfBytes, fileName, download, PDF_ENGINE_TEMPLATE);
	}

	/**
	 * 上傳並解析報價單 PDF 檔案
	 * <p>
	 * 供客戶端上傳產出的報價單 PDF 檔案，調用 {@link ParseQuotationPdfUseCase} 用例，
	 * 透過 {@code PdfExtractorPort} (Apache PDFBox) 萃取全文文字並進行結構化欄位分析，
	 * 回傳完整的報價單資訊 (包含單號、客戶網格、8 筆物料明細、小計稅額與條款說明) 供前端畫面即時展示。
	 * </p>
	 *
	 * @param file 上傳之報價單 PDF 檔案 (MultipartFile，不可為空)
	 * @return 報價單解析回應資源物件 (QuotationParsedResource)
	 * @throws IOException 檔案讀取失敗時拋出
	 */
	@Operation(
			summary = "上傳並解析報價單 PDF 檔案",
			description = "供客戶端上傳產出之報價單 PDF 檔案，調用應用層用例並透過 PdfExtractorPort (Apache PDFBox) 萃取文字與結構化欄位，回傳完整的報價單資料供前端畫面展示。"
	)
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "成功解析報價單 PDF 並回傳結構化資料",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = QuotationParsedResource.class)
					)
			),
			@ApiResponse(
					responseCode = "400",
					description = "上傳檔案為空或非有效 PDF 格式",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorOccurredResource.class)
					)
			),
			@ApiResponse(
					responseCode = "500",
					description = "伺服器內部錯誤或 PDF 解析失敗",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorOccurredResource.class)
					)
			)
	})
	@PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<QuotationParsedResource> parseQuotationPdf(
			@Parameter(
					description = "欲上傳解析之報價單 PDF 檔案",
					required = true,
					content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
			)
			@RequestParam("file") MultipartFile file) throws IOException {

		if (file == null || file.isEmpty()) {
			log.warn("上傳之報價單 PDF 檔案為空");
			return ResponseEntity.badRequest().build();
		}

		String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded.pdf";
		log.info("【API】收到上傳解析報價單 PDF 請求: fileName={}, byteSize={}", fileName, file.getSize());

		// 1. 封裝應用層 Command (防腐層：傳入 byte[]，嚴禁將 Spring MultipartFile 傳入 Port)
		ParseQuotationPdfCommand command = new ParseQuotationPdfCommand(file.getBytes(), fileName);

		// 2. 調用 UseCase 執行解析
		QuotationGottenResult result = parseQuotationPdfUseCase.parseQuotationPdf(command);

		// 3. 防腐層轉換為表現層 Response Resource
		QuotationParsedResource response = quotationResourceAssembler.toParsedResource(result);

		log.info("【API】報價單 PDF 解析成功並回傳前端: quotationNo={}, 客戶={}", response.quotationNo(), response.customerName());
		return ResponseEntity.ok(response);
	}

	/**
	 * 為上傳之 PDF 檔案加蓋防偽浮水印
	 * <p>
	 * 供客戶端上傳任意 PDF 檔案並指定浮水印文字，調用 {@link AddWatermarkUseCase} 用例，
	 * 透過 {@code PdfWatermarkPort} (OpenPdfWatermarkAdapter) 於每一頁中央繪製 45 度傾斜半透明防偽字樣，
	 * 支援微軟正黑體中文字型與任意紙張尺寸/旋轉角度，回傳加蓋完成之 PDF 二進位位元組串流。
	 * </p>
	 *
	 * @param file      欲加蓋浮水印之 PDF 檔案 (MultipartFile，不可為空)
	 * @param watermark 欲印製之浮水印文字 (例如：機密、草稿、作廢等，長度限 30 字元)
	 * @param download  是否以附件方式下載檔案 (預設為 true)
	 * @return 加蓋浮水印後之 PDF 二進位串流
	 * @throws IOException 檔案讀取失敗時拋出
	 */
	@Operation(
			summary = "為上傳之 PDF 檔案加蓋防偽浮水印",
			description = "提供通用 PDF 浮水印蓋印能力：客戶端上傳任何 PDF 檔案並指定浮水印文字，"
					+ "由 OpenPdfWatermarkAdapter 為每一頁中央繪製 45 度傾斜、半透明 (Opacity 0.12) 之防偽文字，"
					+ "回傳加工完成之 PDF 檔案串流，並標註 X-Pdf-Engine: OpenPDF (Watermark)。"
	)
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "成功為 PDF 加蓋浮水印並回傳檔案串流",
					content = @Content(
							mediaType = MediaType.APPLICATION_PDF_VALUE,
							schema = @Schema(type = "string", format = "binary", description = "PDF 檔案二進位位元組陣列")
					),
					headers = {
							@Header(
									name = "Content-Disposition",
									description = "指示瀏覽器下載檔案與檔名",
									schema = @Schema(type = "string")
							),
							@Header(
									name = "X-Pdf-Engine",
									description = "底層使用的渲染引擎名稱 (固定為 OpenPDF (Watermark))",
									schema = @Schema(type = "string")
							)
					}
			),
			@ApiResponse(
					responseCode = "400",
					description = "上傳檔案為空或浮水印文字未填",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorOccurredResource.class)
					)
			),
			@ApiResponse(
					responseCode = "500",
					description = "伺服器內部錯誤或浮水印蓋印失敗",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ErrorOccurredResource.class)
					)
			)
	})
	@PostMapping(value = "/watermark", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<byte[]> addWatermark(
			@Parameter(
					description = "欲加蓋浮水印之 PDF 檔案",
					required = true,
					content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
			)
			@RequestParam("file") MultipartFile file,

			@Parameter(
					description = "欲印製之浮水印文字 (例如：機密、草稿、正式報價、作廢等)",
					example = "機密草稿",
					required = true
			)
			@RequestParam("watermark")
			@NotBlank(message = "浮水印文字不可為空")
			@Size(max = 30, message = "浮水印文字長度不可超過 30 個字元")
			String watermark,

			@Parameter(
					name = "download",
					description = "是否以附件方式下載檔案 (預設為 true，回傳 attachment 標頭並於 Swagger UI 呈現「Download file」按鈕；若為 false 則為 inline 預覽)",
					example = "true"
			)
			@RequestParam(name = "download", required = false, defaultValue = "true")
			boolean download) throws IOException {

		if (file == null || file.isEmpty()) {
			log.warn("加蓋浮水印失敗：上傳之 PDF 檔案為空");
			throw new IllegalArgumentException("待加蓋浮水印之 PDF 檔案不可為空");
		}

		String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
		log.info("【API】收到上傳加蓋浮水印請求: fileName={}, watermark={}, byteSize={}",
				originalFilename, watermark, file.getSize());

		// 1. 封裝應用層 Command (防腐層：傳入 byte[]，嚴禁將 Spring MultipartFile 傳入 Port)
		AddWatermarkCommand command = quotationResourceAssembler.toWatermarkCommand(file, watermark);

		// 2. 調用 UseCase 執行浮水印蓋印
		byte[] watermarkedPdf = addWatermarkUseCase.addWatermark(command);

		String baseName = originalFilename.toLowerCase().endsWith(".pdf")
				? originalFilename.substring(0, originalFilename.length() - 4)
				: originalFilename;
		String downloadFileName = baseName + "_watermarked.pdf";

		log.info("【API】PDF 浮水印蓋印成功: downloadFileName={}, outputSize={}", downloadFileName, watermarkedPdf.length);
		return buildPdfResponse(watermarkedPdf, downloadFileName, download, PDF_ENGINE_WATERMARK);
	}

	/**
	 * 對上傳之 PDF 檔案進行標準安全加密與權限控管
	 *
	 * @param file          欲加密之原始 PDF 檔案 (MultipartFile，不可為空)
	 * @param userPassword  使用者閱讀開啟密碼 (可為空，若未設定則開啟不需密碼，但受限於權限限制)
	 * @param ownerPassword 擁有者完整權限密碼 (若未設定則自動與使用者密碼對齊或採用安全預設)
	 * @param allowPrinting 是否允許列印文件 (預設 true)
	 * @param allowCopy     是否允許複製文件內容 (預設 true)
	 * @param download      是否以附件方式下載檔案 (預設 true)
	 * @return 加密後之 PDF 二進位位元組串流
	 * @throws IOException 檔案讀取異常
	 */
	@Operation(
			summary = "對上傳之 PDF 檔案進行安全加密與權限保護",
			description = "提供通用 PDF 加密保護服務 (以 Apache PDFBox 為專屬核心引擎)：支援設定使用者開啟密碼、擁有者密碼，"
					+ "並可個別啟用或禁用文件列印與內容複製權限，回傳加密受保護之 PDF 檔案串流。"
	)
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "成功完成 PDF 加密並回傳檔案串流",
					content = @Content(
							mediaType = MediaType.APPLICATION_PDF_VALUE,
							schema = @Schema(type = "string", format = "binary", description = "加密後之 PDF 檔案二進位位元組陣列")
					),
					headers = {
							@Header(name = "Content-Disposition", description = "指示瀏覽器下載檔案與檔名", schema = @Schema(type = "string")),
							@Header(name = "X-Pdf-Engine", description = "底層渲染引擎名稱 (固定為 PDFBox (Security))", schema = @Schema(type = "string"))
					}
			),
			@ApiResponse(
					responseCode = "400",
					description = "上傳檔案為空或參數錯誤",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorOccurredResource.class))
			)
	})
	@PostMapping(value = "/encrypt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<byte[]> encryptPdf(
			@Parameter(description = "欲加密之 PDF 檔案", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
			@RequestParam("file") MultipartFile file,

			@Parameter(description = "使用者開啟密碼 (可為空或留白)", example = "userSecret")
			@RequestParam(value = "userPassword", required = false) String userPassword,

			@Parameter(description = "擁有者/管理密碼 (可為空或留白)", example = "ownerSecret")
			@RequestParam(value = "ownerPassword", required = false) String ownerPassword,

			@Parameter(description = "是否允許列印文件", example = "true")
			@RequestParam(value = "allowPrinting", required = false, defaultValue = "true") boolean allowPrinting,

			@Parameter(description = "是否允許複製文字內容", example = "true")
			@RequestParam(value = "allowCopy", required = false, defaultValue = "true") boolean allowCopy,

			@Parameter(description = "是否以附件方式下載檔案", example = "true")
			@RequestParam(name = "download", required = false, defaultValue = "true") boolean download) throws IOException {

		if (file == null || file.isEmpty()) {
			log.warn("PDF 加密失敗：上傳之檔案為空");
			throw new IllegalArgumentException("待加密之 PDF 檔案不可為空");
		}

		String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
		log.info("【API】收到上傳 PDF 加密請求: fileName={}, userPwdSet={}, ownerPwdSet={}",
				originalFilename, userPassword != null && !userPassword.isEmpty(), ownerPassword != null && !ownerPassword.isEmpty());

		// 1. 防腐層組裝 Command (全面採用 PDFBox 引擎)
		EncryptPdfCommand command = quotationResourceAssembler.toEncryptCommand(
				file, userPassword, ownerPassword, allowPrinting, allowCopy
		);

		// 2. 調用 UseCase 執行加密
		byte[] encryptedPdf = encryptPdfUseCase.encryptPdf(command);

		String baseName = originalFilename.toLowerCase().endsWith(".pdf")
				? originalFilename.substring(0, originalFilename.length() - 4)
				: originalFilename;
		String downloadFileName = baseName + "_encrypted.pdf";

		log.info("【API】PDF 加密完成: downloadFileName={}, outputSize={}, engine={}", downloadFileName, encryptedPdf.length, PDF_ENGINE_PDFBOX_SECURITY);
		return buildPdfResponse(encryptedPdf, downloadFileName, download, PDF_ENGINE_PDFBOX_SECURITY);
	}

	/**
	 * 對受密碼保護之 PDF 檔案進行解密並解除限制
	 *
	 * @param file     欲解密之加密 PDF 檔案 (MultipartFile，不可為空)
	 * @param password 解密驗證密碼 (可為空，若未設定則嘗試以空密碼開啟)
	 * @param download 是否以附件方式下載檔案 (預設 true)
	 * @return 解密後無安全限制之標準 PDF 二進位位元組串流
	 * @throws IOException 檔案讀取異常
	 */
	@Operation(
			summary = "對受密碼保護之 PDF 檔案進行解密",
			description = "提供通用 PDF 解密服務 (以 Apache PDFBox 為專屬核心引擎)：接收受保護之 PDF 文件與密碼，驗證成功後解除密碼保護與操作權限限制，"
					+ "回傳乾淨之標準 PDF 檔案串流。若密碼不符將回傳 400 Bad Request。"
	)
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "成功完成 PDF 解密並回傳檔案串流",
					content = @Content(
							mediaType = MediaType.APPLICATION_PDF_VALUE,
							schema = @Schema(type = "string", format = "binary", description = "解密後之標準 PDF 檔案二進位位元組陣列")
					),
					headers = {
							@Header(name = "Content-Disposition", description = "指示瀏覽器下載檔案與檔名", schema = @Schema(type = "string")),
							@Header(name = "X-Pdf-Engine", description = "底層渲染引擎名稱 (固定為 PDFBox (Security))", schema = @Schema(type = "string"))
					}
			),
			@ApiResponse(
					responseCode = "400",
					description = "密碼錯誤、檔案損毀或未上傳檔案",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorOccurredResource.class))
			)
	})
	@PostMapping(value = "/decrypt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<byte[]> decryptPdf(
			@Parameter(description = "欲解密之受保護 PDF 檔案", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
			@RequestParam("file") MultipartFile file,

			@Parameter(description = "解密密碼 (使用者密碼或擁有者密碼)", example = "userSecret")
			@RequestParam(value = "password", required = false) String password,

			@Parameter(description = "是否以附件方式下載檔案", example = "true")
			@RequestParam(name = "download", required = false, defaultValue = "true") boolean download) throws IOException {

		if (file == null || file.isEmpty()) {
			log.warn("PDF 解密失敗：上傳之檔案為空");
			throw new IllegalArgumentException("待解密之 PDF 檔案不可為空");
		}

		String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
		log.info("【API】收到上傳 PDF 解密請求: fileName={}, passwordProvided={}",
				originalFilename, password != null && !password.isEmpty());

		// 1. 防腐層組裝 Command (全面採用 PDFBox 引擎)
		DecryptPdfCommand command = quotationResourceAssembler.toDecryptCommand(file, password);

		// 2. 調用 UseCase 執行解密
		byte[] decryptedPdf = decryptPdfUseCase.decryptPdf(command);

		String baseName = originalFilename.toLowerCase().endsWith(".pdf")
				? originalFilename.substring(0, originalFilename.length() - 4)
				: originalFilename;
		String downloadFileName = baseName + "_decrypted.pdf";

		log.info("【API】PDF 解密完成: downloadFileName={}, outputSize={}, engine={}", downloadFileName, decryptedPdf.length, PDF_ENGINE_PDFBOX_SECURITY);
		return buildPdfResponse(decryptedPdf, downloadFileName, download, PDF_ENGINE_PDFBOX_SECURITY);
	}

	/**
	 * 對 PDF 檔案進行標準 PKCS#7 數位簽章與加蓋防偽印章
	 *
	 * @param file        欲簽章之原始 PDF 檔案 (MultipartFile，不可為空)
	 * @param keystore    PKCS#12 (.p12/.pfx) 金鑰庫檔案 (MultipartFile，不可為空)
	 * @param password    金鑰庫密碼 (不可為空)
	 * @param alias       私鑰別名 (可選，預設取金鑰庫第一組別名)
	 * @param reason      簽章理由說明 (例如：核准報價單)
	 * @param location    簽章地點 (例如：台北市)
	 * @param contact     簽署人聯絡資訊 (可選)
	 * @param visual      是否印製可見視覺印章 (預設 true)
	 * @param page        視覺印章印製頁碼 (預設第 1 頁)
	 * @param template    指定套用之單據範本名稱 (例如：bom，可選，未傳入時使用系統預設簽章規範)
	 * @param download    是否以附件方式下載檔案 (預設 true)
	 * @return 完成數位簽署之 PDF 二進位位元組串流
	 * @throws IOException 檔案讀取異常
	 */
	@Operation(
			summary = "對 PDF 檔案進行 PKCS#7 數位簽章與防偽印章印製",
			description = "提供不可否認性之 PDF 數位簽署能力 (以 Apache PDFBox 為專屬核心引擎)：支援上傳標準 PKCS#12 (.p12/.pfx) 金鑰庫憑證，"
					+ "依據 PKCS#7 / X.509 標準對 PDF 進行加密數位簽名，支援個別單據範本 (PdfDocumentTemplate) 自定義簽章規範 (Policy / Visual Stamp) 或自動採用預設簽章規範。"
	)
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "成功完成 PDF 數位簽署並回傳檔案串流",
					content = @Content(
							mediaType = MediaType.APPLICATION_PDF_VALUE,
							schema = @Schema(type = "string", format = "binary", description = "完成數位簽名之 PDF 檔案二進位位元組陣列")
					),
					headers = {
							@Header(name = "Content-Disposition", description = "指示瀏覽器下載檔案與檔名", schema = @Schema(type = "string")),
							@Header(name = "X-Pdf-Engine", description = "底層渲染引擎名稱 (固定為 PDFBox (Digital Signature))", schema = @Schema(type = "string"))
					}
			),
			@ApiResponse(
					responseCode = "400",
					description = "憑證金鑰庫密碼錯誤、格式不合或檔案為空",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorOccurredResource.class))
			)
	})
	@PostMapping(value = "/sign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<byte[]> signPdf(
			@Parameter(description = "欲進行數位簽章之原始 PDF 檔案", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
			@RequestParam("file") MultipartFile file,

			@Parameter(description = "PKCS#12 憑證金鑰庫檔案 (.p12 / .pfx)", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
			@RequestParam("keystore") MultipartFile keystore,

			@Parameter(description = "金鑰庫解鎖密碼", example = "secret123", required = true)
			@RequestParam("password") @NotBlank(message = "金鑰庫解鎖密碼不可為空") String password,

			@Parameter(description = "私鑰別名 (Alias，可選)", example = "my-alias")
			@RequestParam(value = "alias", required = false) String alias,

			@Parameter(description = "簽章理由說明 (若單據範本有特別定義且此處未提供，則優先套用範本理由)", example = "核准正式報價")
			@RequestParam(value = "reason", required = false) String reason,

			@Parameter(description = "簽章地點 (若單據範本有特別定義且此處未提供，則優先套用範本地點)", example = "台北總部")
			@RequestParam(value = "location", required = false) String location,

			@Parameter(description = "簽署人聯絡資訊", example = "sign@company.com")
			@RequestParam(value = "contact", required = false) String contact,

			@Parameter(description = "是否加蓋可見印章方塊", example = "true")
			@RequestParam(value = "visual", required = false, defaultValue = "true") boolean visual,

			@Parameter(description = "視覺印章所在頁碼 (自 1 起算)", example = "1")
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,

			@Parameter(description = "指定套用之單據範本名稱 (例如：bom，可選，未指定或範本未定義時自動使用預設簽章規範)", example = "bom")
			@RequestParam(value = "template", required = false) String template,

			@Parameter(description = "是否以附件方式下載檔案", example = "true")
			@RequestParam(name = "download", required = false, defaultValue = "true") boolean download) throws IOException {

		if (file == null || file.isEmpty()) {
			log.warn("PDF 數位簽章失敗：上傳之 PDF 檔案為空");
			throw new IllegalArgumentException("待簽章之 PDF 檔案不可為空");
		}
		if (keystore == null || keystore.isEmpty()) {
			log.warn("PDF 數位簽章失敗：上傳之金鑰庫檔案為空");
			throw new IllegalArgumentException("PKCS#12 金鑰庫檔案不可為空");
		}

		String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
		log.info("【API】收到上傳 PDF 數位簽章請求: fileName={}, template={}, reason={}, location={}",
				originalFilename, template, reason, location);

		// 1. 防腐層組裝 Command (全面採用 PDFBox 引擎，支援個別範本簽章規範)
		SignPdfCommand command = quotationResourceAssembler.toSignCommand(
				file, keystore, password, alias, reason, location, contact, visual, page, 360f, 50f, 180f, 60f, template
		);

		// 2. 調用 UseCase 執行簽署
		byte[] signedPdf = signPdfUseCase.signPdf(command);

		String baseName = originalFilename.toLowerCase().endsWith(".pdf")
				? originalFilename.substring(0, originalFilename.length() - 4)
				: originalFilename;
		String downloadFileName = baseName + "_signed.pdf";

		log.info("【API】PDF 數位簽章完成: downloadFileName={}, outputSize={}, engine={}", downloadFileName, signedPdf.length, PDF_ENGINE_PDFBOX_SIGNATURE);
		return buildPdfResponse(signedPdf, downloadFileName, download, PDF_ENGINE_PDFBOX_SIGNATURE);
	}

	/**
	 * 建立包含標準 HTTP 標頭的 PDF 回應實體
	 *
	 * @param pdfBytes PDF 檔案二進位位元組陣列
	 * @param fileName 下載或預覽之檔案名稱
	 * @param download 是否以附件 (attachment) 方式下載
	 * @param engine   底層渲染引擎名稱 (例如 OpenPDF 或 BIRT (Template))
	 * @return ResponseEntity 二進位回應
	 */
	private ResponseEntity<byte[]> buildPdfResponse(byte[] pdfBytes, String fileName, boolean download, String engine) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentLength(pdfBytes.length);

		// 設定 Content-Disposition：預設為 attachment 以觸發瀏覽器下載與 Swagger UI 的「Download file」按鈕；若為 false 則設定 inline 供瀏覽器預覽
		ContentDisposition contentDisposition = download
				? ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build()
				: ContentDisposition.inline().filename(fileName, StandardCharsets.UTF_8).build();
		headers.setContentDisposition(contentDisposition);

		// 暴露 Content-Disposition 等標頭，確保 Swagger UI 等 Fetch/XHR 客戶端能識別 attachment 下載
		headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition, Content-Length, " + HEADER_PDF_ENGINE);

		// 明確標註渲染引擎名稱
		headers.set(HEADER_PDF_ENGINE, engine);

		return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
	}
}
