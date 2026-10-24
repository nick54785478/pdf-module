package com.example.demo.application.port.out;

import com.example.demo.application.shared.dto.QuotationGottenResult;

/**
 * 報價單 PDF 文件產出外向端口 (Outbound Port)
 * <p>
 * 依據 Clean Architecture 與六角形架構規範，代表應用層對外向基礎設施發出的文件生成契約。
 * 方法簽名嚴格遵循零技術框架相依原則，僅接收領域 DTO 物件並回傳原生二進位 byte 陣列，
 * 避免任何底層 PDF 框架技術（如 OpenPDF、iText 或 PDFBox）侵入應用層。
 * 遵循命名規範 3-8：Outbound Port 一律以 Port 結尾。
 * </p>
 */
public interface QuotationPdfGeneratorPort {

	/**
	 * 依據報價單資料傳輸物件生成 PDF 二進位位元組陣列
	 *
	 * @param quotation 報價單完整資訊 (強型別 DTO)
	 * @return PDF 檔案之二進位 byte 陣列；若生成失敗則回傳 null 或空陣列
	 */
	default byte[] generateQuotationPdf(QuotationGottenResult quotation) {
		return generateQuotationPdf(quotation, null);
	}

	/**
	 * 依據報價單資料傳輸物件與防偽浮水印文字生成 PDF 二進位位元組陣列 (生成時單次印製)
	 *
	 * @param quotation     報價單完整資訊 (強型別 DTO)
	 * @param watermarkText 可選之防偽浮水印文字 (若為 null 或空白則不繪製浮水印)
	 * @return PDF 檔案之二進位 byte 陣列；若生成失敗則回傳 null 或空陣列
	 */
	byte[] generateQuotationPdf(QuotationGottenResult quotation, String watermarkText);
}

