package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OpenAPI 配置類別單元測試 (Config Test)
 * <p>
 * 遵循 AGENTS.md 測試規範，以 BDD Gherkin 語法撰寫測試案例註解。
 * </p>
 */
class OpenApiConfigTest {

	private final OpenApiConfig openApiConfig = new OpenApiConfig();

	/**
	 * Feature: OpenAPI 3.0 元資料配置
	 * Scenario: 自訂 OpenAPI Bean 正確產出包含標題、版本、聯絡人與授權條款之元資料物件
	 *
	 * Given: 建立 OpenApiConfig 實體
	 * When: 調用 customOpenAPI() 方法
	 * Then: 回傳非空 OpenAPI 物件，標題包含 "pdf-module"，版本為 "1.0.0"，且包含有效的聯絡人與授權資訊
	 */
	@Test
	@DisplayName("驗證OpenAPI配置-正常情境-成功建立包含完整Info元資料之OpenAPI物件")
	void customOpenAPI_success() {
		// When
		OpenAPI openAPI = openApiConfig.customOpenAPI();

		// Then
		assertNotNull(openAPI, "OpenAPI 實體不得為空");
		assertNotNull(openAPI.getInfo(), "Info 元資料不得為空");
		assertTrue(openAPI.getInfo().getTitle().contains("pdf-module"), "標題應包含 pdf-module");
		assertEquals("1.0.0", openAPI.getInfo().getVersion(), "版本應為 1.0.0");
		assertNotNull(openAPI.getInfo().getContact(), "聯絡人資訊不得為空");
		assertNotNull(openAPI.getInfo().getLicense(), "授權條款資訊不得為空");
	}
}
