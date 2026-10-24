package com.example.demo.infrastructure.openpdf.core;

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

import java.awt.*;

/**
 * OpenPDF 分頁與動態頁碼事件處理器 (Page Event Helper)
 * <p>
 * 實作 {@link PdfPageEventHelper}，核心職責包含：
 * 1. 兩階段渲染機制 (Two-pass via PdfTemplate) 動態計算並填入「第 X 頁 / 共 Y 頁」精確總頁數。
 * 2. 頁首頁尾繪製及可選的半透明防偽浮水印（Watermark）。
 * 3. <b>防孤兒區塊 (Anti-Orphan Block) 排版度量支援</b>：提供頁面剩餘垂直可用高度計算
 *    ({@link #getRemainingVerticalSpace})、主動防孤兒換頁決策 ({@link #ensureSpaceOrNewPage})
 *    以及精準表格高度測量 ({@link #calculateTableHeight})。
 * 4. 追蹤當前頁面已渲染之資料行數，杜絕下方結尾區塊（合計、條款、簽核）孤立在新頁之排版缺陷。
 * </p>
 */
@Slf4j
public class OpenPdfPageEventHelper extends PdfPageEventHelper {

    private final BaseFont baseFont;
    private PdfTemplate totalPagesTemplate;
    private int totalPages = 0;

    /**
     * 當前頁面已渲染之明細資料列計數器 (用於偵測與防範孤兒頁)
     */
    private int itemsRenderedOnCurrentPage = 0;

    /**
     * 可選的水印文字 (如：正式報價、草稿、機密)
     */
    @Setter
    private String watermarkText;

    public OpenPdfPageEventHelper(BaseFont baseFont) {
        this.baseFont = baseFont;
    }

    public String getWatermarkText() {
        return watermarkText;
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
     * 取得當前頁面已渲染之明細行數
     *
     * @return 當前頁面已有的明細列數量
     */
    public int getItemsRenderedOnCurrentPage() {
        return itemsRenderedOnCurrentPage;
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

        // 2. 繪製頁尾「第 X 頁 / 共 Y 頁」
        drawFooterPageNumber(writer, document, cb);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        // 文件關閉時將計算出的總頁數寫入 PdfTemplate
        if (totalPagesTemplate != null && baseFont != null) {
            totalPagesTemplate.beginText();
            totalPagesTemplate.setFontAndSize(baseFont, 8f);
            totalPagesTemplate.setColorFill(OpenPdfStylePalette.TEXT_MUTED);
            totalPagesTemplate.showText(totalPages + " 頁");
            totalPagesTemplate.endText();
        }
    }

    /**
     * 於頁底右側繪製當前頁碼與總頁數佔位符
     */
    private void drawFooterPageNumber(PdfWriter writer, Document document, PdfContentByte cb) {
        if (baseFont == null || totalPagesTemplate == null) {
            return;
        }

        String currentPageText = "第 " + writer.getPageNumber() + " 頁 / 共 ";
        float textSize = baseFont.getWidthPoint(currentPageText, 8f);
        float textBaseY = 18f;
        float startX = document.right() - 55f;

        cb.saveState();
        cb.beginText();
        cb.setFontAndSize(baseFont, 8f);
        cb.setColorFill(OpenPdfStylePalette.TEXT_MUTED);
        cb.setTextMatrix(startX, textBaseY);
        cb.showText(currentPageText);
        cb.endText();

        // 將總頁數範本貼在當前頁碼文字之後
        cb.addTemplate(totalPagesTemplate, startX + textSize, textBaseY);
        cb.restoreState();
    }

    /**
     * 於頁面中央繪製傾斜半透明浮水印
     */
    private void drawWatermark(PdfWriter writer, Document document, String text) {
        try {
            PdfContentByte under = writer.getDirectContentUnder();
            under.saveState();

            PdfGState gs = new PdfGState();
            gs.setFillOpacity(0.07f);
            under.setGState(gs);

            under.beginText();
            under.setFontAndSize(baseFont, 46f);
            under.setColorFill(Color.DARK_GRAY);

            float centerX = (document.left() + document.right()) / 2f;
            float centerY = (document.top() + document.bottom()) / 2f;
            under.showTextAligned(Element.ALIGN_CENTER, text, centerX, centerY, 45f);

            under.endText();
            under.restoreState();
        } catch (Exception e) {
            log.warn("繪製浮水印發生非致命例外: text={}", text, e);
        }
    }
}
