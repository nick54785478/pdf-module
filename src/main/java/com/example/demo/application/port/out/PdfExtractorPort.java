package com.example.demo.application.port.out;

import com.example.demo.application.shared.dto.PdfColumnRect;
import com.example.demo.application.shared.dto.PdfTextPositionGottenResult;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * PDF 內容抽取輸出端口 (Outbound Port)
 * <p>
 * 遵循六角形架構規範：
 * 1. 位於應用層 {@code application.port.out}，作為應用層對外要求的合約介面。
 * 2. 介面命名結尾必須為 {@code Port}。
 * 3. 嚴禁在方法簽名中接發技術類物件 (如 Spring {@code MultipartFile} 或資料庫相關物件)。
 */
public interface PdfExtractorPort {

	/**
	 * 讀取 PDF 文字內容 (全部頁數)
	 *
	 * @param inputStream PDF 資料輸入流
	 * @return PDF 文字內容字串
	 */
	String readPdfContent(InputStream inputStream);

	/**
	 * 讀取 PDF 指定頁數範圍的文字內容
	 *
	 * @param inputStream PDF 資料輸入流
	 * @param startPage   起始頁碼 (從 1 開始)
	 * @param endPage     結束頁碼
	 * @return 指定範圍之 PDF 文字內容
	 */
	String readPdfContent(InputStream inputStream, int startPage, int endPage);

	/**
	 * 逐頁抽取 PDF 文字內容
	 *
	 * @param inputStream PDF 資料輸入流
	 * @return Map&lt;頁碼, 該頁文字內容&gt;
	 */
	Map<Integer, String> readPdfByPage(InputStream inputStream);

	/**
	 * 從實體檔案路徑讀取 PDF 文字內容
	 *
	 * @param path 檔案絕對或相對路徑
	 * @return PDF 文字內容
	 */
	String readPdfFromPath(String path);

	/**
	 * 讀取 PDF 指定頁面指定區域之文字
	 *
	 * @param inputStream PDF 資料輸入流
	 * @param pageNo      頁碼 (從 1 開始)
	 * @param x           區域左上角 X 座標
	 * @param y           區域左上角 Y 座標
	 * @param width       區域寬度
	 * @param height      區域高度
	 * @return 該區域萃取之文字內容
	 */
	String readPdfArea(InputStream inputStream, int pageNo, float x, float y, float width, float height);

	/**
	 * 自動抽取整頁表格 (依列高度迭代偵測)
	 *
	 * @param inputStream PDF 資料輸入流
	 * @param pageNo      頁碼 (從 1 開始)
	 * @param colRects    每個欄位的水平座標與寬度定義清單
	 * @param startY      首列左上角 Y 座標
	 * @param rowHeight   每列高度
	 * @return 表格資料二維清單 (外層為列，內層為欄)
	 */
	List<List<String>> extractFullPageTable(InputStream inputStream, int pageNo,
			List<PdfColumnRect> colRects, float startY, float rowHeight);

	/**
	 * 抽取 PDF 指定頁所有文字與精確座標資訊
	 *
	 * @param inputStream PDF 資料輸入流
	 * @param pageNo      頁碼 (從 1 開始)
	 * @return 文字位置資訊清單 (封裝為 {@link PdfTextPositionGottenResult})
	 */
	List<PdfTextPositionGottenResult> extractTextPositions(InputStream inputStream, int pageNo);

	/**
	 * 從 PDF 指定頁依座標容差抽取表格資料 (未過濾非表格列)
	 *
	 * @param inputStream     PDF 資料輸入流
	 * @param pageNo          要抽取的頁碼 (從 1 開始)
	 * @param yTolerance      同一列 Y 座標容差 (pt)
	 * @param xMergeThreshold 合併同欄文字的 X 座標容差 (pt)
	 * @return 表格資料二維清單
	 */
	List<List<String>> extractTable(InputStream inputStream, int pageNo, float yTolerance,
			float xMergeThreshold);

	/**
	 * 從 PDF 指定頁依座標容差抽取表格資料，過濾非表格列並補齊預期欄位數
	 *
	 * @param inputStream     PDF 資料輸入流
	 * @param pageNo          要抽取的頁碼 (從 1 開始)
	 * @param yTolerance      同一列 Y 座標容差 (pt)
	 * @param xMergeThreshold 合併同欄文字的 X 座標容差 (pt)
	 * @param expectedColumns 預期欄位數，不足自動以空字串補齊
	 * @return 清理後的表格資料二維清單
	 */
	List<List<String>> extractCleanedTable(InputStream inputStream, int pageNo, float yTolerance,
			float xMergeThreshold, int expectedColumns);
}
