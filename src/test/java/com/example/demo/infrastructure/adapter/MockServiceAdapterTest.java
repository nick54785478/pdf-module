package com.example.demo.infrastructure.adapter;

import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.query.GetQuotationQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 模擬資料提供適配器單元測試 (Adapter Test)
 * <p>
 * 遵循 AGENTS.md 規範：
 * 1. 驗證 MockServiceAdapter 在給定 ID 或預設情況下提供完整範本資料。
 * 2. 測試方法皆以 BDD Gherkin 語法撰寫 Javadoc。
 * </p>
 */
class MockServiceAdapterTest {

	private final MockServiceAdapter mockServiceAdapter = new MockServiceAdapter();

	/**
	 * Feature: 提供報價單模擬範本資料
	 * Scenario: 給定特定報價單 ID，回傳包含該 ID 與 8 筆物料明細之完整報價單範本
	 *
	 * Given: 包含自訂 ID "QT-CUSTOM-999" 的 GetQuotationQuery
	 * When: 調用 MockServiceAdapter.getQuotation(query)
	 * Then: 回傳物件的 quotationNo 為 "QT-CUSTOM-999"，物料明細項目數量為 8，且總金額大於零
	 */
	@Test
	@DisplayName("提供Mock範本-指定ID情境-成功回傳包含該ID之完整資料")
	void getQuotation_withSpecificId_success() {
		// Given
		GetQuotationQuery query = new GetQuotationQuery("QT-CUSTOM-999");

		// When
		QuotationGottenResult result = mockServiceAdapter.getQuotation(query);

		// Then
		assertNotNull(result);
		assertEquals("QT-CUSTOM-999", result.quotationNo());
		assertNotNull(result.customerName());
		assertNotNull(result.items());
		assertTrue(result.items().size() >= 8, "範本物料明細應至少包含 8 筆品項");
		assertTrue(result.totalAmount().signum() > 0, "含稅總計應大於零");
	}

	/**
	 * Feature: 提供報價單模擬範本資料
	 * Scenario: 查詢條件為空或 null ID，回傳預設單號之報價單範本
	 *
	 * Given: 查詢條件 query 為 null 或 id 為空
	 * When: 調用 MockServiceAdapter.getQuotation(null)
	 * Then: 回傳預設單號 "QT-20261023-001" 之完整範本
	 */
	@Test
	@DisplayName("提供Mock範本-未指定ID情境-回傳預設單號範本")
	void getQuotation_withNullQuery_returnsDefault() {
		// When
		QuotationGottenResult result = mockServiceAdapter.getQuotation(null);

		// Then
		assertNotNull(result);
		assertEquals("QT-20261023-001", result.quotationNo());
		assertFalse(result.items().isEmpty());
	}
}
