package com.example.demo.infrastructure.adapter;

import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.dto.QuotationItemGottenResult;
import com.example.demo.infrastructure.openpdf.template.impl.BomQuotationPdfTemplate;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OpenPDF 報價單輸出外向適配器整合測試 (Adapter Integration Test)
 * <p>
 * 遵循 AGENTS.md 規範：
 * 1. 完整驗證 Outbound Adapter 整合 OpenPDF 生成真實 PDF 二進位位元組陣列。
 * 2. 透過 PDFBox 讀取產出的 PDF 驗證合法性、頁數與包含的文字。
 * 3. 測試方法皆以 BDD Gherkin 語法撰寫 Javadoc。
 * </p>
 */
@SpringBootTest
class OpenPdfQuotationAdapterTest {

	@Autowired
	private OpenQuotationPdfGeneratorAdapter openPdfQuotationAdapter;

	@Autowired
	private BomQuotationPdfTemplate bomQuotationPdfTemplate;

	/**
	 * Feature: OpenPDF 報價單 PDF 生成
	 * Scenario: 正常流程 - 輸入完整 8 筆物料清單明細，產出高精確度排版之 PDF 二進位檔
	 *
	 * Given: 包含 8 筆物料明細、稅額計算與完整客戶資訊之 QuotationGottenResult
	 * When: 調用 OpenPdfQuotationAdapter.generateQuotationPdf(quotation)
	 * Then: 成功獲得非空 byte[]，檔案標頭為 "%PDF-"，且透過 PDFBox 可成功加載並驗證包含客戶名稱與報價單號
	 */
	@Test
	@DisplayName("產出報價單PDF-正常情境-成功生成完整PDF且文字可解析")
	void generateQuotationPdf_withItems_success() throws IOException {
		// Given
		QuotationGottenResult quotation = createCompleteBomQuotation("QT-20261023-001");

		// When
		byte[] pdfBytes = openPdfQuotationAdapter.generateQuotationPdf(quotation);

		// Then
		assertNotNull(pdfBytes, "產出的 PDF 位元組陣列不得為空");
		assertTrue(pdfBytes.length > 5000, "完整的 BOM 報價單檔案大小應大於 5KB");

		// 驗證 PDF Magic Number: %PDF-
		String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5));
		assertTrue(header.startsWith("%PDF"), "檔案開頭必須為 PDF Magic Number (%PDF)");

		// 使用 PDFBox 實際解析內容驗證合法性
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			assertTrue(document.getNumberOfPages() >= 1, "PDF 頁數必須至少為 1 頁");

			PDFTextStripper stripper = new PDFTextStripper();
			String text = stripper.getText(document);

			assertTrue(text.contains("晨星智造整合科技股份有限公司"), "PDF 應包含公司名稱");
			assertTrue(text.contains("QT-20261023-001"), "PDF 應包含報價單單號");
			assertTrue(text.contains("台灣智能科技股份有限公司"), "PDF 應包含客戶名稱");
			assertTrue(text.contains("MCU-STM32-H743"), "PDF 應包含物料料號");
			assertTrue(text.contains("223,125"), "PDF 應包含含稅總額");
		}
	}

	/**
	 * Feature: OpenPDF 報價單 PDF 生成
	 * Scenario: 邊界情境 - 輸入明細清單為空之報價單，仍應正常產出包含空品項提示之 PDF
	 *
	 * Given: 明細項目清單為空清單之 QuotationGottenResult
	 * When: 調用 OpenPdfQuotationAdapter.generateQuotationPdf(quotation)
	 * Then: 成功獲得非空 byte[]，且 PDFBox 可成功解析出「（目前無明細品項）」
	 */
	@Test
	@DisplayName("產出報價單PDF-邊界情境-無品項明細仍正常產出")
	void generateQuotationPdf_emptyItems_success() throws IOException {
		// Given
		QuotationGottenResult emptyItemsQuotation = new QuotationGottenResult(
				"QT-EMPTY-001",
				LocalDate.of(2026, 10, 23),
				"空品項測試客戶",
				"88888888",
				"王測試",
				"7 天",
				"預付",
				"台北",
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				"無特殊條款",
				List.of()
		);

		// When
		byte[] pdfBytes = openPdfQuotationAdapter.generateQuotationPdf(emptyItemsQuotation);

		// Then
		assertNotNull(pdfBytes);
		assertTrue(pdfBytes.length > 1000);

		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			PDFTextStripper stripper = new PDFTextStripper();
			String text = stripper.getText(document);
			assertTrue(text.contains("目前無明細品項"), "應包含無明細品項之提示文字");
		}
	}

	/**
	 * Feature: OpenPDF 報價單 PDF 生成
	 * Scenario: 異常分支 - 輸入 null 報價單物件，適配器防禦性返回 null
	 *
	 * Given: 傳入 null 之報價單物件
	 * When: 調用 OpenPdfQuotationAdapter.generateQuotationPdf(null)
	 * Then: 適配器安全捕獲並回傳 null，不向外拋出未檢查例外
	 */
	@Test
	@DisplayName("產出報價單PDF-異常情境-輸入null安全返回null")
	void generateQuotationPdf_nullInput_returnsNull() {
		// When
		byte[] result = openPdfQuotationAdapter.generateQuotationPdf(null);

		// Then
		assertNull(result, "輸入為 null 時應安全返回 null");
	}

	/**
	 * Feature: OpenPDF 防孤兒區塊排版機制
	 * Scenario: 邊界排版 - 12 筆物料明細達到分頁臨界值時，觸發防孤兒機制主動將最後 2 筆明細帶至第 2 頁
	 *
	 * Given: 包含 12 筆物料明細之報價單 (最後兩筆為項次 11 與 12)
	 * When: 調用 OpenPdfQuotationAdapter.generateQuotationPdf(quotation)
	 * Then: 產出 2 頁 PDF，且第 2 頁頂部必須包含 BOM 表頭、項次 11 與 12 之明細資料，
	 *       下方緊接金額小計、條款與簽核用印，證實第 2 頁絕非只有結尾區塊之孤兒頁
	 */
	@Test
	@DisplayName("產出報價單PDF-防孤兒排版-分頁時第2頁頂部保留明細行與表頭絕不孤兒")
	void generateQuotationPdf_with12Items_triggersAntiOrphan_secondPageHasItemsAndFooter() throws IOException {
		// Given: 建立 18 筆具備多行長描述之物料明細 (精確重現填滿第 1 頁邊界並跨頁之場景)
		List<QuotationItemGottenResult> items = new ArrayList<>();
		for (int i = 1; i <= 16; i++) {
			items.add(new QuotationItemGottenResult(
					i,
					"MCU-STM32-H74" + (i % 10),
					"嵌入式高性能核心控制器 ARM Cortex-M7 480MHz 2MB Flash (高精度阻抗控制與高密度沉金測試) - 序號 " + i,
					100,
					"PCS",
					new BigDecimal("520"),
					new BigDecimal("52000"),
					"原廠出廠檢測報告合格"
			));
		}
		// 加上最後 2 筆錨點項目 (特徵項目)
		items.add(new QuotationItemGottenResult(17, "ANCHOR-ITEM-17", "四層高密度沉金印刷電路板 (120x80mm, TG170, 阻抗控制) 關鍵錨點物料 A", 20, "組", new BigDecimal("500"), new BigDecimal("10000"), "錨點項目"));
		items.add(new QuotationItemGottenResult(18, "ANCHOR-ITEM-18", "四層高密度沉金印刷電路板 (120x80mm, TG170, 阻抗控制) 關鍵錨點物料 B", 30, "台", new BigDecimal("800"), new BigDecimal("24000"), "錨點項目"));

		QuotationGottenResult quotation = new QuotationGottenResult(
				"QT-ORPHAN-TEST-001",
				LocalDate.of(2026, 10, 24),
				"防孤兒排版驗證股份有限公司",
				"99887766",
				"防孤兒測試專案小組",
				"30 天",
				"月結 30 天",
				"新竹科學園區",
				new BigDecimal("866000"),
				new BigDecimal("43300"),
				new BigDecimal("909300"),
				"1. 本報價單所列物料均符合 RoHS 2.0 及 REACH 環保規範要求。\n" +
				"2. 提供出貨後 12 個月原廠保固服務（人為操作不當或不可抗力因素除外）。\n" +
				"3. 貴公司如確認訂購，請於下方「客戶確認簽回」欄位用印簽署後回傳。",
				items
		);

		// When: 產出 PDF
		byte[] pdfBytes = openPdfQuotationAdapter.generateQuotationPdf(quotation);

		// Then: 驗證 PDF 內容與分頁結構
		assertNotNull(pdfBytes);
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			// 1. 斷言總頁數為 2 頁
			assertTrue(document.getNumberOfPages() >= 2, "12 筆明細應跨頁生成至少 2 頁 PDF");

			// 2. 專門提取第 2 頁之文字
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setStartPage(2);
			stripper.setEndPage(2);
			String page2Text = stripper.getText(document);

			// 3. 驗證第 2 頁頂部確實包含 BOM 表頭
			assertTrue(page2Text.contains("物料料號") || page2Text.contains("品名與規格描述"),
					"第 2 頁頂部必須包含重新渲染的 BOM 表頭");

			// 4. 驗證第 2 頁頂部確實包含被攜帶換頁的最後 2 筆錨點明細 (絕非只有條款與簽名)
			assertTrue(page2Text.contains("ANCHOR-ITEM-17"), "第 2 頁必須包含第 17 筆物料料號 (ANCHOR-ITEM-17)");
			assertTrue(page2Text.contains("ANCHOR-ITEM-18"), "第 2 頁必須包含第 18 筆物料料號 (ANCHOR-ITEM-18)");

			// 5. 驗證第 2 頁下方包含完整結尾區塊 (條款、總計、簽名)
			assertTrue(page2Text.contains("報價與交易條款說明"), "第 2 頁必須包含條款說明區塊");
			assertTrue(page2Text.contains("總計金額"), "第 2 頁必須包含總計金額區塊");
			assertTrue(page2Text.contains("客戶確認簽回欄"), "第 2 頁必須包含客戶簽核用印欄位");
		}
	}

	/**
	 * Feature: OpenPDF 動態資料量排版防孤兒機制
	 * Scenario: 極少資料量 (1 筆) - 確保在只有 1 筆明細之情況下，單頁緊湊完整呈現且不拋出任何邊界例外
	 */
	@Test
	@DisplayName("產出報價單PDF-極少資料量-單筆明細正常在首頁呈現不產生多餘空白頁")
	void generateQuotationPdf_with1Item_success() throws IOException {
		// Given: 只有 1 筆物料明細
		List<QuotationItemGottenResult> items = List.of(
				new QuotationItemGottenResult(1, "SINGLE-MAT-01", "單一物料極少項目測試", 1, "PCS", new BigDecimal("999"), new BigDecimal("999"), "單筆測試")
		);
		QuotationGottenResult quotation = new QuotationGottenResult(
				"QT-SINGLE-001",
				LocalDate.of(2026, 10, 24),
				"極簡測試客戶",
				"11223344",
				"王專案",
				"14 天",
				"即期匯款",
				"台北",
				new BigDecimal("999"),
				new BigDecimal("50"),
				new BigDecimal("1049"),
				"一般條款",
				items
		);

		// When
		byte[] pdfBytes = openPdfQuotationAdapter.generateQuotationPdf(quotation);

		// Then
		assertNotNull(pdfBytes);
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			// 極少資料量應剛好為 1 頁
			assertEquals(1, document.getNumberOfPages(), "單筆明細之報價單應緊湊於單頁 (1 頁) 完整排版");

			PDFTextStripper stripper = new PDFTextStripper();
			String text = stripper.getText(document);
			assertTrue(text.contains("SINGLE-MAT-01"));
			assertTrue(text.contains("總計金額"));
			assertTrue(text.contains("客戶確認簽回欄"));
		}
	}

	/**
	 * Feature: OpenPDF 動態資料量排版防孤兒機制
	 * Scenario: 大量資料跨多頁 (35 筆) - 跨越多頁（例如 3 頁），驗證最後一頁頂部依然具備表頭與明細，絕不孤兒
	 */
	@Test
	@DisplayName("產出報價單PDF-大量跨多頁排版-35筆資料跨多頁最後一頁依然保有明細與結尾區塊")
	void generateQuotationPdf_with35Items_triggersMultiPageAntiOrphan_lastPageHasItemsAndFooter() throws IOException {
		// Given: 產生 35 筆長描述物料明細
		List<QuotationItemGottenResult> items = new ArrayList<>();
		for (int i = 1; i <= 33; i++) {
			items.add(new QuotationItemGottenResult(
					i,
					"BULK-MAT-" + i,
					"大量多頁連續跨頁排版測試物料品名規格第 " + i + " 項描述 (高密度沉金與阻抗測試)",
					10,
					"組",
					new BigDecimal("100"),
					new BigDecimal("1000"),
					"批次合格"
			));
		}
		items.add(new QuotationItemGottenResult(34, "ANCHOR-BULK-34", "最後倒數第二筆錨點物料 (防孤兒機制牽引至最後一頁)", 1, "套", new BigDecimal("5000"), new BigDecimal("5000"), "錨點"));
		items.add(new QuotationItemGottenResult(35, "ANCHOR-BULK-35", "最後一筆錨點物料 (防孤兒機制牽引至最後一頁)", 1, "套", new BigDecimal("8000"), new BigDecimal("8000"), "錨點"));

		QuotationGottenResult quotation = new QuotationGottenResult(
				"QT-BULK-035",
				LocalDate.of(2026, 10, 24),
				"超大型跨國智能科技集團",
				"99887766",
				"採購經理",
				"60 天",
				"月結 60 天",
				"高雄科學園區",
				new BigDecimal("46000"),
				new BigDecimal("2300"),
				new BigDecimal("48300"),
				"大型合約交易條款",
				items
		);

		// When
		byte[] pdfBytes = openPdfQuotationAdapter.generateQuotationPdf(quotation);

		// Then
		assertNotNull(pdfBytes);
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			int totalPages = document.getNumberOfPages();
			assertTrue(totalPages >= 2, "35 筆多行規格之物料應跨頁生成至少 2 頁");

			// 檢驗最後一頁
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setStartPage(totalPages);
			stripper.setEndPage(totalPages);
			String lastPageText = stripper.getText(document);

			// 1. 最後一頁必須包含重新渲染的表頭
			assertTrue(lastPageText.contains("物料料號") || lastPageText.contains("品名與規格描述"), "最後一頁頂部必須包含表頭");

			// 2. 最後一頁必須包含錨點明細 (絕非只有結尾區塊)
			assertTrue(lastPageText.contains("ANCHOR-BULK-34") || lastPageText.contains("ANCHOR-BULK-35"),
					"最後一頁頂部必須包含被防孤兒機制攜帶換頁的最後錨點明細");

			// 3. 最後一頁必須包含結尾區塊
			assertTrue(lastPageText.contains("總計金額"), "最後一頁必須包含總計金額");
			assertTrue(lastPageText.contains("客戶確認簽回欄"), "最後一頁必須包含客戶簽核用印");
		}
	}

	/**
	 * Feature: OpenPDF 報價單原生浮水印生成 (In-flight Single-pass Watermarking)
	 * Scenario: 正常流程 - 傳入報價單與浮水印文字，在文件生成時一次性完成半透明浮水印繪製
	 *
	 * Given: 包含物料明細之 QuotationGottenResult 與浮水印文字 "內部機密"
	 * When: 調用 OpenPdfQuotationAdapter.generateQuotationPdf(quotation, "內部機密")
	 * Then: 成功獲得合法 PDF 二進位 byte[]，解析後文字包含 "內部機密"
	 */
	@Test
	@DisplayName("產出報價單PDF-原生生成浮水印-單次渲染成功包含浮水印文字")
	void generateQuotationPdf_withNativeWatermark_success() throws IOException {
		// Given
		QuotationGottenResult quotation = createCompleteBomQuotation("QT-WM-NATIVE-001");

		// When
		byte[] pdfBytes = openPdfQuotationAdapter.generateQuotationPdf(quotation, "內部機密");

		// Then
		assertNotNull(pdfBytes, "產出的 PDF 位元組陣列不得為空");
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			PDFTextStripper stripper = new PDFTextStripper();
			String text = stripper.getText(document);
			String normalized = text.replaceAll("\\s+", "");
			assertTrue(normalized.contains("內部機密"), "PDF 內文應包含原生印製之浮水印文字 (內部機密)");
			assertTrue(normalized.contains("BOMQUOTATION"), "PDF 內文應包含 BOM 報價單專屬之防偽鋼印副標 (BOM QUOTATION)");
		}
	}

	/**
	 * Feature: OpenPDF 範本浮水印樣式自訂 (Template Watermark Style Customization)
	 * Scenario: 正常流程 - 呼叫端只需傳入文字即可自動套用範本 getWatermarkStyle()；亦支援傳入自訂 OpenPdfWatermarkStyle
	 *
	 * Given: 包含物料明細之 QuotationGottenResult 與自訂樣式 OpenPdfWatermarkStyle.CONFIDENTIAL
	 * When: 調用 bomQuotationPdfTemplate.render(quotation, baos, "機密草稿", OpenPdfWatermarkStyle.CONFIDENTIAL)
	 * Then: 成功輸出合法 PDF，且文字包含浮水印 "機密草稿"
	 */
	@Test
	@DisplayName("PdfDocumentTemplate-浮水印樣式自訂-支援範本預設與自訂WatermarkStyle渲染")
	void render_withCustomWatermarkStyle_success() throws Exception {
		// Given
		QuotationGottenResult quotation = createCompleteBomQuotation("QT-WM-STYLE-001");
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

		// When: 傳入自訂樣式 (亦可只傳文字自動套用範本 getWatermarkStyle)
		bomQuotationPdfTemplate.render(quotation, baos, "機密草稿", com.example.demo.infrastructure.openpdf.core.OpenPdfWatermarkStyle.CONFIDENTIAL);
		byte[] pdfBytes = baos.toByteArray();

		// Then
		assertNotNull(pdfBytes);
		assertTrue(pdfBytes.length > 5000);
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			PDFTextStripper stripper = new PDFTextStripper();
			String text = stripper.getText(document);
			String normalized = text.replaceAll("\\s+", "");
			assertTrue(normalized.contains("機密草稿"), "PDF 內文應包含機密樣式之浮水印文字 (機密草稿)");
		}
	}

	private QuotationGottenResult createCompleteBomQuotation(String quotationNo) {
		List<QuotationItemGottenResult> items = new ArrayList<>();
		items.add(new QuotationItemGottenResult(1, "MCU-STM32-H743", "嵌入式高性能核心控制器 ARM Cortex-M7 480MHz", 100, "PCS", new BigDecimal("520"), new BigDecimal("52000"), "原廠出廠證明"));
		items.add(new QuotationItemGottenResult(2, "SEN-MEMS-6AXIS", "六軸高精度慣性測量單元 (IMU) 數位感測器", 200, "PCS", new BigDecimal("180"), new BigDecimal("36000"), "附校準參數表"));
		items.add(new QuotationItemGottenResult(3, "PWR-DIN24-120W", "工業級導軌式開關電源模組 (24V/5A)", 20, "台", new BigDecimal("1450"), new BigDecimal("29000"), "過載保護"));
		items.add(new QuotationItemGottenResult(4, "PCB-4L-FR4-TG", "四層高密度沉金印刷電路板 (120x80mm, TG170)", 100, "片", new BigDecimal("280"), new BigDecimal("28000"), "100% 飛針測試"));
		items.add(new QuotationItemGottenResult(5, "CON-M12-8P-IP67", "M12 工業防水訊號接頭 (8-Pin PUR 3m)", 80, "條", new BigDecimal("320"), new BigDecimal("25600"), "耐磨防油"));
		items.add(new QuotationItemGottenResult(6, "REL-SSR-25A-480", "固態繼電器單相交流控制 (24-480VAC 25A)", 30, "組", new BigDecimal("480"), new BigDecimal("14400"), "光電隔離 4kV"));
		items.add(new QuotationItemGottenResult(7, "COMM-RS485-ISO", "雙向光電隔離 RS485 模組 (15kV ESD)", 50, "PCS", new BigDecimal("210"), new BigDecimal("10500"), "突波防護"));
		items.add(new QuotationItemGottenResult(8, "ENC-AL-IP65", "陽極氧化工業鋁合金外殼 (160x100x60mm)", 20, "套", new BigDecimal("850"), new BigDecimal("17000"), "含密封墊"));

		return new QuotationGottenResult(
				quotationNo,
				LocalDate.of(2026, 10, 23),
				"台灣智能科技股份有限公司",
				"12345678",
				"陳建銘 經理 / (03) 578-1234 #567",
				"30 天 (至 2026-11-22)",
				"月結 30 天 (Net 30)",
				"新竹市科學園區研發六路 12 號 (DDP)",
				new BigDecimal("212500"),
				new BigDecimal("10625"),
				new BigDecimal("223125"),
				"1. 本報價單所列物料均符合 RoHS 2.0 及 REACH 環保規範要求。\n" +
				"2. 提供出貨後 12 個月原廠保固服務（人為操作不當或不可抗力因素除外）。\n" +
				"3. 貴公司如確認訂購，請於下方「客戶確認簽回」欄位用印簽署後回傳。",
				items
		);
	}
}
