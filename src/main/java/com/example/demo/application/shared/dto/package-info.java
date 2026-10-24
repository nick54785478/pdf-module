/**
 * 應用層資料傳輸物件 (Application DTOs)
 * <p>
 * 嚴格作為純資料載體 (Pure Data Carrier)，禁止包含轉換邏輯 (如 {@code fromDomain} 等)。
 * 命名規範：{@code N + Gotten/Searched + Result} (例如：{@code PdfGottenResult}, {@code PdfSearchedResult})。
 */
package com.example.demo.application.shared.dto;
