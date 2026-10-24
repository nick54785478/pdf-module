package com.example.demo.application.port.out;

import com.example.demo.application.shared.dto.QuotationGottenResult;

/**
 * 報價單內容解析外向端口 (Outbound Port)
 * <p>
 * 遵循 Clean Architecture 與六角形架構規範：
 * 1. 位於應用層 {@code application.port.out}，代表應用層要求基礎設施提供的報價單格式剖析合約。
 * 2. 嚴格遵守零技術框架依賴與防腐原則，方法簽名僅接收原生 {@link String} 文字並回傳應用層純資料載體 {@link QuotationGottenResult}。
 * 3. 遵從命名規範 3-8：Outbound Port 一律以 {@code Port} 結尾。
 * </p>
 */
public interface QuotationParserPort {

	/**
	 * 將從報價單文件抽取之原始文字內容解析為結構化之報價單資料傳輸物件
	 *
	 * @param rawText PDF 或文件抽取所得之原始文字字串
	 * @return 結構化報價單完整資訊資料傳輸物件 (QuotationGottenResult)
	 */
	QuotationGottenResult parse(String rawText);
}
