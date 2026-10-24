/**
 * 表現層請求資源 (Request Resources / Inbound Data)
 * <p>
 * 對外接收的請求載體，嚴格作為純資料載體 (Pure Data Carrier)，不得包含 {@code toCommand} 等轉換邏輯。
 * 命名規範：{@code V + N + Resource} (例如：{@code GeneratePdfResource}, {@code UpdatePdfResource})。
 */
package com.example.demo.presentation.resource.in;
