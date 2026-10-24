package com.example.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * PDF 字體配置屬性類 (PDF Font Configuration Properties)
 * <p>
 * 屬於六角形架構中的配置層 (Configuration Layer - The Dirtiest Layer)，
 * 統一封裝外部化於 {@code application.properties} 中以 {@code pdf.font.*} 為前綴的字型路徑相關配置。
 * </p>
 * <p>
 * <b>架構與設計職責遵循：</b>
 * <ul>
 *   <li><b>DDD 領域獨立性</b>：嚴格限制字型路徑技術設定不得滲透入 {@code domain/} 或 {@code application/}。</li>
 *   <li><b>六角形架構收斂</b>：作為全域配置 Bean，由 {@code config/} 層集中管理，並依賴注入至基礎設施層 (Infrastructure Layer) 供字型提供者使用。</li>
 *   <li><b>高容錯性階層式字型備援</b>：提供 Classpath 內嵌字型路徑、本機開發相對路徑與作業系統本機系統字型路徑的多層備援機制。</li>
 * </ul>
 * </p>
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "pdf.font")
public class PdfFontProperties {

	/**
	 * Classpath 內嵌字型資源相對路徑 (預設: fonts/MSJHL.TTC)
	 */
	private String path = "fonts/MSJHL.TTC";

	/**
	 * 開發環境本機資源相對路徑備援 (預設: src/main/resources/fonts/MSJHL.TTC)
	 */
	private String devFallbackPath = "src/main/resources/fonts/MSJHL.TTC";

	/**
	 * 作業系統系統字型絕對路徑備援 (預設: C:/Windows/Fonts/msjh.ttc)
	 */
	private String systemFallbackPath = "C:/Windows/Fonts/msjh.ttc";

	/**
	 * 預設建構子，維持所有預設路徑
	 */
	public PdfFontProperties() {
	}

	/**
	 * 全參數建構子，便於單元測試或自定義組態注入
	 *
	 * @param path               Classpath 內嵌字型資源相對路徑
	 * @param devFallbackPath    開發環境相對路徑備援
	 * @param systemFallbackPath 作業系統本機字型絕對路徑備援
	 */
	public PdfFontProperties(String path, String devFallbackPath, String systemFallbackPath) {
		this.path = path;
		this.devFallbackPath = devFallbackPath;
		this.systemFallbackPath = systemFallbackPath;
	}
}
