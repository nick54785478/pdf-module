package com.example.demo.infrastructure.adapter;

import com.example.demo.application.shared.dto.PdfImageGottenResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PDFBox 圖片抽取適配器單元測試 (Outbound Adapter Unit Test)
 * <p>
 * 遵循 AGENTS.md 規範：
 * 1. 驗證 Outbound Adapter 整合 Apache PDFBox 進行單一圖片、全部圖片及 ZIP 封裝抽取之正確性。
 * 2. 測試包含內嵌圖片之 PDF 與無圖片之空白 PDF 邊界情境。
 * 3. 測試方法遵循 BDD Gherkin 語法撰寫 Javadoc。
 * </p>
 */
class PdfBoxImageExtractorAdapterTest {

	private PdfBoxImageExtractorAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new PdfBoxImageExtractorAdapter();
	}

	/**
	 * Feature: PDF 單一圖片抽取
	 * Scenario: 成功自含有圖片的 PDF 中抽取第一張圖片
	 * Given 建立包含一張 RGB 紅色方塊圖片的 PDF 文件
	 * When 執行 adapter.extractFirstImage
	 * Then 成功回傳非空之 Optional 且包含有效之圖片二進位內容與 Base64
	 */
	@Test
	@DisplayName("驗證成功抽取 PDF 第一張圖片與 Base64 轉換")
	void shouldExtractFirstImageSuccessfully() throws IOException {
		// Given
		byte[] pdfBytes = createPdfWithSampleImage(1);

		// When
		Optional<PdfImageGottenResult> result = adapter.extractFirstImage(pdfBytes);
		Optional<String> base64Result = adapter.extractFirstImageAsBase64(pdfBytes);

		// Then
		assertTrue(result.isPresent(), "應成功抽取到第一張圖片");
		PdfImageGottenResult image = result.get();
		assertNotNull(image.data());
		assertTrue(image.data().length > 0);
		assertEquals(1, image.pageNumber());
		assertNotNull(image.getFileName());

		assertTrue(base64Result.isPresent(), "Base64 結果應存在");
		assertFalse(base64Result.get().isBlank(), "Base64 字串不應為空");
	}

	/**
	 * Feature: PDF 批次全部圖片抽取
	 * Scenario: 成功抽取多頁多張圖片並壓縮為 ZIP
	 * Given 建立包含兩頁且各有 1 張圖片的 PDF 文件
	 * When 執行 adapter.extractAllImages 與 adapter.extractAllImagesAsZip
	 * Then 成功抽取兩張圖片且 ZIP 壓縮檔內含對應之 ZipEntry
	 */
	@Test
	@DisplayName("驗證成功抽取所有圖片並壓縮為 ZIP 檔案")
	void shouldExtractAllImagesAndZipSuccessfully() throws IOException {
		// Given: 2 頁各含有 1 張圖片
		byte[] pdfBytes = createPdfWithSampleImage(2);

		// When
		List<PdfImageGottenResult> allImages = adapter.extractAllImages(pdfBytes);
		byte[] zipBytes = adapter.extractAllImagesAsZip(pdfBytes);
		String zipBase64 = adapter.extractAllImagesAsZipBase64(pdfBytes);

		// Then
		assertEquals(2, allImages.size(), "應抽取到 2 張圖片");
		assertNotNull(zipBytes);
		assertTrue(zipBytes.length > 0, "ZIP 壓縮二進位資料不應為空");
		assertFalse(zipBase64.isBlank(), "ZIP Base64 不應為空");

		// 驗證 ZIP 檔案結構
		int entryCount = 0;
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
			while (zis.getNextEntry() != null) {
				entryCount++;
				zis.closeEntry();
			}
		}
		assertEquals(2, entryCount, "ZIP 檔內應含有 2 個圖片項目");
	}

	/**
	 * Feature: 無圖片 PDF 邊界情境防禦
	 * Scenario: 傳入完全無圖片之純文字空白 PDF
	 * Given 建立僅有純文字的空白 PDF 文件
	 * When 嘗試抽取第一張圖片與全部圖片
	 * Then 回傳 Optional.empty()、空清單與空 ZIP 陣列，不拋出任何未預期例外
	 */
	@Test
	@DisplayName("驗證無圖片 PDF 傳入時優雅回傳空結果")
	void shouldReturnEmptyWhenPdfHasNoImages() throws IOException {
		// Given: 無圖片的空白 PDF
		byte[] emptyPdf = createBlankPdf();

		// When & Then
		Optional<PdfImageGottenResult> firstImage = adapter.extractFirstImage(emptyPdf);
		List<PdfImageGottenResult> allImages = adapter.extractAllImages(emptyPdf);
		byte[] zipBytes = adapter.extractAllImagesAsZip(emptyPdf);
		String zipBase64 = adapter.extractAllImagesAsZipBase64(emptyPdf);

		assertFalse(firstImage.isPresent(), "無圖片時第一張圖片應為 empty");
		assertTrue(allImages.isEmpty(), "無圖片時清單應為空");
		assertEquals(0, zipBytes.length, "無圖片時 ZIP byte[] 應長度為 0");
		assertEquals("", zipBase64, "無圖片時 ZIP Base64 應為空字串");
	}

	/**
	 * Feature: InputStream 介面重載驗證
	 * Scenario: 透過 InputStream 輸入流呼叫適配器
	 * Given 含有圖片之 PDF 輸入流
	 * When 執行 adapter.extractFirstImage(inputStream)
	 * Then 行為與 byte[] 重載版本完全一致
	 */
	@Test
	@DisplayName("驗證 InputStream 重載方法呼叫正常")
	void shouldSupportInputStreamOverload() throws IOException {
		// Given
		byte[] pdfBytes = createPdfWithSampleImage(1);

		// When
		Optional<PdfImageGottenResult> result = adapter.extractFirstImage(new ByteArrayInputStream(pdfBytes));

		// Then
		assertTrue(result.isPresent());
		assertTrue(result.get().data().length > 0);
	}

	/**
	 * 輔助方法：產生包含指定圖片數量之範例 PDF
	 */
	private byte[] createPdfWithSampleImage(int pageCount) throws IOException {
		try (PDDocument doc = new PDDocument()) {
			for (int i = 0; i < pageCount; i++) {
				PDPage page = new PDPage();
				doc.addPage(page);

				BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
				Graphics2D g = img.createGraphics();
				g.setColor(i % 2 == 0 ? Color.RED : Color.BLUE);
				g.fillRect(0, 0, 20, 20);
				g.dispose();

				PDImageXObject pdImage = LosslessFactory.createFromImage(doc, img);
				try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
					contentStream.drawImage(pdImage, 50, 50, 20, 20);
				}
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			doc.save(baos);
			return baos.toByteArray();
		}
	}

	/**
	 * 輔助方法：產生完全無圖片之空白 PDF
	 */
	private byte[] createBlankPdf() throws IOException {
		try (PDDocument doc = new PDDocument()) {
			doc.addPage(new PDPage());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			doc.save(baos);
			return baos.toByteArray();
		}
	}
}
