package com.example.demo.application.port.out;

import com.example.demo.application.shared.dto.PdfImageGottenResult;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * PDF 圖片抽取輸出端口 (Outbound Port)
 * <p>
 * 遵循六角形架構規範：
 * 1. 位於應用層 {@code application.port.out}，作為應用層對外要求基礎設施層提供的契約介面。
 * 2. 介面命名結尾必須為 {@code Port}。
 * 3. 嚴禁在方法簽名中接發技術類或環境相依物件 (如 Spring {@code InputStreamResource}、{@code MultipartFile})，
 *    全面使用純 Java 原生型別 ({@code byte[]}) 與應用層 DTO ({@link PdfImageGottenResult})。
 * </p>
 */
public interface PdfImageExtractorPort {

	/**
	 * 從 PDF 位元組陣列中抽取第一張圖片
	 *
	 * @param pdfBytes PDF 內容位元組陣列
	 * @return 第一張圖片之 DTO，若無圖片則為 Optional.empty()
	 */
	Optional<PdfImageGottenResult> extractFirstImage(byte[] pdfBytes);

	/**
	 * 從 PDF 輸入流中抽取第一張圖片
	 *
	 * @param inputStream PDF 資料輸入流
	 * @return 第一張圖片之 DTO，若無圖片則為 Optional.empty()
	 */
	Optional<PdfImageGottenResult> extractFirstImage(InputStream inputStream);

	/**
	 * 從 PDF 中抽取第一張圖片並轉換為 Base64 字串
	 *
	 * @param pdfBytes PDF 內容位元組陣列
	 * @return Base64 字串，若無圖片則為 Optional.empty()
	 */
	Optional<String> extractFirstImageAsBase64(byte[] pdfBytes);

	/**
	 * 抽取 PDF 中的所有圖片
	 *
	 * @param pdfBytes PDF 內容位元組陣列
	 * @return 抽取之圖片清單，若無圖片則回傳空清單
	 */
	List<PdfImageGottenResult> extractAllImages(byte[] pdfBytes);

	/**
	 * 抽取 PDF 輸入流中的所有圖片
	 *
	 * @param inputStream PDF 資料輸入流
	 * @return 抽取之圖片清單，若無圖片則回傳空清單
	 */
	List<PdfImageGottenResult> extractAllImages(InputStream inputStream);

	/**
	 * 從 PDF 中抽取所有圖片並壓縮為 ZIP 壓縮檔位元組陣列
	 *
	 * @param pdfBytes PDF 內容位元組陣列
	 * @return ZIP 壓縮檔之 byte[]，若無圖片則回傳空 ZIP
	 */
	byte[] extractAllImagesAsZip(byte[] pdfBytes);

	/**
	 * 從 PDF 輸入流中抽取所有圖片並壓縮為 ZIP 壓縮檔位元組陣列
	 *
	 * @param inputStream PDF 資料輸入流
	 * @return ZIP 壓縮檔之 byte[]，若無圖片則回傳空 ZIP
	 */
	byte[] extractAllImagesAsZip(InputStream inputStream);

	/**
	 * 從 PDF 中抽取所有圖片並壓縮為 ZIP，回傳 Base64 字串
	 *
	 * @param pdfBytes PDF 內容位元組陣列
	 * @return ZIP 格式之 Base64 字串，若無圖片則回傳空字串
	 */
	String extractAllImagesAsZipBase64(byte[] pdfBytes);
}
