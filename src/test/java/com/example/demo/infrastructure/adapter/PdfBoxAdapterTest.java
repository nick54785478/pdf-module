package com.example.demo.infrastructure.adapter;

import com.example.demo.application.shared.dto.PdfColumnRect;
import com.example.demo.application.shared.dto.PdfTextPositionGottenResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PDFBox 適配器測試類別
 * <p>
 * 驗證 {@link PdfExtractorAdapter} 各項文字抽取與表格辨識邏輯。
 */
class PdfBoxAdapterTest {

	private PdfExtractorAdapter pdfBoxAdapter;

	@BeforeEach
	void setUp() {
		pdfBoxAdapter = new PdfExtractorAdapter();
	}

	/**
	 * 建立測試用的 PDF 位元組陣列
	 */
	private byte[] createSamplePdfBytes(String text) throws IOException {
		try (PDDocument document = new PDDocument();
			 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			PDPage page = new PDPage();
			document.addPage(page);

			try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
				contentStream.beginText();
				contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				contentStream.newLineAtOffset(100, 700);
				contentStream.showText(text);
				contentStream.endText();
			}

			document.save(baos);
			return baos.toByteArray();
		}
	}

	/**
	 * Feature: PDF 全文文字讀取
	 * Scenario: 讀取包含指定文字的單頁 PDF
	 * Given: 包含 "Hello Clean Architecture" 文字的 PDF 輸入流
	 * When: 呼叫 readPdfContent(inputStream)
	 * Then: 成功抽取出包含 "Hello Clean Architecture" 的文字字串
	 */
	@Test
	@DisplayName("成功讀取 PDF 全部文字內容")
	void shouldReadPdfContentSuccessfully() throws IOException {
		// Given
		byte[] pdfBytes = createSamplePdfBytes("Hello Clean Architecture");
		InputStream inputStream = new ByteArrayInputStream(pdfBytes);

		// When
		String result = pdfBoxAdapter.readPdfContent(inputStream);

		// Then
		assertNotNull(result);
		assertTrue(result.contains("Hello Clean Architecture"));
	}

	/**
	 * Feature: PDF 逐頁文字讀取
	 * Scenario: 讀取 PDF 並依頁碼分組回傳
	 * Given: 包含第 1 頁內容的 PDF 輸入流
	 * When: 呼叫 readPdfByPage(inputStream)
	 * Then: 回傳包含第 1 頁文字且 Map 鍵包含 1
	 */
	@Test
	@DisplayName("成功逐頁讀取 PDF 文字內容")
	void shouldReadPdfByPageSuccessfully() throws IOException {
		// Given
		byte[] pdfBytes = createSamplePdfBytes("Page 1 Content");
		InputStream inputStream = new ByteArrayInputStream(pdfBytes);

		// When
		Map<Integer, String> pageMap = pdfBoxAdapter.readPdfByPage(inputStream);

		// Then
		assertNotNull(pageMap);
		assertTrue(pageMap.containsKey(1));
		assertTrue(pageMap.get(1).contains("Page 1 Content"));
	}

	/**
	 * Feature: PDF 文字精確座標抽取
	 * Scenario: 抽取指定頁面每個字元的座標與寬高
	 * Given: 包含文字的 PDF 輸入流與指定頁碼 1
	 * When: 呼叫 extractTextPositions(inputStream, 1)
	 * Then: 回傳非空的文字位置清單，且每筆資料包含正確座標
	 */
	@Test
	@DisplayName("成功抽取文字座標資訊")
	void shouldExtractTextPositionsSuccessfully() throws IOException {
		// Given
		byte[] pdfBytes = createSamplePdfBytes("Position Test");
		InputStream inputStream = new ByteArrayInputStream(pdfBytes);

		// When
		List<PdfTextPositionGottenResult> positions = pdfBoxAdapter.extractTextPositions(inputStream, 1);

		// Then
		assertNotNull(positions);
		assertFalse(positions.isEmpty());
		PdfTextPositionGottenResult first = positions.get(0);
		assertNotNull(first.text());
		assertTrue(first.x() > 0);
		assertTrue(first.y() > 0);
	}

	/**
	 * Feature: PDF 區域文字讀取
	 * Scenario: 讀取指定座標範圍內的文字
	 * Given: 位於 (100, 700) 座標的文字 "Hello Area"
	 * When: 呼叫 readPdfArea 指定覆蓋該文字之矩形範圍 (90, 80, 200, 50)
	 * Then: 成功抽取出該區域包含的文字
	 */
	@Test
	@DisplayName("成功讀取指定區域文字")
	void shouldReadPdfAreaSuccessfully() throws IOException {
		// Given (注意 PDF 座標系與 AWT 座標系轉換)
		byte[] pdfBytes = createSamplePdfBytes("Hello Area");
		InputStream inputStream = new ByteArrayInputStream(pdfBytes);

		// When
		String result = pdfBoxAdapter.readPdfArea(inputStream, 1, 50, 50, 400, 200);

		// Then
		assertNotNull(result);
		assertTrue(result.contains("Hello Area"));
	}

	/**
	 * Feature: 物料清單報價單範本解析
	 * Scenario: 讀取 example 目錄下的物料清單報價單 PDF
	 * Given: 位於 src/main/resources/example/bom_quotation_template.pdf 之報價單範本
	 * When: 呼叫 readPdfFromPath
	 * Then: 成功抽取出報價單號、客戶名稱與物料料號文字
	 */
	@Test
	@DisplayName("成功讀取 BOM 報價單範本 PDF")
	void shouldReadBomQuotationTemplatePdf() {
		// Given
		String path = "src/main/resources/example/bom_quotation_template.pdf";

		// When
		String content = pdfBoxAdapter.readPdfFromPath(path);

		// Then
		assertNotNull(content);
		assertTrue(content.contains("報 價 單") || content.contains("報價單"));
		assertTrue(content.contains("QT-20261023-001"));
		assertTrue(content.contains("MCU-STM32-H743"));
		assertTrue(content.contains("台灣智能科技股份有限公司"));
	}
}
