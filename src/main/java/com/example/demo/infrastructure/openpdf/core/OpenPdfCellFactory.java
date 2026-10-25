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
	 * 建立無邊框儲存格 (常用於小計合計、備註列表、按鍵佈局等自訂排版)
	 *
	 * @param text      文字內容
	 * @param font      字型
	 * @param alignment 水平對齊方式 (如 Element.ALIGN_RIGHT)
	 * @param bgColor   背景顏色 (若為 null 則透明無背景)
	 * @return PdfPCell 實例
	 */
	public static PdfPCell createBorderlessCell(String text, Font font, int alignment, Color bgColor) {
		PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
		cell.setBorder(Rectangle.NO_BORDER);
		cell.setHorizontalAlignment(alignment);
		cell.setPadding(3f);
		if (bgColor != null) {
			cell.setBackgroundColor(bgColor);
		}
		return cell;
	}

	/**
	 * 加入標準鍵值對 (Key-Value) 網格儲存格
	 *
	 * @param table       目標 PdfPTable
	 * @param text        顯示文字
	 * @param font        字體
	 * @param isKey       是否為鍵/標籤欄位 (鍵向右對齊，值向左對齊)
	 * @param bgColor     背景底色
	 * @param borderColor 邊線顏色
	 */
	public static void addKeyValueCell(PdfPTable table, String text, Font font, boolean isKey, Color bgColor, Color borderColor) {
		PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
		cell.setBackgroundColor(bgColor);
		cell.setBorderColor(borderColor);
		cell.setPadding(4f);
		cell.setHorizontalAlignment(isKey ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.addCell(cell);
	}

	/**
	 * 加入客戶或交易資訊網格儲存格 (標籤或值)
	 *
	 * @deprecated 請改用通用語意之 {@link #addKeyValueCell(PdfPTable, String, Font, boolean, Color, Color)}。
	 */
	@Deprecated
	public static void addInfoCell(PdfPTable table, String text, Font font, boolean isLabel, Color bgColor, Color borderColor) {
		addKeyValueCell(table, text, font, isLabel, bgColor, borderColor);
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
	 * @deprecated 涉及特定 2 欄合計結構排版，建議業務範本自行調用 {@link #createBorderlessCell} 組裝或收斂於各範本內部。
	 */
	@Deprecated
	public static void addCalcRow(PdfPTable table, String label, String value, Font labelFont, Font valFont, boolean isTotal, Color totalBg) {
		table.addCell(createBorderlessCell(label, labelFont, Element.ALIGN_RIGHT, isTotal ? totalBg : null));
		table.addCell(createBorderlessCell(value, valFont, Element.ALIGN_RIGHT, isTotal ? totalBg : null));
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
