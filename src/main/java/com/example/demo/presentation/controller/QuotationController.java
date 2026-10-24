package com.example.demo.presentation.controller;

import com.example.demo.application.port.in.GenerateQuotationPdfUseCase;
import com.example.demo.application.port.in.GenerateTemplateQuotationPdfUseCase;
import com.example.demo.application.port.in.ParseQuotationPdfUseCase;
import com.example.demo.application.shared.command.ParseQuotationPdfCommand;
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
 * 遵循六角形架構與防腐層規範。
 * </p>
 */
@Tag(name = "報價單 PDF 生成與解析 API", description = "提供基於 OpenPDF (2.0.4) 與 BIRT 報表範本之報價單 PDF 生成下載，以及基於 Apache PDFBox (3.0.8) 之 PDF 上傳內容抽取與欄位解析服務")
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/quotations")
@RequiredArgsConstructor
public class QuotationController {

	/** 標註底層使用的 PDF 渲染引擎 */
	public static final String PDF_ENGINE_OPENPDF = "OpenPDF";
	public static final String PDF_ENGINE_TEMPLATE = "BIRT (Template)";
	public static final String HEADER_PDF_ENGINE = "X-Pdf-Engine";

	private final GenerateQuotationPdfUseCase generateQuotationPdfUseCase;
	private final GenerateTemplateQuotationPdfUseCase generateTemplateQuotationPdfUseCase;
	private final ParseQuotationPdfUseCase parseQuotationPdfUseCase;
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
					name = "download",
					description = "是否以附件方式下載檔案 (預設為 true，回傳 attachment 標頭並於 Swagger UI 呈現「Download file」按鈕；若為 false 則為 inline 預覽)",
					example = "true"
			)
			@RequestParam(name = "download", required = false, defaultValue = "true")
			boolean download) {

		log.info("【API】收到取得報價單 PDF 請求: id={}, download={}, [引擎: {}]", id, download, PDF_ENGINE_OPENPDF);

		// 1. 封裝表現層 Request Data
		GetQuotationPdfResource resource = new GetQuotationPdfResource(id);

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
					name = "download",
					description = "是否以附件方式下載檔案 (預設為 true，回傳 attachment 標頭並於 Swagger UI 呈現「Download file」按鈕；若為 false 則為 inline 預覽)",
					example = "true"
			)
			@RequestParam(name = "download", required = false, defaultValue = "true")
			boolean download) {

		log.info("【API】收到取得報價單 PDF 請求 (使用範本): id={}, download={}, [引擎: {}]", id, download, PDF_ENGINE_TEMPLATE);

		// 1. 封裝表現層 Request Data
		GetQuotationPdfResource resource = new GetQuotationPdfResource(id);

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
