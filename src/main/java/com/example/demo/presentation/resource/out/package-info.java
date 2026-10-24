/**
 * 表現層回應資源 (Response Resources / Outbound Data)
 * <p>
 * 對外回傳的結果載體，嚴格作為純資料載體 (Pure Data Carrier)，不得包含 {@code fromResult} 等轉換邏輯。
 * 命名規範：{@code N + Ved + Resource} (例如：{@code PdfGeneratedResource}, {@code PdfRetrievedResource})。
 */
package com.example.demo.presentation.resource.out;
