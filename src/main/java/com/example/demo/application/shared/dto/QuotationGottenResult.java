package com.example.demo.application.shared.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 報價單完整資訊資料傳輸物件 (DTO - Pure Data Carrier)
 * <p>
 * 在 Clean Architecture 與 Hexagonal Architecture 架構中作為應用層傳遞的純資料載體，
 * 嚴格禁止於內部包含任何轉換邏輯（如 toCommand 或 fromDomain）。
 * 遵循命名規範 3-6：名詞 + 查詢類分詞 (Gotten) + Result。
 * </p>
 *
 * @param quotationNo    報價單號
 * @param quotationDate  報價日期
 * @param customerName   客戶公司名稱
 * @param customerTaxId  客戶統一編號
 * @param contactPerson  客戶聯絡窗口與聯絡方式
 * @param validityPeriod 報價單有效期限說明
 * @param paymentTerm    付款條件說明
 * @param deliveryPlace  交貨地點與條件 (如 DDP 新竹科學園區)
 * @param subtotalAmount 未稅合計金額
 * @param taxAmount      營業稅額 (5%)
 * @param totalAmount    含稅總計金額
 * @param terms          報價與交易條款說明
 * @param items          物料清單明細清單
 */
public record QuotationGottenResult(
		String quotationNo,
		LocalDate quotationDate,
		String customerName,
		String customerTaxId,
		String contactPerson,
		String validityPeriod,
		String paymentTerm,
		String deliveryPlace,
		BigDecimal subtotalAmount,
		BigDecimal taxAmount,
		BigDecimal totalAmount,
		String terms,
		List<QuotationItemGottenResult> items
) {
}
