package com.example.demo.presentation.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 錯誤發生回應資料載體 (Response Resource - Pure Data Carrier)
 * <p>
 * 遵循命名規範 3-2：N (Error) + Ved (Occurred) + Resource。
 * 作為表現層全域例外處理器回傳給前端或客戶端的標準錯誤訊息物件。
 * 內部維持純資料載體，嚴禁包含業務與轉換邏輯。
 * </p>
 *
 * @param timestamp 錯誤發生時間
 * @param status    HTTP 狀態碼 (如 400, 500)
 * @param error     HTTP 狀態描述 (如 Bad Request, Internal Server Error)
 * @param message   錯誤摘要訊息
 * @param path      觸發錯誤之請求端點路徑 (URI)
 * @param details   詳細欄位檢核錯誤清單 (可為空清單)
 */
@Schema(description = "API 錯誤回應資料載體 (ErrorOccurredResource)")
public record ErrorOccurredResource(
		@Schema(description = "錯誤發生時間戳記", example = "2026-10-23T16:30:00")
		LocalDateTime timestamp,

		@Schema(description = "HTTP 狀態碼", example = "400")
		int status,

		@Schema(description = "HTTP 狀態原因短語", example = "Bad Request")
		String error,

		@Schema(description = "錯誤摘要說明訊息", example = "請求參數驗證失敗")
		String message,

		@Schema(description = "觸發錯誤的 API 請求路徑", example = "/api/v1/quotations/openpdf")
		String path,

		@Schema(description = "詳細欄位驗證錯誤清單", example = "[\"id: 報價單 ID 長度不可超過 50 個字元\"]")
		List<String> details
) {
}
