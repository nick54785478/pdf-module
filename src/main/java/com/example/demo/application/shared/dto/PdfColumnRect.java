package com.example.demo.application.shared.dto;

/**
 * PDF 表格欄位區域定義 (純資料載體)
 * <p>
 * 用於定義表格抽取的欄位水平起始座標與寬度。
 *
 * @param x     欄位左上角 X 座標
 * @param width 欄位寬度
 */
public record PdfColumnRect(
		float x,
		float width
) {
}
