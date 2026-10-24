package com.example.demo.application.shared.dto;

import java.math.BigDecimal;

/**
 * 報價單物料明細項目資料傳輸物件 (DTO - Pure Data Carrier)
 * <p>
 * 在 Clean Architecture 與 Hexagonal Architecture 架構中作為純資料載體，
 * 嚴格禁止於內部包含任何轉換邏輯（如 toCommand 或 fromDomain）。
 * 遵循命名規範 3-6：名詞 + 查詢類分詞 (Gotten) + Result。
 * </p>
 *
 * @param itemNo       項次
 * @param materialNo   物料料號
 * @param description  品名與規格描述
 * @param quantity     數量
 * @param unit         單位 (如 PCS, 台, 片)
 * @param unitPrice    單價 (TWD)
 * @param subtotal     小計金額 (TWD)
 * @param remark       備註說明
 */
public record QuotationItemGottenResult(
		Integer itemNo,
		String materialNo,
		String description,
		Integer quantity,
		String unit,
		BigDecimal unitPrice,
		BigDecimal subtotal,
		String remark
) {
}
