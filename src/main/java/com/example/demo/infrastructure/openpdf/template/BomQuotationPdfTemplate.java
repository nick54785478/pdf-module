package com.example.demo.infrastructure.openpdf.template;

import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.dto.QuotationItemGottenResult;
import com.example.demo.infrastructure.openpdf.core.OpenPdfCellFactory;
import com.example.demo.infrastructure.openpdf.core.OpenPdfFontProvider;
import com.example.demo.infrastructure.openpdf.core.OpenPdfPageEventHelper;
import com.example.demo.infrastructure.openpdf.core.OpenPdfStylePalette;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 物料清單報價單 PDF 業務專屬範本 (BOM Quotation PDF Template)
 * <p>
 * 實作 {@link PdfDocumentTemplate} 契約，負責將 {@link QuotationGottenResult} 業務 DTO
 * 依據高精準度版面規格渲染為正式 BOM 報價單 PDF。
 * 內部協調整合 {@link OpenPdfFontProvider}、{@link OpenPdfStylePalette}、
 * {@link OpenPdfCellFactory} 與 {@link OpenPdfPageEventHelper} 等通用積木。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BomQuotationPdfTemplate implements PdfDocumentTemplate<QuotationGottenResult> {

	private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0");
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final OpenPdfFontProvider fontProvider;

	@Override
	public void render(QuotationGottenResult quotation, OutputStream outputStream) throws Exception {
		if (quotation == null) {
			throw new IllegalArgumentException("報價單資料 (QuotationGottenResult) 不可為 null");
		}
		if (outputStream == null) {
			throw new IllegalArgumentException("輸出串流 (outputStream) 不可為 null");
		}

		// 建立 A4 文件 (左右邊距 32pt，上下邊距 36pt)
		Document document = new Document(PageSize.A4, 32f, 32f, 36f, 36f);
		PdfWriter writer = PdfWriter.getInstance(document, outputStream);

		// 掛載分頁與頁碼控制器 (第 X 頁 / 共 Y 頁)
		OpenPdfPageEventHelper pageEventHelper = new OpenPdfPageEventHelper(fontProvider.getBaseFont());
		writer.setPageEvent(pageEventHelper);

		document.open();

		// 1. 公司標頭與報價單號區塊
		renderHeader(document, quotation);

		// 裝飾分隔線
		document.add(OpenPdfCellFactory.createSeparatorTable(OpenPdfStylePalette.ACCENT_BLUE, 2f, 8f, 8f));

		// 2. 客戶與交易條件網格區塊
		renderCustomerGrid(document, quotation);

		// 額外間距
		Paragraph space = new Paragraph(" ");
		space.setLeading(10f);
		document.add(space);

		// 3. 智慧防孤兒物料清單 (BOM) 與結尾區塊協同渲染 (保證跨頁時最後一頁至少伴隨明細行，杜絕孤兒區塊)
		renderBomAndFooterSections(document, writer, pageEventHelper, quotation);

		document.close();
		log.info("BOM 報價單 PDF 渲染成功: quotationNo={}", quotation.quotationNo());
	}

	/**
	 * 1. 渲染公司標頭與報價單號
	 */
	private void renderHeader(Document document, QuotationGottenResult quotation) throws Exception {
		PdfPTable headerTable = new PdfPTable(2);
		headerTable.setWidthPercentage(100);
		headerTable.setWidths(new float[]{65f, 35f});

		// 左側：公司名稱與基本聯絡資料
		PdfPCell compCell = new PdfPCell();
		compCell.setBorder(Rectangle.NO_BORDER);

		Paragraph compName = new Paragraph("晨星智造整合科技股份有限公司", fontProvider.getTitleFont());
		Paragraph compSub = new Paragraph("MorningStar Smart Manufacturing Integration Co., Ltd.", fontProvider.getSubtitleFont());
		Paragraph compDetails = new Paragraph(
				"統一編號：88996633  |  電話：(02) 2788-9900  |  傳真：(02) 2788-9901\n地址：115 台北市南港區軟體園區二路 88 號 12 樓",
				fontProvider.getBodyMuted()
		);
		compDetails.setSpacingBefore(3f);

		compCell.addElement(compName);
		compCell.addElement(compSub);
		compCell.addElement(compDetails);
		headerTable.addCell(compCell);

		// 右側：報價單顯著標題與單號日期
		PdfPCell quoteCell = new PdfPCell();
		quoteCell.setBorder(Rectangle.NO_BORDER);
		quoteCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

		Paragraph quoteTitle = new Paragraph("報 價 單", fontProvider.getQuotationTitleFont());
		quoteTitle.setAlignment(Element.ALIGN_RIGHT);

		Paragraph quoteEn = new Paragraph("QUOTATION", fontProvider.createFont(10f, Font.BOLD, new Color(148, 163, 184)));
		quoteEn.setAlignment(Element.ALIGN_RIGHT);

		String dateStr = quotation.quotationDate() != null ? quotation.quotationDate().format(DATE_FORMATTER) : "";
		Paragraph quoteNo = new Paragraph("單號：" + safeString(quotation.quotationNo()) + "\n日期：" + dateStr, fontProvider.getBodyBold());
		quoteNo.setAlignment(Element.ALIGN_RIGHT);
		quoteNo.setSpacingBefore(4f);

		quoteCell.addElement(quoteTitle);
		quoteCell.addElement(quoteEn);
		quoteCell.addElement(quoteNo);
		headerTable.addCell(quoteCell);

		document.add(headerTable);
	}

	/**
	 * 2. 渲染客戶與交易條件網格
	 */
	private void renderCustomerGrid(Document document, QuotationGottenResult quotation) throws Exception {
		PdfPTable infoTable = new PdfPTable(4);
		infoTable.setWidthPercentage(100);
		infoTable.setWidths(new float[]{15f, 35f, 15f, 35f});

		Font labelFont = fontProvider.getBodyBold();
		Font valFont = fontProvider.getBodyRegular();
		Color labelBg = OpenPdfStylePalette.BG_LIGHT_GRAY;
		Color valBg = Color.WHITE;
		Color border = OpenPdfStylePalette.BORDER_GRAY;

		// Row 1
		OpenPdfCellFactory.addInfoCell(infoTable, "客戶名稱：", labelFont, true, labelBg, border);
		OpenPdfCellFactory.addInfoCell(infoTable, safeString(quotation.customerName()), valFont, false, valBg, border);
		OpenPdfCellFactory.addInfoCell(infoTable, "有效期限：", labelFont, true, labelBg, border);
		OpenPdfCellFactory.addInfoCell(infoTable, safeString(quotation.validityPeriod()), valFont, false, valBg, border);

		// Row 2
		OpenPdfCellFactory.addInfoCell(infoTable, "統一編號：", labelFont, true, labelBg, border);
		OpenPdfCellFactory.addInfoCell(infoTable, safeString(quotation.customerTaxId()), valFont, false, valBg, border);
		OpenPdfCellFactory.addInfoCell(infoTable, "付款條件：", labelFont, true, labelBg, border);
		OpenPdfCellFactory.addInfoCell(infoTable, safeString(quotation.paymentTerm()), valFont, false, valBg, border);

		// Row 3
		OpenPdfCellFactory.addInfoCell(infoTable, "聯絡人員：", labelFont, true, labelBg, border);
		OpenPdfCellFactory.addInfoCell(infoTable, safeString(quotation.contactPerson()), valFont, false, valBg, border);
		OpenPdfCellFactory.addInfoCell(infoTable, "交貨地點：", labelFont, true, labelBg, border);
		OpenPdfCellFactory.addInfoCell(infoTable, safeString(quotation.deliveryPlace()), valFont, false, valBg, border);

		document.add(infoTable);
	}

	/**
	 * 3. 智慧防孤兒物料清單 (BOM) 與結尾區塊協同排版
	 * <p>
	 * 核心防孤兒演算法：
	 * 1. 將金額總計、交易條款與雙方簽核組合成一個不可跨頁拆分的「結尾防拆複合區塊」({@code setKeepTogether(true)})。
	 * 2. 設定最後 2 筆明細為「防孤兒錨點 (Anchor Rows)」。
	 * 3. 繪製完前置明細後，精準計算當前頁面剩餘垂直空間。
	 * 4. 若剩餘空間無法同時容納「最後 2 筆明細」與「結尾複合區塊」，主動換頁 (newPage)，
	 *    並在新頁面頂部以完整表頭帶出最後 2 筆明細與結尾區塊，徹底根除下方區塊孤立在最後一頁的缺陷。
	 * </p>
	 */
	private void renderBomAndFooterSections(Document document, PdfWriter writer,
											OpenPdfPageEventHelper pageEventHelper,
											QuotationGottenResult quotation) throws Exception {
		List<QuotationItemGottenResult> items = quotation.items();
		PdfPTable footerBlock = buildFooterCompositeBlock(quotation);
		float footerHeight = OpenPdfPageEventHelper.calculateTableHeight(footerBlock, document);
		float footerTotalNeeded = footerHeight + 6f; // 加上 spacingBefore

		// 情境 A：無明細項目
		if (items == null || items.isEmpty()) {
			PdfPTable emptyTable = new PdfPTable(1);
			emptyTable.setWidthPercentage(100);
			PdfPCell emptyCell = new PdfPCell(new Phrase("（目前無明細品項）", fontProvider.getBodyMuted()));
			emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
			emptyCell.setPadding(8f);
			emptyCell.setBorderColor(OpenPdfStylePalette.BORDER_GRAY);
			emptyTable.addCell(emptyCell);
			document.add(emptyTable);

			pageEventHelper.ensureSpaceOrNewPage(writer, document, footerTotalNeeded);
			document.add(footerBlock);
			return;
		}

		// 防孤兒錨點保留明細筆數 (預設為 2 筆，確保跨頁時新頁面具備足夠的明細骨幹)
		final int ANCHOR_COUNT = 2;

		// 情境 B：資料筆數極少 (<= 2 筆)
		if (items.size() <= ANCHOR_COUNT) {
			PdfPTable fullTable = createBomTable(items, true, 0);
			float tableHeight = OpenPdfPageEventHelper.calculateTableHeight(fullTable, document);
			float totalNeeded = tableHeight + footerTotalNeeded + 8f;

			// 若當前頁面空間無法同時容納全部明細與結尾區塊，則主動換頁以攜帶明細至新頁
			pageEventHelper.ensureSpaceOrNewPage(writer, document, totalNeeded);
			document.add(fullTable);
			pageEventHelper.recordItemRendered(items.size());
			document.add(footerBlock);
			return;
		}

		// 情境 C：常規多筆明細 (> 2 筆)，拆分前置明細 (Primary Rows) 與 尾隨錨點明細 (Anchor Rows)
		int splitIndex = items.size() - ANCHOR_COUNT;
		List<QuotationItemGottenResult> primaryItems = items.subList(0, splitIndex);
		List<QuotationItemGottenResult> anchorItems = items.subList(splitIndex, items.size());

		// 1. 繪製主力前段明細表格 (支援中途跨頁與表頭重複)
		PdfPTable primaryTable = createBomTable(primaryItems, true, 0);
		document.add(primaryTable);
		pageEventHelper.recordItemRendered(primaryItems.size());

		// 2. 獲取排版游標在此刻頁面上的真實剩餘垂直空間
		float remainingSpace = pageEventHelper.getRemainingVerticalSpace(writer, document);

		// 3. 建立帶表頭與不帶表頭的錨點明細表格
		PdfPTable anchorTableWithHeader = createBomTable(anchorItems, true, splitIndex);
		PdfPTable anchorTableWithoutHeader = createBomTable(anchorItems, false, splitIndex);

		float anchorWithoutHeaderHeight = OpenPdfPageEventHelper.calculateTableHeight(anchorTableWithoutHeader, document);
		float neededOnCurrentPage = anchorWithoutHeaderHeight + footerTotalNeeded + 6f;

		// 4. 關鍵防孤兒空間判定
		if (remainingSpace < neededOnCurrentPage) {
			log.info("【防孤兒排版觸發】當前頁面剩餘高度 {}pt 不足容納最後 {} 筆明細與結尾區塊 (需 {}pt)，主動換頁以攜帶明細至新頁",
					remainingSpace, ANCHOR_COUNT, neededOnCurrentPage);
			document.newPage();

			// 換頁後於新頁面頂部以完整表頭帶出最後明細列
			document.add(anchorTableWithHeader);
			pageEventHelper.recordItemRendered(anchorItems.size());
			document.add(footerBlock);
		} else {
			// 當前頁空間充足，直接無縫接續繪製不帶表頭之錨點明細，並緊接繪製結尾區塊
			document.add(anchorTableWithoutHeader);
			pageEventHelper.recordItemRendered(anchorItems.size());
			document.add(footerBlock);
		}
	}

	/**
	 * 建立 BOM 明細表格 (支援指定是否產生表頭與指定起始項次編號)
	 *
	 * @param items         明細項目列表
	 * @param includeHeader 是否包含 8 欄深藍表頭
	 * @param startRowIndex 起始行索引 (用於斑馬紋底色連續計算)
	 * @return PdfPTable 實例
	 */
	private PdfPTable createBomTable(List<QuotationItemGottenResult> items, boolean includeHeader, int startRowIndex) {
		PdfPTable table = new PdfPTable(8);
		table.setWidthPercentage(100);
		table.setWidths(new float[]{6f, 18f, 34f, 7f, 6f, 10f, 12f, 12f});
		table.setSpacingBefore(0f);
		table.setSpacingAfter(0f);

		if (includeHeader) {
			table.setHeaderRows(1);
			String[] headers = {"項次", "物料料號", "品名與規格描述", "數量", "單位", "單價 (TWD)", "小計 (TWD)", "備註"};
			Font hFont = fontProvider.getHeaderFont();
			Color hBg = OpenPdfStylePalette.PRIMARY_NAVY;
			for (String header : headers) {
				table.addCell(OpenPdfCellFactory.createHeaderCell(header, hFont, hBg));
			}
		}

		if (items != null) {
			Font cellRegular = fontProvider.getBodyRegular();
			Color border = OpenPdfStylePalette.BORDER_GRAY;

			for (int i = 0; i < items.size(); i++) {
				QuotationItemGottenResult item = items.get(i);
				int globalIndex = startRowIndex + i;
				Color rowBg = (globalIndex % 2 == 1) ? OpenPdfStylePalette.ROW_ALT_BG : Color.WHITE;

				table.addCell(OpenPdfCellFactory.createDataCell(String.valueOf(item.itemNo() != null ? item.itemNo() : globalIndex + 1), cellRegular, Element.ALIGN_CENTER, rowBg, border));
				table.addCell(OpenPdfCellFactory.createDataCell(safeString(item.materialNo()), cellRegular, Element.ALIGN_LEFT, rowBg, border));
				table.addCell(OpenPdfCellFactory.createDataCell(safeString(item.description()), cellRegular, Element.ALIGN_LEFT, rowBg, border));
				table.addCell(OpenPdfCellFactory.createDataCell(item.quantity() != null ? String.valueOf(item.quantity()) : "0", cellRegular, Element.ALIGN_RIGHT, rowBg, border));
				table.addCell(OpenPdfCellFactory.createDataCell(safeString(item.unit()), cellRegular, Element.ALIGN_CENTER, rowBg, border));
				table.addCell(OpenPdfCellFactory.createDataCell(formatAmount(item.unitPrice()), cellRegular, Element.ALIGN_RIGHT, rowBg, border));
				table.addCell(OpenPdfCellFactory.createDataCell(formatAmount(item.subtotal()), cellRegular, Element.ALIGN_RIGHT, rowBg, border));
				table.addCell(OpenPdfCellFactory.createDataCell(safeString(item.remark()), cellRegular, Element.ALIGN_LEFT, rowBg, border));
			}
		}

		return table;
	}

	/**
	 * 建立不可跨頁拆分之「結尾防拆複合區塊」 (Composite Footer Block)
	 * <p>
	 * 封裝備註條款、金額合計與雙方簽核用印框，並設定 {@code setKeepTogether(true)}，
	 * 保證三者永遠在同一頁緊密結合，杜絕「條款在第一頁、簽名被擠到第二頁」之次級孤兒問題。
	 * </p>
	 */
	private PdfPTable buildFooterCompositeBlock(QuotationGottenResult quotation) {
		PdfPTable compositeTable = new PdfPTable(1);
		compositeTable.setWidthPercentage(100);
		compositeTable.setSpacingBefore(6f);
		compositeTable.setKeepTogether(true); // 關鍵：防拆保證

		// 1. 金額匯總與備註條款
		PdfPTable summaryTable = buildSummaryAndTermsTable(quotation);
		PdfPCell summaryCell = new PdfPCell(summaryTable);
		summaryCell.setBorder(Rectangle.NO_BORDER);
		summaryCell.setPadding(0f);
		compositeTable.addCell(summaryCell);

		// 2. 雙方簽核用印框
		PdfPTable signTable = buildSignatureTable();
		PdfPCell signCell = new PdfPCell(signTable);
		signCell.setBorder(Rectangle.NO_BORDER);
		signCell.setPadding(0f);
		signCell.setPaddingTop(10f); // 簽核框上方留白間距
		compositeTable.addCell(signCell);

		return compositeTable;
	}

	/**
	 * 建立備註條款與金額小計/稅額/總計表格
	 */
	private PdfPTable buildSummaryAndTermsTable(QuotationGottenResult quotation) {
		PdfPTable summaryTable = new PdfPTable(3);
		summaryTable.setWidthPercentage(100);
		summaryTable.setWidths(new float[]{55f, 20f, 25f});

		// 左側備註條款框
		PdfPCell termsCell = new PdfPCell();
		termsCell.setBorder(Rectangle.BOX);
		termsCell.setBorderColor(OpenPdfStylePalette.BORDER_GRAY);
		termsCell.setBackgroundColor(OpenPdfStylePalette.CARD_BG_LIGHT);
		termsCell.setPadding(6f);

		Paragraph termsTitle = new Paragraph("【報價與交易條款說明】", fontProvider.getBodyBold());
		String termsContent = quotation.terms() != null && !quotation.terms().trim().isEmpty()
				? quotation.terms()
				: """
					1. 本報價單所列物料均符合 RoHS 2.0 及 REACH 環保規範要求。
					2. 提供出貨後 12 個月原廠保固服務（人為操作不當或不可抗力因素除外）。
					3. 貴公司如確認訂購，請於下方「客戶確認簽回」欄位用印簽署後回傳。""";

		Paragraph termsBody = new Paragraph(termsContent, fontProvider.getBodyMuted());
		termsCell.addElement(termsTitle);
		termsCell.addElement(termsBody);
		summaryTable.addCell(termsCell);

		// 右側小計明細 (跨 2 欄)
		PdfPTable calcTable = new PdfPTable(2);
		calcTable.setWidthPercentage(100);
		calcTable.setWidths(new float[]{45f, 55f});

		Font regular = fontProvider.getBodyRegular();
		Font bold = fontProvider.getBodyBold();
		Font totalFont = fontProvider.getTotalFont();

		OpenPdfCellFactory.addCalcRow(calcTable, "未稅合計 (Subtotal)：", "NT$ " + formatAmount(quotation.subtotalAmount()), regular, bold, false, null);
		OpenPdfCellFactory.addCalcRow(calcTable, "營業稅 5% (VAT)：", "NT$ " + formatAmount(quotation.taxAmount()), regular, bold, false, null);
		OpenPdfCellFactory.addCalcRow(calcTable, "總計金額 (Grand Total)：", "NT$ " + formatAmount(quotation.totalAmount()), bold, totalFont, true, OpenPdfStylePalette.TOTAL_BG_LIGHT);

		PdfPCell calcContainer = new PdfPCell(calcTable);
		calcContainer.setColspan(2);
		calcContainer.setBorder(Rectangle.BOX);
		calcContainer.setBorderColor(OpenPdfStylePalette.BORDER_GRAY);
		summaryTable.addCell(calcContainer);

		return summaryTable;
	}

	/**
	 * 建立雙方簽核用印表格
	 */
	private PdfPTable buildSignatureTable() {
		PdfPTable signTable = new PdfPTable(2);
		signTable.setWidthPercentage(100);
		signTable.setWidths(new float[]{50f, 50f});

		Color border = OpenPdfStylePalette.BORDER_GRAY;
		Font bold = fontProvider.getBodyBold();
		Font regular = fontProvider.getBodyRegular();
		Font muted = fontProvider.getBodyMuted();

		// 左側：賣方主管簽核
		PdfPCell signLeft = new PdfPCell();
		signLeft.setBorder(Rectangle.BOX);
		signLeft.setBorderColor(border);
		signLeft.setPadding(8f);
		signLeft.addElement(new Paragraph("晨星智造整合科技股份有限公司 (經辦/主管簽核)", bold));
		signLeft.addElement(new Paragraph("\n\n業務經辦：王大衛                    主管簽章：____________", regular));
		signLeft.addElement(new Paragraph("日期：2026 年 10 月 23 日", muted));
		signTable.addCell(signLeft);

		// 右側：買方客戶用印
		PdfPCell signRight = new PdfPCell();
		signRight.setBorder(Rectangle.BOX);
		signRight.setBorderColor(border);
		signRight.setPadding(8f);
		signRight.addElement(new Paragraph("客戶確認簽回欄 (請簽署或用印後掃描回傳)", bold));
		signRight.addElement(new Paragraph("\n\n客戶簽署：_____________________   公司印鑑：[           ]", regular));
		signRight.addElement(new Paragraph("簽署日期：2026 年    月    日", muted));
		signTable.addCell(signRight);

		return signTable;
	}

	private String safeString(String str) {
		return str != null ? str : "";
	}

	private String formatAmount(BigDecimal amount) {
		if (amount == null) {
			return "0";
		}
		return CURRENCY_FORMAT.format(amount);
	}
}
