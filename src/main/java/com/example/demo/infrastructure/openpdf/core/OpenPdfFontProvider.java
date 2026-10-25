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
	 * <p>
	 * 依序嘗試以下來源策略進行字體載入：
	 * <ol>
	 *   <li>Classpath (application.properties 之 pdf.font.path)</li>
	 *   <li>開發環境工作目錄相對路徑 (pdf.font.dev-fallback-path)</li>
	 *   <li>Windows 系統字體庫 (pdf.font.system-fallback-path)</li>
	 *   <li>標準 HELVETICA 內建字體（最後備選降級）</li>
	 * </ol>
	 * </p>
	 *
	 * @return 已載入之 BaseFont 實例
	 */
	private BaseFont initializeBaseFont() {
		BaseFont font = loadFontFromClasspath(fontProperties.getPath());
		if (font != null) {
			return font;
		}

		font = loadFontFromFile(fontProperties.getDevFallbackPath(), "相對路徑");
		if (font != null) {
			return font;
		}

		font = loadFontFromFile(fontProperties.getSystemFallbackPath(), "Windows 系統字體庫");
		if (font != null) {
			return font;
		}

		return loadFallbackHelvetica();
	}

	/**
	 * 由 Classpath 載入字體（優先嘗試實體檔案，若為 JAR 內打包資源則自動退回為串流讀取）
	 *
	 * @param classpathLocation Classpath 相對資源路徑
	 * @return BaseFont 實例，若路徑無效或載入失敗則回傳 null
	 */
	private BaseFont loadFontFromClasspath(String classpathLocation) {
		if (classpathLocation == null || classpathLocation.isBlank()) {
			return null;
		}
		URL fontUrl = getClass().getClassLoader().getResource(classpathLocation);
		if (fontUrl == null) {
			return null;
		}

		BaseFont font = tryLoadClasspathFile(fontUrl);
		if (font != null) {
			return font;
		}

		return tryLoadClasspathStream(fontUrl, classpathLocation);
	}

	/**
	 * 嘗試將 Classpath URL 轉為實體檔案載入字體（適用於未打包 IDE 執行環境）
	 *
	 * @param fontUrl Classpath 資源 URL
	 * @return BaseFont 實例，若無法轉為 File 或不存在則回傳 null
	 */
	private BaseFont tryLoadClasspathFile(URL fontUrl) {
		try {
			File fontFile = new File(fontUrl.toURI());
			if (fontFile.exists()) {
				log.info("成功由 Classpath 檔案路徑載入字體: {}", fontFile.getAbsolutePath());
				return BaseFont.createFont(fontFile.getAbsolutePath() + ",0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
			}
		} catch (Exception ignored) {
			// 若為 jar 內資源，URI 可能無法轉為 File，由串流接手
		}
		return null;
	}

	/**
	 * 透過 InputStream 串流方式自 Classpath URL 讀取字體二進位資料並註冊
	 *
	 * @param fontUrl           Classpath 資源 URL
	 * @param classpathLocation 原始資源路徑（用於解析字體名稱）
	 * @return BaseFont 實例，若讀取或註冊失敗則回傳 null
	 */
	private BaseFont tryLoadClasspathStream(URL fontUrl, String classpathLocation) {
		try (InputStream is = fontUrl.openStream()) {
			byte[] fontBytes = is.readAllBytes();
			log.info("成功由 Classpath 串流載入字體 (bytes={})", fontBytes.length);
			String fontName = resolveFontName(classpathLocation);
			return BaseFont.createFont(fontName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
		} catch (Exception e) {
			log.warn("由 Classpath 串流載入字體失敗", e);
			return null;
		}
	}

	/**
	 * 嘗試由指定實體檔案路徑載入字體（共用於工作目錄相對路徑與系統字體庫路徑）
	 *
	 * @param path              檔案路徑
	 * @param sourceDescription 來源描述（用於 Log 追蹤輸出）
	 * @return BaseFont 實例，若路徑為空、檔案不存在或載入失敗則回傳 null
	 */
	private BaseFont loadFontFromFile(String path, String sourceDescription) {
		if (path == null || path.isBlank()) {
			return null;
		}
		File fontFile = new File(path);
		if (!fontFile.exists()) {
			return null;
		}
		try {
			log.info("成功由 {} 載入: {}", sourceDescription, fontFile.getAbsolutePath());
			return BaseFont.createFont(fontFile.getAbsolutePath() + ",0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
		} catch (Exception e) {
			log.warn("由 {} 載入字體失敗", sourceDescription, e);
			return null;
		}
	}

	/**
	 * 載入預設 HELVETICA 字體作為終極備援
	 *
	 * @return HELVETICA BaseFont 實例
	 * @throws IllegalStateException 若連內建字體亦無法載入時拋出
	 */
	private BaseFont loadFallbackHelvetica() {
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
	 * 取得展示型超大標題字體 (22pt 粗體 裝飾藍)
	 * <p>
	 * 適用於報表頂部視覺主標題（如「報價單」、「出貨驗收單」、「工單」等展示大標）。
	 * </p>
	 *
	 * @return 22pt 粗體字型實例
	 */
	public Font getDisplayTitleFont() {
		return createFont(22f, Font.BOLD, OpenPdfStylePalette.ACCENT_BLUE);
	}

	/**
	 * @deprecated 請改用通用層級命名 {@link #getDisplayTitleFont()}，避免字型提供者綁定特定業務單據名稱。
	 */
	@Deprecated
	public Font getQuotationTitleFont() {
		return getDisplayTitleFont();
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
