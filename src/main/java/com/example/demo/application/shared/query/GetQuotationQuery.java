package com.example.demo.application.shared.query;

/**
 * 取得報價單查詢物件 (Query Object)
 * <p>
 * 遵循命名規範 3-5：V (Get) + N (Quotation) + Query。
 * 負責在應用層傳遞查詢報價單之標識條件（ID / 單號）。
 * </p>
 *
 * @param id            報價單單號或識別碼 (ID)
 * @param watermarkText 可選的浮水印文字 (例如：機密草稿、正式報價等)
 */
public record GetQuotationQuery(
		String id,
		String watermarkText
) {
	/**
	 * 相容舊有僅傳入 ID 之建構子
	 *
	 * @param id 報價單單號或識別碼
	 */
	public GetQuotationQuery(String id) {
		this(id, null);
	}
}
