package com.example.demo.application.port.out;

/**
 * PDF 浮水印蓋印輸出端口 (Outbound Port)
 * <p>
 * 依據 Clean Architecture 與六角形架構規範，代表應用層向基礎設施層索求的通用 PDF 浮水印印製技術能力契約。
 * 不受限於特定單據或排版引擎，支援對任何既有二進位 PDF 檔案（包含多頁、直式、橫式單據）進行半透明文字浮水印之渲染蓋印。
 * 遵循命名規範 3-8：Outbound Port 一律以 Port 結尾。
 * 方法簽章嚴禁使用技術類或環境相依之物件（如 HttpServletRequest 或 Spring Resource），維持純粹的 Java 基礎資料型別。
 * </p>
 */
public interface PdfWatermarkExecutorPort {

	/**
	 * 於傳入之 PDF 二進位位元組陣列中，為每一頁動態疊加半透明傾斜浮水印
	 *
	 * @param sourcePdf     原始 PDF 檔案位元組陣列 (不可為空)
	 * @param watermarkText 欲蓋印之浮水印文字 (例如：機密、草稿、正式報價、作廢等，若為空或空白則不印製直接回傳原始檔案)
	 * @return 蓋印完成之 PDF 檔案位元組陣列
	 */
	byte[] stampWatermark(byte[] sourcePdf, String watermarkText);
}
