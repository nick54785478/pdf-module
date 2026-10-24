package com.example.demo.application.service;

import com.example.demo.application.assembler.QuotationReportAssembler;
import com.example.demo.application.port.in.AddWatermarkUseCase;
import com.example.demo.application.port.in.GenerateQuotationPdfUseCase;
import com.example.demo.application.port.in.GenerateTemplateQuotationPdfUseCase;
import com.example.demo.application.port.in.ParseQuotationPdfUseCase;
import com.example.demo.application.port.out.MockServicePort;
import com.example.demo.application.port.out.PdfExtractorPort;
import com.example.demo.application.port.out.PdfWatermarkPort;
import com.example.demo.application.port.out.QuotationParserPort;
import com.example.demo.application.port.out.QuotationPdfGeneratorPort;
import com.example.demo.application.port.out.ReportGeneratorPort;
import com.example.demo.application.shared.command.AddWatermarkCommand;
import com.example.demo.application.shared.command.ParseQuotationPdfCommand;
import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.query.GetQuotationQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Map;

/**
 * 報價單應用層服務 (Application Service / Inbound Adapter)
 * <p>
 * 實作 {@link GenerateQuotationPdfUseCase}、{@link GenerateTemplateQuotationPdfUseCase}、{@link ParseQuotationPdfUseCase}
 * 與 {@link AddWatermarkUseCase} 入向端口，
 * 協調 {@link MockServicePort}、{@link QuotationPdfGeneratorPort}、{@link ReportGeneratorPort}、
 * {@link PdfExtractorPort}、{@link QuotationParserPort} 與 {@link PdfWatermarkPort} 進行報價單生成、解析與浮水印蓋印。
 * 遵循命名規範 3-9：Inbound Adapter 實作以 ApplicationService 結尾。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotationApplicationService implements GenerateQuotationPdfUseCase, GenerateTemplateQuotationPdfUseCase, ParseQuotationPdfUseCase, AddWatermarkUseCase {

	private final MockServicePort mockServicePort;
	private final QuotationPdfGeneratorPort quotationPdfPort;
	private final ReportGeneratorPort reportGeneratorPort;
	private final QuotationReportAssembler quotationReportAssembler;
	private final PdfExtractorPort pdfExtractorPort;
	private final QuotationParserPort quotationParserPort;
	private final PdfWatermarkPort pdfWatermarkPort;

	/**
	 * 依據查詢條件 (ID) 取得報價單資料並產出 PDF 檔案
	 * <p>
	 * 依據業務指示「不必檢核只要提供回傳資料就好」，直接由 {@link MockServicePort} 提供對應 ID 之範本資料，
	 * 並傳入 {@link QuotationPdfGeneratorPort} 產出 PDF 二進位 byte 陣列。
	 * 若查詢條件包含 {@code watermarkText}，則調用 {@link PdfWatermarkPort} 為其加蓋防偽浮水印。
	 * </p>
	 *
	 * @param query 查詢條件物件 (包含 ID 與可選之浮水印文字)
	 * @return PDF 檔案之二進位 byte 陣列
	 */
	@Override
	public byte[] generateQuotationPdf(GetQuotationQuery query) {
		log.info("應用層收到 Query 產出報價單 PDF 請求: id={}, watermark={}",
				query != null ? query.id() : null, query != null ? query.watermarkText() : null);
		QuotationGottenResult quotation = mockServicePort.getQuotation(query);
		byte[] pdfBytes = quotationPdfPort.generateQuotationPdf(quotation);

		if (pdfBytes != null && query != null && query.watermarkText() != null && !query.watermarkText().trim().isEmpty()) {
			log.info("為 OpenPDF 報價單蓋印浮水印: watermark={}", query.watermarkText());
			pdfBytes = pdfWatermarkPort.stampWatermark(pdfBytes, query.watermarkText());
		}
		return pdfBytes;
	}

	/**
	 * 依據查詢條件 (ID) 取得報價單資料並透過 BIRT 報表範本產出 PDF 檔案
	 * <p>
	 * 協調 {@link MockServicePort} 取得報價單資料，並由 {@link QuotationReportAssembler} 組裝為報表參數與資料集，
	 * 最後調用 {@link ReportGeneratorPort} 讀取 bom_quotation_template 報表設計檔生成 PDF。
	 * 若查詢條件包含 {@code watermarkText}，則調用 {@link PdfWatermarkPort} 為其加蓋防偽浮水印。
	 * </p>
	 *
	 * @param query 查詢條件物件 (包含 ID 與可選之浮水印文字)
	 * @return PDF 檔案之二進位 byte 陣列，若查無資料或生成失敗則回傳 null
	 */
	@Override
	public byte[] generateTemplateQuotationPdf(GetQuotationQuery query) {
		log.info("應用層收到 Query 透過範本產出報價單 PDF 請求: id={}, watermark={}",
				query != null ? query.id() : null, query != null ? query.watermarkText() : null);
		QuotationGottenResult quotation = mockServicePort.getQuotation(query);
		if (quotation == null) {
			log.warn("MockServicePort 未能取得報價單資料: id={}", query != null ? query.id() : null);
			return null;
		}

		Map<String, Object> parameters = quotationReportAssembler.toParameters(quotation);
		Map<String, Object> dataContext = quotationReportAssembler.toDataContext(quotation);

		byte[] pdfBytes = reportGeneratorPort.generatePdfReport("templates", "bom_quotation_template", parameters, dataContext);
		if (pdfBytes != null && query != null && query.watermarkText() != null && !query.watermarkText().trim().isEmpty()) {
			log.info("為 BIRT 報價單蓋印浮水印: watermark={}", query.watermarkText());
			pdfBytes = pdfWatermarkPort.stampWatermark(pdfBytes, query.watermarkText());
		}
		return pdfBytes;
	}

	/**
	 * 於指定 PDF 文件的每一頁中央加蓋傾斜半透明防偽浮水印
	 *
	 * @param command 加蓋浮水印命令物件 (包含 PDF 二進位位元組與浮水印文字，不可為 null)
	 * @return 加蓋浮水印後之 PDF 二進位 byte 陣列
	 */
	@Override
	public byte[] addWatermark(AddWatermarkCommand command) {
		if (command == null || command.fileBytes() == null || command.fileBytes().length == 0) {
			log.error("加蓋浮水印失敗：command 或 fileBytes 為空");
			throw new IllegalArgumentException("待加蓋浮水印之 PDF 檔案內容不可為空");
		}

		log.info("應用層收到獨立加蓋浮水印請求: watermarkText={}, byteSize={}",
				command.watermarkText(), command.fileBytes().length);
		return pdfWatermarkPort.stampWatermark(command.fileBytes(), command.watermarkText());
	}

	/**
	 * 解析上傳之報價單 PDF 檔案並回傳結構化資料
	 * <p>
	 * 透過 {@link PdfExtractorPort} 讀取二進位 PDF 檔案全文文字內容，
	 * 並由 {@link QuotationParserPort} 解析為結構化之 {@link QuotationGottenResult}。
	 * </p>
	 *
	 * @param command 解析報價單 PDF 命令物件 (包含 byte[] 檔案與檔名)
	 * @return 報價單完整資訊資料傳輸物件 (QuotationGottenResult)
	 */
	@Override
	public QuotationGottenResult parseQuotationPdf(ParseQuotationPdfCommand command) {
		if (command == null || command.fileBytes() == null || command.fileBytes().length == 0) {
			log.error("解析報價單 PDF 失敗：command 或 fileBytes 為空");
			throw new IllegalArgumentException("上傳之 PDF 檔案內容不可為空");
		}

		log.info("應用層開始解析上傳之報價單 PDF: fileName={}, byteSize={}", command.fileName(), command.fileBytes().length);
		try (ByteArrayInputStream is = new ByteArrayInputStream(command.fileBytes())) {
			String extractedText = pdfExtractorPort.readPdfContent(is);
			log.info("PdfExtractorPort 成功自 PDF 萃取純文字內容長度: {} 字元", extractedText != null ? extractedText.length() : 0);
			QuotationGottenResult result = quotationParserPort.parse(extractedText);
			log.info("報價單 PDF 解析完成: quotationNo={}, 客戶={}, 明細品項數={}",
					result.quotationNo(), result.customerName(), result.items() != null ? result.items().size() : 0);
			return result;
		} catch (Exception e) {
			log.error("解析報價單 PDF 發生異常: fileName={}", command.fileName(), e);
			throw new IllegalStateException("解析報價單 PDF 發生錯誤: " + e.getMessage(), e);
		}
	}
}
