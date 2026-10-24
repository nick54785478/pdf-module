package com.example.demo.presentation.controller;

import com.example.demo.application.port.in.GenerateQuotationPdfUseCase;
import com.example.demo.application.port.in.GenerateTemplateQuotationPdfUseCase;
import com.example.demo.application.port.in.ParseQuotationPdfUseCase;
import com.example.demo.application.shared.command.ParseQuotationPdfCommand;
import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.dto.QuotationItemGottenResult;
import com.example.demo.application.shared.query.GetQuotationQuery;
import com.example.demo.presentation.assembler.QuotationResourceAssembler;
import com.example.demo.presentation.exception.GlobalExceptionHandler;
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

	private final QuotationResourceAssembler quotationResourceAssembler = new QuotationResourceAssembler();

	@BeforeEach
	void setUp() {
		QuotationController controller = new QuotationController(
				generateQuotationPdfUseCase,
				generateTemplateQuotationPdfUseCase,
				parseQuotationPdfUseCase,
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
}

