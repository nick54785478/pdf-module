package com.example.demo.application.service;

import com.example.demo.application.assembler.QuotationReportAssembler;
import com.example.demo.application.port.out.MockServicePort;
import com.example.demo.application.port.out.PdfExtractorPort;
import com.example.demo.application.port.out.QuotationParserPort;
import com.example.demo.application.port.out.QuotationPdfGeneratorPort;
import com.example.demo.application.port.out.ReportGeneratorPort;
import com.example.demo.application.port.out.PdfSecurityExecutorPort;
import com.example.demo.application.port.out.PdfWatermarkExecutorPort;
import com.example.demo.application.shared.command.AddWatermarkCommand;
import com.example.demo.application.shared.command.DecryptPdfCommand;
import com.example.demo.application.shared.command.EncryptPdfCommand;
import com.example.demo.application.shared.command.ParseQuotationPdfCommand;
import com.example.demo.application.shared.command.SignPdfCommand;
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
import static org.mockito.Mockito.never;
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

	@Mock
	private PdfWatermarkExecutorPort pdfWatermarkPort;

	@Mock
	private PdfSecurityExecutorPort pdfSecurityPort;

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
		when(quotationPdfPort.generateQuotationPdf(mockQuotation, null)).thenReturn(expectedBytes);

		// When
		byte[] result = quotationApplicationService.generateQuotationPdf(query);

		// Then
		assertNotNull(result);
		assertArrayEquals(expectedBytes, result);
		verify(mockServicePort, times(1)).getQuotation(query);
		verify(quotationPdfPort, times(1)).generateQuotationPdf(mockQuotation, null);
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
		when(quotationPdfPort.generateQuotationPdf(defaultQuotation, null)).thenReturn(expectedBytes);

		// When
		byte[] result = quotationApplicationService.generateQuotationPdf(query);

		// Then
		assertNotNull(result);
		assertArrayEquals(expectedBytes, result);
		verify(mockServicePort, times(1)).getQuotation(query);
		verify(quotationPdfPort, times(1)).generateQuotationPdf(defaultQuotation, null);
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

	/**
	 * Feature: 加蓋 PDF 浮水印功能 (AddWatermarkUseCase)
	 * Scenario: 正常流程 - 接收 AddWatermarkCommand 並調用 PdfWatermarkPort 完成蓋印
	 *
	 * Given: 包含原始 PDF 位元組與浮水印文字 "機密草稿" 的 AddWatermarkCommand
	 * When: 調用 QuotationApplicationService.addWatermark(command)
	 * Then: 成功調用 PdfWatermarkPort.stampWatermark 並回傳蓋印後之位元組陣列
	 */
	@Test
	@DisplayName("加蓋PDF浮水印-正常情境-成功調用PdfWatermarkPort")
	void addWatermark_success() {
		// Given
		byte[] sourceBytes = "%PDF-1.4 raw document".getBytes();
		byte[] stampedBytes = "%PDF-1.4 watermarked document".getBytes();
		AddWatermarkCommand command = new AddWatermarkCommand(sourceBytes, "機密草稿");

		when(pdfWatermarkPort.stampWatermark(sourceBytes, "機密草稿")).thenReturn(stampedBytes);

		// When
		byte[] result = quotationApplicationService.addWatermark(command);

		// Then
		assertNotNull(result);
		assertArrayEquals(stampedBytes, result);
		verify(pdfWatermarkPort, times(1)).stampWatermark(sourceBytes, "機密草稿");
	}

	/**
	 * Feature: 加蓋 PDF 浮水印功能 (AddWatermarkUseCase)
	 * Scenario: 異常分支 - 傳入空命令或空檔案位元組拋出 IllegalArgumentException
	 *
	 * Given: command 為 null 或 fileBytes 為空
	 * When: 調用 QuotationApplicationService.addWatermark
	 * Then: 拋出 IllegalArgumentException
	 */
	@Test
	@DisplayName("加蓋PDF浮水印-異常情境-傳入空命令或空檔案拋出例外")
	void addWatermark_emptyCommand_throwsException() {
		assertThrows(IllegalArgumentException.class, () -> quotationApplicationService.addWatermark(null));
		assertThrows(IllegalArgumentException.class, () -> quotationApplicationService.addWatermark(new AddWatermarkCommand(null, "TEST")));
		assertThrows(IllegalArgumentException.class, () -> quotationApplicationService.addWatermark(new AddWatermarkCommand(new byte[0], "TEST")));
	}

	/**
	 * Feature: 產出 OpenPDF 報價單並動態加蓋浮水印
	 * Scenario: 正常流程 - Query 含有浮水印文字時，直接調用 QuotationPdfGeneratorPort 於文件生成時單次印製 (Single-pass)
	 *
	 * Given: 包含 ID 與 watermarkText="正式報價" 之 GetQuotationQuery
	 * When: 調用 QuotationApplicationService.generateQuotationPdf(query)
	 * Then: 驗證由 quotationPdfPort 於生成時單次完成浮水印繪製，無需額外調用 pdfWatermarkPort 進行二次讀寫
	 */
	@Test
	@DisplayName("OpenPDF報價單生成-包含浮水印-由QuotationPdfPort原生生成時一次印製")
	void generateQuotationPdf_withWatermark_success() {
		// Given
		GetQuotationQuery query = new GetQuotationQuery("QT-2026-WM", "正式報價");
		QuotationGottenResult quotation = createSampleQuotation("QT-2026-WM");
		byte[] watermarkedPdf = "%PDF-1.4 OpenPDF native watermarked".getBytes();

		when(mockServicePort.getQuotation(query)).thenReturn(quotation);
		when(quotationPdfPort.generateQuotationPdf(quotation, "正式報價")).thenReturn(watermarkedPdf);

		// When
		byte[] result = quotationApplicationService.generateQuotationPdf(query);

		// Then
		assertNotNull(result);
		assertArrayEquals(watermarkedPdf, result);
		verify(quotationPdfPort, times(1)).generateQuotationPdf(quotation, "正式報價");
		verify(pdfWatermarkPort, never()).stampWatermark(any(), any());
	}

	/**
	 * Feature: 產出 BIRT 範本報價單並動態加蓋浮水印
	 * Scenario: 正常流程 - Query 含有浮水印文字時，產出 BIRT 報表後調用 PdfWatermarkPort 蓋印
	 *
	 * Given: 包含 ID 與 watermarkText="機密草稿" 之 GetQuotationQuery
	 * When: 調用 QuotationApplicationService.generateTemplateQuotationPdf(query)
	 * Then: 驗證先由 reportGeneratorPort 產出，再由 pdfWatermarkPort 進行浮水印蓋印
	 */
	@Test
	@DisplayName("BIRT報表生成-包含浮水印-成功串接PdfWatermarkPort蓋印")
	void generateTemplateQuotationPdf_withWatermark_success() {
		// Given
		GetQuotationQuery query = new GetQuotationQuery("QT-2026-BIRT-WM", "機密草稿");
		QuotationGottenResult quotation = createSampleQuotation("QT-2026-BIRT-WM");
		byte[] rawPdf = "%PDF-1.5 BIRT raw".getBytes();
		byte[] watermarkedPdf = "%PDF-1.5 BIRT watermarked".getBytes();

		when(mockServicePort.getQuotation(query)).thenReturn(quotation);
		when(quotationReportAssembler.toParameters(quotation)).thenReturn(new HashMap<>());
		when(quotationReportAssembler.toDataContext(quotation)).thenReturn(new HashMap<>());
		when(reportGeneratorPort.generatePdfReport(any(), any(), any(), any())).thenReturn(rawPdf);
		when(pdfWatermarkPort.stampWatermark(rawPdf, "機密草稿")).thenReturn(watermarkedPdf);

		// When
		byte[] result = quotationApplicationService.generateTemplateQuotationPdf(query);

		// Then
		assertNotNull(result);
		assertArrayEquals(watermarkedPdf, result);
		verify(pdfWatermarkPort, times(1)).stampWatermark(rawPdf, "機密草稿");
	}

	/**
	 * Feature: PDF 安全加密 UseCase
	 * Scenario: 正常流程 - 傳入有效之 EncryptPdfCommand，協調 PdfSecurityExecutorPort 執行加密
	 *
	 * Given: 包含原始 PDF 與密碼之合法 EncryptPdfCommand
	 * When: 調用 quotationApplicationService.encryptPdf(command)
	 * Then: 成功回傳加密後之 byte[]，且 PdfSecurityExecutorPort 被精準調用一次
	 */
	@Test
	@DisplayName("PDF加密UseCase-正常流程-調用port並回傳加密內容")
	void encryptPdf_validCommand_success() {
		// Given
		byte[] raw = "%PDF-1.5 test raw".getBytes();
		byte[] encrypted = "%PDF-1.5 test encrypted".getBytes();
		EncryptPdfCommand command = new EncryptPdfCommand(raw, "pwd123", "owner123", true, false);
		when(pdfSecurityPort.encryptPdf(command)).thenReturn(encrypted);

		// When
		byte[] result = quotationApplicationService.encryptPdf(command);

		// Then
		assertNotNull(result);
		assertArrayEquals(encrypted, result);
		verify(pdfSecurityPort, times(1)).encryptPdf(command);
	}

	/**
	 * Feature: PDF 安全加密 UseCase 防禦分支
	 * Scenario: 傳入 null command 應拋出 IllegalArgumentException
	 */
	@Test
	@DisplayName("PDF加密UseCase-防禦分支-傳入null應拋出異常")
	void encryptPdf_nullCommand_throwsException() {
		assertThrows(IllegalArgumentException.class, () ->
				quotationApplicationService.encryptPdf(null)
		);
		verify(pdfSecurityPort, never()).encryptPdf(any());
	}

	/**
	 * Feature: PDF 安全解密 UseCase
	 * Scenario: 正常流程 - 傳入有效之 DecryptPdfCommand，協調 PdfSecurityExecutorPort 執行解密
	 *
	 * Given: 包含加密 PDF 與正確密碼之 DecryptPdfCommand
	 * When: 調用 quotationApplicationService.decryptPdf(command)
	 * Then: 成功回傳解密後之標準 byte[]，且 PdfSecurityExecutorPort 被精準調用一次
	 */
	@Test
	@DisplayName("PDF解密UseCase-正常流程-調用port並回傳解密內容")
	void decryptPdf_validCommand_success() {
		// Given
		byte[] encrypted = "%PDF-1.5 encrypted".getBytes();
		byte[] decrypted = "%PDF-1.5 decrypted".getBytes();
		DecryptPdfCommand command = new DecryptPdfCommand(encrypted, "pwd123");
		when(pdfSecurityPort.decryptPdf(command)).thenReturn(decrypted);

		// When
		byte[] result = quotationApplicationService.decryptPdf(command);

		// Then
		assertNotNull(result);
		assertArrayEquals(decrypted, result);
		verify(pdfSecurityPort, times(1)).decryptPdf(command);
	}

	/**
	 * Feature: PDF 安全解密 UseCase 防禦分支
	 * Scenario: 傳入 null command 應拋出 IllegalArgumentException
	 */
	@Test
	@DisplayName("PDF解密UseCase-防禦分支-傳入null應拋出異常")
	void decryptPdf_nullCommand_throwsException() {
		assertThrows(IllegalArgumentException.class, () ->
				quotationApplicationService.decryptPdf(null)
		);
		verify(pdfSecurityPort, never()).decryptPdf(any());
	}

	/**
	 * Feature: PDF 數位簽章 UseCase
	 * Scenario: 正常流程 - 傳入有效之 SignPdfCommand，協調 PdfSecurityExecutorPort 執行 PKCS#7 數位簽署
	 *
	 * Given: 包含原始 PDF、PKCS#12 金鑰庫與簽章詮釋資料之 SignPdfCommand
	 * When: 調用 quotationApplicationService.signPdf(command)
	 * Then: 成功回傳簽署後之 byte[]，且 PdfSecurityExecutorPort 被精準調用一次
	 */
	@Test
	@DisplayName("PDF數位簽章UseCase-正常流程-調用port並回傳簽章內容")
	void signPdf_validCommand_success() {
		// Given
		byte[] raw = "%PDF-1.5 raw".getBytes();
		byte[] signed = "%PDF-1.5 signed".getBytes();
		SignPdfCommand command = new SignPdfCommand(raw, "fakeP12".getBytes(), "secret", "Approval", "Taipei");
		when(pdfSecurityPort.signPdf(command)).thenReturn(signed);

		// When
		byte[] result = quotationApplicationService.signPdf(command);

		// Then
		assertNotNull(result);
		assertArrayEquals(signed, result);
		verify(pdfSecurityPort, times(1)).signPdf(command);
	}

	/**
	 * Feature: PDF 數位簽章 UseCase 防禦分支
	 * Scenario: 傳入 null command 應拋出 IllegalArgumentException
	 */
	@Test
	@DisplayName("PDF數位簽章UseCase-防禦分支-傳入null應拋出異常")
	void signPdf_nullCommand_throwsException() {
		assertThrows(IllegalArgumentException.class, () ->
				quotationApplicationService.signPdf(null)
		);
		verify(pdfSecurityPort, never()).signPdf(any());
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
