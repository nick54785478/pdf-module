package com.example.demo.infrastructure.openpdf.template.impl;

import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.dto.QuotationItemGottenResult;
import com.example.demo.infrastructure.openpdf.core.OpenPdfCellFactory;
import com.example.demo.infrastructure.openpdf.core.OpenPdfFontProvider;
import com.example.demo.infrastructure.openpdf.core.OpenPdfPageEventHelper;
import com.example.demo.infrastructure.openpdf.core.OpenPdfStylePalette;
import com.example.demo.infrastructure.openpdf.core.OpenPdfWatermarkStyle;
import com.example.demo.infrastructure.openpdf.template.PdfDocumentTemplate;
import com.example.demo.infrastructure.pdfbox.security.customizer.impl.BomQuotationVisualSignatureCustomizer;
import com.example.demo.infrastructure.pdfbox.security.customizer.PdfVisualSignatureCustomizer;
import com.example.demo.infrastructure.pdfbox.security.policy.impl.PAdESCompliantSignaturePolicy;
import com.example.demo.infrastructure.pdfbox.security.policy.PdfSignaturePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 物料清單報價單 PDF 業務專屬範本實作 (BOM Quotation PDF Template Implementation)
 * <p>
 * <b>六角形架構與 DDD 定位：</b><br>
 * 本類別隸屬於基礎設施層 (Infrastructure Layer) 的 OpenPDF 範本適配器實作，
 * 實作 {@link PdfDocumentTemplate} 抽象合約，負責將應用層傳入的 {@link QuotationGottenResult} 業務 DTO
 * 依據高精準度工業規格渲染為正式的 BOM 原物料報價單 PDF 二進位位元組串流。
 * </p>
 * <p>
 * <b>核心技術協調整合：</b>
 * <ul>
 *   <li><b>字型快取管理：</b>整合 {@link OpenPdfFontProvider}，提供單例快取之微軟正黑體 BaseFont 與層級化 Typography 字級。</li>
 *   <li><b>色彩視覺規範：</b>整合 {@link OpenPdfStylePalette}，嚴格遵循企業標準調色盤（晨星深藍、裝飾藍、柔和灰）。</li>
 *   <li><b>儲存格工廠：</b>整合 {@link OpenPdfCellFactory}，產生具備一致 Padding、對齊與邊框的標準儲存格。</li>
 *   <li><b>分頁事件與動態頁碼：</b>內聚 {@link BomQuotationPageEventHelper}，透過兩階段渲染填入「第 X 頁 / 共 Y 頁」動態頁碼與防偽鋼印浮水印。</li>
 *   <li><b>智慧防孤兒演算法 (Anti-Orphan Algorithm)：</b>透過「結尾防拆複合區塊」({@code setKeepTogether(true)})
 *       搭配「倒數 2 筆物料錨點」與即時可用垂直高度測量 ({@link OpenPdfPageEventHelper#getRemainingVerticalSpace})，
 *       確保換頁時新頁面頂部永遠伴隨表頭與最後明細，徹底杜絕合計、條款與簽章孤立在最後一頁的嚴重排版缺陷。</li>
 *   <li><b>數位簽章規範擴充：</b>提供專屬之 {@link PAdESCompliantSignaturePolicy} 簽章規範與
 *       {@link BomQuotationVisualSignatureCustomizer} 專屬核准鋼印印章配置。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BomQuotationPdfTemplate implements PdfDocumentTemplate<QuotationGottenResult> {

	/**
	 * 幣別金額千分位格式化器 (例如 "223,125")
	 */
	private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0");

	/**
	 * 報價單日期標準輸出格式化器 (yyyy-MM-dd)
	 */
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	/**
	 * BOM 報價單專屬尊榮防偽鋼印浮水印樣式 (內聚於此業務範本，避免業務細節洩漏至通用核心層)
	 * <p>
	 * 採用企業主品牌深海軍藍 (Primary Navy, RGB 26, 54, 93)、42pt 尊榮主標、
	 * 45 度經典傾角、0.075f 極致通透透明度，搭配雙線圓角防偽鋼印外框
	 * 以及晨星智造英文副標 "MORNING STAR INTELLIGENT • BOM QUOTATION"。
	 * </p>
	 */
	private static final OpenPdfWatermarkStyle BOM_QUOTATION_WATERMARK_STYLE = OpenPdfWatermarkStyle.builder()
			.fontSize(42f)
			.opacity(0.075f)
			.color(OpenPdfStylePalette.PRIMARY_NAVY)
			.rotation(45f)
			.boxed(true)
			.subText("MORNING STAR INTELLIGENT • BOM QUOTATION")
			.build();

	/**
	 * OpenPDF 中文字型與層級樣式提供者
	 */
	private final OpenPdfFontProvider fontProvider;

	/**
	 * BOM 報價單預設數位簽章規範 (遵循 PAdES-BES 企業核准標準)
	 */
	private static final PdfSignaturePolicy BOM_SIGNATURE_POLICY = new PAdESCompliantSignaturePolicy();

	/**
	 * BOM 報價單專屬視覺簽章鋼印客製化器
	 */
	private static final PdfVisualSignatureCustomizer BOM_VISUAL_CUSTOMIZER = new BomQuotationVisualSignatureCustomizer();

	/**
	 * {@inheritDoc}
	 * <p>
	 * 回傳此業務範本之唯一識別名稱，對應 {@code "bom"}。
	 * </p>
	 *
	 * @return 範本識別字串 "bom"
	 */
	@Override
	public String getTemplateName() {
		return "bom";
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * 回傳 BOM 報價單專屬之尊榮防偽鋼印浮水印樣式。
	 * </p>
	 *
	 * @return BOM 專屬浮水印樣式物件
	 */
	@Override
	public OpenPdfWatermarkStyle getWatermarkStyle() {
		return BOM_QUOTATION_WATERMARK_STYLE;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * 回傳 BOM 報價單專屬之 PAdES-BES 合規數位簽章規範。
	 * </p>
	 *
	 * @return PAdES 合規簽章規範實例
	 */
	@Override
	public PdfSignaturePolicy getSignaturePolicy() {
		return BOM_SIGNATURE_POLICY;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * 回傳 BOM 報價單專屬之核准簽章視覺外觀客製化器。
	 * </p>
	 *
	 * @return 視覺簽章客製化器實例
	 */
	@Override
	public PdfVisualSignatureCustomizer getVisualSignatureCustomizer() {
		return BOM_VISUAL_CUSTOMIZER;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return 預設簽核事由說明 "BOM 原物料報價核准簽署"
	 */
	@Override
	public String getDefaultSignatureReason() {
		return "BOM 原物料報價核准簽署";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return 預設簽核地點說明 "台北營運總部"
	 */
	@Override
	public String getDefaultSignatureLocation() {
		return "台北營運總部";
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * 回傳視覺印章左下角 X 座標 (80pt)，精準對齊 BOM 報價單「經辦/主管簽核」欄位左界。
	 * </p>
	 *
	 * @return X 座標 80f
	 */
	@Override
	public Float getDefaultSignatureX() {
		return 80f; // 精準對齊 BOM 報價單「經辦/主管簽核」簽章格位
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * 回傳視覺印章左下角 Y 座標 (50pt)，精準對齊 BOM 報價單底端簽核方框底界。
	 * </p>
	 *
	 * @return Y 座標 50f
	 */
	@Override
	public Float getDefaultSignatureY() {
		return 50f;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * 回傳視覺印章寬度 (180pt)。
	 * </p>
	 *
	 * @return 印章寬度 180f
	 */
	@Override
	public Float getDefaultSignatureWidth() {
		return 180f;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * 回傳視覺印章高度 (62pt)。
	 * </p>
	 *
	 * @return 印章高度 62f
	 */
	@Override
	public Float getDefaultSignatureHeight() {
		return 62f;
	}

	/**
	 * 執行 BOM 報價單標準排版渲染（無浮水印）
	 *
	 * @param quotation    報價單資料 DTO
	 * @param outputStream 目標 PDF 輸出串流
     */
	@Override
	public void render(QuotationGottenResult quotation, OutputStream outputStream) {
		render(quotation, outputStream, null, getWatermarkStyle());
	}

	/**
	 * 執行 BOM 報價單標準排版渲染（套用預設浮水印樣式）
	 *
	 * @param quotation     報價單資料 DTO
	 * @param outputStream  目標 PDF 輸出串流
	 * @param watermarkText 浮水印文字 (例如 "正式報價"、"機密")
     */
	@Override
	public void render(QuotationGottenResult quotation, OutputStream outputStream, String watermarkText) {
		render(quotation, outputStream, watermarkText, getWatermarkStyle());
	}

	/**
	 * 執行 BOM 報價單高精度排版渲染核心邏輯
	 * <p>
	 * <b>標準渲染管線 (Rendering Pipeline)：</b>
	 * <ol>
	 *   <li><b>防禦性檢查：</b>驗證 {@code quotation} 與 {@code outputStream} 不可為 null。</li>
	 *   <li><b>文檔與頁面配置：</b>建立標準 A4 尺寸 {@link Document}，設定左右邊距 32pt、上下邊距 36pt。</li>
	 *   <li><b>掛載分頁事件：</b>建立並掛載 {@link BomQuotationPageEventHelper}，注入字型並配置浮水印。</li>
	 *   <li><b>區塊 1：公司抬頭與報價單號：</b>調用 {@link #renderHeader}，建立左右雙欄標頭。</li>
	 *   <li><b>視覺分隔線：</b>加入主品牌主題藍 2pt 裝飾線。</li>
	 *   <li><b>區塊 2：客戶與交易條件網格：</b>調用 {@link #renderCustomerGrid}，建立 4 欄鍵值對資訊框。</li>
	 *   <li><b>區塊 3：智慧防孤兒物料清單與結尾區塊協同渲染：</b>調用 {@link #renderBomAndFooterSections}，
	 *       執行高精度防孤兒跨頁演算法。</li>
	 *   <li><b>關閉文檔：</b>完成 PdfTemplate 總頁數回填並關閉輸出串流。</li>
	 * </ol>
	 * </p>
	 *
	 * @param quotation      報價單資料 DTO
	 * @param outputStream   目標 PDF 輸出串流
	 * @param watermarkText  可選之防偽浮水印文字 (若為 null 則不印製)
	 * @param watermarkStyle 浮水印視覺樣式 (若為 null 則自動套用 {@link #getWatermarkStyle()})
     */
	@Override
	public void render(QuotationGottenResult quotation, OutputStream outputStream, String watermarkText, OpenPdfWatermarkStyle watermarkStyle) {
		if (quotation == null) {
			throw new IllegalArgumentException("報價單資料 (QuotationGottenResult) 不可為 null");
		}
		if (outputStream == null) {
			throw new IllegalArgumentException("輸出串流 (outputStream) 不可為 null");
		}

		// 建立 A4 文件 (左右邊距 32pt，上下邊距 36pt)
		Document document = new Document(PageSize.A4, 32f, 32f, 36f, 36f);
		PdfWriter writer = PdfWriter.getInstance(document, outputStream);

		// 掛載 BOM 報價單專屬分頁與頁碼控制器 (第 X 頁 / 共 Y 頁) 及可選浮水印
		OpenPdfPageEventHelper pageEventHelper = new BomQuotationPageEventHelper(fontProvider.getBaseFont());
		if (watermarkText != null && !watermarkText.trim().isEmpty()) {
			pageEventHelper.setWatermarkText(watermarkText.trim());
			OpenPdfWatermarkStyle effectiveStyle = watermarkStyle != null ? watermarkStyle : getWatermarkStyle();
			pageEventHelper.setWatermarkStyle(effectiveStyle);
		}
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
		log.info("BOM 報價單 PDF 渲染成功: quotationNo={}, watermark={}", quotation.quotationNo(), watermarkText);
	}

	/**
	 * 渲染公司頂部標頭與報價單號區塊
	 * <p>
	 * 採用 2 欄比例 (65% : 35%) 排版：
	 * <ul>
	 *   <li>左欄：企業名稱（晨星智造整合科技）、英文副標、統編、電話、傳真與總部地址。</li>
	 *   <li>右欄：展示型主標題「報 價 單」、英文「QUOTATION」、報價單號與報價日期。</li>
	 * </ul>
	 * </p>
	 *
	 * @param document  PDF 文件實例
	 * @param quotation 報價單資料 DTO
     */
	private void renderHeader(Document document, QuotationGottenResult quotation) {
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

		Paragraph quoteTitle = new Paragraph("報 價 單", fontProvider.getDisplayTitleFont());
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
	 * 渲染客戶資訊與商業交易條件網格區塊
	 * <p>
	 * 採用 4 欄對稱比例 (15% : 35% : 15% : 35%) 鍵值對網格：
	 * <ul>
	 *   <li>第 1 列：客戶名稱 / 有效期限</li>
	 *   <li>第 2 列：統一編號 / 付款條件</li>
	 *   <li>第 3 列：聯絡人員 / 交貨地點</li>
	 * </ul>
	 * 標籤儲存格採用淺灰背景 ({@link OpenPdfStylePalette#BG_LIGHT_GRAY}) 靠右對齊，
	 * 數值儲存格採用純白背景靠左對齊，形成高可讀性專業商務外觀。
	 * </p>
	 *
	 * @param document  PDF 文件實例
	 * @param quotation 報價單資料 DTO
     */
	private void renderCustomerGrid(Document document, QuotationGottenResult quotation) {
		PdfPTable infoTable = new PdfPTable(4);
		infoTable.setWidthPercentage(100);
		infoTable.setWidths(new float[]{15f, 35f, 15f, 35f});

		Font labelFont = fontProvider.getBodyBold();
		Font valFont = fontProvider.getBodyRegular();
		Color labelBg = OpenPdfStylePalette.BG_LIGHT_GRAY;
		Color valBg = Color.WHITE;
		Color border = OpenPdfStylePalette.BORDER_GRAY;

		// Row 1
		OpenPdfCellFactory.addKeyValueCell(infoTable, "客戶名稱：", labelFont, true, labelBg, border);
		OpenPdfCellFactory.addKeyValueCell(infoTable, safeString(quotation.customerName()), valFont, false, valBg, border);
		OpenPdfCellFactory.addKeyValueCell(infoTable, "有效期限：", labelFont, true, labelBg, border);
		OpenPdfCellFactory.addKeyValueCell(infoTable, safeString(quotation.validityPeriod()), valFont, false, valBg, border);

		// Row 2
		OpenPdfCellFactory.addKeyValueCell(infoTable, "統一編號：", labelFont, true, labelBg, border);
		OpenPdfCellFactory.addKeyValueCell(infoTable, safeString(quotation.customerTaxId()), valFont, false, valBg, border);
		OpenPdfCellFactory.addKeyValueCell(infoTable, "付款條件：", labelFont, true, labelBg, border);
		OpenPdfCellFactory.addKeyValueCell(infoTable, safeString(quotation.paymentTerm()), valFont, false, valBg, border);

		// Row 3
		OpenPdfCellFactory.addKeyValueCell(infoTable, "聯絡人員：", labelFont, true, labelBg, border);
		OpenPdfCellFactory.addKeyValueCell(infoTable, safeString(quotation.contactPerson()), valFont, false, valBg, border);
		OpenPdfCellFactory.addKeyValueCell(infoTable, "交貨地點：", labelFont, true, labelBg, border);
		OpenPdfCellFactory.addKeyValueCell(infoTable, safeString(quotation.deliveryPlace()), valFont, false, valBg, border);

		document.add(infoTable);
	}

	/**
	 * 智慧防孤兒物料清單 (BOM) 與結尾區塊協同排版核心演算法
	 * <p>
	 * <b>防孤兒區塊 (Anti-Orphan Block) 演算法邏輯：</b>
	 * <ol>
	 *   <li><b>結尾區塊防拆封裝：</b>將金額總計、交易條款與雙方簽核欄位封裝於單一複合區塊
	 *       ({@link #buildFooterCompositeBlock})，並設定 {@code setKeepTogether(true)}，保證絕不中途跨頁拆解。</li>
	 *   <li><b>防孤兒錨點保留 (Anchor Rows)：</b>保留物料清單最後 2 筆明細作為錨點項目。</li>
	 *   <li><b>情境 A (無明細)：</b>印製「（目前無明細品項）」提示列，檢查剩餘空間後印製結尾區塊。</li>
	 *   <li><b>情境 B (極少明細 <= 2 筆)：</b>一次性計算全部明細加結尾區塊所需高度，空間不足則主動換頁。</li>
	 *   <li><b>情境 C (多筆明細 > 2 筆)：</b>
	 *     <ul>
	 *       <li>先繪製前段明細 (Primary Items)，支援自然跨頁與表頭重複。</li>
	 *       <li>調用 {@link OpenPdfPageEventHelper#getRemainingVerticalSpace} 測量當前頁面剩餘垂直可用高度。</li>
	 *       <li>測量最後 2 筆錨點明細（不帶表頭）加上結尾複合區塊之總需求高度。</li>
	 *       <li><b>決策判定：</b>若剩餘高度不足，<b>主動觸發 {@link Document#newPage()}</b>，
	 *           並在新頁面頂部以完整 8 欄表頭帶出最後 2 筆錨點明細與結尾區塊；
	 *           徹底根除最後一頁「只有簽名和金額，沒有任何明細」的孤兒頁缺陷！</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 * </p>
	 *
	 * @param document        PDF 文件實例
	 * @param writer          PDF 寫入器實例
	 * @param pageEventHelper 分頁與版面事件控制器
	 * @param quotation       報價單資料 DTO
     */
	private void renderBomAndFooterSections(Document document, PdfWriter writer,
											OpenPdfPageEventHelper pageEventHelper,
											QuotationGottenResult quotation) {
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

			// 檢查當前頁面剩餘垂直空間是否足以容納指定高度。若剩餘空間小於指定高度，主動執行換頁 (newPage())，
			// 以避免後續區塊被動拆分或變成孤兒區塊。
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
			// 記錄當前頁面成功渲染之明細行數
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
	 * 建立物料清單 (BOM) 明細表格
	 * <p>
	 * 支援指定是否產生 8 欄深藍主題表頭，並支援指定起始列編號以利跨頁斑馬紋交替底色連續計算。
	 * 欄寬比例配置：
	 * <ul>
	 *   <li>項次 (6%)、物料料號 (18%)、品名與規格描述 (34%)、數量 (7%)、單位 (6%)、單價 (10%)、小計 (12%)、備註 (12%)</li>
	 * </ul>
	 * </p>
	 *
	 * @param items         明細項目清單
	 * @param includeHeader 是否印製 8 欄深藍表頭
	 * @param startRowIndex 起始行索引 (用於交替斑馬紋計算)
	 * @return 已完成排版之 {@link PdfPTable} 實例
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
	 *
	 * @param quotation 報價單資料 DTO
	 * @return 結尾防拆複合表格實例
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
	 * 建立備註條款說明與金額小計/稅額/總計表格
	 * <p>
	 * 採用 3 欄比例 (55% : 20% : 25%)：
	 * <ul>
	 *   <li>左側 (55%)：報價與交易條款說明卡片框。</li>
	 *   <li>右側 (跨 2 欄，合計 45%)：未稅合計、營業稅 5% 與總計金額 (Grand Total) 計算列表。</li>
	 * </ul>
	 * </p>
	 *
	 * @param quotation 報價單資料 DTO
	 * @return 匯總與條款表格實例
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

		addCalculationRow(calcTable, "未稅合計 (Subtotal)：", "NT$ " + formatAmount(quotation.subtotalAmount()), regular, bold, false, null);
		addCalculationRow(calcTable, "營業稅 5% (VAT)：", "NT$ " + formatAmount(quotation.taxAmount()), regular, bold, false, null);
		addCalculationRow(calcTable, "總計金額 (Grand Total)：", "NT$ " + formatAmount(quotation.totalAmount()), bold, totalFont, true, OpenPdfStylePalette.BG_SOFT_RED);

		PdfPCell calcContainer = new PdfPCell(calcTable);
		calcContainer.setColspan(2);
		calcContainer.setBorder(Rectangle.BOX);
		calcContainer.setBorderColor(OpenPdfStylePalette.BORDER_GRAY);
		summaryTable.addCell(calcContainer);

		return summaryTable;
	}

	/**
	 * 建立雙方簽核用印表格
	 * <p>
	 * 採用左右等寬 2 欄 (50% : 50%) 方框排版：
	 * <ul>
	 *   <li>左側：賣方（晨星智造）業務經辦與主管簽章欄，預留支援 PAdES 數位憑證蓋印空間。</li>
	 *   <li>右側：買方客戶確認簽回欄，提供客戶簽署與公司印鑑欄位。</li>
	 * </ul>
	 * </p>
	 *
	 * @return 雙方簽核用印表格實例
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
		signLeft.addElement(new Paragraph("\n\n業務經辦：王大衛                    主管簽章：[ 支援 PAdES 數位憑證簽署 ]", regular));
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

	/**
	 * 於 2 欄計算表格中加入單一無邊框計算列 (標籤與數值)
	 *
	 * @param table     目標計算表格 (需為 2 欄)
	 * @param label     左側標籤文字 (例如 "未稅合計 (Subtotal)：")
	 * @param value     右側金額文字 (例如 "NT$ 212,500")
	 * @param labelFont 標籤字型
	 * @param valFont   數值字型
	 * @param isTotal   是否為總計列 (若是則套用強調底色)
	 * @param totalBg   總計列專屬強調底色 (例如 {@link OpenPdfStylePalette#BG_SOFT_RED})
	 */
	private void addCalculationRow(PdfPTable table, String label, String value, Font labelFont, Font valFont, boolean isTotal, Color totalBg) {
		table.addCell(OpenPdfCellFactory.createBorderlessCell(label, labelFont, Element.ALIGN_RIGHT, isTotal ? totalBg : null));
		table.addCell(OpenPdfCellFactory.createBorderlessCell(value, valFont, Element.ALIGN_RIGHT, isTotal ? totalBg : null));
	}

	/**
	 * 字串防禦性 null 處理
	 *
	 * @param str 原始字串
	 * @return 非 null 字串 (若為 null 則回傳空字串 "")
	 */
	private String safeString(String str) {
		return str != null ? str : "";
	}

	/**
	 * 數值金額格式化為千分位格式 (例如 223125 -> "223,125")
	 *
	 * @param amount 原始金額
	 * @return 千分位金額字串 (若為 null 則回傳 "0")
	 */
	private String formatAmount(BigDecimal amount) {
		if (amount == null) {
			return "0";
		}
		return CURRENCY_FORMAT.format(amount);
	}

	/**
	 * 物料清單 (BOM) 報價單專屬分頁與頁尾事件處理器 (BOM Quotation Page Event Helper)
	 * <p>
	 * 繼承通用 {@link OpenPdfPageEventHelper} 積木，內聚 BOM 報價單專屬的分頁與頁尾排版配置：
	 * 1. 於頁底右側 (X: 右邊界 - 55pt, Y: 18pt) 精準印製「第 X 頁 / 共 Y 頁」頁尾頁碼。
	 * 2. 採用專屬字級 (8pt) 與柔和灰字調 ({@link OpenPdfStylePalette#TEXT_MUTED})。
	 * 3. 繼承基底之防偽浮水印與防孤兒區塊高度度量能力，確保跨頁排版品質。
	 * </p>
	 */
	private static class BomQuotationPageEventHelper extends OpenPdfPageEventHelper {

		/**
		 * 建構 BOM 專屬分頁事件處理器
		 *
		 * @param baseFont 基礎中文字型
		 */
		public BomQuotationPageEventHelper(BaseFont baseFont) {
			super(baseFont);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * 於頁底右側精準繪製「第 X 頁 / 共 Y 頁」當前頁碼與總頁數佔位範本。
		 * </p>
		 */
		@Override
		protected void onDrawFooter(PdfWriter writer, Document document, PdfContentByte cb) {
			String currentPageText = "第 " + writer.getPageNumber() + " 頁 / 共 ";
			float startX = document.right() - 55f;
			float textBaseY = 18f;
			drawFooterPageNumberWithTemplate(cb, currentPageText, 8f, OpenPdfStylePalette.TEXT_MUTED, startX, textBaseY);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * 在文件關閉時將最終確認之總頁數填入佔位範本 (例如 "2 頁")。
		 * </p>
		 */
		@Override
		protected void onFillTotalPages(PdfWriter writer, Document document, PdfTemplate totalPagesTemplate, int totalPages) {
			fillTotalPagesTemplate(totalPages + " 頁", 8f, OpenPdfStylePalette.TEXT_MUTED);
		}
	}
}
