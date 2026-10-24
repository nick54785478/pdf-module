package com.example.demo.infrastructure.pdfbox.security.customizer.impl;

import com.example.demo.application.shared.command.SignPdfCommand;
import com.example.demo.config.PdfFontProperties;
import com.example.demo.infrastructure.pdfbox.security.customizer.PdfVisualSignatureCustomizer;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 物料清單 (BOM) 報價單專屬數位簽章視覺印章客製化器 (BOM Quotation Visual Signature Customizer)
 * <p>
 * 為 {@code BomQuotationPdfTemplate} 專屬設計之企業級電子簽署視覺外觀：
 * 1. 外觀幾何：晨星智造企業深藍 (#1A365D) 與金桐色雙重防偽鋼印圓角方塊。
 * 2. 尊榮標頭：硃砂朱紅 (#B91C1C)「★ 晨星智造 • 原物料放行審批 ★」專屬印記。
 * 3. 雙語標準：標註「BOM APPROVAL SEAL • PAdES-BES VERIFIED」。
 * 4. 業務元數據：包含簽署主管/經辦人員名稱、審批事由、核准地點、PAdES 認證等級與完整時間戳。
 * 字型路徑遵循六角形架構由 {@link PdfFontProperties} 外部化集中管理。
 * </p>
 */
@Slf4j
@Component
public class BomQuotationVisualSignatureCustomizer implements PdfVisualSignatureCustomizer {

	private final PdfFontProperties fontProperties;

	// 晨星智造主品牌海軍深藍 (Navy)
	private static final Color PRIMARY_NAVY = new Color(26, 54, 93);
	// 尊榮審批硃砂紅 (Approval Seal Red)
	private static final Color SEAL_RED = new Color(185, 28, 28);
	// 輔助防偽金銅色 (Gold / Bronze Border)
	private static final Color ACCENT_GOLD = new Color(180, 83, 9);
	// 柔和底色
	private static final Color BG_COLOR = new Color(254, 252, 246);
	// 內文字墨深灰
	private static final Color TEXT_DARK = new Color(15, 23, 42);

	/**
	 * Spring 依賴注入建構子
	 *
	 * @param fontProperties 字體配置屬性
	 */
	public BomQuotationVisualSignatureCustomizer(PdfFontProperties fontProperties) {
		this.fontProperties = fontProperties != null ? fontProperties : new PdfFontProperties();
	}

	/**
	 * 無參預設建構子，便於獨立單元測試或離線調用
	 */
	public BomQuotationVisualSignatureCustomizer() {
		this(new PdfFontProperties());
	}

	@Override
	public void customizeAppearance(PDDocument document, int pageIndex, SignPdfCommand command, PDSignature signature) {
		if (!command.visualSignature()) {
			return;
		}

		if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
			pageIndex = 0;
		}

		PDPage page = document.getPage(pageIndex);
		float x = command.x();
		float y = command.y();
		float w = command.width();
		float h = command.height();

		PDFont font = resolveFont(document);

		try (PDPageContentStream cs = new PDPageContentStream(
				document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

			// 1. 半透明典雅底色
			PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
			gs.setNonStrokingAlphaConstant(0.95f);
			cs.setGraphicsStateParameters(gs);

			cs.setNonStrokingColor(BG_COLOR);
			cs.addRect(x, y, w, h);
			cs.fill();

			// 2. 雙重防偽鋼印框線 (外框深海軍藍，內框金銅色)
			cs.setStrokingColor(PRIMARY_NAVY);
			cs.setLineWidth(1.3f);
			cs.addRect(x, y, w, h);
			cs.stroke();

			cs.setStrokingColor(ACCENT_GOLD);
			cs.setLineWidth(0.6f);
			cs.addRect(x + 2f, y + 2f, w - 4f, h - 4f);
			cs.stroke();

			// 3. 繪製防偽認證印章文字
			cs.beginText();

			float textX = x + 6f;
			float currentY = y + h - 13f;

			// (1) 尊榮放行印章主標題 (硃砂紅)
			cs.setNonStrokingColor(SEAL_RED);
			cs.setFont(font, 8f);
			cs.newLineAtOffset(textX, currentY);
			cs.showText("★ 晨星智造 • 原物料放行審批 ★");

			// (2) 國際 PAdES 電子簽名英文認證副標
			currentY -= 9.5f;
			cs.newLineAtOffset(0, -9.5f);
			cs.setNonStrokingColor(PRIMARY_NAVY);
			cs.setFont(font, 6.5f);
			cs.showText("BOM APPROVAL SEAL • PAdES-BES VERIFIED");

			// (3) 簽署人 / 私鑰別名
			currentY -= 10f;
			cs.newLineAtOffset(0, -10f);
			cs.setNonStrokingColor(TEXT_DARK);
			cs.setFont(font, 7.5f);
			String signerName = signature.getName() != null && !signature.getName().isBlank()
					? signature.getName()
					: (command.keyAlias() != null ? command.keyAlias() : "主管/經辦核准");
			cs.showText("簽署人員: " + signerName);

			// (4) 審批理由 (Reason)
			currentY -= 10f;
			cs.newLineAtOffset(0, -10f);
			String reason = command.reason() != null && !command.reason().isBlank()
					? command.reason()
					: "BOM 原物料報價核准簽署";
			cs.showText("審批事由: " + reason);

			// (5) 核准地點與時間戳
			currentY -= 10f;
			cs.newLineAtOffset(0, -10f);
			String location = command.location() != null && !command.location().isBlank()
					? command.location()
					: "台北營運總部";
			String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
			cs.setFont(font, 7f);
			cs.showText("地點/時間: " + location + " • " + timeStr);

			cs.endText();
			log.info("【BOM 範本簽章外觀】成功加蓋 BOM 專屬數位簽章鋼印: 座標=({}, {}), 尺寸={}x{}", x, y, w, h);
		} catch (Exception e) {
			log.warn("繪製 BOM 專屬視覺印章時發生非致命異常，跳過視覺圖層: {}", e.getMessage());
		}
	}

	private PDFont resolveFont(PDDocument doc) {
		try {
			String primaryPath = fontProperties.getPath();
			if (primaryPath != null && !primaryPath.isBlank()) {
				ClassPathResource resource = new ClassPathResource(primaryPath);
				if (resource.exists()) {
					try (InputStream is = resource.getInputStream()) {
						log.debug("BOM 視覺簽章成功由 Classpath 載入字體: {}", primaryPath);
						return PDType0Font.load(doc, is);
					}
				}
			}

			String devPath = fontProperties.getDevFallbackPath();
			if (devPath != null && !devPath.isBlank()) {
				File devFile = new File(devPath);
				if (devFile.exists()) {
					try (InputStream is = new FileInputStream(devFile)) {
						log.debug("BOM 視覺簽章成功由本機相對路徑載入字體: {}", devFile.getAbsolutePath());
						return PDType0Font.load(doc, is);
					}
				}
			}

			String sysPath = fontProperties.getSystemFallbackPath();
			if (sysPath != null && !sysPath.isBlank()) {
				File sysFile = new File(sysPath);
				if (sysFile.exists()) {
					try (InputStream is = new FileInputStream(sysFile)) {
						log.debug("BOM 視覺簽章成功由系統字體庫載入: {}", sysFile.getAbsolutePath());
						return PDType0Font.load(doc, is);
					}
				}
			}
		} catch (Exception e) {
			log.debug("未能載入自定義正黑體，降級至標準 Helvetica 字體: {}", e.getMessage());
		}
		return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
	}
}
