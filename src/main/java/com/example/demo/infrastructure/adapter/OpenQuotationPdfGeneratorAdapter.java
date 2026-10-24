package com.example.demo.infrastructure.adapter;

import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.port.out.QuotationPdfGeneratorPort;
import com.example.demo.infrastructure.openpdf.template.BomQuotationPdfTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

/**
 * OpenPDF 報價單輸出外向適配器 (Outbound Adapter)
 * <p>
 * 實作應用層 {@link QuotationPdfGeneratorPort} 外向端口，作為基礎設施模組的對外出口，
 * 協調整合 {@link BomQuotationPdfTemplate} 業務範本將資料轉換為二進位 PDF 檔案。
 * 遵循命名規範 3-9：Outbound Adapter 一律以 Adapter 結尾。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenQuotationPdfGeneratorAdapter implements QuotationPdfGeneratorPort {

	private final BomQuotationPdfTemplate quotationPdfTemplate;

	/**
	 * 依據報價單資料傳輸物件生成 PDF 二進位位元組陣列
	 *
	 * @param quotation 報價單完整資訊 (強型別 DTO)
	 * @return PDF 檔案之二進位 byte 陣列；若生成過程發生非預期例外則回傳 null
	 */
	@Override
	public byte[] generateQuotationPdf(QuotationGottenResult quotation) {
		return generateQuotationPdf(quotation, null);
	}

	/**
	 * 依據報價單資料傳輸物件與防偽浮水印文字生成 PDF 二進位位元組陣列 (生成時單次印製)
	 *
	 * @param quotation     報價單完整資訊 (強型別 DTO)
	 * @param watermarkText 可選之防偽浮水印文字
	 * @return PDF 檔案之二進位 byte 陣列；若生成過程發生非預期例外則回傳 null
	 */
	@Override
	public byte[] generateQuotationPdf(QuotationGottenResult quotation, String watermarkText) {
		if (quotation == null) {
			log.error("產出報價單失敗：輸入之 quotation 為 null");
			return null;
		}

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			log.info("基礎設施層開始渲染報價單 PDF: quotationNo={}, watermark={}", quotation.quotationNo(), watermarkText);
			quotationPdfTemplate.render(quotation, baos, watermarkText);
			byte[] pdfBytes = baos.toByteArray();
			log.info("報價單 PDF 產出完成: quotationNo={}, byteSize={}", quotation.quotationNo(), pdfBytes.length);
			return pdfBytes;
		} catch (Exception e) {
			log.error("渲染報價單 PDF 發生未預期例外: quotationNo={}", quotation.quotationNo(), e);
			return null;
		}
	}
}

