package com.example.demo.infrastructure.adapter;

import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.dto.QuotationItemGottenResult;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OpenPDF 浮水印蓋印適配器測試 (Adapter Integration Test)
 * <p>
 * 遵循 AGENTS.md 規範：
 * 1. 完整驗證 Outbound Adapter 整合 OpenPDF PdfStamper 加蓋浮水印之正確性。
 * 2. 透過 PDFBox 讀取蓋印後之 PDF 驗證合法性、頁數與浮水印文字。
 * 3. 測試方法皆以 BDD Gherkin 語法撰寫 Javadoc。
 * </p>
 */
@SpringBootTest
class OpenPdfWatermarkAdapterTest {

	@Autowired
	private OpenPdfWatermarkAdapter openPdfWatermarkAdapter;

	@Autowired
	private OpenQuotationPdfGeneratorAdapter openPdfQuotationAdapter;

	/**
	 * Feature: PDF 浮水印蓋印功能
	 * Scenario: 正常流程 - 輸入合法 PDF 與浮水印文字，成功於每一頁疊加半透明防偽浮水印
	 *
	 * Given: 一份合法的單頁或多頁報價單 PDF 二進位位元組陣列，與浮水印文字 "機密草稿"
	 * When: 調用 OpenPdfWatermarkAdapter.stampWatermark(pdfBytes, "機密草稿")
	 * Then: 成功獲得蓋印後之 PDF byte[]，透過 PDFBox 載入確認頁數保持不變，且能擷取出浮水印文字
	 */
	@Test
	@DisplayName("加蓋浮水印-正常情境-成功為PDF疊加浮水印且頁數保持不變")
	void stampWatermark_validPdf_success() throws IOException {
		// Given
		QuotationGottenResult quotation = createSampleQuotation("QT-WM-001");
		byte[] sourcePdf = openPdfQuotationAdapter.generateQuotationPdf(quotation);
		assertNotNull(sourcePdf);

		int originalPageCount;
		try (PDDocument doc = Loader.loadPDF(sourcePdf)) {
			originalPageCount = doc.getNumberOfPages();
		}

		// When
		byte[] watermarkedPdf = openPdfWatermarkAdapter.stampWatermark(sourcePdf, "機密草稿");

		// Then
		assertNotNull(watermarkedPdf);
		assertTrue(watermarkedPdf.length > 0);

		try (PDDocument doc = Loader.loadPDF(watermarkedPdf)) {
			assertEquals(originalPageCount, doc.getNumberOfPages(), "加蓋浮水印後頁數應保持一致");
			PDFTextStripper stripper = new PDFTextStripper();
			String fullText = stripper.getText(doc);
			// 由於 45 度傾斜浮水印各字元之 Y 軸基準線不同，PDFTextStripper 抽取時會於字元間加入換行/空白
			String normalizedText = fullText.replaceAll("\\s+", "");
			assertTrue(normalizedText.contains("機密草稿"), "加蓋浮水印之 PDF 應能透過文字抽取器檢測出浮水印字樣");
		}
	}

	/**
	 * Feature: PDF 浮水印蓋印功能
	 * Scenario: 邊界情境 - 未傳入浮水印文字或文字為空字串時，直接安全回傳原始 PDF
	 *
	 * Given: 合法的 PDF 位元組陣列，浮水印文字為 null 或純空白字元
	 * When: 調用 OpenPdfWatermarkAdapter.stampWatermark
	 * Then: 直接回傳原始位元組陣列，不進行無謂的蓋印運算
	 */
	@Test
	@DisplayName("加蓋浮水印-邊界情境-浮水印文字為空時安全回傳原始PDF")
	void stampWatermark_emptyWatermarkText_returnsOriginalPdf() {
		// Given
		byte[] sourcePdf = "%PDF-1.4 dummy content".getBytes();

		// When
		byte[] result1 = openPdfWatermarkAdapter.stampWatermark(sourcePdf, null);
		byte[] result2 = openPdfWatermarkAdapter.stampWatermark(sourcePdf, "   ");

		// Then
		assertArrayEquals(sourcePdf, result1);
		assertArrayEquals(sourcePdf, result2);
	}

	/**
	 * Feature: PDF 浮水印蓋印功能
	 * Scenario: 異常分支 - 傳入為 null 或長度為 0 之原始 PDF 位元組拋出 IllegalArgumentException
	 *
	 * Given: sourcePdf 為 null 或空陣列
	 * When: 調用 OpenPdfWatermarkAdapter.stampWatermark
	 * Then: 拋出 IllegalArgumentException
	 */
	@Test
	@DisplayName("加蓋浮水印-異常情境-傳入空PDF檔案拋出IllegalArgumentException")
	void stampWatermark_nullOrEmptyPdf_throwsException() {
		assertThrows(IllegalArgumentException.class, () -> openPdfWatermarkAdapter.stampWatermark(null, "機密"));
		assertThrows(IllegalArgumentException.class, () -> openPdfWatermarkAdapter.stampWatermark(new byte[0], "機密"));
	}

	private QuotationGottenResult createSampleQuotation(String quotationNo) {
		QuotationItemGottenResult item = new QuotationItemGottenResult(
				1, "MCU-STM32", "核心晶片", 10, "PCS",
				new BigDecimal("100"), new BigDecimal("1000"), "原廠出廠證明"
		);

		return new QuotationGottenResult(
				quotationNo,
				LocalDate.of(2026, 10, 25),
				"智邦科技股份有限公司",
				"88888888",
				"王專員",
				"30 天",
				"電匯",
				"台北市信義區信義路五段 7 號",
				new BigDecimal("1000"),
				new BigDecimal("50"),
				new BigDecimal("1050"),
				"交貨日起保固一年",
				List.of(item)
		);
	}
}
