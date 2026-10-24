package com.example.demo.infrastructure.openpdf.core;

import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.awt.Color;

/**
 * OpenPDF 儲存格與行排版工廠 (Cell Factory)
 * <p>
 * 負責產生具備標準 padding、邊框、對齊特性的 {@link PdfPCell} 與表格行，
 * 消除範本中的重複樣板代碼並確保視覺規格一致。
 * </p>
 */
public final class OpenPdfCellFactory {

	private OpenPdfCellFactory() {
		// 工具工廠類禁止實例化
	}

	/**
	 * 建立標準表頭儲存格 (置中對齊、指定背景色)
	 *
	 * @param text    表頭文字
	 * @param font    字體
	 * @param bgColor 背景顏色
	 * @return PdfPCell 實例
	 */
	public static PdfPCell createHeaderCell(String text, Font font, Color bgColor) {
		PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
		cell.setBackgroundColor(bgColor);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setPaddingTop(5f);
		cell.setPaddingBottom(5f);
		cell.setBorderColor(bgColor);
		return cell;
	}

	/**
	 * 建立標準內文資料儲存格
	 *
	 * @param text        內文文字
	 * @param font        字體
	 * @param alignment   水平對齊方式 (如 Element.ALIGN_LEFT, Element.ALIGN_CENTER, Element.ALIGN_RIGHT)
	 * @param bgColor     背景顏色
	 * @param borderColor 邊框顏色
	 * @return PdfPCell 實例
	 */
	public static PdfPCell createDataCell(String text, Font font, int alignment, Color bgColor, Color borderColor) {
		PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
		cell.setBackgroundColor(bgColor);
		cell.setBorderColor(borderColor);
		cell.setPaddingTop(4f);
		cell.setPaddingBottom(4f);
		cell.setHorizontalAlignment(alignment);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		return cell;
	}

	/**
	 * 加入客戶或交易資訊網格儲存格 (標籤或值)
	 *
	 * @param table       目標 PdfPTable
	 * @param text        顯示文字
	 * @param font        字體
	 * @param isLabel     是否為標籤欄位 (標籤向右對齊，內容值向左對齊)
	 * @param bgColor     背景底色
	 * @param borderColor 邊線顏色
	 */
	public static void addInfoCell(PdfPTable table, String text, Font font, boolean isLabel, Color bgColor, Color borderColor) {
		PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
		cell.setBackgroundColor(bgColor);
		cell.setBorderColor(borderColor);
		cell.setPadding(4f);
		cell.setHorizontalAlignment(isLabel ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.addCell(cell);
	}

	/**
	 * 加入計算列 (如未稅合計、營業稅、含稅總計)
	 *
	 * @param table     目標 PdfPTable (必須為 2 欄)
	 * @param label     標籤文字 (靠右)
	 * @param value     金額文字 (靠右)
	 * @param labelFont 標籤字體
	 * @param valFont   金額字體
	 * @param isTotal   是否為總計列 (若是則上底色)
	 * @param totalBg   總計底色
	 */
	public static void addCalcRow(PdfPTable table, String label, String value, Font labelFont, Font valFont, boolean isTotal, Color totalBg) {
		PdfPCell cLabel = new PdfPCell(new Phrase(label != null ? label : "", labelFont));
		cLabel.setBorder(Rectangle.NO_BORDER);
		cLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
		cLabel.setPadding(3f);

		PdfPCell cVal = new PdfPCell(new Phrase(value != null ? value : "", valFont));
		cVal.setBorder(Rectangle.NO_BORDER);
		cVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
		cVal.setPadding(3f);

		if (isTotal && totalBg != null) {
			cLabel.setBackgroundColor(totalBg);
			cVal.setBackgroundColor(totalBg);
		}

		table.addCell(cLabel);
		table.addCell(cVal);
	}

	/**
	 * 建立水平裝飾分隔線表格
	 *
	 * @param color         線條顏色
	 * @param thickness     線條粗細 (點數)
	 * @param spacingBefore 前方邊距
	 * @param spacingAfter  後方邊距
	 * @return PdfPTable 實例
	 */
	public static PdfPTable createSeparatorTable(Color color, float thickness, float spacingBefore, float spacingAfter) {
		PdfPTable lineTable = new PdfPTable(1);
		lineTable.setWidthPercentage(100);
		lineTable.setSpacingBefore(spacingBefore);
		lineTable.setSpacingAfter(spacingAfter);

		PdfPCell lineCell = new PdfPCell(new Phrase(""));
		lineCell.setBorder(Rectangle.BOTTOM);
		lineCell.setBorderColorBottom(color);
		lineCell.setBorderWidthBottom(thickness);
		lineCell.setFixedHeight(thickness);
		lineTable.addCell(lineCell);

		return lineTable;
	}
}
