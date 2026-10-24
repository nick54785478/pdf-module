package com.example.demo.presentation.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 報價單明細項目解析回應資料載體 (Response Resource - Pure Data Carrier)
 * <p>
 * 遵循命名規範 3-2：N (QuotationItem) + Ved (Parsed) + Resource。
 * 作為表現層回傳給前端顯示之報價單物料明細項目純資料載體。
 * </p>
 *
 * @param itemNo      項次
 * @param materialNo  物料料號
 * @param description 品名與規格描述
 * @param quantity    數量
 * @param unit        單位 (如 PCS, 台, 片)
 * @param unitPrice   單價 (TWD)
 * @param subtotal    小計金額 (TWD)
 * @param remark      備註說明
 */
@Schema(description = "報價單明細項目解析回應資料載體 (QuotationItemParsedResource)")
public record QuotationItemParsedResource(
		@Schema(description = "項次", example = "1")
		Integer itemNo,

		@Schema(description = "物料料號", example = "MCU-STM32-H743")
		String materialNo,

		@Schema(description = "品名與規格描述", example = "嵌入式高性能核心控制器 ARM Cortex-M7 480MHz 2MB Flash")
		String description,

		@Schema(description = "數量", example = "100")
		Integer quantity,

		@Schema(description = "單位", example = "PCS")
		String unit,

		@Schema(description = "單價 (TWD)", example = "520.00")
		BigDecimal unitPrice,

		@Schema(description = "小計金額 (TWD)", example = "52000.00")
		BigDecimal subtotal,

		@Schema(description = "備註說明", example = "原廠出廠證明")
		String remark
) {
}
