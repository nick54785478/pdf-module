package com.example.demo.application.port.out;

import java.io.InputStream;
import java.util.Map;

/**
 * 報表生成輸出端口 (Report Generator Outbound Port)
 * <p>
 * 遵循六角形架構規範：
 * 1. 位於應用層 {@code application.port.out}，以業務用途命名，定義報表生成能力的對外合約。
 * 2. 命名為純業務語意，嚴禁帶有特定技術庫名稱 (如 BIRT、Jasper 等)。
 * 3. 介面命名結尾必須為 {@code Port}。
 * 4. 嚴禁在方法簽名中接發技術類物件 (如 Spring {@code ByteArrayResource} 或特定第三方套件例外)。
 *    報表輸出格式統一採用 Java 標準原生 {@code byte[]}。
 */
public interface ReportGeneratorPort {

	/**
	 * 從指定路徑與檔名讀取報表設計檔，並產生 PDF 格式報表
	 *
	 * @param filePath    報表檔案路徑 (例如：reports/)
	 * @param fileName    報表檔案名稱 (例如：sample 或 sample.rptdesign)
	 * @param parameters  報表參數 Map (key: 參數名稱, value: 參數值)
	 * @param dataContext 報表資料上下文 Map (key: 名稱, value: 供報表腳本遍歷之資料物件/清單)
	 * @return 產出之 PDF 檔案位元組陣列 (byte[])，若失敗則回傳 null
	 */
	byte[] generatePdfReport(String filePath, String fileName, Map<String, Object> parameters,
			Map<String, Object> dataContext);

	/**
	 * 從資料流讀取報表設計檔，並產生 PDF 格式報表
	 *
	 * @param inputStream 報表設計檔輸入流
	 * @param parameters  報表參數 Map (key: 參數名稱, value: 參數值)
	 * @param dataContext 報表資料上下文 Map (key: 名稱, value: 供報表腳本遍歷之資料物件/清單)
	 * @return 產出之 PDF 檔案位元組陣列 (byte[])，若失敗則回傳 null
	 */
	byte[] generatePdfReport(InputStream inputStream, Map<String, Object> parameters,
			Map<String, Object> dataContext);

	/**
	 * 從資料流讀取報表設計檔，並產生 HTML 格式報表
	 *
	 * @param inputStream 報表設計檔輸入流
	 * @param parameters  報表參數 Map (key: 參數名稱, value: 參數值)
	 * @return 產出之 HTML 檔案位元組陣列 (byte[])，若失敗則回傳 null
	 */
	byte[] generateHtmlReport(InputStream inputStream, Map<String, Object> parameters);

	/**
	 * 從 Classpath 讀取報表設計檔案輸入流
	 *
	 * @param filePath 檔案路徑
	 * @param fileName 檔案名稱
	 * @return 報表設計檔輸入流，若檔案不存在則回傳 null
	 */
	InputStream getResourceInputStream(String filePath, String fileName);
}
