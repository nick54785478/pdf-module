package com.example.demo.infrastructure.adapter;

import com.example.demo.application.port.out.ReportGeneratorPort;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.EngineException;
import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportEngineFactory;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.PDFRenderOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Eclipse BIRT 報表引擎輸出適配器 (Outbound Adapter)
 * <p>
 * 遵循六角形架構規範：
 * 1. 位於基礎設施層 {@code infrastructure.adapter}。
 * 2. 實作應用層 {@link ReportGeneratorPort} 業務輸出端口。
 * 3. 命名規範結尾為 {@code Adapter}。
 * 4. 封裝 Eclipse BIRT Engine 的完整生命週期管理（透過 Spring 託管 {@code @PostConstruct} 與 {@code @PreDestroy}），
 *    避免原先 static 靜態類別造成的資源洩漏與難以測試的問題。
 */
@Slf4j
@Component
public class BirtReportAdapter implements ReportGeneratorPort {

	private IReportEngine birtReportEngine;

	/**
	 * 初始化 BIRT 報表引擎
	 */
	@PostConstruct
	public void init() {
		EngineConfig config = new EngineConfig();
		try {
			// 取得 fontsConfig.xml 字體配置檔 (須放在 src/main/resources 目錄下)
			URL url = getClass().getClassLoader().getResource("fontsConfig.xml");
			if (url != null) {
				config.setFontConfig(url);
			}

			Platform.startup(config);
			IReportEngineFactory factory = (IReportEngineFactory) Platform
					.createFactoryObject(IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY);

			if (factory != null) {
				this.birtReportEngine = factory.createReportEngine(config);
				log.info("BIRT Report Engine 初始化成功");
			} else {
				log.warn("無法取得 BIRT Report Engine Factory");
			}
		} catch (Exception e) {
			log.error("BIRT Engine 初始化失敗: ", e);
		}
	}

	/**
	 * 關閉並釋放 BIRT 報表引擎資源
	 */
	@PreDestroy
	public void destroy() {
		if (birtReportEngine != null) {
			try {
				birtReportEngine.destroy();
			} catch (Exception e) {
				log.warn("關閉 BIRT Engine 發生錯誤: ", e);
			}
		}
		try {
			Platform.shutdown();
			log.info("BIRT Platform 已安全關閉");
		} catch (Exception e) {
			log.warn("關閉 BIRT Platform 發生錯誤: ", e);
		}
	}

	/**
	 * 建立 PDF 報表 (指定路徑與檔名)
	 *
	 * @param filePath    檔案路徑
	 * @param fileName    檔案名稱
	 * @param parameters  報表參數
	 * @param dataContext 遍歷渲染內容資料上下文
	 * @return PDF 位元組陣列
	 */
	@Override
	public byte[] generatePdfReport(String filePath, String fileName, Map<String, Object> parameters,
			Map<String, Object> dataContext) {
		InputStream inputStream = getResourceInputStream(filePath, fileName);
		if (inputStream == null) {
			log.error("讀取報表設計檔失敗，檔案不存在: filePath={}, fileName={}", filePath, fileName);
			return null;
		}
		return generatePdfReport(inputStream, parameters, dataContext);
	}

	/**
	 * 產生 PDF 格式的報表 (從 InputStream)
	 *
	 * @param inputStream 報表設計檔的 InputStream (.rptdesign)
	 * @param params      報表參數 Map
	 * @param dataContext 遍歷渲染內容資料上下文
	 * @return PDF 位元組陣列
	 */
	@Override
	public byte[] generatePdfReport(InputStream inputStream, Map<String, Object> params,
			Map<String, Object> dataContext) {
		if (birtReportEngine == null) {
			log.error("BIRT Engine 尚未初始化完成，無法生成報表");
			return null;
		}

		IRunAndRenderTask task = null;
		try {
			// 載入報表設計檔
			IReportRunnable design = birtReportEngine.openReportDesign(inputStream);

			// 建立報表執行與渲染任務
			task = birtReportEngine.createRunAndRenderTask(design);

			// 設定報表參數
			if (params != null && !params.isEmpty()) {
				params.forEach(task::setParameterValue);
			}

			// 設定報表 AppContext
			if (dataContext != null && !dataContext.isEmpty()) {
				task.getAppContext().putAll(dataContext);
			}

			// 設定輸出格式與輸出流
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			PDFRenderOption options = new PDFRenderOption();
			options.setOutputFormat("pdf");
			options.setOutputStream(bos);
			task.setRenderOption(options);

			// 執行報表渲染任務
			task.run();
			return bos.toByteArray();
		} catch (EngineException e) {
			log.error("發生錯誤，BIRT PDF 報表轉換失敗: ", e);
			return null;
		} finally {
			if (task != null) {
				task.close();
			}
		}
	}

	/**
	 * 產生 HTML 格式的報表
	 *
	 * @param inputStream 報表設計檔的 InputStream (.rptdesign)
	 * @param parameters  報表參數 Map
	 * @return HTML 位元組陣列
	 */
	@Override
	public byte[] generateHtmlReport(InputStream inputStream, Map<String, Object> parameters) {
		if (birtReportEngine == null) {
			log.error("BIRT Engine 尚未初始化完成，無法生成報表");
			return null;
		}

		IRunAndRenderTask task = null;
		try {
			// 開啟報表設計檔
			IReportRunnable design = birtReportEngine.openReportDesign(inputStream);

			// 建立執行及渲染任務
			task = birtReportEngine.createRunAndRenderTask(design);

			// 將傳入的參數設定給報表任務
			if (parameters != null) {
				for (Map.Entry<String, Object> entry : parameters.entrySet()) {
					task.setParameterValue(entry.getKey(), entry.getValue());
				}
			}
			System.setProperty("birt.viewer.fonts", "msjh.ttf");

			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			// 建立 HTML 專用的渲染選項
			HTMLRenderOption options = new HTMLRenderOption();
			options.setOutputFormat("html");
			options.setOutputStream(bos);
			task.setRenderOption(options);

			// 執行報表產生與渲染
			task.run();
			return bos.toByteArray();
		} catch (EngineException e) {
			log.error("發生錯誤，BIRT HTML 報表轉換失敗: ", e);
			return null;
		} finally {
			if (task != null) {
				task.close();
			}
		}
	}

	/**
	 * 從 Classpath 讀取檔案
	 *
	 * @param filePath 檔案路徑
	 * @param fileName 檔案名稱
	 * @return 資料流
	 */
	@Override
	public InputStream getResourceInputStream(String filePath, String fileName) {
		String url = assemblePath(filePath, fileName);
		log.debug("讀取報表設計檔資源路徑: {}", url);
		return getClass().getResourceAsStream(url);
	}

	/**
	 * 組合路徑，自動清理多餘的斜線，忽略 null 與空字串，確保開頭為 / 且結尾為 .rptdesign
	 *
	 * @param segments 路徑片段
	 * @return 組合後的路徑字串
	 */
	private String assemblePath(String... segments) {
		String joined = Arrays.stream(segments)
				.filter(Objects::nonNull)
				.map(s -> s.replaceAll("^/+", "").replaceAll("/+$", ""))
				.filter(s -> !s.isEmpty())
				.collect(Collectors.joining("/"));

		if (!joined.startsWith("/")) {
			joined = "/" + joined;
		}
		if (!joined.endsWith(".rptdesign")) {
			joined += ".rptdesign";
		}
		return joined;
	}
}
