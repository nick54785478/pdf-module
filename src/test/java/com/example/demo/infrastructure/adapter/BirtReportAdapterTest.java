package com.example.demo.infrastructure.adapter;

import com.example.demo.application.port.out.ReportGeneratorPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * BIRT 報表適配器測試類別
 * <p>
 * 驗證 {@link BirtReportAdapter} 實作之 {@link ReportGeneratorPort} 業務合約與生命週期、例外防禦。
 */
class BirtReportAdapterTest {

	private BirtReportAdapter birtReportAdapter;
	private ReportGeneratorPort reportGeneratorPort;

	@BeforeEach
	void setUp() {
		birtReportAdapter = new BirtReportAdapter();
		birtReportAdapter.init();
		reportGeneratorPort = birtReportAdapter;
	}

	@AfterEach
	void tearDown() {
		birtReportAdapter.destroy();
	}

	/**
	 * Feature: BIRT 報表設計檔路徑組裝與讀取
	 * Scenario: 讀取不存在的報表設計檔
	 * Given: 指定不存在之路徑 "non/existent/path" 與檔名 "testReport"
	 * When: 呼叫 getResourceInputStream
	 * Then: 回傳 null 且不拋出未捕捉之例外
	 */
	@Test
	@DisplayName("讀取不存在的報表設計檔應回傳 null")
	void shouldReturnNullWhenReportDesignNotFound() {
		// Given
		String path = "non/existent/path";
		String fileName = "testReport";

		// When
		InputStream inputStream = reportGeneratorPort.getResourceInputStream(path, fileName);

		// Then
		assertNull(inputStream);
	}

	/**
	 * Feature: 報表生成保護
	 * Scenario: 當傳入不存在之檔案路徑時
	 * Given: 不存在的報表路徑
	 * When: 呼叫 generatePdfReport(filePath, fileName, params, dataContext)
	 * Then: 安全回傳 null，並記錄錯誤日誌
	 */
	@Test
	@DisplayName("路徑無效時生成 PDF 應安全回傳 null")
	void shouldReturnNullWhenFileDoesNotExist() {
		// Given
		Map<String, Object> params = new HashMap<>();
		Map<String, Object> context = Collections.emptyMap();

		// When
		byte[] result = reportGeneratorPort.generatePdfReport("invalid/path", "report", params, context);

		// Then
		assertNull(result);
	}

	/**
	 * Feature: BOM 報價單 BIRT 範本渲染生成 PDF
	 * Scenario: 提供完整報價單參數與 BOM 項目清單，成功產出 PDF 二進位檔
	 * Given: 包含報價單單號、客戶資訊、金額與項目清單之參數與上下文
	 * When: 呼叫 generatePdfReport("templates", "bom_quotation_template", params, context)
	 * Then: 成功生成大於 0 位元組之 PDF 資料且開頭為 %PDF
	 */
	@Test
	@DisplayName("驗證 BOM 報價單 BIRT 報表設計檔生成 PDF")
	void shouldRenderBomQuotationPdfWhenReportDesignExists() {
		// Given
		Map<String, Object> params = new HashMap<>();
		params.put("quotationNo", "QT-20261023-001");
		params.put("quotationDate", "2026-10-23");
		params.put("customerName", "台灣智能科技股份有限公司");
		params.put("customerTaxId", "12345678");
		params.put("contactPerson", "陳建銘 經理 / (03) 578-1234 #567");
		params.put("validityPeriod", "30 天 (至 2026-11-22)");
		params.put("paymentTerm", "月結 30 天 (Net 30)");
		params.put("deliveryPlace", "新竹市科學園區研發六路 12 號 (DDP)");
		params.put("subtotalAmount", "212,500");
		params.put("taxAmount", "10,625");
		params.put("totalAmount", "223,125");
		params.put("terms", "1. 本報價單所列物料均符合 RoHS 2.0 及 REACH 環保規範要求。\n2. 提供出貨後 12 個月原廠保固服務（人為操作不當或不可抗力因素除外）。\n3. 貴公司如確認訂購，請於下方「客戶確認簽回」欄位用印簽署後回傳。");

		java.util.List<Map<String, Object>> items = new java.util.ArrayList<>();
		Map<String, Object> item1 = new HashMap<>();
		item1.put("itemNo", 1);
		item1.put("materialNo", "MCU-STM32-H743");
		item1.put("description", "嵌入式高性能核心控制器 ARM Cortex-M7 480MHz 2MB Flash");
		item1.put("quantity", 100);
		item1.put("unit", "PCS");
		item1.put("unitPrice", "520");
		item1.put("subtotal", "52,000");
		item1.put("remark", "原廠出廠證明");
		items.add(item1);

		Map<String, Object> context = new HashMap<>();
		context.put("items", items);

		// When
		byte[] result = reportGeneratorPort.generatePdfReport("templates", "bom_quotation_template", params, context);

		// Then
		assertNotNull(result);
		org.junit.jupiter.api.Assertions.assertTrue(result.length > 0);
		String pdfHeader = new String(result, 0, Math.min(result.length, 5));
		org.junit.jupiter.api.Assertions.assertEquals("%PDF-", pdfHeader);

		try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.Loader.loadPDF(result)) {
			org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
			String text = stripper.getText(doc);
			org.junit.jupiter.api.Assertions.assertFalse(text.contains("<VALUE-OF>"), "PDF 不應包含未解析之 <VALUE-OF> 原始標籤");
			org.junit.jupiter.api.Assertions.assertTrue(text.contains("QT-20261023-001"), "PDF 應包含報價單單號");
			org.junit.jupiter.api.Assertions.assertTrue(text.contains("MCU-STM32-H743"), "PDF 應包含物料項目料號");
			org.junit.jupiter.api.Assertions.assertTrue(text.contains("223,125"), "PDF 應包含總計金額");
			org.junit.jupiter.api.Assertions.assertFalse(text.contains("Grand Total)\n"), "總計金額冒號不應換行至第二行");
			org.junit.jupiter.api.Assertions.assertFalse(text.contains("Grand Total)\r"), "總計金額冒號不應換行至第二行");
			org.junit.jupiter.api.Assertions.assertTrue(text.contains("1 / 1"), "頁尾頁碼應維持水平同一行 (1 / 1)");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
