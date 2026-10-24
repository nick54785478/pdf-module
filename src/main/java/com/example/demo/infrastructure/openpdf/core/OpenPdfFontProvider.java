package com.example.demo.infrastructure.openpdf.core;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * OpenPDF 字體提供者與快取管理者 (Font Provider)
 * <p>
 * 負責單例快取微軟正黑體 Light (MSJHL.TTC) BaseFont 實例，
 * 避免每次產生 PDF 時重複解析 12MB 字體二進位資料造成記憶體與 CPU 飆高。
 * 統一封裝字體階層樣式（標題、副標、表頭、內文、金額等）。
 * </p>
 */
@Slf4j
@Component
public class OpenPdfFontProvider {

	private final BaseFont chineseBaseFont;

	public OpenPdfFontProvider() {
		try {
			com.lowagie.text.pdf.LayoutProcessor.disable();
		} catch (Throwable ignored) {
		}
		this.chineseBaseFont = initializeBaseFont();
	}

	/**
	 * 初始化並快取中文字體 BaseFont
	 *
	 * @return 已載入之 BaseFont 實例
	 */
	private BaseFont initializeBaseFont() {
		// 1. 優先由 Classpath (src/main/resources/fonts/MSJHL.TTC) 載入
		URL fontUrl = getClass().getClassLoader().getResource("fonts/MSJHL.TTC");
		if (fontUrl != null) {
			try {
				File fontFile = new File(fontUrl.toURI());
				if (fontFile.exists()) {
					log.info("成功由 Classpath 檔案路徑載入字體: {}", fontFile.getAbsolutePath());
					return BaseFont.createFont(fontFile.getAbsolutePath() + ",0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
				}
			} catch (Exception ignored) {
				// 若為 jar 內資源，URI 可能無法轉為 File，由串流接手
			}

			try (InputStream is = fontUrl.openStream()) {
				byte[] fontBytes = is.readAllBytes();
				log.info("成功由 Classpath 串流載入字體 (bytes={})", fontBytes.length);
				return BaseFont.createFont("MSJHL.TTC,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
			} catch (Exception e) {
				log.warn("由 Classpath 串流載入字體失敗", e);
			}
		}

		// 2. 次選：嘗試直接由工作目錄相對路徑載入
		File directFont = new File("src/main/resources/fonts/MSJHL.TTC");
		if (directFont.exists()) {
			try {
				log.info("成功由相對路徑載入字體: {}", directFont.getAbsolutePath());
				return BaseFont.createFont(directFont.getAbsolutePath() + ",0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
			} catch (Exception e) {
				log.warn("由相對路徑載入字體失敗", e);
			}
		}

		// 3. 備選：嘗試系統 Windows 字體庫
		File sysFont = new File("C:/Windows/Fonts/msjh.ttc");
		if (sysFont.exists()) {
			try {
				log.info("成功由 Windows 系統字體庫載入: {}", sysFont.getAbsolutePath());
				return BaseFont.createFont(sysFont.getAbsolutePath() + ",0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
			} catch (Exception e) {
				log.warn("由系統字體庫載入微軟正黑體失敗", e);
			}
		}

		// 4. 最後備選：標準 Helvetica
		try {
			log.error("無法載入微軟正黑體，降級使用內建 HELVETICA (不支援中文渲染)");
			return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
		} catch (Exception e) {
			throw new IllegalStateException("無法初始化任何 PDF 字體", e);
		}
	}

	/**
	 * 取得底層快取之 BaseFont 實例
	 *
	 * @return BaseFont 物件
	 */
	public BaseFont getBaseFont() {
		return chineseBaseFont;
	}

	/**
	 * 自訂建立字體實例
	 *
	 * @param size  字體大小 (點數 pt)
	 * @param style 字體樣式 (Font.NORMAL, Font.BOLD 等)
	 * @param color 字體顏色
	 * @return Font 物件
	 */
	public Font createFont(float size, int style, Color color) {
		return new Font(chineseBaseFont, size, style, color);
	}

	/**
	 * 取得文件主標題字體 (18pt 粗體 主品牌深藍)
	 */
	public Font getTitleFont() {
		return createFont(18f, Font.BOLD, OpenPdfStylePalette.PRIMARY_NAVY);
	}

	/**
	 * 取得報價單強調主標字體 (22pt 粗體 裝飾藍)
	 */
	public Font getQuotationTitleFont() {
		return createFont(22f, Font.BOLD, OpenPdfStylePalette.ACCENT_BLUE);
	}

	/**
	 * 取得副標題文字字體 (8.5pt 常規 次要文字灰)
	 */
	public Font getSubtitleFont() {
		return createFont(8.5f, Font.NORMAL, OpenPdfStylePalette.TEXT_MUTED);
	}

	/**
	 * 取得表格主表頭字體 (8.5pt 粗體 白色)
	 */
	public Font getHeaderFont() {
		return createFont(8.5f, Font.BOLD, Color.WHITE);
	}

	/**
	 * 取得內文粗體字體 (8pt 粗體 深黑灰)
	 */
	public Font getBodyBold() {
		return createFont(8f, Font.BOLD, OpenPdfStylePalette.TEXT_DARK);
	}

	/**
	 * 取得內文常規字體 (8pt 常規 深黑灰)
	 */
	public Font getBodyRegular() {
		return createFont(8f, Font.NORMAL, OpenPdfStylePalette.TEXT_DARK);
	}

	/**
	 * 取得內文註解/淡化字體 (7.5pt 常規 次要文字灰)
	 */
	public Font getBodyMuted() {
		return createFont(7.5f, Font.NORMAL, OpenPdfStylePalette.TEXT_MUTED);
	}

	/**
	 * 取得總金額強調字體 (11pt 粗體 警示紅)
	 */
	public Font getTotalFont() {
		return createFont(11f, Font.BOLD, OpenPdfStylePalette.HIGHLIGHT_RED);
	}
}
