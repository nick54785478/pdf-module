/**
 * 基礎設施輸出適配器 (Outbound Adapters)
 * <p>
 * 作為所有基礎設施模組的匯聚點與對外出口，實作 Application 層的 Outbound Port。
 * 協調並呼叫 persistence、messaging 或第三方套件 (如 OpenPDF / PDFBox)。
 * 命名規範：以 {@code Adapter} 結尾 (例如：{@code PdfRepositoryAdapter}, {@code PdfGeneratorAdapter})。
 */
package com.example.demo.infrastructure.adapter;
