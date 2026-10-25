package com.example.demo.infrastructure.openpdf.core;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;

/**
 * OpenPDF 基礎分頁與版面事件處理器 (Base Page Event Helper)
 * <p>
 * 實作 {@link PdfPageEventHelper}，作為六角形架構中 OpenPDF 通用排版核心積木：
 * 1. 兩階段渲染機制 (Two-pass via {@link PdfTemplate}) 支援動態延遲填寫總頁數佔位符。
 * 2. 頁面事件生命週期鈎子 (Template Method Pattern)：提供 {@link #onDrawHeader}、{@link #onDrawFooter}
 *    與 {@link #onFillTotalPages} 擴充點，交由具體業務單據範本（如報價單、出貨單）自由自訂頁首頁尾排版。
 * 3. 通用防偽浮水印渲染 ({@link #drawWatermark})：完全解耦業務文字，支援任意 {@link OpenPdfWatermarkStyle} 樣式。
 * 4. <b>防孤兒區塊 (Anti-Orphan Block) 排版度量支援</b>：提供頁面剩餘垂直可用高度計算
 *    ({@link #getRemainingVerticalSpace})、主動防孤兒換頁決策 ({@link #ensureSpaceOrNewPage})
 *    以及精準表格高度測量 ({@link #calculateTableHeight})。
 * 5. 追蹤當前頁面已渲染之資料行數，杜絕下方結尾區塊（合計、條款、簽核）孤立在新頁之排版缺陷。
 * </p>
 */
@Slf4j
public class OpenPdfPageEventHelper extends PdfPageEventHelper {

    @Getter
    private final BaseFont baseFont;
    @Getter
    private PdfTemplate totalPagesTemplate;
    @Getter
    private int totalPages = 0;

    /**
     * 當前頁面已渲染之明細資料列計數器 (用於偵測與防範孤兒頁)
     * -- GETTER --
     *  取得當前頁面已渲染之明細行數
     */
    @Getter
    private int itemsRenderedOnCurrentPage = 0;

    /**
     * 可選的水印文字 (如：正式報價、草稿、機密)
     */
    @Getter
    @Setter
    private String watermarkText;

    /**
     * 浮水印外觀樣式配置 (預設為 OpenPdfWatermarkStyle.DEFAULT)
     */
    @Setter
    private OpenPdfWatermarkStyle watermarkStyle = OpenPdfWatermarkStyle.DEFAULT;

    public OpenPdfPageEventHelper(BaseFont baseFont) {
        this.baseFont = baseFont;
    }

    public OpenPdfWatermarkStyle getWatermarkStyle() {
        return watermarkStyle != null ? watermarkStyle : OpenPdfWatermarkStyle.DEFAULT;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        // 建立用於延遲填寫總頁數的 PdfTemplate (寬度 30pt, 高度 16pt)
        totalPagesTemplate = writer.getDirectContent().createTemplate(30, 16);
    }

    @Override
    public void onStartPage(PdfWriter writer, Document document) {
        // 每當開啟新的一頁時，重置當前頁面的明細渲染計數器
        this.itemsRenderedOnCurrentPage = 0;
    }

    /**
     * 記錄當前頁面成功渲染之明細行數
     *
     * @param count 本次加入的明細筆數
     */
    public void recordItemRendered(int count) {
        this.itemsRenderedOnCurrentPage += count;
    }

    /**
     * 當前頁面是否已有明細行
     *
     * @return true 若已有至少 1 筆明細列
     */
    public boolean hasItemsOnCurrentPage() {
        return itemsRenderedOnCurrentPage > 0;
    }

    /**
     * 計算當前頁面排版游標距離底邊界 (document.bottom()) 的剩餘垂直可用高度 (點數 pt)
     *
     * @param writer   當前 PDF 寫入器
     * @param document 當前文件實例
     * @return 剩餘垂直可用高度 (pt)
     */
    public float getRemainingVerticalSpace(PdfWriter writer, Document document) {
        if (writer == null || document == null) {
            return 0f;
        }
        return writer.getVerticalPosition(false) - document.bottom();
    }

    /**
     * 檢查當前頁面剩餘垂直空間是否足以容納指定高度。
     * 若剩餘空間小於指定高度，主動執行換頁 (newPage())，以避免後續區塊被動拆分或變成孤兒區塊。
     *
     * @param writer         當前 PDF 寫入器
     * @param document       當前文件實例
     * @param requiredHeight 預期區塊所需最小高度 (pt)
     * @return true 若觸發了主動換頁；false 若當前頁面空間足夠無需換頁
     */
    public boolean ensureSpaceOrNewPage(PdfWriter writer, Document document, float requiredHeight) {
        float remaining = getRemainingVerticalSpace(writer, document);
        if (remaining < requiredHeight) {
            log.info("【防孤兒排版】頁面剩餘空間 {}pt 不足容納區塊 (需 {}pt)，觸發主動換頁", remaining, requiredHeight);
            document.newPage();
            return true;
        }
        return false;
    }

    /**
     * 精準計算指定 PdfPTable 在當前文件可用寬度下的總高度 (pt)
     *
     * @param table    目標表格
     * @param document 當前文件實例
     * @return 表格在當前寬度約束下之計算總高度 (pt)
     */
    public static float calculateTableHeight(PdfPTable table, Document document) {
        if (table == null || document == null) {
            return 0f;
        }
        float availableWidth = document.right() - document.left();
        table.setTotalWidth(availableWidth);
        table.setLockedWidth(true);
        table.calculateHeights(true);
        return table.getTotalHeight();
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        this.totalPages = writer.getPageNumber();
        PdfContentByte cb = writer.getDirectContent();

        // 1. 繪製浮水印 (若有設定)
        if (watermarkText != null && !watermarkText.trim().isEmpty()) {
            drawWatermark(writer, document, watermarkText);
        }

        // 2. 觸發頁首繪製鈎子 (交由具體單據範本擴充)
        onDrawHeader(writer, document, cb);

        // 3. 觸發頁尾繪製鈎子 (交由具體單據範本自訂)
        onDrawFooter(writer, document, cb);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        // 觸發文件關閉與總頁數填寫鈎子 (交由具體單據範本自訂)
        onFillTotalPages(writer, document, totalPagesTemplate, totalPages);
    }

    /**
     * 頁首繪製擴充鈎子 (Template Method Hook)
     * <p>
     * 預設為空實作。具體單據範本可覆寫此方法，繪製該單據專屬之重複頁首標頭、LOGO 或文件標記。
     * </p>
     *
     * @param writer   PDF 寫入器
     * @param document 當前文件實例
     * @param cb       直接內容畫布 (DirectContent)
     */
    protected void onDrawHeader(PdfWriter writer, Document document, PdfContentByte cb) {
        // 預設為空實作，由具體單據範本覆寫自訂
    }

    /**
     * 頁尾繪製擴充鈎子 (Template Method Hook)
     * <p>
     * 預設為空實作。具體單據範本可覆寫此方法，繪製該單據專屬之頁尾排版（例如「第 X 頁 / 共 Y 頁」、特定座標與樣式）。
     * </p>
     *
     * @param writer   PDF 寫入器
     * @param document 當前文件實例
     * @param cb       直接內容畫布 (DirectContent)
     */
    protected void onDrawFooter(PdfWriter writer, Document document, PdfContentByte cb) {
        // 預設為空實作，由具體單據範本覆寫自訂
    }

    /**
     * 總頁數範本填寫擴充鈎子 (Template Method Hook)
     * <p>
     * 預設為空實作。具體單據範本若使用兩階段渲染總頁數，可覆寫此方法將最終總頁數填入 PdfTemplate。
     * </p>
     *
     * @param writer             PDF 寫入器
     * @param document           當前文件實例
     * @param totalPagesTemplate 總頁數佔位範本
     * @param totalPages         計算得出之總頁數
     */
    protected void onFillTotalPages(PdfWriter writer, Document document, PdfTemplate totalPagesTemplate, int totalPages) {
        // 預設為空實作，由具體單據範本覆寫自訂
    }

    /**
     * 於指定座標繪製帶有兩階段總頁數佔位符的標準頁碼輔助方法
     * <p>
     * 封裝 OpenPDF 底層繪製當前頁碼與掛載 {@link PdfTemplate} 之樣板代碼，
     * 提供具體範本在 {@link #onDrawFooter} 中快速調用。
     * </p>
     *
     * @param cb              直接內容畫布
     * @param currentPageText 當前頁碼前置文字 (例如 "第 1 頁 / 共 ")
     * @param fontSize        字型大小
     * @param fontColor       字型顏色
     * @param startX          文字起始 X 座標
     * @param startY          文字基準 Y 座標
     */
    protected void drawFooterPageNumberWithTemplate(PdfContentByte cb, String currentPageText,
                                                   float fontSize, Color fontColor,
                                                   float startX, float startY) {
        if (baseFont == null || totalPagesTemplate == null || cb == null) {
            return;
        }

        float textSize = baseFont.getWidthPoint(currentPageText, fontSize);

        cb.saveState();
        cb.beginText();
        cb.setFontAndSize(baseFont, fontSize);
        if (fontColor != null) {
            cb.setColorFill(fontColor);
        }
        cb.setTextMatrix(startX, startY);
        cb.showText(currentPageText);
        cb.endText();

        // 將總頁數範本貼在當前頁碼文字之後
        cb.addTemplate(totalPagesTemplate, startX + textSize, startY);
        cb.restoreState();
    }

    /**
     * 將總頁數文字填入 PdfTemplate 的輔助方法
     * <p>
     * 封裝 OpenPDF 底層在文件關閉時將計算出之總頁數填入佔位範本之樣板代碼，
     * 提供具體範本在 {@link #onFillTotalPages} 中快速調用。
     * </p>
     *
     * @param totalPagesText 總頁數後綴文字 (例如 "2 頁" 或 "2")
     * @param fontSize       字型大小
     * @param fontColor      字型顏色
     */
    protected void fillTotalPagesTemplate(String totalPagesText, float fontSize, Color fontColor) {
        if (totalPagesTemplate != null && baseFont != null) {
            totalPagesTemplate.beginText();
            totalPagesTemplate.setFontAndSize(baseFont, fontSize);
            if (fontColor != null) {
                totalPagesTemplate.setColorFill(fontColor);
            }
            totalPagesTemplate.showText(totalPagesText);
            totalPagesTemplate.endText();
        }
    }

    /**
     * 於頁面中央繪製傾斜半透明浮水印 (依據 watermarkStyle 樣式屬性，支援次級副標與防偽鋼印外框)
     */
    protected void drawWatermark(PdfWriter writer, Document document, String text) {
        try {
            OpenPdfWatermarkStyle style = getWatermarkStyle();
            PdfContentByte under = writer.getDirectContentUnder();
            under.saveState();

            // 1. 設定圖層不透明度
            PdfGState gs = new PdfGState();
            gs.setFillOpacity(style.opacity());
            under.setGState(gs);

            // 2. 計算頁面中心點與旋轉矩陣 (以中心為原點 (0,0) 進行旋轉排版)
            float centerX = (document.left() + document.right()) / 2f;
            float centerY = (document.top() + document.bottom()) / 2f;
            double rad = Math.toRadians(style.rotation());
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            under.concatCTM(cos, sin, -sin, cos, centerX, centerY);

            // 3. 測量文字長度與垂直排版座標
            float mainWidth = baseFont.getWidthPoint(text, style.fontSize());
            boolean hasSub = style.subText() != null && !style.subText().trim().isEmpty();
            float subFontSize = Math.max(8.5f, style.fontSize() * 0.22f);
            float subWidth = hasSub ? baseFont.getWidthPoint(style.subText().trim(), subFontSize) : 0f;

            float mainY = hasSub ? 2f : (-style.fontSize() * 0.32f);
            float subY = hasSub ? (-style.fontSize() * 0.40f - subFontSize) : 0f;

            // 4. 繪製防偽鋼印圓角雙外框 (若啟用 boxed)
            if (style.boxed()) {
                float boxW = Math.max(mainWidth, subWidth) + 38f;
                float boxH = style.fontSize() + (hasSub ? subFontSize + 26f : 20f);
                float boxX = -boxW / 2f;
                float boxY = -boxH / 2f;

                // 主外框 (線寬 1.5pt, 圓角 6pt)
                under.setLineWidth(1.5f);
                under.setColorStroke(style.color());
                under.roundRectangle(boxX, boxY, boxW, boxH, 6f);
                under.stroke();

                // 內細裝飾線 (線寬 0.6pt, 圓角 4pt, 間距 3pt)
                under.setLineWidth(0.6f);
                under.roundRectangle(boxX + 3f, boxY + 3f, boxW - 6f, boxH - 6f, 4f);
                under.stroke();
            }

            // 5. 繪製主浮水印文字與副標
            under.beginText();
            under.setFontAndSize(baseFont, style.fontSize());
            under.setColorFill(style.color());
            under.showTextAligned(Element.ALIGN_CENTER, text, 0f, mainY, 0f);

            if (hasSub) {
                under.setFontAndSize(baseFont, subFontSize);
                under.setColorFill(style.color());
                under.showTextAligned(Element.ALIGN_CENTER, style.subText().trim(), 0f, subY, 0f);
            }
            under.endText();

            under.restoreState();
        } catch (Exception e) {
            log.warn("繪製浮水印發生非致命例外: text={}", text, e);
        }
    }
}
