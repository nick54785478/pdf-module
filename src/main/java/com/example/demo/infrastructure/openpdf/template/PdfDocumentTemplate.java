package com.example.demo.infrastructure.openpdf.template;

import com.example.demo.infrastructure.openpdf.core.OpenPdfWatermarkStyle;

import java.io.OutputStream;

/**
 * PDF 業務範本通用抽象契約介面 (Template Contract)
 * <p>
 * 定義具體業務單據範本（如物料清單報價單、出貨單、工單憑證等）統一的排版渲染合約，
 * 規範將強型別業務 DTO 資料排版並寫入目標輸出串流的標準行為。
 * </p>
 *
 * @param <T> 業務資料物件型別 (DTO)
 */
public interface PdfDocumentTemplate<T> {

	/**
	 * 執行版面排版並將產出的 PDF 文件寫入指定輸出串流
	 *
	 * @param data         業務資料物件
	 * @param outputStream PDF 二進位輸出串流
	 * @throws Exception 當排版、資料格式化或串流寫入過程發生例外時拋出
	 */
	void render(T data, OutputStream outputStream) throws Exception;

	/**
	 * 取得此單據範本定義的預設浮水印樣式 (Template Method Hook)
	 * <p>
	 * 具體單據範本可覆寫此方法，自訂該單據類型專屬的浮水印字號 (fontSize)、透明度 (opacity)、
	 * 填色 (color) 與傾斜角度 (rotation)。
	 * 呼叫端呼叫 {@link #render(Object, OutputStream, String)} 時，<b>只需傳入文字即可</b>，
	 * 排版引擎會自動套用此處定義的專屬樣式。
	 * </p>
	 *
	 * @return 浮水印樣式物件，預設為 {@link OpenPdfWatermarkStyle#DEFAULT}
	 */
	default OpenPdfWatermarkStyle getWatermarkStyle() {
		return OpenPdfWatermarkStyle.DEFAULT;
	}

	/**
	 * 執行版面排版並於生成時直接印上指定防偽浮水印（自動套用範本專屬浮水印樣式）
	 * <p>
	 * <b>呼叫端只需傳入文字即可</b>，排版引擎自動取得 {@link #getWatermarkStyle()} 定義的樣式進行繪製。
	 * </p>
	 *
	 * @param data          業務資料物件
	 * @param outputStream  PDF 二進位輸出串流
	 * @param watermarkText 可選之防偽浮水印文字 (若為 null 或空字串則不繪製浮水印)
	 * @throws Exception 當排版、資料格式化或串流寫入過程發生例外時拋出
	 */
	default void render(T data, OutputStream outputStream, String watermarkText) throws Exception {
		render(data, outputStream, watermarkText, getWatermarkStyle());
	}

	/**
	 * 執行版面排版並於生成時直接印上指定防偽浮水印（支援外部動態自訂樣式）
	 *
	 * @param data           業務資料物件
	 * @param outputStream   PDF 二進位輸出串流
	 * @param watermarkText  可選之防偽浮水印文字 (若為 null 或空字串則不繪製浮水印)
	 * @param watermarkStyle 浮水印樣式物件 (若為 null 則自動套用 {@link #getWatermarkStyle()})
	 * @throws Exception 當排版、資料格式化或串流寫入過程發生例外時拋出
	 */
	default void render(T data, OutputStream outputStream, String watermarkText, OpenPdfWatermarkStyle watermarkStyle) throws Exception {
		render(data, outputStream);
	}
}

