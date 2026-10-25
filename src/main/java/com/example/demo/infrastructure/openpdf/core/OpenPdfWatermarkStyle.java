package com.example.demo.infrastructure.openpdf.core;

import java.awt.Color;

/**
 * OpenPDF 浮水印視覺樣式配置值物件 (Watermark Visual Style)
 * <p>
 * 封裝浮水印在 PDF 頁面中渲染時的外觀屬性，包含字型大小 (fontSize)、不透明度 (opacity)、
 * 填色 (color)、傾斜旋轉角度 (rotation)、防偽外框裝飾 (boxed) 以及次級防偽標記副標 (subText)。
 * 採用不可變物件 (Immutable Object) 設計，並提供開箱即用的通用預設樣式 (DEFAULT, CONFIDENTIAL, DRAFT) 與 Fluent Builder 構造器。
 * </p>
 *
 * @param fontSize 字型大小 (pt，預設 46f)
 * @param opacity  不透明度 (0.0f 完全透明 ~ 1.0f 完全不透明，預設 0.07f)
 * @param color    文字填色 (AWT Color，預設 {@link Color#DARK_GRAY})
 * @param rotation 旋轉角度 (度數，預設 45f)
 * @param boxed    是否在文字外圍繪製防偽圓角鋼印邊框 (預設 false)
 * @param subText  可選之次級防偽標註文字 (如企業英文標語或單據識別，繪製於主文字下方)
 */
public record OpenPdfWatermarkStyle(
		float fontSize,
		float opacity,
		Color color,
		float rotation,
		boolean boxed,
		String subText
) {

	/**
	 * 預設標準防偽浮水印樣式 (46pt, 0.07 透明度, 深灰, 45度傾斜, 無外框)
	 */
	public static final OpenPdfWatermarkStyle DEFAULT = new OpenPdfWatermarkStyle(
			46f, 0.07f, Color.DARK_GRAY, 45f, false, null
	);

	/**
	 * 機密文件專用浮水印樣式 (50pt, 0.10 透明度, 醒目緋紅, 45度傾斜, 帶外框)
	 */
	public static final OpenPdfWatermarkStyle CONFIDENTIAL = new OpenPdfWatermarkStyle(
			48f, 0.10f, new Color(180, 40, 40), 45f, true, "CONFIDENTIAL & PROPRIETARY"
	);

	/**
	 * 草稿文件專用浮水印樣式 (46f, 0.08 透明度, 灰藍色, 45度傾斜, 無外框)
	 */
	public static final OpenPdfWatermarkStyle DRAFT = new OpenPdfWatermarkStyle(
			46f, 0.08f, new Color(70, 90, 120), 45f, false, null
	);

	/**
	 * 四參數便利建構子 (維持向下相容性)
	 */
	public OpenPdfWatermarkStyle(float fontSize, float opacity, Color color, float rotation) {
		this(fontSize, opacity, color, rotation, false, null);
	}

	/**
	 * 驗證並修正參數範圍
	 */
	public OpenPdfWatermarkStyle {
		if (fontSize <= 0f) {
			fontSize = 46f;
		}
		if (opacity < 0.0f || opacity > 1.0f) {
			opacity = 0.07f;
		}
		if (color == null) {
			color = Color.DARK_GRAY;
		}
	}

	/**
	 * 建立自訂浮水印樣式 Builder
	 *
	 * @return Builder 實例
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 複製當前樣式並調整字號
	 */
	public OpenPdfWatermarkStyle withFontSize(float newFontSize) {
		return new OpenPdfWatermarkStyle(newFontSize, this.opacity, this.color, this.rotation, this.boxed, this.subText);
	}

	/**
	 * 複製當前樣式並調整透明度
	 */
	public OpenPdfWatermarkStyle withOpacity(float newOpacity) {
		return new OpenPdfWatermarkStyle(this.fontSize, newOpacity, this.color, this.rotation, this.boxed, this.subText);
	}

	/**
	 * 複製當前樣式並調整顏色
	 */
	public OpenPdfWatermarkStyle withColor(Color newColor) {
		return new OpenPdfWatermarkStyle(this.fontSize, this.opacity, newColor, this.rotation, this.boxed, this.subText);
	}

	/**
	 * 複製當前樣式並調整旋轉角度
	 */
	public OpenPdfWatermarkStyle withRotation(float newRotation) {
		return new OpenPdfWatermarkStyle(this.fontSize, this.opacity, this.color, newRotation, this.boxed, this.subText);
	}

	/**
	 * 複製當前樣式並調整是否具備防偽邊框
	 */
	public OpenPdfWatermarkStyle withBoxed(boolean newBoxed) {
		return new OpenPdfWatermarkStyle(this.fontSize, this.opacity, this.color, this.rotation, newBoxed, this.subText);
	}

	/**
	 * 複製當前樣式並調整次級防偽標註
	 */
	public OpenPdfWatermarkStyle withSubText(String newSubText) {
		return new OpenPdfWatermarkStyle(this.fontSize, this.opacity, this.color, this.rotation, this.boxed, newSubText);
	}

	/**
	 * 浮水印樣式建構器 (Builder)
	 */
	public static class Builder {
		private float fontSize = 46f;
		private float opacity = 0.07f;
		private Color color = Color.DARK_GRAY;
		private float rotation = 45f;
		private boolean boxed = false;
		private String subText = null;

		public Builder fontSize(float fontSize) {
			this.fontSize = fontSize;
			return this;
		}

		public Builder opacity(float opacity) {
			this.opacity = opacity;
			return this;
		}

		public Builder color(Color color) {
			this.color = color;
			return this;
		}

		public Builder rotation(float rotation) {
			this.rotation = rotation;
			return this;
		}

		public Builder boxed(boolean boxed) {
			this.boxed = boxed;
			return this;
		}

		public Builder subText(String subText) {
			this.subText = subText;
			return this;
		}

		public OpenPdfWatermarkStyle build() {
			return new OpenPdfWatermarkStyle(fontSize, opacity, color, rotation, boxed, subText);
		}
	}
}
