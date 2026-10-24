package com.example.demo.infrastructure.adapter;

import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.dto.QuotationItemGottenResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 報價單文字剖析適配器單元測試 (Adapter Test)
 * <p>
 * 遵循 AGENTS.md 規範：
 * 1. 驗證 TextQuotationParserAdapter 實作 QuotationParserPort 之文字抽取與語法狀態機解析。
 * 2. 測試覆蓋正常完整報價單文字、單行與多行品項、空字串防禦處理。
 * 3. 測試方法皆以 BDD Gherkin 語法撰寫 Javadoc。
 * </p>
 */
class TextQuotationParserAdapterTest {

	private TextQuotationParserAdapter adapter;

	@BeforeEach
	void setUp() {
		this.adapter = new TextQuotationParserAdapter();
	}

	/**
	 * Feature: 報價單文字抽取與結構化解析
	 * Scenario: 提供包含完整標頭、客戶網格、多品項與條款金額之 PDF 純文字，成功解析為強型別 DTO
	 *
	 * Given: 模擬 PDFBox 自 PDF 抽取出的標準排版純文字內容 (包含單行品項與跨行品項)
	 * When: 調用 TextQuotationParserAdapter.parse(rawText)
	 * Then: 成功解析出單號、日期、客戶資訊、8 碼統編、未稅與含稅金額、交易條款以及所有品項明細
	 */
	@Test
	@DisplayName("解析報價單文字-正常情境-完整欄位與品項解析成功")
	void parse_completeQuotationText_success() {
		// Given
		String rawText = """
				晨星智造股份有限公司
				MORNINGSTAR INTELLIGENT MANUFACTURING CO., LTD.
				物料清單報價單 (BOM Quotation)
				單號： QT-2026-TEST01
				日期： 2026-10-23
				
				客戶名稱： 台積電測試股份有限公司
				有效期限： 報價日起 30 天內有效
				統一編號： 12345678 付款條件： 月結 30 天 (Net 30)
				聯絡人員： 張經理 / (03) 578-1234 交貨地點： DDP 新竹科學園區研新一路 1 號
				
				項次 物料料號 品名與規格描述 數量 單位 單價(TWD) 小計(TWD) 備註說明
				1 MAT-BOM-001 工業級主控制板 (含散熱模組) 10 PCS 4,500 45,000 含 RoHS 認證
				2 MAT-BOM-002 高精度類比訊號採集子卡 (16-ch, 24-bit)
				20 PCS 2,200 44,000 高低溫校正出廠
				
				【報價與交易條款說明】
				1. 保固期限：交貨驗收合格日起提供原廠保固一年。
				2. 交貨週期：訂單正式確認後 4 至 6 週內交貨。
				
				未稅合計： NT$ 89,000
				營業稅 (5%)： NT$ 4,450
				總計金額： NT$ 93,450
				""";

		// When
		QuotationGottenResult result = adapter.parse(rawText);

		// Then
		assertNotNull(result);
		assertEquals("QT-2026-TEST01", result.quotationNo());
		assertEquals(LocalDate.of(2026, 10, 23), result.quotationDate());
		assertEquals("台積電測試股份有限公司", result.customerName());
		assertEquals("12345678", result.customerTaxId());
		assertEquals("月結 30 天 (Net 30)", result.paymentTerm());
		assertEquals("報價日起 30 天內有效", result.validityPeriod());
		assertEquals("張經理 / (03) 578-1234", result.contactPerson());
		assertEquals("DDP 新竹科學園區研新一路 1 號", result.deliveryPlace());

		assertEquals(new BigDecimal("89000"), result.subtotalAmount());
		assertEquals(new BigDecimal("4450"), result.taxAmount());
		assertEquals(new BigDecimal("93450"), result.totalAmount());

		assertTrue(result.terms().contains("保固期限"));
		assertNotNull(result.items());
		assertEquals(2, result.items().size());

		// 驗證單行品項 1
		assertEquals(1, result.items().get(0).itemNo());
		assertEquals("MAT-BOM-001", result.items().get(0).materialNo());
		assertEquals(10, result.items().get(0).quantity());
		assertEquals("PCS", result.items().get(0).unit());
		assertEquals(new BigDecimal("4500"), result.items().get(0).unitPrice());
		assertEquals(new BigDecimal("45000"), result.items().get(0).subtotal());
		assertEquals("含 RoHS 認證", result.items().get(0).remark());

		// 驗證跨行品項 2
		assertEquals(2, result.items().get(1).itemNo());
		assertEquals("MAT-BOM-002", result.items().get(1).materialNo());
		assertEquals(20, result.items().get(1).quantity());
		assertEquals("PCS", result.items().get(1).unit());
		assertEquals(new BigDecimal("2200"), result.items().get(1).unitPrice());
		assertEquals(new BigDecimal("44000"), result.items().get(1).subtotal());
		assertEquals("高低溫校正出廠", result.items().get(1).remark());
	}

	/**
	 * Feature: 報價單文字抽取與結構化解析
	 * Scenario: 當品項描述過長換行且第二行與數值欄位共存時，成功解析品項數值並完整拼接品名描述
	 *
	 * Given: 模擬 PDF 抽取文字中品名換行且尾行包含「描述末段 + 數量 + 單位 + 單價 + 小計 + 備註」
	 * When: 調用 TextQuotationParserAdapter.parse(rawText)
	 * Then: 成功解析出正確的數量、單位、單價、小計，且品名描述完整合併前後兩行文字
	 */
	@Test
	@DisplayName("解析報價單文字-多行品項描述末段含數值-成功解析")
	void parse_multiLineItemWithWrappedDescription_success() {
		// Given
		String rawText = """
				單號： QT-20261023-001
				日期： 2026-10-23
				項次 物料料號 品名與規格描述 數量 單位 單價 (TWD) 小計 (TWD) 備註
				1 MCU-STM32-H743 嵌入式高性能核心控制器 ARM Cortex-M7
				480MHz 2MB Flash 100 PCS 520 52,000 原廠出廠證明
				2 SEN-MEMS-6AXIS 六軸高精度慣性測量單元 (IMU) 數位感測器 (含
				溫度補償) 200 PCS 180 36,000 附校準參數表
				【報價與交易條款說明】
				未稅合計： NT$ 88,000
				""";

		// When
		QuotationGottenResult result = adapter.parse(rawText);

		// Then
		assertNotNull(result);
		assertNotNull(result.items());
		assertEquals(2, result.items().size());

		QuotationItemGottenResult item1 = result.items().get(0);
		assertEquals(1, item1.itemNo());
		assertEquals("MCU-STM32-H743", item1.materialNo());
		assertEquals("嵌入式高性能核心控制器 ARM Cortex-M7 480MHz 2MB Flash", item1.description());
		assertEquals(100, item1.quantity());
		assertEquals("PCS", item1.unit());
		assertEquals(new BigDecimal("520"), item1.unitPrice());
		assertEquals(new BigDecimal("52000"), item1.subtotal());
		assertEquals("原廠出廠證明", item1.remark());

		QuotationItemGottenResult item2 = result.items().get(1);
		assertEquals(2, item2.itemNo());
		assertEquals("SEN-MEMS-6AXIS", item2.materialNo());
		assertEquals("六軸高精度慣性測量單元 (IMU) 數位感測器 (含 溫度補償)", item2.description());
		assertEquals(200, item2.quantity());
		assertEquals("PCS", item2.unit());
		assertEquals(new BigDecimal("180"), item2.unitPrice());
		assertEquals(new BigDecimal("36000"), item2.subtotal());
		assertEquals("附校準參數表", item2.remark());
	}

	/**
	 * Feature: 報價單文字抽取防禦處理
	 * Scenario: 傳入 null 或空字串時，解析器回傳安全之預設 DTO 物件，避免拋出例外中斷流程
	 *
	 * Given: null 或空空白字串
	 * When: 調用 TextQuotationParserAdapter.parse(rawText)
	 * Then: 回傳包含預設值 (單號為 UNKNOWN、金額為 0、空品項清單) 之有效 QuotationGottenResult 物件
	 */
	@Test
	@DisplayName("解析報價單文字-邊界情境-空字串回傳預設DTO")
	void parse_nullOrEmptyText_returnsDefaultDto() {
		// When
		QuotationGottenResult resultNull = adapter.parse(null);
		QuotationGottenResult resultEmpty = adapter.parse("   ");

		// Then
		assertNotNull(resultNull);
		assertEquals("UNKNOWN", resultNull.quotationNo());
		assertEquals(BigDecimal.ZERO, resultNull.totalAmount());
		assertTrue(resultNull.items().isEmpty());

		assertNotNull(resultEmpty);
		assertEquals("UNKNOWN", resultEmpty.quotationNo());
		assertEquals(BigDecimal.ZERO, resultEmpty.totalAmount());
		assertTrue(resultEmpty.items().isEmpty());
	}
}
