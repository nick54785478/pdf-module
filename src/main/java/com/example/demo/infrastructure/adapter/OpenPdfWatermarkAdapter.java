package com.example.demo.infrastructure.adapter;

import com.example.demo.application.port.out.PdfWatermarkPort;
import com.example.demo.infrastructure.openpdf.core.OpenPdfFontProvider;
import com.lowagie.text.Element;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

/**
 * OpenPDF 浮水印蓋印適配器 (Outbound Adapter)
 * <p>
 * 遵循 Clean Architecture 與六角形架構規範，實作應用層 {@link PdfWatermarkPort} 輸出端口。
 * 作為基礎設施層與外部 OpenPDF 引擎的匯聚點，透過 {@link PdfReader} 與 {@link PdfStamper}，
 * 在現有 PDF 檔案之每一頁幾何中心疊加 45 度傾斜、半透明 (Opacity 0.12)、支援微軟正黑體的防偽浮水印。
 * 遵循命名規範 3-9：Outbound Adapter 一律以 Adapter 結尾。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenPdfWatermarkAdapter implements PdfWatermarkPort {

	private final OpenPdfFontProvider fontProvider;

	/**
	 * 於傳入之 PDF 二進位位元組陣列中，為每一頁動態疊加半透明傾斜浮水印
	 *
	 * @param sourcePdf     原始 PDF 檔案位元組陣列 (不可為空)
	 * @param watermarkText 欲蓋印之浮水印文字 (例如：機密、草稿、正式報價等)
	 * @return 蓋印完成之 PDF 檔案位元組陣列
	 */
	@Override
	public byte[] stampWatermark(byte[] sourcePdf, String watermarkText) {
		if (sourcePdf == null || sourcePdf.length == 0) {
			log.error("加蓋浮水印失敗：傳入之 sourcePdf 位元組陣列為空");
			throw new IllegalArgumentException("待加蓋浮水印之原始 PDF 檔案不可為空");
		}

		if (watermarkText == null || watermarkText.trim().isEmpty()) {
			log.debug("未指定浮水印文字，直接回傳原始 PDF 內容");
			return sourcePdf;
		}

		String trimmedWatermark = watermarkText.trim();
		log.info("開始為 PDF 加蓋浮水印: watermarkText={}, pdfSize={} bytes", trimmedWatermark, sourcePdf.length);

		PdfReader reader = null;
		PdfStamper stamper = null;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			reader = new PdfReader(sourcePdf);
			stamper = new PdfStamper(reader, baos);

			BaseFont baseFont = fontProvider.getBaseFont();
			int totalPages = reader.getNumberOfPages();

			for (int page = 1; page <= totalPages; page++) {
				Rectangle pageSize = reader.getPageSizeWithRotation(page);
				PdfContentByte over = stamper.getOverContent(page);

				over.saveState();
				PdfGState gState = new PdfGState();
				gState.setFillOpacity(0.12f);
				over.setGState(gState);

				over.beginText();
				over.setFontAndSize(baseFont, 46f);
				over.setColorFill(Color.DARK_GRAY);

				float centerX = (pageSize.getLeft() + pageSize.getRight()) / 2f;
				float centerY = (pageSize.getTop() + pageSize.getBottom()) / 2f;
				over.showTextAligned(Element.ALIGN_CENTER, trimmedWatermark, centerX, centerY, 45f);

				over.endText();
				over.restoreState();
			}

			stamper.close();
			reader.close();

			byte[] resultPdf = baos.toByteArray();
			log.info("PDF 浮水印加蓋完成: totalPages={}, outputSize={} bytes", totalPages, resultPdf.length);
			return resultPdf;
		} catch (Exception e) {
			log.error("PDF 浮水印蓋印過程發生異常: watermarkText={}", trimmedWatermark, e);
			throw new RuntimeException("加蓋 PDF 浮水印失敗: " + e.getMessage(), e);
		} finally {
			if (stamper != null) {
				try {
					stamper.close();
				} catch (Exception ignored) {
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ignored) {
				}
			}
		}
	}
}
