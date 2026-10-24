package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 與 Swagger UI 全域配置類別 (Configuration Layer)
 * <p>
 * 遵循 Clean Architecture 架構原則，本類別位於技術配置層 ({@code config})，
 * 負責配置 SpringDoc OpenAPI 規範與 API 文件元資料 (Metadata)。
 * 透過集中式設定將表現層對外暴露之 RESTful API 規格自動發布為標準 OpenAPI 3.0 定義。
 * </p>
 */
@Configuration
public class OpenApiConfig {

	/**
	 * 建構自訂 OpenAPI 規格定義 Bean
	 * <p>
	 * 提供 PDF 處理模組之系統標題、詳細說明、版本資訊、聯絡窗口與開源授權聲明。
	 * </p>
	 *
	 * @return OpenAPI 規格實體物件
	 */
	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("PDF 模組 RESTful API (pdf-module)")
						.version("1.0.0")
						.description("基於 Spring Boot 4 與 Clean Architecture / 六角形架構打造之 PDF 處理模組。<br/>"
								+ "提供高精準度 OpenPDF (3.0.5) 報表文件渲染、Apache PDFBox (3.0.8) 內容抽取與文字辨識等服務。")
						.contact(new Contact()
								.name("PDF 模組開發團隊")
								.email("dev-team@example.com"))
						.license(new License()
								.name("Apache 2.0")
								.url("https://www.apache.org/licenses/LICENSE-2.0.html")));
	}
}
