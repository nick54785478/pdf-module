package com.example.demo.application.shared.query;

/**
 * 取得報價單查詢物件 (CQRS Query Object)
 * <p>
 * 遵循命名規範 3-5：V (Get) + N (Quotation) + Query。
 * 負責在應用層傳遞查詢報價單之標識條件（ID / 單號）。
 * </p>
 *
 * @param id 報價單單號或識別碼 (ID)
 */
public record GetQuotationQuery(String id) {
}
