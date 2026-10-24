package com.example.demo.presentation.resource.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 取得報價單 PDF 請求資料載體 (Request Resource - Pure Data Carrier)
 * <p>
 * 遵循命名規範 3-1：V (Get) + N (QuotationPdf) + Resource。
 * 作為表現層接收客戶端請求的純資料物件，僅攜帶報價單識別碼 (ID)。
 * 導入 Jakarta Validation 規範進行基本參數校驗。
 * 內部嚴格禁止包含任何業務或轉換邏輯，轉換由 {@code QuotationResourceAssembler} 負責。
 * </p>
 *
 * @param id        報價單識別碼或單號 (ID)
 * @param watermark 可選的浮水印文字 (例如：機密、草稿、正式報價等)
 */
@Schema(description = "取得報價單 PDF 請求資料載體 (GetQuotationPdfResource)")
public record GetQuotationPdfResource(
		@Schema(description = "報價單識別碼或單號 (可選，若未傳入則由系統套用預設範本 ID)", example = "QT-20261023-001", maxLength = 50)
		@Size(max = 50, message = "報價單 ID 長度不可超過 50 個字元")
		@Pattern(regexp = "^[A-Za-z0-9_-]{0,50}$", message = "報價單 ID 格式不符 (僅允許半形英數字、減號與底線)")
		String id,

		@Schema(description = "可選的浮水印文字 (例如：機密草稿、正式報價、作廢)", example = "機密草稿", maxLength = 30)
		@Size(max = 30, message = "浮水印文字長度不可超過 30 個字元")
		String watermark
) {
	/**
	 * 相容舊有僅傳入 ID 之建構子
	 *
	 * @param id 報價單識別碼或單號
	 */
	public GetQuotationPdfResource(String id) {
		this(id, null);
	}
}
