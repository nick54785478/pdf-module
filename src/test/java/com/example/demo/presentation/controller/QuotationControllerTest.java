package com.example.demo.presentation.controller;

import com.example.demo.application.port.in.AddWatermarkUseCase;
import com.example.demo.application.port.in.DecryptPdfUseCase;
import com.example.demo.application.port.in.EncryptPdfUseCase;
import com.example.demo.application.port.in.GenerateQuotationPdfUseCase;
import com.example.demo.application.port.in.GenerateTemplateQuotationPdfUseCase;
import com.example.demo.application.port.in.ParseQuotationPdfUseCase;
import com.example.demo.application.port.in.SignPdfUseCase;
import com.example.demo.application.shared.command.DecryptPdfCommand;
import com.example.demo.application.shared.command.EncryptPdfCommand;
import com.example.demo.application.shared.command.ParseQuotationPdfCommand;
import com.example.demo.application.shared.command.SignPdfCommand;
import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.dto.QuotationItemGottenResult;
import com.example.demo.application.shared.query.GetQuotationQuery;
import com.example.demo.presentation.assembler.QuotationResourceAssembler;
import com.example.demo.presentation.exception.GlobalExceptionHandler;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 報價單 PDF 控制器單元測試 (Controller Test)
 * <p>
 * 遵循 AGENTS.md 規範：
 * 1. 測試收斂為單一接口之 GET /api/v1/quotations/openpdf 與 GET /api/v1/quotations/template 路由、標頭與狀態碼。
 * 2. 測試上傳解析 PDF POST /api/v1/quotations/parse 接口之正常回傳與空檔檢核。
 * 3. 測試掛載 GlobalExceptionHandler 驗證異常回應。
 * 4. 測試方法皆以 BDD Gherkin 語法撰寫 Javadoc。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class QuotationControllerTest {

	private MockMvc mockMvc;

	@Mock
	private GenerateQuotationPdfUseCase generateQuotationPdfUseCase;

	@Mock
	private GenerateTemplateQuotationPdfUseCase generateTemplateQuotationPdfUseCase;

	@Mock
	private ParseQuotationPdfUseCase parseQuotationPdfUseCase;

	@Mock
	private AddWatermarkUseCase addWatermarkUseCase;

	@Mock
	private EncryptPdfUseCase encryptPdfUseCase;

	@Mock
	private DecryptPdfUseCase decryptPdfUseCase;

	@Mock
	private SignPdfUseCase signPdfUseCase;

	private final QuotationResourceAssembler quotationResourceAssembler = new QuotationResourceAssembler();

	@BeforeEach
	void setUp() {
		QuotationController controller = new QuotationController(
				generateQuotationPdfUseCase,
				generateTemplateQuotationPdfUseCase,
				parseQuotationPdfUseCase,
				addWatermarkUseCase,
				encryptPdfUseCase,
				decryptPdfUseCase,
				signPdfUseCase,
				quotationResourceAssembler
		);
		this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	/**
	 * Feature: 單一接口取得報價單 PDF API (使用 OpenPDF)
	 * Scenario: 客戶端傳入指定 ID 參數，成功產出對應單號之 PDF 檔案且標註 OpenPDF 引擎
	 *
	 * Given: 查詢 ID 為 "QT-2026-TEST"，Mock 用例回傳有效的二進位 PDF byte 陣列
	 * When: 發送 GET /api/v1/quotations/openpdf?id=QT-2026-TEST 請求
	 * Then: 回傳狀態碼 200 OK，Content-Type 為 application/pdf，Header X-Pdf-Engine 為 OpenPDF，檔名包含該 ID
	 */
	@Test
	@DisplayName("GET取得報價單PDF-指定ID情境-回傳200與OpenPDF標頭並預設attachment下載")
	void getQuotationPdf_withSpecificId_success() throws Exception {
		// Given
		byte[] mockPdf = "%PDF-1.4 Mock Quotation Content".getBytes();
		when(generateQuotationPdfUseCase.generateQuotationPdf(any(GetQuotationQuery.class))).thenReturn(mockPdf);

		// When & Then
		mockMvc.perform(get("/api/v1/quotations/openpdf").param("id", "QT-2026-TEST"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string("X-Pdf-Engine", "OpenPDF"))
				.andExpect(header().string("Access-Control-Expose-Headers", org.hamcrest.Matchers.containsString("Content-Disposition")))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;")))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("QT-2026-TEST_openpdf.pdf")))
				.andExpect(content().bytes(mockPdf));

		verify(generateQuotationPdfUseCase).generateQuotationPdf(new GetQuotationQuery("QT-2026-TEST"));
	}

	/**
	 * Feature: 單一接口取得報價單 PDF API (使用 OpenPDF)
	 * Scenario: 客戶端未傳入 ID 參數，自動套用預設單號並產出 PDF 檔案
	 *
	 * Given: 未傳入 id 參數，Mock 用例回傳有效的二進位 PDF byte 陣列
	 * When: 發送 GET /api/v1/quotations/openpdf 請求
	 * Then: 回傳狀態碼 200 OK，Content-Type 為 application/pdf，Header X-Pdf-Engine 為 OpenPDF，預設為 attachment
	 */
	@Test
	@DisplayName("GET取得報價單PDF-未傳入ID情境-回傳200並套用預設單號檔名")
	void getQuotationPdf_withoutId_success() throws Exception {
		// Given
		byte[] mockPdf = "%PDF-1.4 Default Quotation Content".getBytes();
		when(generateQuotationPdfUseCase.generateQuotationPdf(any(GetQuotationQuery.class))).thenReturn(mockPdf);

		// When & Then
		mockMvc.perform(get("/api/v1/quotations/openpdf"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string("X-Pdf-Engine", "OpenPDF"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;")))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("quotation_openpdf.pdf")))
				.andExpect(content().bytes(mockPdf));

		verify(generateQuotationPdfUseCase).generateQuotationPdf(new GetQuotationQuery(null));
	}

	/**
	 * Feature: 單一接口取得報價單 PDF API (使用 OpenPDF)
	 * Scenario: 客戶端指定 download=false 參數，以 inline 方式預覽 PDF
	 *
	 * Given: 傳入 download=false，Mock 用例回傳有效的二進位 PDF byte 陣列
	 * When: 發送 GET /api/v1/quotations/openpdf?id=QT-2026-INLINE&download=false 請求
	 * Then: 回傳狀態碼 200 OK，Content-Disposition 開頭為 inline;
	 */
	@Test
	@DisplayName("GET取得報價單PDF-預覽情境-回傳200並設定inline預覽標頭")
	void getQuotationPdf_withDownloadFalse_returnsInline() throws Exception {
		// Given
		byte[] mockPdf = "%PDF-1.4 Inline Quotation Content".getBytes();
		when(generateQuotationPdfUseCase.generateQuotationPdf(any(GetQuotationQuery.class))).thenReturn(mockPdf);

		// When & Then
		mockMvc.perform(get("/api/v1/quotations/openpdf").param("id", "QT-2026-INLINE").param("download", "false"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("inline;")))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("QT-2026-INLINE_openpdf.pdf")))
				.andExpect(content().bytes(mockPdf));

		verify(generateQuotationPdfUseCase).generateQuotationPdf(new GetQuotationQuery("QT-2026-INLINE"));
	}

	/**
	 * Feature: 報價單 PDF API 異常處理
	 * Scenario: 底層渲染失敗回傳 null 時，API 正確回應 500 狀態碼
	 *
	 * Given: Mock 用例回傳 null (代表渲染發生例外或失敗)
	 * When: 發送 GET /api/v1/quotations/openpdf 請求
	 * Then: 應回傳 500 Internal Server Error 狀態碼
	 */
	@Test
	@DisplayName("GET報價單PDF-異常情境-渲染失敗回傳500")
	void getQuotationPdf_failure_returns500() throws Exception {
		// Given
		when(generateQuotationPdfUseCase.generateQuotationPdf(any(GetQuotationQuery.class))).thenReturn(null);

		// When & Then
		mockMvc.perform(get("/api/v1/quotations/openpdf"))
				.andExpect(status().isInternalServerError());
	}

	/**
	 * Feature: 單一接口取得報價單 PDF API (使用範本 / BIRT)
	 * Scenario: 客戶端傳入指定 ID 參數，成功透過範本產出對應單號之 PDF 檔案且標註 BIRT (Template) 引擎
	 *
	 * Given: 查詢 ID 為 "QT-2026-TEST"，Mock 用例回傳有效的二進位 PDF byte 陣列
	 * When: 發送 GET /api/v1/quotations/template?id=QT-2026-TEST 請求
	 * Then: 回傳狀態碼 200 OK，Content-Type 為 application/pdf，Header X-Pdf-Engine 為 BIRT (Template)，檔名包含該 ID
	 */
	@Test
	@DisplayName("GET取得報價單PDF(範本)-指定ID情境-回傳200與BIRT(Template)標頭並預設attachment下載")
	void getTemplateQuotationPdf_withSpecificId_success() throws Exception {
		// Given
		byte[] mockPdf = "%PDF-1.5 Mock BIRT Quotation Content".getBytes();
		when(generateTemplateQuotationPdfUseCase.generateTemplateQuotationPdf(any(GetQuotationQuery.class))).thenReturn(mockPdf);

		// When & Then
		mockMvc.perform(get("/api/v1/quotations/template").param("id", "QT-2026-TEST"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string("X-Pdf-Engine", "BIRT (Template)"))
				.andExpect(header().string("Access-Control-Expose-Headers", org.hamcrest.Matchers.containsString("Content-Disposition")))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;")))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("QT-2026-TEST_template.pdf")))
				.andExpect(content().bytes(mockPdf));

		verify(generateTemplateQuotationPdfUseCase).generateTemplateQuotationPdf(new GetQuotationQuery("QT-2026-TEST"));
	}

	/**
	 * Feature: 單一接口取得報價單 PDF API (使用範本 / BIRT)
	 * Scenario: 客戶端未傳入 ID 參數，自動套用預設單號產出 PDF 檔案
	 *
	 * Given: 未傳入 ID 參數，Mock 用例依預設單號回傳二進位 byte 陣列
	 * When: 發送 GET /api/v1/quotations/template 請求
	 * Then: 回傳狀態碼 200 OK，檔名使用預設單號，Header X-Pdf-Engine 為 BIRT (Template)
	 */
	@Test
	@DisplayName("GET取得報價單PDF(範本)-未傳入ID情境-回傳200並套用預設單號")
	void getTemplateQuotationPdf_withoutId_usesDefaultTemplate() throws Exception {
		// Given
		byte[] mockPdf = "%PDF-1.5 Mock Default Template Content".getBytes();
		when(generateTemplateQuotationPdfUseCase.generateTemplateQuotationPdf(any(GetQuotationQuery.class))).thenReturn(mockPdf);

		// When & Then
		mockMvc.perform(get("/api/v1/quotations/template"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string("X-Pdf-Engine", "BIRT (Template)"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;")))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("quotation_template.pdf")))
				.andExpect(content().bytes(mockPdf));

		verify(generateTemplateQuotationPdfUseCase).generateTemplateQuotationPdf(new GetQuotationQuery(null));
	}

	/**
	 * Feature: 單一接口取得報價單 PDF API (使用範本 / BIRT)
	 * Scenario: 客戶端指定 download=false，回應標頭 Content-Disposition 設定為 inline
	 *
	 * Given: 傳入 download=false 參數
	 * When: 發送 GET /api/v1/quotations/template?id=QT-2026-INLINE&download=false 請求
	 * Then: 回傳狀態碼 200 OK，Content-Disposition 為 inline 預覽模式
	 */
	@Test
	@DisplayName("GET取得報價單PDF(範本)-預覽情境-download為false時設定inline標頭")
	void getTemplateQuotationPdf_withDownloadFalse_setsInlineDisposition() throws Exception {
		// Given
		byte[] mockPdf = "%PDF-1.5 Mock Inline Content".getBytes();
		when(generateTemplateQuotationPdfUseCase.generateTemplateQuotationPdf(any(GetQuotationQuery.class))).thenReturn(mockPdf);

		// When & Then
		mockMvc.perform(get("/api/v1/quotations/template")
						.param("id", "QT-2026-INLINE")
						.param("download", "false"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("inline;")))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("QT-2026-INLINE_template.pdf")))
				.andExpect(content().bytes(mockPdf));

		verify(generateTemplateQuotationPdfUseCase).generateTemplateQuotationPdf(new GetQuotationQuery("QT-2026-INLINE"));
	}

	/**
	 * Feature: 報價單 PDF API (使用範本 / BIRT) 異常處理
	 * Scenario: 範本渲染失敗回傳 null 時，API 正確回應 500 狀態碼
	 *
	 * Given: Mock 用例回傳 null (代表範本渲染發生例外或失敗)
	 * When: 發送 GET /api/v1/quotations/template 請求
	 * Then: 應回傳 500 Internal Server Error 狀態碼
	 */
	@Test
	@DisplayName("GET報價單PDF(範本)-異常情境-渲染失敗回傳500")
	void getTemplateQuotationPdf_failure_returns500() throws Exception {
		// Given
		when(generateTemplateQuotationPdfUseCase.generateTemplateQuotationPdf(any(GetQuotationQuery.class))).thenReturn(null);

		// When & Then
		mockMvc.perform(get("/api/v1/quotations/template"))
				.andExpect(status().isInternalServerError());
	}

	/**
	 * Feature: 上傳並解析報價單 PDF API (使用 PDFBox 引擎)
	 * Scenario: 使用者上傳有效 PDF 檔案，API 成功解析並回傳完整結構化 JSON 資訊
	 *
	 * Given: 包含 PDF 位元組串流之 MockMultipartFile，且 UseCase 回傳結構化報價單資料
	 * When: 發送 POST /api/v1/quotations/parse 檔案上傳請求
	 * Then: 應回傳 HTTP 200 OK，Content-Type 為 application/json，JSON 內各欄位正確對應
	 */
	@Test
	@DisplayName("POST上傳解析報價單PDF-正常情境-回傳200及結構化JSON")
	void parseQuotationPdf_success_returns200AndJson() throws Exception {
		// Given
		byte[] mockPdfBytes = "%PDF-1.4 Mock Quotation Content".getBytes();
		MockMultipartFile multipartFile = new MockMultipartFile(
				"file",
				"QT-2026-TEST.pdf",
				MediaType.APPLICATION_PDF_VALUE,
				mockPdfBytes
		);

		QuotationGottenResult mockResult = new QuotationGottenResult(
				"QT-2026-TEST",
				LocalDate.of(2026, 10, 23),
				"台積電測試股份有限公司",
				"12345678",
				"張經理 (03-578-1234)",
				"報價日起 30 天內有效",
				"月結 30 天 (Net 30)",
				"DDP 新竹科學園區研新一路 1 號",
				new BigDecimal("182000"),
				new BigDecimal("9100"),
				new BigDecimal("191100"),
				"1. 保固期限：交貨驗收合格日起提供原廠保固一年。",
				List.of(
						new QuotationItemGottenResult(
								1,
								"MAT-BOM-001",
								"工業級主控制板 (含散熱模組與高階嵌入式 CPU)",
								10,
								"PCS",
								new BigDecimal("4500"),
								new BigDecimal("45000"),
								"含 RoHS 認證標籤"
						)
				)
		);

		when(parseQuotationPdfUseCase.parseQuotationPdf(any(ParseQuotationPdfCommand.class))).thenReturn(mockResult);

		// When & Then
		mockMvc.perform(multipart("/api/v1/quotations/parse").file(multipartFile))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.quotationNo").value("QT-2026-TEST"))
				.andExpect(jsonPath("$.quotationDate").value("2026-10-23"))
				.andExpect(jsonPath("$.customerName").value("台積電測試股份有限公司"))
				.andExpect(jsonPath("$.customerTaxId").value("12345678"))
				.andExpect(jsonPath("$.subtotalAmount").value(182000))
				.andExpect(jsonPath("$.taxAmount").value(9100))
				.andExpect(jsonPath("$.totalAmount").value(191100))
				.andExpect(jsonPath("$.items[0].itemNo").value(1))
				.andExpect(jsonPath("$.items[0].materialNo").value("MAT-BOM-001"))
				.andExpect(jsonPath("$.items[0].description").value("工業級主控制板 (含散熱模組與高階嵌入式 CPU)"))
				.andExpect(jsonPath("$.items[0].quantity").value(10))
				.andExpect(jsonPath("$.items[0].unit").value("PCS"))
				.andExpect(jsonPath("$.items[0].unitPrice").value(4500))
				.andExpect(jsonPath("$.items[0].subtotal").value(45000))
				.andExpect(jsonPath("$.items[0].remark").value("含 RoHS 認證標籤"));

		verify(parseQuotationPdfUseCase).parseQuotationPdf(any(ParseQuotationPdfCommand.class));
	}

	/**
	 * Feature: 上傳並解析報價單 PDF API 檢核
	 * Scenario: 客戶端上傳空檔案時，API 正確回應 400 Bad Request
	 *
	 * Given: 內容為空之 MockMultipartFile
	 * When: 發送 POST /api/v1/quotations/parse 檔案上傳請求
	 * Then: 應回傳 HTTP 400 Bad Request
	 */
	@Test
	@DisplayName("POST上傳解析報價單PDF-驗證情境-空檔案回傳400")
	void parseQuotationPdf_emptyFile_returns400() throws Exception {
		// Given
		MockMultipartFile emptyFile = new MockMultipartFile(
				"file",
				"empty.pdf",
				MediaType.APPLICATION_PDF_VALUE,
				new byte[0]
		);

		// When & Then
		mockMvc.perform(multipart("/api/v1/quotations/parse").file(emptyFile))
				.andExpect(status().isBadRequest());
	}

	/**
	 * Feature: 取得 OpenPDF 報價單並指定浮水印文字
	 * Scenario: 客戶端傳入 watermark="機密草稿" 參數時，API 成功回應 200 與 PDF 串流
	 *
	 * Given: 查詢條件帶有 id=QT-2026-WM 與 watermark=機密草稿
	 * When: 發送 GET /api/v1/quotations/openpdf?id=QT-2026-WM&watermark=機密草稿
	 * Then: 成功回傳 HTTP 200 與 PDF 二進位資料，標頭為 X-Pdf-Engine: OpenPDF
	 */
	@Test
	@DisplayName("GET取得OpenPDF報價單-傳入浮水印參數-成功產出並回傳200")
	void getQuotationPdf_withWatermark_success() throws Exception {
		// Given
		byte[] mockPdf = "%PDF-1.4 openpdf with watermark".getBytes();
		when(generateQuotationPdfUseCase.generateQuotationPdf(any(GetQuotationQuery.class)))
				.thenReturn(mockPdf);

		// When & Then
		mockMvc.perform(get("/api/v1/quotations/openpdf")
						.param("id", "QT-2026-WM")
						.param("watermark", "機密草稿"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string("X-Pdf-Engine", "OpenPDF"))
				.andExpect(content().bytes(mockPdf));
	}

	/**
	 * Feature: 上傳 PDF 並加蓋浮水印 API
	 * Scenario: 正常情境 - 客戶端上傳 PDF 並指定浮水印文字，API 成功回傳蓋印後之 PDF 二進位檔
	 *
	 * Given: 合法 PDF 檔案與浮水印文字 "作廢"
	 * When: 發送 POST /api/v1/quotations/watermark
	 * Then: 成功回傳 HTTP 200 與 PDF 二進位資料，標頭為 X-Pdf-Engine: OpenPDF (Watermark)
	 */
	@Test
	@DisplayName("POST加蓋PDF浮水印-正常情境-成功回傳蓋印後之PDF二進位串流")
	void addWatermark_success() throws Exception {
		// Given
		MockMultipartFile validFile = new MockMultipartFile(
				"file",
				"sample.pdf",
				MediaType.APPLICATION_PDF_VALUE,
				"%PDF-1.4 sample file".getBytes()
		);
		byte[] watermarkedPdf = "%PDF-1.4 watermarked output".getBytes();
		when(addWatermarkUseCase.addWatermark(any())).thenReturn(watermarkedPdf);

		// When & Then
		mockMvc.perform(multipart("/api/v1/quotations/watermark")
						.file(validFile)
						.param("watermark", "作廢"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string("X-Pdf-Engine", "OpenPDF (Watermark)"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("sample_watermarked.pdf")))
				.andExpect(content().bytes(watermarkedPdf));
	}

	/**
	 * Feature: 上傳 PDF 並加蓋浮水印 API
	 * Scenario: 異常情境 - 客戶端上傳空檔案時，API 回應 400 Bad Request
	 *
	 * Given: 內容為空之檔案
	 * When: 發送 POST /api/v1/quotations/watermark
	 * Then: 回傳 HTTP 400 Bad Request
	 */
	@Test
	@DisplayName("POST加蓋PDF浮水印-驗證情境-空檔案回傳400")
	void addWatermark_emptyFile_returns400() throws Exception {
		MockMultipartFile emptyFile = new MockMultipartFile(
				"file",
				"empty.pdf",
				MediaType.APPLICATION_PDF_VALUE,
				new byte[0]
		);

		mockMvc.perform(multipart("/api/v1/quotations/watermark")
						.file(emptyFile)
						.param("watermark", "機密"))
				.andExpect(status().isBadRequest());
	}

	/**
	 * Feature: 上傳 PDF 進行安全加密 API
	 * Scenario: 正常流程 - 上傳 PDF 並設定密碼，預設使用 PDFBox 安全引擎，成功回傳加密檔案與 PDFBox 標頭
	 */
	@Test
	@DisplayName("POST加密PDF-預設引擎情境-成功回傳加密檔案與PDFBox Security標頭")
	void encryptPdf_defaultEngine_success() throws Exception {
		byte[] mockRaw = "%PDF-1.5 Raw".getBytes();
		byte[] mockEncrypted = "%PDF-1.5 Encrypted".getBytes();

		MockMultipartFile file = new MockMultipartFile(
				"file", "doc.pdf", MediaType.APPLICATION_PDF_VALUE, mockRaw
		);

		when(encryptPdfUseCase.encryptPdf(any(EncryptPdfCommand.class))).thenReturn(mockEncrypted);

		mockMvc.perform(multipart("/api/v1/quotations/encrypt")
						.file(file)
						.param("userPassword", "user123")
						.param("ownerPassword", "owner123")
						.param("allowPrinting", "true")
						.param("allowCopy", "false")
						.param("download", "true"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string("X-Pdf-Engine", "PDFBox (Security)"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("doc_encrypted.pdf")))
				.andExpect(content().bytes(mockEncrypted));
	}

	/**
	 * Feature: 上傳 PDF 進行安全加密 API
	 * Scenario: 正常流程 - 上傳 PDF 檔案並提供密碼與權限控管設定，全面採用 PDFBox 安全引擎
	 */
	@Test
	@DisplayName("POST加密PDF-正常情境-成功回傳加密檔案與PDFBox Security標頭")
	void encryptPdf_pdfboxEngine_success() throws Exception {
		byte[] mockRaw = "%PDF-1.5 Raw".getBytes();
		byte[] mockEncrypted = "%PDF-1.5 Encrypted".getBytes();

		MockMultipartFile file = new MockMultipartFile(
				"file", "doc.pdf", MediaType.APPLICATION_PDF_VALUE, mockRaw
		);

		when(encryptPdfUseCase.encryptPdf(any(EncryptPdfCommand.class))).thenReturn(mockEncrypted);

		mockMvc.perform(multipart("/api/v1/quotations/encrypt")
						.file(file)
						.param("userPassword", "user123")
						.param("ownerPassword", "owner123")
						.param("download", "true"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string("X-Pdf-Engine", "PDFBox (Security)"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("doc_encrypted.pdf")))
				.andExpect(content().bytes(mockEncrypted));
	}

	/**
	 * Feature: 上傳加密 PDF 進行解密 API
	 * Scenario: 正常流程 - 上傳加密 PDF 並提供密碼，預設使用 PDFBox 安全引擎解密
	 */
	@Test
	@DisplayName("POST解密PDF-正常情境-成功回傳解密檔案與PDFBox Security標頭")
	void decryptPdf_defaultEngine_success() throws Exception {
		byte[] mockEncrypted = "%PDF-1.5 Encrypted".getBytes();
		byte[] mockDecrypted = "%PDF-1.5 Decrypted".getBytes();

		MockMultipartFile file = new MockMultipartFile(
				"file", "protected.pdf", MediaType.APPLICATION_PDF_VALUE, mockEncrypted
		);

		when(decryptPdfUseCase.decryptPdf(any(DecryptPdfCommand.class))).thenReturn(mockDecrypted);

		mockMvc.perform(multipart("/api/v1/quotations/decrypt")
						.file(file)
						.param("password", "secret")
						.param("download", "true"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string("X-Pdf-Engine", "PDFBox (Security)"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("protected_decrypted.pdf")))
				.andExpect(content().bytes(mockDecrypted));
	}

	/**
	 * Feature: 上傳 PDF 進行解密 API 異常防禦
	 * Scenario: 密碼錯誤異常分支 - 解密密碼錯誤時，系統攔截 IllegalArgumentException 並回傳 400 Bad Request JSON
	 *
	 * Given: 上傳受保護之 PDF，但傳入錯誤密碼，UseCase 拋出 IllegalArgumentException("PDF 解密失敗：密碼錯誤或未授權存取")
	 * When: 發送 POST /api/v1/quotations/decrypt 請求
	 * Then: 回傳狀態碼 400 Bad Request，Content-Type 為 application/json，回應結構符合 ErrorOccurredResource
	 */
	@Test
	@DisplayName("POST解密PDF-異常分支-密碼錯誤回傳400BadRequest與ErrorOccurredResource")
	void decryptPdf_wrongPassword_returnsBadRequestJson() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "protected.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-1.5 Encrypted".getBytes()
		);

		when(decryptPdfUseCase.decryptPdf(any(DecryptPdfCommand.class)))
				.thenThrow(new IllegalArgumentException("PDF 解密失敗：密碼錯誤或未授權存取"));

		mockMvc.perform(multipart("/api/v1/quotations/decrypt")
						.file(file)
						.param("password", "wrongSecret")
						.param("download", "true"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value("PDF 解密失敗：密碼錯誤或未授權存取"));
	}

	/**
	 * Feature: 上傳 PDF 進行解密 API 異常防禦
	 * Scenario: 全域密碼錯誤例外攔截 - GlobalExceptionHandler 攔截 PDFBox InvalidPasswordException 並回傳 400 Bad Request JSON
	 *
	 * Given: 捕獲底層 InvalidPasswordException
	 * When: GlobalExceptionHandler.handleInvalidPassword 處理該例外
	 * Then: 回傳狀態碼 400 Bad Request，Content-Type 為 application/json，回應結構包含密碼錯誤提示
	 */
	@Test
	@DisplayName("GlobalExceptionHandler-PDFBox密碼錯誤攔截-回傳400BadRequest與ErrorOccurredResource")
	void handleInvalidPassword_returnsBadRequestJson() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		InvalidPasswordException mockEx = org.mockito.Mockito.mock(InvalidPasswordException.class);
		org.mockito.Mockito.when(mockEx.getMessage()).thenReturn("Cannot decrypt PDF, the password is incorrect");
		org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
		request.setRequestURI("/api/v1/quotations/decrypt");

		org.springframework.http.ResponseEntity<com.example.demo.presentation.resource.out.ErrorOccurredResource> response =
				handler.handleInvalidPassword(mockEx, request);

		org.junit.jupiter.api.Assertions.assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, response.getStatusCode());
		org.junit.jupiter.api.Assertions.assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
		org.junit.jupiter.api.Assertions.assertNotNull(response.getBody());
		org.junit.jupiter.api.Assertions.assertEquals(400, response.getBody().status());
		org.junit.jupiter.api.Assertions.assertEquals("PDF 解密失敗：密碼錯誤或未授權存取", response.getBody().message());
	}

	/**
	 * Feature: 上傳 PDF 進行解密 API 參數檢核
	 * Scenario: 空檔案異常分支 - 未選擇檔案或上傳空檔案時，回傳 400 Bad Request JSON
	 *
	 * Given: 一個空的 MultipartFile
	 * When: 發送 POST /api/v1/quotations/decrypt 請求
	 * Then: 回傳狀態碼 400 Bad Request，Content-Type 為 application/json，提示待解密檔案不可為空
	 */
	@Test
	@DisplayName("POST解密PDF-異常分支-空檔案上傳回傳400BadRequest")
	void decryptPdf_emptyFile_returnsBadRequestJson() throws Exception {
		MockMultipartFile emptyFile = new MockMultipartFile(
				"file", "empty.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]
		);

		mockMvc.perform(multipart("/api/v1/quotations/decrypt")
						.file(emptyFile)
						.param("password", "secret"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("待解密之 PDF 檔案不可為空"));
	}

	/**
	 * Feature: 上傳 PDF 進行 PKCS#7 數位簽章 API
	 * Scenario: 正常流程 - 上傳 PDF 與 PKCS#12 金鑰庫，未指定 template，自動套用系統預設簽章規範
	 */
	@Test
	@DisplayName("POST數位簽章-預設簽章規範情境-成功回傳簽名檔案與PDFBox Digital Signature標頭")
	void signPdf_defaultSignature_success() throws Exception {
		byte[] mockRaw = "%PDF-1.5 Raw".getBytes();
		byte[] mockSigned = "%PDF-1.5 Signed".getBytes();

		MockMultipartFile file = new MockMultipartFile(
				"file", "contract.pdf", MediaType.APPLICATION_PDF_VALUE, mockRaw
		);
		MockMultipartFile keystore = new MockMultipartFile(
				"keystore", "cert.p12", "application/x-pkcs12", "mockKeystoreBytes".getBytes()
		);

		when(signPdfUseCase.signPdf(any(SignPdfCommand.class))).thenReturn(mockSigned);

		mockMvc.perform(multipart("/api/v1/quotations/sign")
						.file(file)
						.file(keystore)
						.param("password", "storePass")
						.param("reason", "核准簽章")
						.param("location", "台北總部")
						.param("visual", "true")
						.param("page", "1")
						.param("download", "true"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string("X-Pdf-Engine", "PDFBox (Digital Signature)"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("contract_signed.pdf")))
				.andExpect(content().bytes(mockSigned));
	}

	/**
	 * Feature: 上傳 PDF 進行 PKCS#7 數位簽章 API
	 * Scenario: 指定單據範本 - 上傳 PDF 與 PKCS#12 金鑰庫，指定 template=bom，套用 BOM 專屬簽章規範
	 */
	@Test
	@DisplayName("POST數位簽章-指定單據範本簽章規範-成功回傳簽名檔案與PDFBox Digital Signature標頭")
	void signPdf_withBomTemplate_success() throws Exception {
		byte[] mockRaw = "%PDF-1.5 Raw".getBytes();
		byte[] mockSigned = "%PDF-1.5 Signed".getBytes();

		MockMultipartFile file = new MockMultipartFile(
				"file", "contract.pdf", MediaType.APPLICATION_PDF_VALUE, mockRaw
		);
		MockMultipartFile keystore = new MockMultipartFile(
				"keystore", "cert.p12", "application/x-pkcs12", "mockKeystoreBytes".getBytes()
		);

		when(signPdfUseCase.signPdf(any(SignPdfCommand.class))).thenReturn(mockSigned);

		mockMvc.perform(multipart("/api/v1/quotations/sign")
						.file(file)
						.file(keystore)
						.param("password", "storePass")
						.param("template", "bom")
						.param("visual", "true")
						.param("page", "1")
						.param("download", "true"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF))
				.andExpect(header().string("X-Pdf-Engine", "PDFBox (Digital Signature)"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("contract_signed.pdf")))
				.andExpect(content().bytes(mockSigned));
	}
}

