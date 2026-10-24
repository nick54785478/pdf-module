package com.example.demo.infrastructure.openpdf.core;

import com.example.demo.config.PdfFontProperties;
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
 * 負責單例快取中文字體 (如微軟正黑體 Light) BaseFont 實例，
 * 避免每次產生 PDF 時重複解析 12MB 字體二進位資料造成記憶體與 CPU 飆高。
 * 字體路徑遵循六角形架構由 {@link PdfFontProperties} 外部化統一集中管理。
 * 統一封裝字體階層樣式（標題、副標、表頭、內文、金額等）。
 * </p>
 */
@Slf4j
@Component
public class OpenPdfFontProvider {

	private final BaseFont chineseBaseFont;
	private final PdfFontProperties fontProperties;

	/**
	 * Spring 依賴注入建構子
	 *
	 * @param fontProperties 字體配置屬性
	 */
	public OpenPdfFontProvider(PdfFontProperties fontProperties) {
		this.fontProperties = fontProperties != null ? fontProperties : new PdfFontProperties();
		try {
			com.lowagie.text.pdf.LayoutProcessor.disable();
		} catch (Throwable ignored) {
		}
		this.chineseBaseFont = initializeBaseFont();
	}

	/**
	 * 無參預設建構子，便於獨立單元測試或離線調用
	 */
	public OpenPdfFontProvider() {
		this(new PdfFontProperties());
	}

	/**
	 * 初始化並快取中文字體 BaseFont
	 *
	 * @return 已載入之 BaseFont 實例
	 */
	private BaseFont initializeBaseFont() {
		// 1. 優先由 Classpath (application.properties 之 pdf.font.path) 載入
		String classpathLocation = fontProperties.getPath();
		if (classpathLocation != null && !classpathLocation.isBlank()) {
			URL fontUrl = getClass().getClassLoader().getResource(classpathLocation);
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
					String fontName = resolveFontName(classpathLocation);
					return BaseFont.createFont(fontName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
				} catch (Exception e) {
					log.warn("由 Classpath 串流載入字體失敗", e);
				}
			}
		}

		// 2. 次選：嘗試直接由工作目錄相對路徑 (pdf.font.dev-fallback-path) 載入
		String devFallback = fontProperties.getDevFallbackPath();
		if (devFallback != null && !devFallback.isBlank()) {
			File directFont = new File(devFallback);
			if (directFont.exists()) {
				try {
					log.info("成功由相對路徑載入字體: {}", directFont.getAbsolutePath());
					return BaseFont.createFont(directFont.getAbsolutePath() + ",0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
				} catch (Exception e) {
					log.warn("由相對路徑載入字體失敗", e);
				}
			}
		}

		// 3. 備選：嘗試系統 Windows 字體庫 (pdf.font.system-fallback-path)
		String systemFallback = fontProperties.getSystemFallbackPath();
		if (systemFallback != null && !systemFallback.isBlank()) {
			File sysFont = new File(systemFallback);
			if (sysFont.exists()) {
				try {
					log.info("成功由 Windows 系統字體庫載入: {}", sysFont.getAbsolutePath());
					return BaseFont.createFont(sysFont.getAbsolutePath() + ",0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
				} catch (Exception e) {
					log.warn("由系統字體庫載入微軟正黑體失敗", e);
				}
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

	private String resolveFontName(String path) {
		if (path == null || path.isBlank()) {
			return "MSJHL.TTC,0";
		}
		int slashIndex = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		String fileName = slashIndex >= 0 ? path.substring(slashIndex + 1) : path;
		return fileName.toLowerCase().endsWith(".ttc") ? fileName + ",0" : fileName;
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
