package com.example.demo.presentation.resource.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 報價單明細項目請求資料載體 (Request Resource - Pure Data Carrier)
 * <p>
 * 表現層接收客戶端請求的純資料物件，嚴格禁止於內部包含任何轉換邏輯（如 toDto 或 toCommand）。
 * 導入 Jakarta Validation 進行基礎欄位檢核。轉換由獨立的 Assembler (防腐層) 負責。
 * </p>
 *
 * @param itemNo      項次 (必須大於 0)
 * @param materialNo  物料料號 (不可為空，長度最多 50)
 * @param description 品名規格描述 (不可為空)
 * @param quantity    數量 (必須大於 0)
 * @param unit        單位 (如 PCS, 台, 片)
 * @param unitPrice   單價 (不可為負數)
 * @param subtotal    小計金額 (不可為負數)
 * @param remark      備註說明
 */
@Schema(description = "報價單物料明細項目載體 (QuotationItemResource)")
public record QuotationItemResource(
		@Schema(description = "項次編號", example = "1")
		@NotNull(message = "項次不可為 null")
		@Positive(message = "項次必須大於 0")
		Integer itemNo,

		@Schema(description = "物料料號", example = "MCU-STM32-H743", maxLength = 50)
		@NotBlank(message = "物料料號不可為空")
		@Size(max = 50, message = "物料料號長度不可超過 50 個字元")
		String materialNo,

		@Schema(description = "品名與規格描述", example = "ARM Cortex-M7 480MHz 2MB Flash")
		@NotBlank(message = "品名與規格描述不可為空")
		String description,

		@Schema(description = "採購數量", example = "100")
		@NotNull(message = "數量不可為 null")
		@Positive(message = "數量必須大於 0")
		Integer quantity,

		@Schema(description = "計量單位", example = "PCS")
		@NotBlank(message = "單位不可為空")
		String unit,

		@Schema(description = "單價 (TWD)", example = "520.00")
		@NotNull(message = "單價不可為 null")
		@PositiveOrZero(message = "單價不可小於 0")
		BigDecimal unitPrice,

		@Schema(description = "小計金額 (TWD)", example = "52000.00")
		@NotNull(message = "小計金額不可為 null")
		@PositiveOrZero(message = "小計金額不可小於 0")
		BigDecimal subtotal,

		@Schema(description = "備註說明事項", example = "原廠出廠證明")
		String remark
) {
}
