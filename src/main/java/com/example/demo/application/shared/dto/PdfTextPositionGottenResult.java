package com.example.demo.application.shared.dto;

/**
 * PDF 文字位置資訊查詢結果 (純資料載體)
 * <p>
 * 遵循 AGENTS.md 規範：命名為 N + Gotten + Result，嚴格作為純資料載體。
 *
 * @param text   文字內容
 * @param x      X 座標
 * @param y      Y 座標
 * @param width  文字寬度
 * @param height 文字高度
 */
public record PdfTextPositionGottenResult(
		String text,
		float x,
		float y,
		float width,
		float height
) {
}
