package com.example.demo.application.shared.command;

/**
 * 加蓋 PDF 浮水印命令物件 (Command Object - Pure Data Carrier)
 * <p>
 * 遵循命名規範 3-4：{@code V (Add) + N (Watermark) + Command}。
 * 作為應用層用例 {@code AddWatermarkUseCase} 之輸入資料載體，
 * 封裝待加蓋浮水印之原始 PDF 檔案二進位位元組與目標浮水印文字。
 * 遵循六角形架構規範，內部僅使用純 Java 原生資料型別 (byte[] 與 String)，
 * 絕不引入 Spring MultipartFile 等表現層技術物件，確保應用層的技術純粹性。
 * </p>
 *
 * @param fileBytes     原始 PDF 檔案之二進位位元組陣列 (不可為空)
 * @param watermarkText 待印製之浮水印文字 (例如：機密、草稿、正式報價、作廢等)
 */
public record AddWatermarkCommand(
		byte[] fileBytes,
		String watermarkText
) {
}
