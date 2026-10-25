package com.example.demo.infrastructure.openpdf.core;

import com.example.demo.config.PdfFontProperties;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OpenPDF 字體提供者與快取管理者單元測試 (Unit Test)
 * <p>
 * 遵循 AGENTS.md 規範：
 * 1. 驗證字體提供者在預設環境、多階備援策略 (Classpath, 相對路徑, 系統字體庫, HELVETICA 終極備援) 之運作正常。
 * 2. 驗證各階層 Typography 字級樣式產生（標題、副標、表頭、內文、金額等）。
 * 3. 測試方法遵循 BDD Gherkin 語法撰寫 Javadoc。
 * </p>
 */
class OpenPdfFontProviderTest {

	/**
	 * Feature: PDF 字體提供者預設初始化
	 * Scenario: 使用預設建構子初始化字體提供者
	 * Given 系統提供預設字體配置屬性
	 * When 初始化 OpenPdfFontProvider
	 * Then BaseFont 實例成功快取且各 Typography 字級樣式均正確產生
	 */
	@Test
	@DisplayName("驗證預設字體提供者初始化與各 Typography 字級產製正常")
	void shouldInitializeWithDefaultPropertiesAndProvideTypographyFonts() {
		// Given & When
		OpenPdfFontProvider provider = new OpenPdfFontProvider();

		// Then
		assertNotNull(provider.getBaseFont(), "快取之 BaseFont 不應為 null");
		assertNotNull(provider.getTitleFont(), "標題字型不應為 null");
		assertNotNull(provider.getDisplayTitleFont(), "展示大標題字型不應為 null");
		assertNotNull(provider.getQuotationTitleFont(), "向後相容報價單大標題字型不應為 null");
		assertNotNull(provider.getSubtitleFont(), "副標題字型不應為 null");
		assertNotNull(provider.getHeaderFont(), "表頭字型不應為 null");
		assertNotNull(provider.getBodyBold(), "內文粗體字型不應為 null");
		assertNotNull(provider.getBodyRegular(), "內文常規字型不應為 null");
		assertNotNull(provider.getBodyMuted(), "內文註解字型不應為 null");
		assertNotNull(provider.getTotalFont(), "總計強調字型不應為 null");

		Font customFont = provider.createFont(12f, Font.BOLD, Color.BLACK);
		assertNotNull(customFont);
		assertEquals(12f, customFont.getSize());
		assertEquals(Font.BOLD, customFont.getStyle());
	}

	/**
	 * Feature: PDF 字體多階備援降級機制
	 * Scenario: 當 Classpath 與相對路徑均不存在時降級至系統路徑或內建 Helvetica
	 * Given 配置無效的 Classpath 路徑與開發環境相對路徑
	 * When 初始化 OpenPdfFontProvider
	 * Then 能夠觸發備援策略成功載入系統字型或內建 HELVETICA 實例而不中斷
	 */
	@Test
	@DisplayName("驗證無效 Classpath 路徑時自動觸發降級備援機制")
	void shouldFallbackGracefullyWhenClasspathFontNotFound() {
		// Given: Classpath 與相對路徑皆設定為不存在之路徑
		PdfFontProperties properties = new PdfFontProperties(
				"non/existing/font.ttf",
				"non/existing/dev/font.ttf",
				"C:/Windows/Fonts/msjh.ttc"
		);

		// When
		OpenPdfFontProvider provider = new OpenPdfFontProvider(properties);

		// Then
		BaseFont baseFont = provider.getBaseFont();
		assertNotNull(baseFont, "即使前置路徑不存在，降級機制亦應成功提供 BaseFont");
	}

	/**
	 * Feature: PDF 終極降級至內建 Helvetica
	 * Scenario: 當所有字體檔案路徑皆無效時
	 * Given 所有外部檔案與系統路徑均為無效路徑
	 * When 初始化 OpenPdfFontProvider
	 * Then 自動降級至內建 HELVETICA 字型
	 */
	@Test
	@DisplayName("驗證所有路徑均無效時降級至 HELVETICA 內建字型")
	void shouldFallbackToHelveticaWhenAllCustomPathsAreInvalid() {
		// Given: 所有路徑均不存在
		PdfFontProperties properties = new PdfFontProperties(
				"invalid/path1.ttf",
				"invalid/path2.ttf",
				"invalid/path3.ttf"
		);

		// When
		OpenPdfFontProvider provider = new OpenPdfFontProvider(properties);

		// Then
		BaseFont baseFont = provider.getBaseFont();
		assertNotNull(baseFont, "應成功降級至內建 HELVETICA 字型");
		assertTrue(baseFont.getPostscriptFontName().toUpperCase().contains("HELVETICA"),
				"當無實體字型時應降級使用 HELVETICA");
	}
}
