package com.example.demo.infrastructure.openpdf.template;

import com.example.demo.infrastructure.openpdf.core.OpenPdfWatermarkStyle;
import com.example.demo.infrastructure.pdfbox.security.policy.PdfSignaturePolicy;
import com.example.demo.infrastructure.pdfbox.security.customizer.PdfVisualSignatureCustomizer;

import java.io.OutputStream;

/**
 * PDF 業務範本通用抽象契約介面 (Template Contract)
 * <p>
 * 定義具體業務單據範本（如物料清單報價單、出貨單、工單憑證等）統一的排版渲染合約，
 * 規範將強型別業務 DTO 資料排版並寫入目標輸出串流的標準行為。
 * 同時作為 Template Method 設計模式核心，提供範本專屬的浮水印樣式 (Watermark)
 * 以及個別範本自定義的數位簽章規範 (Signature Policy) 與視覺防偽鋼印 (Visual Customizer) 之擴充鈎子。
 * </p>
 *
 * @param <T> 業務資料物件型別 (DTO)
 */
public interface PdfDocumentTemplate<T> {

	/**
	 * 取得此範本的唯一識別名稱 (例如 "bom", "quotation")
	 *
	 * @return 範本識別字串，預設為類別簡稱
	 */
	default String getTemplateName() {
		return getClass().getSimpleName();
	}

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
	 * 取得此單據範本專屬定義的數位簽章規範 (Template Method Hook)
	 * <p>
	 * 具體單據範本可覆寫此方法，自訂該單據類型專屬的簽章規範 (例如 PAdES 高級規範、嚴格法務存檔規範等)。
	 * <b>若未覆寫或回傳 null，簽署時自動默認使用系統預設簽章規範</b>。
	 * </p>
	 *
	 * @return 數位簽章規範物件，預設為 null (代表採用預設簽章規範)
	 */
	default PdfSignaturePolicy getSignaturePolicy() {
		return null;
	}

	/**
	 * 取得此單據範本專屬定義的視覺印章客製化器 (Template Method Hook)
	 * <p>
	 * 具體單據範本可覆寫此方法，自訂該單據類型專屬的防偽視覺鋼印外觀與繪製邏輯。
	 * <b>若未覆寫或回傳 null，簽署時自動默認使用系統預設視覺印章</b>。
	 * </p>
	 *
	 * @return 視覺印章客製化器，預設為 null (代表採用預設視覺印章)
	 */
	default PdfVisualSignatureCustomizer getVisualSignatureCustomizer() {
		return null;
	}

	/**
	 * 取得此單據範本預設的簽章理由說明 (Template Method Hook)
	 *
	 * @return 預設簽章事由，若未覆寫則預設為 null
	 */
	default String getDefaultSignatureReason() {
		return null;
	}

	/**
	 * 取得此單據範本預設的簽章地點 (Template Method Hook)
	 *
	 * @return 預設簽章地點，若未覆寫則預設為 null
	 */
	default String getDefaultSignatureLocation() {
		return null;
	}

	/**
	 * 取得此單據範本預設的視覺印章左下角 X 座標點 (Template Method Hook)
	 *
	 * @return 預設 X 座標，若未覆寫則預設為 null
	 */
	default Float getDefaultSignatureX() {
		return null;
	}

	/**
	 * 取得此單據範本預設的視覺印章左下角 Y 座標點 (Template Method Hook)
	 *
	 * @return 預設 Y 座標，若未覆寫則預設為 null
	 */
	default Float getDefaultSignatureY() {
		return null;
	}

	/**
	 * 取得此單據範本預設的視覺印章寬度 (Template Method Hook)
	 *
	 * @return 預設印章寬度，若未覆寫則預設為 null
	 */
	default Float getDefaultSignatureWidth() {
		return null;
	}

	/**
	 * 取得此單據範本預設的視覺印章高度 (Template Method Hook)
	 *
	 * @return 預設印章高度，若未覆寫則預設為 null
	 */
	default Float getDefaultSignatureHeight() {
		return null;
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

