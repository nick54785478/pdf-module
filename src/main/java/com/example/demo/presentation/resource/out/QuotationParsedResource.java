package com.example.demo.presentation.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 報價單解析回應資料載體 (Response Resource - Pure Data Carrier)
 * <p>
 * 遵循命名規範 3-2：N (Quotation) + Ved (Parsed) + Resource。
 * 作為表現層上傳 PDF 解析端點回傳給前端畫面顯示之報價單完整資料模型。
 * 內部嚴格作為純資料載體，禁止包含任何業務與轉換邏輯。
 * </p>
 *
 * @param quotationNo    報價單號
 * @param quotationDate  報價日期
 * @param customerName   客戶公司名稱
 * @param customerTaxId  客戶統一編號
 * @param contactPerson  聯絡人員與聯絡方式
 * @param validityPeriod 報價有效期限
 * @param paymentTerm    付款條件
 * @param deliveryPlace  交貨地點與條件
 * @param subtotalAmount 未稅合計金額
 * @param taxAmount      營業稅額 (5%)
 * @param totalAmount    含稅總計金額
 * @param terms          報價與交易條款說明
 * @param items          物料清單明細項目清單
 */
@Schema(description = "報價單解析回應資料載體 (QuotationParsedResource)")
public record QuotationParsedResource(
		@Schema(description = "報價單號", example = "QT-20261023-001")
		String quotationNo,

		@Schema(description = "報價日期", example = "2026-10-23")
		LocalDate quotationDate,

		@Schema(description = "客戶公司名稱", example = "台灣智能科技股份有限公司")
		String customerName,

		@Schema(description = "客戶統一編號", example = "12345678")
		String customerTaxId,

		@Schema(description = "聯絡人員與聯絡方式", example = "陳建銘 經理 / (03) 578-1234 #567")
		String contactPerson,

		@Schema(description = "報價有效期限", example = "30 天 (至 2026-11-22)")
		String validityPeriod,

		@Schema(description = "付款條件", example = "月結 30 天 (Net 30)")
		String paymentTerm,

		@Schema(description = "交貨地點與條件", example = "新竹市科學園區研發六路 12 號 (DDP)")
		String deliveryPlace,

		@Schema(description = "未稅合計金額", example = "212500.00")
		BigDecimal subtotalAmount,

		@Schema(description = "營業稅額 (5%)", example = "10625.00")
		BigDecimal taxAmount,

		@Schema(description = "含稅總計金額", example = "223125.00")
		BigDecimal totalAmount,

		@Schema(description = "報價與交易條款說明", example = "1. 本報價單所列物料均符合 RoHS 2.0 及 REACH 環保規範要求。")
		String terms,

		@Schema(description = "物料清單明細項目清單")
		List<QuotationItemParsedResource> items
) {
}
