package com.example.demo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * PDF 字體配置屬性單元與整合測試 (Configuration Test)
 * <p>
 * 遵循 AGENTS.md 測試規範，以 BDD Gherkin 語法撰寫測試案例註解。
 * 驗證字型路徑配置屬性在 Spring 環境下的外部化綁定、預設值與自定義屬性變更能力。
 * </p>
 */
@SpringBootTest
class PdfFontPropertiesTest {

	@Autowired
	private PdfFontProperties fontProperties;

	/**
	 * Feature: PDF 字型路徑配置外部化讀取
	 * Scenario: Spring Boot 啟動時正確自 application.properties 注入 pdf.font.* 配置屬性
	 *
	 * Given: Spring 測試容器啟動並注入 PdfFontProperties Bean
	 * When: 檢驗注入之字型路徑屬性值
	 * Then: 應成功讀取 application.properties 所設定之值，且各路徑不得為空
	 */
	@Test
	@DisplayName("驗證PDF字型配置-Spring環境注入-成功自application.properties載入配置屬性")
	void fontProperties_springContextInjection_success() {
		assertNotNull(fontProperties, "PdfFontProperties Bean 不得為空");
		assertEquals("fonts/MSJHL.TTC", fontProperties.getPath(), "Classpath 字型路徑應與 application.properties 相符");
		assertEquals("src/main/resources/fonts/MSJHL.TTC", fontProperties.getDevFallbackPath(), "本機相對備援路徑應相符");
		assertEquals("C:/Windows/Fonts/msjh.ttc", fontProperties.getSystemFallbackPath(), "系統字型備援路徑應相符");
	}

	/**
	 * Feature: PDF 字型配置物件純 Java 預設行為
	 * Scenario: 獨立 new 出實例時具有預設的微軟正黑體備援路徑，支援離線單元測試
	 *
	 * Given: 直接透過 new PdfFontProperties() 建立物件
	 * When: 讀取各項屬性
	 * Then: path 預設為 "fonts/MSJHL.TTC"，devFallbackPath 預設為 "src/main/resources/fonts/MSJHL.TTC"，systemFallbackPath 預設為 "C:/Windows/Fonts/msjh.ttc"
	 */
	@Test
	@DisplayName("驗證PDF字型配置-預設建構子-具備標準預設字型備援路徑")
	void fontProperties_defaultConstructor_success() {
		PdfFontProperties properties = new PdfFontProperties();

		assertEquals("fonts/MSJHL.TTC", properties.getPath());
		assertEquals("src/main/resources/fonts/MSJHL.TTC", properties.getDevFallbackPath());
		assertEquals("C:/Windows/Fonts/msjh.ttc", properties.getSystemFallbackPath());
	}

	/**
	 * Feature: PDF 字型配置全參數建構與 Setter 異動
	 * Scenario: 自定義設定各路徑，驗證屬性更新正確
	 *
	 * Given: 透過全參數建構子初始化自定義字型路徑
	 * When: 呼叫 setter 異動路徑並重新讀取
	 * Then: 回傳值應精準反映所設定之新字型路徑
	 */
	@Test
	@DisplayName("驗證PDF字型配置-全參數與Setter-正確反映自定義字型路徑變更")
	void fontProperties_customValuesAndSetters_success() {
		PdfFontProperties properties = new PdfFontProperties(
				"custom/fonts/font.ttf",
				"dev/fonts/font.ttf",
				"/usr/share/fonts/font.ttf"
		);

		assertEquals("custom/fonts/font.ttf", properties.getPath());
		assertEquals("dev/fonts/font.ttf", properties.getDevFallbackPath());
		assertEquals("/usr/share/fonts/font.ttf", properties.getSystemFallbackPath());

		properties.setPath("updated/font.ttc");
		properties.setDevFallbackPath("updated/dev.ttc");
		properties.setSystemFallbackPath("updated/sys.ttc");

		assertEquals("updated/font.ttc", properties.getPath());
		assertEquals("updated/dev.ttc", properties.getDevFallbackPath());
		assertEquals("updated/sys.ttc", properties.getSystemFallbackPath());
	}
}
