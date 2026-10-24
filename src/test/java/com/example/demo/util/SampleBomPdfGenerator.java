package com.example.demo.util;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 物料清單報價單 (BOM Quotation) PDF 範本產生器
 */
public class SampleBomPdfGenerator {

	@Test
	void generateBomQuotationPdf() throws Exception {

		// 確保目標輸出目錄存在
		File targetDir1 = new File("src/main/resources/example");
		if (!targetDir1.exists()) {
			targetDir1.mkdirs();
		}
		File targetDir2 = new File("resource/example");
		if (!targetDir2.exists()) {
			targetDir2.mkdirs();
		}

		File outputFile1 = new File(targetDir1, "bom_quotation_template.pdf");
		File outputFile2 = new File(targetDir2, "bom_quotation_template.pdf");

		createQuotationPdf(outputFile1);
		createQuotationPdf(outputFile2);

		assertTrue(outputFile1.exists() && outputFile1.length() > 0);
		assertTrue(outputFile2.exists() && outputFile2.length() > 0);
	}

	public static void createQuotationPdf(File file) throws Exception {
		Document document = new Document(PageSize.A4, 32, 32, 36, 36);
		PdfWriter.getInstance(document, new FileOutputStream(file));
		document.open();

		// 字體設定：優先使用專案 resources/fonts/MSJHL.TTC (微軟正黑體 Light)
		BaseFont bf = loadChineseBaseFont();

		Color primaryColor = new Color(26, 54, 93);      // 深藍
		Color secondaryColor = new Color(237, 242, 247); // 淺灰底
		Color accentColor = new Color(49, 130, 206);     // 裝飾藍
		Color borderColor = new Color(203, 213, 225);    // 邊界灰
		Color darkText = new Color(30, 41, 59);

		Font titleFont = new Font(bf, 18, Font.BOLD, primaryColor);
		Font subtitleFont = new Font(bf, 8.5f, Font.NORMAL, new Color(100, 116, 139));
		Font headerFont = new Font(bf, 8.5f, Font.BOLD, Color.WHITE);
		Font cellBold = new Font(bf, 8f, Font.BOLD, darkText);
		Font cellRegular = new Font(bf, 8f, Font.NORMAL, darkText);
		Font cellMuted = new Font(bf, 7.5f, Font.NORMAL, new Color(100, 116, 139));
		Font totalBigFont = new Font(bf, 11f, Font.BOLD, new Color(197, 48, 48));

		// 1. 公司與報價單標題區 (Header Table)
		PdfPTable headerTable = new PdfPTable(2);
		headerTable.setWidthPercentage(100);
		headerTable.setWidths(new float[]{65f, 35f});

		// 左側：公司資訊
		PdfPCell compCell = new PdfPCell();
		compCell.setBorder(Rectangle.NO_BORDER);
		Paragraph compName = new Paragraph("晨星智造整合科技股份有限公司", titleFont);
		Paragraph compSub = new Paragraph("MorningStar Smart Manufacturing Integration Co., Ltd.", subtitleFont);
		Paragraph compDetails = new Paragraph("統一編號：88996633  |  電話：(02) 2788-9900  |  傳真：(02) 2788-9901\n地址：115 台北市南港區軟體園區二路 88 號 12 樓", cellMuted);
		compDetails.setSpacingBefore(3);
		compCell.addElement(compName);
		compCell.addElement(compSub);
		compCell.addElement(compDetails);
		headerTable.addCell(compCell);

		// 右側：報價單標籤與單號
		PdfPCell quoteCell = new PdfPCell();
		quoteCell.setBorder(Rectangle.NO_BORDER);
		quoteCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		Paragraph quoteTitle = new Paragraph("報 價 單", new Font(bf, 22, Font.BOLD, accentColor));
		quoteTitle.setAlignment(Element.ALIGN_RIGHT);
		Paragraph quoteEn = new Paragraph("QUOTATION", new Font(bf, 10, Font.BOLD, new Color(148, 163, 184)));
		quoteEn.setAlignment(Element.ALIGN_RIGHT);
		Paragraph quoteNo = new Paragraph("單號：QT-20261023-001\n日期：2026-10-23", cellBold);
		quoteNo.setAlignment(Element.ALIGN_RIGHT);
		quoteNo.setSpacingBefore(4);
		quoteCell.addElement(quoteTitle);
		quoteCell.addElement(quoteEn);
		quoteCell.addElement(quoteNo);
		headerTable.addCell(quoteCell);

		document.add(headerTable);

		// 分隔線
		PdfPTable lineTable = new PdfPTable(1);
		lineTable.setWidthPercentage(100);
		lineTable.setSpacingBefore(8);
		lineTable.setSpacingAfter(8);
		PdfPCell lineCell = new PdfPCell(new Phrase(""));
		lineCell.setBorder(Rectangle.BOTTOM);
		lineCell.setBorderColorBottom(accentColor);
		lineCell.setBorderWidthBottom(2f);
		lineCell.setFixedHeight(2f);
		lineTable.addCell(lineCell);
		document.add(lineTable);

		// 2. 客戶與交易條件區塊 (Info Table)
		PdfPTable infoTable = new PdfPTable(4);
		infoTable.setWidthPercentage(100);
		infoTable.setWidths(new float[]{15f, 35f, 15f, 35f});

		addInfoCell(infoTable, "客戶名稱：", cellBold, true, secondaryColor);
		addInfoCell(infoTable, "台灣智能科技股份有限公司", cellRegular, false, Color.WHITE);
		addInfoCell(infoTable, "有效期限：", cellBold, true, secondaryColor);
		addInfoCell(infoTable, "30 天 (至 2026-11-22)", cellRegular, false, Color.WHITE);

		addInfoCell(infoTable, "統一編號：", cellBold, true, secondaryColor);
		addInfoCell(infoTable, "12345678", cellRegular, false, Color.WHITE);
		addInfoCell(infoTable, "付款條件：", cellBold, true, secondaryColor);
		addInfoCell(infoTable, "月結 30 天 (Net 30)", cellRegular, false, Color.WHITE);

		addInfoCell(infoTable, "聯絡人員：", cellBold, true, secondaryColor);
		addInfoCell(infoTable, "陳建銘 經理 / (03) 578-1234 #567", cellRegular, false, Color.WHITE);
		addInfoCell(infoTable, "交貨地點：", cellBold, true, secondaryColor);
		addInfoCell(infoTable, "新竹市科學園區研發六路 12 號 (DDP)", cellRegular, false, Color.WHITE);

		document.add(infoTable);

		// 間距
		Paragraph space = new Paragraph(" ");
		space.setLeading(10);
		document.add(space);

		// 3. 物料清單表格 (BOM Table)
		PdfPTable table = new PdfPTable(8);
		table.setWidthPercentage(100);
		table.setWidths(new float[]{6f, 18f, 34f, 7f, 6f, 10f, 12f, 12f});

		// 表頭
		String[] headers = {"項次", "物料料號", "品名與規格描述", "數量", "單位", "單價 (TWD)", "小計 (TWD)", "備註"};
		for (String header : headers) {
			PdfPCell hCell = new PdfPCell(new Phrase(header, headerFont));
			hCell.setBackgroundColor(primaryColor);
			hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
			hCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			hCell.setPaddingTop(5f);
			hCell.setPaddingBottom(5f);
			hCell.setBorderColor(primaryColor);
			table.addCell(hCell);
		}

		// 表身明細資料
		String[][] items = {
				{"1", "MCU-STM32-H743", "嵌入式高性能核心控制器 ARM Cortex-M7 480MHz 2MB Flash", "100", "PCS", "520", "52,000", "原廠出廠證明"},
				{"2", "SEN-MEMS-6AXIS", "六軸高精度慣性測量單元 (IMU) 數位感測器 (含溫度補償)", "200", "PCS", "180", "36,000", "附校準參數表"},
				{"3", "PWR-DIN24-120W", "工業級導軌式開關電源模組 (輸入 100-240VAC，輸出 24V/5A)", "20", "台", "1,450", "29,000", "過載/過溫保護"},
				{"4", "PCB-4L-FR4-TG", "四層高密度沉金印刷電路板 (120x80mm, TG170, 阻抗控制)", "100", "片", "280", "28,000", "100% 飛針測試"},
				{"5", "CON-M12-8P-IP67", "M12 工業防水訊號接頭 (8-Pin A-Code 遮蔽型傳輸線長 3m)", "80", "條", "320", "25,600", "耐磨防油 PUR"},
				{"6", "REL-SSR-25A-480", "固態繼電器單相交流控制 (DC 控 24-480VAC 25A，附鋁座)", "30", "組", "480", "14,400", "光電隔離 4kV"},
				{"7", "COMM-RS485-ISO", "雙向光電隔離 RS485 模組 (3.3V/5V 邏輯相容, 15kV ESD)", "50", "PCS", "210", "10,500", "突波防護設計"},
				{"8", "ENC-AL-IP65", "陽極氧化工業鋁合金外殼 (160x100x60mm，CNC 精密加工)", "20", "套", "850", "17,000", "含防水密封墊"}
		};

		Color rowAlt = new Color(248, 250, 252);
		for (int i = 0; i < items.length; i++) {
			String[] row = items[i];
			Color bgColor = (i % 2 == 1) ? rowAlt : Color.WHITE;

			for (int j = 0; j < row.length; j++) {
				PdfPCell cell = new PdfPCell(new Phrase(row[j], cellRegular));
				cell.setBackgroundColor(bgColor);
				cell.setBorderColor(borderColor);
				cell.setPaddingTop(4f);
				cell.setPaddingBottom(4f);

				// 靠左、置中或靠右對齊
				if (j == 0 || j == 4) {
					cell.setHorizontalAlignment(Element.ALIGN_CENTER);
				} else if (j == 3 || j == 5 || j == 6) {
					cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				} else {
					cell.setHorizontalAlignment(Element.ALIGN_LEFT);
				}
				table.addCell(cell);
			}
		}

		document.add(table);

		// 4. 金額統計區塊 (Summary Table)
		PdfPTable summaryTable = new PdfPTable(3);
		summaryTable.setWidthPercentage(100);
		summaryTable.setWidths(new float[]{55f, 20f, 25f});
		summaryTable.setSpacingBefore(6);

		// 左側備註條款
		PdfPCell termsCell = new PdfPCell();
		termsCell.setBorder(Rectangle.BOX);
		termsCell.setBorderColor(borderColor);
		termsCell.setBackgroundColor(new Color(250, 250, 250));
		termsCell.setPadding(6f);
		Paragraph termsTitle = new Paragraph("【報價與交易條款說明】", cellBold);
		Paragraph termsBody = new Paragraph(
				"1. 本報價單所列物料均符合 RoHS 2.0 及 REACH 環保規範要求。\n" +
				"2. 提供出貨後 12 個月原廠保固服務（人為操作不當或不可抗力因素除外）。\n" +
				"3. 貴公司如確認訂購，請於下方「客戶確認簽回」欄位用印簽署後回傳。",
				cellMuted
		);
		termsCell.addElement(termsTitle);
		termsCell.addElement(termsBody);
		summaryTable.addCell(termsCell);

		// 右側小計明細 (跨 2 欄)
		PdfPTable calcTable = new PdfPTable(2);
		calcTable.setWidthPercentage(100);
		calcTable.setWidths(new float[]{45f, 55f});

		addCalcRow(calcTable, "未稅合計 (Subtotal)：", "NT$ 212,500", cellRegular, cellBold, false);
		addCalcRow(calcTable, "營業稅 5% (VAT)：", "NT$ 10,625", cellRegular, cellBold, false);
		addCalcRow(calcTable, "總計金額 (Grand Total)：", "NT$ 223,125", cellBold, totalBigFont, true);

		PdfPCell calcCellContainer = new PdfPCell(calcTable);
		calcCellContainer.setColspan(2);
		calcCellContainer.setBorder(Rectangle.BOX);
		calcCellContainer.setBorderColor(borderColor);
		summaryTable.addCell(calcCellContainer);

		document.add(summaryTable);

		// 5. 簽核區塊 (Sign-off Section)
		PdfPTable signTable = new PdfPTable(2);
		signTable.setWidthPercentage(100);
		signTable.setWidths(new float[]{50f, 50f});
		signTable.setSpacingBefore(12);

		PdfPCell signLeft = new PdfPCell();
		signLeft.setBorder(Rectangle.BOX);
		signLeft.setBorderColor(borderColor);
		signLeft.setPadding(8f);
		signLeft.addElement(new Paragraph("晨星智造整合科技股份有限公司 (經辦/主管簽核)", cellBold));
		signLeft.addElement(new Paragraph("\n\n業務經辦：王大衛                    主管簽章：____________", cellRegular));
		signLeft.addElement(new Paragraph("日期：2026 年 10 月 23 日", cellMuted));
		signTable.addCell(signLeft);

		PdfPCell signRight = new PdfPCell();
		signRight.setBorder(Rectangle.BOX);
		signRight.setBorderColor(borderColor);
		signRight.setPadding(8f);
		signRight.addElement(new Paragraph("客戶確認簽回欄 (請簽署或用印後掃描回傳)", cellBold));
		signRight.addElement(new Paragraph("\n\n客戶簽署：_____________________   公司印鑑：[           ]", cellRegular));
		signRight.addElement(new Paragraph("簽署日期：2026 年    月    日", cellMuted));
		signTable.addCell(signRight);

		document.add(signTable);

		document.close();
	}

	private static void addInfoCell(PdfPTable table, String text, Font font, boolean isHeader, Color bgColor) {
		PdfPCell cell = new PdfPCell(new Phrase(text, font));
		cell.setBackgroundColor(bgColor);
		cell.setBorderColor(new Color(203, 213, 225));
		cell.setPadding(4f);
		if (isHeader) {
			cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		} else {
			cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		}
		table.addCell(cell);
	}

	private static void addCalcRow(PdfPTable table, String label, String value, Font labelFont, Font valFont, boolean isTotal) {
		PdfPCell cLabel = new PdfPCell(new Phrase(label, labelFont));
		cLabel.setBorder(Rectangle.NO_BORDER);
		cLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
		cLabel.setPadding(3f);

		PdfPCell cVal = new PdfPCell(new Phrase(value, valFont));
		cVal.setBorder(Rectangle.NO_BORDER);
		cVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
		cVal.setPadding(3f);

		if (isTotal) {
			cLabel.setBackgroundColor(new Color(254, 242, 242));
			cVal.setBackgroundColor(new Color(254, 242, 242));
		}

		table.addCell(cLabel);
		table.addCell(cVal);
	}

	private static BaseFont loadChineseBaseFont() throws Exception {
		// 優先由 classpath (src/main/resources/fonts/MSJHL.TTC) 載入
		URL fontUrl = SampleBomPdfGenerator.class.getClassLoader().getResource("fonts/MSJHL.TTC");
		if (fontUrl != null) {
			try {
				File fontFile = new File(fontUrl.toURI());
				if (fontFile.exists()) {
					return BaseFont.createFont(fontFile.getAbsolutePath() + ",0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
				}
			} catch (Exception ignored) {
			}

			try (InputStream is = fontUrl.openStream()) {
				byte[] fontBytes = is.readAllBytes();
				return BaseFont.createFont("MSJHL.TTC,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
			} catch (Exception ignored) {
			}
		}

		// 次選：直接相對路徑
		File directFont = new File("src/main/resources/fonts/MSJHL.TTC");
		if (directFont.exists()) {
			return BaseFont.createFont(directFont.getAbsolutePath() + ",0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
		}

		// 備選：系統字體
		return BaseFont.createFont("C:/Windows/Fonts/msjh.ttc,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
	}
}
