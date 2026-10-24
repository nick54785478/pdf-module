package com.example.demo.infrastructure.adapter;

import com.example.demo.application.shared.dto.PdfColumnRect;
import com.example.demo.application.shared.dto.PdfTextPositionGottenResult;
import com.example.demo.application.port.out.PdfExtractorPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * PDFBox 內容抽取適配器 (Outbound Adapter)
 * <p>
 * 遵循六角形架構規範：
 * 1. 位於基礎設施層 {@code infrastructure.adapter}。
 * 2. 命名規範結尾為 {@code Adapter}。
 * 3. 實作應用層 {@link PdfExtractorPort} 輸出端口，使用 Apache PDFBox 進行 PDF 解析與文字抽取。
 */
@Slf4j
@Component
public class PdfExtractorAdapter implements PdfExtractorPort {

    /**
     * 讀取 PDF 文字檔 (從 InputStream，全部頁數)
     *
     * @param inputStream PDF 輸入流
     * @return PDF 文字內容
     */
    @Override
    public String readPdfContent(InputStream inputStream) {
        try (PDDocument doc = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        } catch (IOException e) {
            log.error("讀取 PDF 發生錯誤: ", e);
            return null;
        }
    }

    /**
     * 讀取 PDF 文字檔 (從 InputStream，指定頁數範圍)
     *
     * @param inputStream PDF 輸入流
     * @param startPage   起始頁 (從 1 開始)
     * @param endPage     結束頁
     * @return 指定頁數範圍的 PDF 文字內容
     */
    @Override
    public String readPdfContent(InputStream inputStream, int startPage, int endPage) {
        try (PDDocument doc = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(startPage);
            stripper.setEndPage(endPage);
            return stripper.getText(doc);
        } catch (IOException e) {
            log.error("讀取 PDF 指定頁失敗: ", e);
            return null;
        }
    }

    /**
     * 逐頁抽取 PDF 文字
     *
     * @param inputStream PDF 輸入流
     * @return Map&lt;頁碼, 文字內容&gt;
     */
    @Override
    public Map<Integer, String> readPdfByPage(InputStream inputStream) {
        Map<Integer, String> pageTextMap = new LinkedHashMap<>();
        try (PDDocument doc = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = doc.getNumberOfPages();
            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(doc);
                pageTextMap.put(page, text);
            }
        } catch (IOException e) {
            log.error("逐頁讀取 PDF 發生錯誤: ", e);
        }
        return pageTextMap;
    }

    /**
     * 讀取 PDF 文字檔 (從檔案路徑)
     *
     * @param path PDF 檔案路徑
     * @return 文字內容
     */
    @Override
    public String readPdfFromPath(String path) {
        File file = new File(path);
        if (!file.exists()) {
            log.error("讀取文件失敗，文件不存在: {}", path);
            return null;
        }
        try (InputStream is = new FileInputStream(file)) {
            return readPdfContent(is);
        } catch (IOException e) {
            log.error("讀取 PDF 發生錯誤: ", e);
            return null;
        }
    }

    /**
     * 讀取 PDF 指定頁面區域文字
     *
     * @param inputStream PDF 輸入流
     * @param pageNo      頁碼 (從 1 開始)
     * @param x           區域左上角 X 座標
     * @param y           區域左上角 Y 座標
     * @param width       區域寬度
     * @param height      區域高度
     * @return 區域文字內容
     */
    @Override
    public String readPdfArea(InputStream inputStream, int pageNo, float x, float y, float width, float height) {
        try (PDDocument doc = Loader.loadPDF(inputStream.readAllBytes())) {
            if (pageNo < 1 || pageNo > doc.getNumberOfPages()) {
                log.error("頁碼超出範圍: {}", pageNo);
                return null;
            }

            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            Rectangle2D rect = new Rectangle2D.Float(x, y, width, height);
            String regionName = "region";
            stripper.addRegion(regionName, rect);
            stripper.extractRegions(doc.getPage(pageNo - 1));

            return stripper.getTextForRegion(regionName);
        } catch (IOException e) {
            log.error("讀取 PDF 區域文字發生錯誤: ", e);
            return null;
        }
    }

    /**
     * 自動抽取整頁表格 (依列高度迭代偵測)
     *
     * @param inputStream PDF 輸入流
     * @param pageNo      頁碼 (從 1 開始)
     * @param colRects    每個欄位的水平座標與寬度定義清單
     * @param startY      首列左上角 Y
     * @param rowHeight   每列高度
     * @return 表格資料 (List&lt;List&lt;String&gt;&gt;)
     */
    @Override
    public List<List<String>> extractFullPageTable(InputStream inputStream, int pageNo,
                                                   List<PdfColumnRect> colRects, float startY, float rowHeight) {
        List<List<String>> table = new ArrayList<>();

        try (PDDocument doc = Loader.loadPDF(inputStream.readAllBytes())) {
            if (pageNo < 1 || pageNo > doc.getNumberOfPages()) {
                throw new IllegalArgumentException("頁碼超出範圍");
            }

            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            int rowIndex = 0;

            while (true) {
                float currentY = startY - rowIndex * rowHeight;
                boolean hasText = false;
                List<String> row = new ArrayList<>();

                // 設定每個欄位的區域
                for (int colIndex = 0; colIndex < colRects.size(); colIndex++) {
                    PdfColumnRect col = colRects.get(colIndex);
                    Rectangle2D.Float rect = new Rectangle2D.Float(col.x(), currentY, col.width(), rowHeight);
                    String regionName = "cell_" + rowIndex + "_" + colIndex;
                    stripper.addRegion(regionName, rect);
                }

                stripper.extractRegions(doc.getPage(pageNo - 1));

                // 讀取每欄文字
                for (int colIndex = 0; colIndex < colRects.size(); colIndex++) {
                    String text = stripper.getTextForRegion("cell_" + rowIndex + "_" + colIndex);
                    if (text != null && !text.trim().isEmpty()) {
                        hasText = true;
                    }
                    row.add(text != null ? text.trim() : "");
                }

                stripper.getRegions().clear(); // 清除上一列區域

                if (!hasText) {
                    break; // 如果整列沒有文字，停止迴圈
                }

                table.add(row);
                rowIndex++;
            }

        } catch (Exception e) {
            log.error("自動抽取表格發生錯誤: ", e);
        }

        return table;
    }

    /**
     * 抽取 PDF 指定頁所有文字與座標
     *
     * @param inputStream PDF 輸入流
     * @param pageNo      頁碼 (從 1 開始)
     * @return 文字資訊清單 (封裝為 {@link PdfTextPositionGottenResult})
     */
    @Override
    public List<PdfTextPositionGottenResult> extractTextPositions(InputStream inputStream, int pageNo) {
        List<PdfTextPositionGottenResult> textPositions = new ArrayList<>();

        try (PDDocument doc = Loader.loadPDF(inputStream.readAllBytes())) {
            if (pageNo < 1 || pageNo > doc.getNumberOfPages()) {
                throw new IllegalArgumentException("頁碼超出範圍: " + pageNo);
            }

            // 使用覆寫的 PDFTextStripper 收集座標
            PDFTextStripper stripper = getPdfTextStripper(pageNo, textPositions);
            stripper.getText(doc);

        } catch (Exception e) {
            log.error("抽取文字座標發生錯誤: ", e);
        }

        return textPositions;
    }

    /**
     * 建立 PDFTextStripper 並覆寫 processTextPosition 以收集座標
     */
    private PDFTextStripper getPdfTextStripper(int pageNo, List<PdfTextPositionGottenResult> textPositions) {
        PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void processTextPosition(TextPosition text) {
                textPositions.add(new PdfTextPositionGottenResult(
                        text.getUnicode(),
                        text.getXDirAdj(),
                        text.getYDirAdj(),
                        text.getWidthDirAdj(),
                        text.getHeightDir()
                ));
            }
        };
        stripper.setStartPage(pageNo);
        stripper.setEndPage(pageNo);
        return stripper;
    }

    /**
     * 從 PDF 指定頁抽取表格資料 (未過濾列)
     *
     * @param inputStream     PDF 資料流
     * @param pageNo          要抽取的頁碼 (從 1 開始)
     * @param yTolerance      同一列 Y 座標容差 (pt)
     * @param xMergeThreshold 合併同欄文字的 X 座標容差 (pt)
     * @return 表格資料 (外層 List 是列，內層 List 是欄)
     */
    @Override
    public List<List<String>> extractTable(InputStream inputStream, int pageNo, float yTolerance,
                                           float xMergeThreshold) {
        return extractTableInternal(inputStream, pageNo, yTolerance, xMergeThreshold, 0);
    }

    /**
     * 從 PDF 指定頁抽取表格資料，並過濾非表格列，自動補空欄位
     *
     * @param inputStream     PDF 資料流
     * @param pageNo          要抽取的頁碼 (從 1 開始)
     * @param yTolerance      同一列 Y 座標容差 (pt)
     * @param xMergeThreshold 合併同欄文字的 X 座標容差 (pt)
     * @param expectedColumns 預期欄位數，不足自動補空字串
     * @return 表格資料
     */
    @Override
    public List<List<String>> extractCleanedTable(InputStream inputStream, int pageNo, float yTolerance,
                                                  float xMergeThreshold, int expectedColumns) {
        return extractTableInternal(inputStream, pageNo, yTolerance, xMergeThreshold, expectedColumns);
    }

    /**
     * 核心邏輯：依文字座標分列/分欄，合併相近文字，並支援欄位補齊
     */
    private List<List<String>> extractTableInternal(InputStream inputStream, int pageNo, float yTolerance,
                                                    float xMergeThreshold, int expectedColumns) {
        List<PdfTextPositionGottenResult> positions = extractTextPositions(inputStream, pageNo);

        // Step 1: 依 Y 座標分列 (由上而下)
        Map<Float, List<PdfTextPositionGottenResult>> rowsMap = new TreeMap<>(Collections.reverseOrder());
        for (PdfTextPositionGottenResult pos : positions) {
            float y = pos.y();
            boolean found = false;
            for (Float key : rowsMap.keySet()) {
                if (Math.abs(key - y) <= yTolerance) {
                    rowsMap.get(key).add(pos);
                    found = true;
                    break;
                }
            }
            if (!found) {
                List<PdfTextPositionGottenResult> list = new ArrayList<>();
                list.add(pos);
                rowsMap.put(y, list);
            }
        }

        // Step 2: 每列依 X 排序 + 合併相近文字
        List<List<String>> table = new ArrayList<>();
        for (List<PdfTextPositionGottenResult> rowPositions : rowsMap.values()) {
            List<PdfTextPositionGottenResult> sortedRow = rowPositions.stream()
                    .sorted(Comparator.comparingDouble(PdfTextPositionGottenResult::x))
                    .toList();

            List<String> row = new ArrayList<>();
            double lastX = -Double.MAX_VALUE;

            for (PdfTextPositionGottenResult cell : sortedRow) {
                double x = cell.x();
                String text = cell.text();

                if (x - lastX <= xMergeThreshold && !row.isEmpty()) {
                    int lastIndex = row.size() - 1;
                    row.set(lastIndex, row.get(lastIndex) + text);
                } else {
                    row.add(text != null ? text : "");
                }

                lastX = x;
            }

            // Step 3: 過濾非表格列 + 補齊欄位
            if (expectedColumns > 0) {
                if (row.size() >= expectedColumns) {
                    while (row.size() < expectedColumns) {
                        row.add("");
                    }
                    table.add(row);
                }
            } else {
                table.add(row);
            }
        }
        return table;
    }
}
