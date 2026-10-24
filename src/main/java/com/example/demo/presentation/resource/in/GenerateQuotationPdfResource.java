package com.example.demo.presentation.resource.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 產生報價單 PDF 請求載體 (Request Resource - Pure Data Carrier)
 * <p>
 * 遵循命名規範 3-1：V (Generate) + N (QuotationPdf) + Resource。
 * 表現層純資料載體，嚴禁於內部包含任何轉換邏輯（如 toDto 或 toCommand）。
 * 導入 Jakarta Validation 進行基礎結構檢核。
 * 資料轉換由 {@code com.example.demo.presentation.assembler.QuotationResourceAssembler} 負責。
 * </p>
 *
 * @param quotationNo    報價單號 (不可為空，長度最多 50)
 * @param quotationDate  報價日期 (不可為 null)
 * @param customerName   客戶公司名稱 (不可為空)
 * @param customerTaxId  客戶統一編號 (不可為空)
 * @param contactPerson  聯絡人員及聯絡方式
 * @param validityPeriod 有效期限
 * @param paymentTerm    付款條件
 * @param deliveryPlace  交貨地點
 * @param subtotalAmount 未稅合計金額 (不可為負數)
 * @param taxAmount      營業稅額 (不可為負數)
 * @param totalAmount    含稅總計金額 (不可為負數)
 * @param terms          交易與保固條款說明
 * @param items          物料清單項目明細清單 (不可為空，內部元素遞迴驗證)
 */
@Schema(description = "產生報價單 PDF 請求載體 (GenerateQuotationPdfResource)")
public record GenerateQuotationPdfResource(
		@Schema(description = "報價單號", example = "QT-20261023-001", maxLength = 50)
		@NotBlank(message = "報價單號不可為空")
		@Size(max = 50, message = "報價單號長度不可超過 50 個字元")
		@Pattern(regexp = "^[A-Za-z0-9_-]{1,50}$", message = "報價單號格式不符 (僅允許半形英數字、減號與底線)")
		String quotationNo,

		@Schema(description = "報價日期", example = "2026-10-23")
		@NotNull(message = "報價日期不可為 null")
		LocalDate quotationDate,

		@Schema(description = "客戶公司名稱", example = "台灣智能科技股份有限公司")
		@NotBlank(message = "客戶公司名稱不可為空")
		String customerName,

		@Schema(description = "客戶統一編號 (8 碼數字)", example = "12345678")
		@NotBlank(message = "客戶統一編號不可為空")
		String customerTaxId,

		@Schema(description = "聯絡窗口與聯絡方式", example = "陳建銘 經理 / (03) 578-1234 #567")
		String contactPerson,

		@Schema(description = "報價有效期限", example = "30 天 (至 2026-11-22)")
		String validityPeriod,

		@Schema(description = "付款條件", example = "月結 30 天 (Net 30)")
		String paymentTerm,

		@Schema(description = "交貨地點與條件", example = "新竹市科學園區研發六路 12 號 (DDP)")
		String deliveryPlace,

		@Schema(description = "未稅合計金額", example = "212500.00")
		@NotNull(message = "未稅合計金額不可為 null")
		@PositiveOrZero(message = "未稅合計金額不可小於 0")
		BigDecimal subtotalAmount,

		@Schema(description = "營業稅額 (5%)", example = "10625.00")
		@NotNull(message = "營業稅額不可為 null")
		@PositiveOrZero(message = "營業稅額不可小於 0")
		BigDecimal taxAmount,

		@Schema(description = "含稅總計金額", example = "223125.00")
		@NotNull(message = "含稅總計金額不可為 null")
		@PositiveOrZero(message = "含稅總計金額不可小於 0")
		BigDecimal totalAmount,

		@Schema(description = "報價與交易條款說明事項", example = "1. 本報價單所列物料均符合 RoHS 2.0 及 REACH 環保規範要求。")
		String terms,

		@Schema(description = "物料清單項目明細清單")
		@Valid
		@NotEmpty(message = "物料清單明細項目不可為空")
		List<QuotationItemResource> items
) {
}
