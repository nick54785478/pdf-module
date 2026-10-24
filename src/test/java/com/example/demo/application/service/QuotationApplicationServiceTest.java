package com.example.demo.application.service;

import com.example.demo.application.assembler.QuotationReportAssembler;
import com.example.demo.application.port.out.MockServicePort;
import com.example.demo.application.port.out.PdfExtractorPort;
import com.example.demo.application.port.out.QuotationParserPort;
import com.example.demo.application.port.out.QuotationPdfGeneratorPort;
import com.example.demo.application.port.out.ReportGeneratorPort;
import com.example.demo.application.shared.command.ParseQuotationPdfCommand;
import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.dto.QuotationItemGottenResult;
import com.example.demo.application.shared.query.GetQuotationQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 報價單應用層服務單元測試 (UseCase Test)
 * <p>
 * 遵循 AGENTS.md 規範：
 * 1. 覆蓋 UseCase 正常業務流程與例外分支。
 * 2. 測試方法皆以 BDD Gherkin 語法撰寫 Javadoc。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class QuotationApplicationServiceTest {

	@Mock
	private MockServicePort mockServicePort;

	@Mock
	private QuotationPdfGeneratorPort quotationPdfPort;

	@Mock
	private ReportGeneratorPort reportGeneratorPort;

	@Mock
	private QuotationReportAssembler quotationReportAssembler;

	@Mock
	private PdfExtractorPort pdfExtractorPort;

	@Mock
	private QuotationParserPort quotationParserPort;

	@InjectMocks
	private QuotationApplicationService quotationApplicationService;

	/**
	 * Feature: 依據 Query 透過範本產出報價單 PDF
	 * Scenario: 正常流程 - 給定包含 ID 之查詢物件，協調 MockServicePort、QuotationReportAssembler 與 ReportGeneratorPort 產出二進位 PDF
	 *
	 * Given: 包含 ID "QT-2026-001" 之 GetQuotationQuery 物件，Mock 服務回傳完整報價單資料，Assembler 轉換參數與資料集
	 * When: 調用 QuotationApplicationService.generateTemplateQuotationPdf(query)
	 * Then: 成功獲得預期的二進位位元組陣列，且驗證 ReportGeneratorPort 接收到 bom_quotation_template 範本名稱
	 */
	@Test
	@DisplayName("Query透過範本產出報價單PDF-正常情境-成功組裝參數並調用ReportGeneratorPort")
	void generateTemplateQuotationPdf_withQuery_success() {
		// Given
		GetQuotationQuery query = new GetQuotationQuery("QT-2026-001");
		QuotationGottenResult mockQuotation = createSampleQuotation("QT-2026-001");
		Map<String, Object> mockParams = new HashMap<>();
		mockParams.put("quotationNo", "QT-2026-001");
		Map<String, Object> mockContext = new HashMap<>();
		mockContext.put("items", Collections.emptyList());
		byte[] expectedBytes = "%PDF-1.5 BIRT Template Output".getBytes();

		when(mockServicePort.getQuotation(query)).thenReturn(mockQuotation);
		when(quotationReportAssembler.toParameters(mockQuotation)).thenReturn(mockParams);
		when(quotationReportAssembler.toDataContext(mockQuotation)).thenReturn(mockContext);
		when(reportGeneratorPort.generatePdfReport("templates", "bom_quotation_template", mockParams, mockContext))
				.thenReturn(expectedBytes);

		// When
		byte[] result = quotationApplicationService.generateTemplateQuotationPdf(query);

		// Then
		assertNotNull(result);
		assertArrayEquals(expectedBytes, result);
		verify(mockServicePort, times(1)).getQuotation(query);
		verify(reportGeneratorPort, times(1)).generatePdfReport("templates", "bom_quotation_template", mockParams, mockContext);
	}

	/**
	 * Feature: 依據 Query 透過範本產出報價單 PDF
	 * Scenario: 異常分支 - MockServicePort 查無資料回傳 null 時，安全返回 null
	 *
	 * Given: 查無資料之 GetQuotationQuery，MockServicePort 回傳 null
	 * When: 調用 QuotationApplicationService.generateTemplateQuotationPdf(query)
	 * Then: 安全回傳 null，且不調用 ReportGeneratorPort
	 */
	@Test
	@DisplayName("Query透過範本產出報價單PDF-異常情境-查無資料安全返回null")
	void generateTemplateQuotationPdf_whenQuotationNotFound_returnsNull() {
		// Given
		GetQuotationQuery query = new GetQuotationQuery("QT-NOT-FOUND");
		when(mockServicePort.getQuotation(query)).thenReturn(null);

		// When
		byte[] result = quotationApplicationService.generateTemplateQuotationPdf(query);

		// Then
		assertNull(result);
		verify(mockServicePort, times(1)).getQuotation(query);
	}

	/**
	 * Feature: 依據 Query 產出報價單 PDF
	 * Scenario: 正常流程 - 給定包含 ID 之查詢物件，協調 MockServicePort 與 QuotationPdfPort 產出二進位 PDF
	 *
	 * Given: 包含 ID "QT-2026-001" 之 GetQuotationQuery 物件，MockServicePort 回傳範本資料，QuotationPdfPort 回傳二進位 byte 陣列
	 * When: 調用 QuotationApplicationService.generateQuotationPdf(query)
	 * Then: 成功獲得預期的二進位位元組陣列，且驗證 MockServicePort 與 QuotationPdfPort 各恰好被調用一次
	 */
	@Test
	@DisplayName("Query產出報價單PDF-正常情境-成功取得Mock範本並產出PDF")
	void generateQuotationPdf_withQuery_success() {
		// Given
		GetQuotationQuery query = new GetQuotationQuery("QT-2026-001");
		QuotationGottenResult mockQuotation = createSampleQuotation("QT-2026-001");
		byte[] expectedBytes = "%PDF-1.4 Mock Query Content".getBytes();

		when(mockServicePort.getQuotation(query)).thenReturn(mockQuotation);
		when(quotationPdfPort.generateQuotationPdf(mockQuotation)).thenReturn(expectedBytes);

		// When
		byte[] result = quotationApplicationService.generateQuotationPdf(query);

		// Then
		assertNotNull(result);
		assertArrayEquals(expectedBytes, result);
		verify(mockServicePort, times(1)).getQuotation(query);
		verify(quotationPdfPort, times(1)).generateQuotationPdf(mockQuotation);
	}

	/**
	 * Feature: 依據未指定 ID 之 Query 產出報價單 PDF
	 * Scenario: 邊界情境 - 給定 null 或空 ID 查詢物件，仍由 MockServicePort 提供預設範本並產出 PDF
	 *
	 * Given: ID 為 null 之 GetQuotationQuery 物件，MockServicePort 回傳預設範本資料
	 * When: 調用 QuotationApplicationService.generateQuotationPdf(query)
	 * Then: 成功獲得預期之二進位位元組陣列
	 */
	@Test
	@DisplayName("Query產出報價單PDF-邊界情境-未傳入ID仍可產出預設範本PDF")
	void generateQuotationPdf_withNullId_success() {
		// Given
		GetQuotationQuery query = new GetQuotationQuery(null);
		QuotationGottenResult defaultQuotation = createSampleQuotation("QT-20261023-001");
		byte[] expectedBytes = "%PDF-1.4 Default Content".getBytes();

		when(mockServicePort.getQuotation(query)).thenReturn(defaultQuotation);
		when(quotationPdfPort.generateQuotationPdf(defaultQuotation)).thenReturn(expectedBytes);

		// When
		byte[] result = quotationApplicationService.generateQuotationPdf(query);

		// Then
		assertNotNull(result);
		assertArrayEquals(expectedBytes, result);
		verify(mockServicePort, times(1)).getQuotation(query);
		verify(quotationPdfPort, times(1)).generateQuotationPdf(defaultQuotation);
	}

	/**
	 * Feature: 上傳並解析報價單 PDF
	 * Scenario: 正常流程 - 輸入包含有效二進位 PDF byte 陣列之 Command，透過 PdfExtractorPort 萃取文字並轉換為結構化 DTO
	 *
	 * Given: 建立有效之 ParseQuotationPdfCommand，PdfExtractorPort 回傳純文字，QuotationPdfParser 回傳解析結果
	 * When: 調用 QuotationApplicationService.parseQuotationPdf(command)
	 * Then: 成功獲得預期之 QuotationGottenResult 業務物件，且各組件被正確調用
	 */
	@Test
	@DisplayName("解析報價單PDF-正常情境-成功透過PdfExtractorPort解析為結構化DTO")
	void parseQuotationPdf_success() {
		// Given
		byte[] mockPdfBytes = "%PDF-1.4 Mock Quotation Content".getBytes();
		ParseQuotationPdfCommand command = new ParseQuotationPdfCommand(mockPdfBytes, "sample.pdf");
		String mockExtractedText = "單號：QT-2026-PARSE\n客戶名稱： 台灣智能科技";
		QuotationGottenResult expectedResult = createSampleQuotation("QT-2026-PARSE");

		when(pdfExtractorPort.readPdfContent(any(InputStream.class))).thenReturn(mockExtractedText);
		when(quotationParserPort.parse(mockExtractedText)).thenReturn(expectedResult);

		// When
		QuotationGottenResult result = quotationApplicationService.parseQuotationPdf(command);

		// Then
		assertNotNull(result);
		assertEquals("QT-2026-PARSE", result.quotationNo());
		verify(pdfExtractorPort, times(1)).readPdfContent(any(InputStream.class));
		verify(quotationParserPort, times(1)).parse(mockExtractedText);
	}

	/**
	 * Feature: 上傳並解析報價單 PDF 異常處理
	 * Scenario: 輸入空 byte 陣列之 Command，應用層防禦性拋出 IllegalArgumentException
	 *
	 * Given: 檔案二進位位元組為空陣列之 ParseQuotationPdfCommand
	 * When: 調用 QuotationApplicationService.parseQuotationPdf(command)
	 * Then: 預期拋出 IllegalArgumentException 例外
	 */
	@Test
	@DisplayName("解析報價單PDF-異常情境-上傳空檔案拋出IllegalArgumentException")
	void parseQuotationPdf_withEmptyFile_throwsException() {
		// Given
		ParseQuotationPdfCommand command = new ParseQuotationPdfCommand(new byte[0], "empty.pdf");

		// When & Then
		assertThrows(IllegalArgumentException.class, () -> quotationApplicationService.parseQuotationPdf(command));
	}

	/**
	 * Feature: 上傳並解析報價單 PDF 異常處理
	 * Scenario: 底層 PdfExtractorPort 發生非預期例外時，包裝拋出 IllegalStateException
	 *
	 * Given: 有效之 Command，但 PdfExtractorPort 拋出 RuntimeException
	 * When: 調用 QuotationApplicationService.parseQuotationPdf(command)
	 * Then: 預期拋出 IllegalStateException 例外
	 */
	@Test
	@DisplayName("解析報價單PDF-異常情境-底層抽取異常包裝為IllegalStateException")
	void parseQuotationPdf_extractorThrows_throwsIllegalStateException() {
		// Given
		ParseQuotationPdfCommand command = new ParseQuotationPdfCommand("%PDF".getBytes(), "corrupt.pdf");
		when(pdfExtractorPort.readPdfContent(any(InputStream.class))).thenThrow(new RuntimeException("PDFBox 底層讀取錯誤"));

		// When & Then
		assertThrows(IllegalStateException.class, () -> quotationApplicationService.parseQuotationPdf(command));
	}

	private QuotationGottenResult createSampleQuotation(String quotationNo) {
		QuotationItemGottenResult item = new QuotationItemGottenResult(
				1, "MCU-STM32-H743", "核心控制器 ARM Cortex-M7", 100, "PCS",
				new BigDecimal("520"), new BigDecimal("52000"), "原廠出廠證明"
		);

		return new QuotationGottenResult(
				quotationNo,
				LocalDate.of(2026, 10, 23),
				"台灣智能科技股份有限公司",
				"12345678",
				"陳建銘 經理",
				"30 天",
				"月結 30 天",
				"新竹市科學園區研發六路 12 號",
				new BigDecimal("52000"),
				new BigDecimal("2600"),
				new BigDecimal("54600"),
				"符合 RoHS 2.0 規範",
				List.of(item)
		);
	}
}
