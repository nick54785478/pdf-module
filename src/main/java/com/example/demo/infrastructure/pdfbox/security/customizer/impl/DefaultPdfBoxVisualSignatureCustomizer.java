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
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 預設 PDFBox 視覺防偽簽章繪製器 (Default Visual Customizer)
 * <p>
 * 於指定頁碼幾何座標疊加具備雙線防偽邊框、微軟正黑體字體、簽署人與時間戳之專業企業認證章戳。
 * 字型路徑遵循六角形架構由 {@link PdfFontProperties} 外部化集中管理。
 * </p>
 */
@Slf4j
@Primary
@Component
public class DefaultPdfBoxVisualSignatureCustomizer implements PdfVisualSignatureCustomizer {

	private final PdfFontProperties fontProperties;

	/**
	 * Spring 依賴注入建構子
	 *
	 * @param fontProperties 字體配置屬性
	 */
	public DefaultPdfBoxVisualSignatureCustomizer(PdfFontProperties fontProperties) {
		this.fontProperties = fontProperties != null ? fontProperties : new PdfFontProperties();
	}

	/**
	 * 無參預設建構子，便於獨立單元測試或離線調用
	 */
	public DefaultPdfBoxVisualSignatureCustomizer() {
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

			// 1. 半透明背景與柔和陰影
			PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
			gs.setNonStrokingAlphaConstant(0.92f);
			cs.setGraphicsStateParameters(gs);

			// 底色方塊 (淡雅灰藍)
			cs.setNonStrokingColor(new Color(248, 250, 252));
			cs.addRect(x, y, w, h);
			cs.fill();

			// 2. 雙線尊榮防偽外框
			cs.setStrokingColor(new Color(30, 58, 138)); // 企業深藍
			cs.setLineWidth(1.2f);
			cs.addRect(x, y, w, h);
			cs.stroke();

			cs.setStrokingColor(new Color(148, 163, 184)); // 內框淺灰
			cs.setLineWidth(0.6f);
			cs.addRect(x + 2f, y + 2f, w - 4f, h - 4f);
			cs.stroke();

			// 3. 標題與簽章元資料文字
			cs.beginText();
			cs.setNonStrokingColor(new Color(15, 23, 42)); // 文字深墨灰
			cs.setFont(font, 8f);

			float textX = x + 6f;
			float currentY = y + h - 14f;

			// 標題行
			cs.newLineAtOffset(textX, currentY);
			cs.showText("✔ 數位簽署防偽認證 (Verified Signature)");

			// 簽署原因
			currentY -= 11f;
			cs.newLineAtOffset(0, -11f);
			String reason = command.reason() != null && !command.reason().isBlank() ? command.reason() : "核准報價單";
			cs.showText("理由: " + reason);

			// 簽署地點
			currentY -= 11f;
			cs.newLineAtOffset(0, -11f);
			String location = command.location() != null && !command.location().isBlank() ? command.location() : "台北市";
			cs.showText("地點: " + location);

			// 簽署時間
			currentY -= 11f;
			cs.newLineAtOffset(0, -11f);
			String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
			cs.showText("日期: " + timeStr);

			cs.endText();
		} catch (Exception e) {
			log.warn("繪製 PDFBox 視覺印章時發生非致命異常，跳過視覺圖層: {}", e.getMessage());
		}
	}

	private PDFont resolveFont(PDDocument doc) {
		try {
			String primaryPath = fontProperties.getPath();
			if (primaryPath != null && !primaryPath.isBlank()) {
				ClassPathResource resource = new ClassPathResource(primaryPath);
				if (resource.exists()) {
					try (InputStream is = resource.getInputStream()) {
						log.debug("PDFBox 視覺簽章成功由 Classpath 載入字體: {}", primaryPath);
						return PDType0Font.load(doc, is);
					}
				}
			}

			String devPath = fontProperties.getDevFallbackPath();
			if (devPath != null && !devPath.isBlank()) {
				File devFile = new File(devPath);
				if (devFile.exists()) {
					try (InputStream is = new FileInputStream(devFile)) {
						log.debug("PDFBox 視覺簽章成功由本機相對路徑載入字體: {}", devFile.getAbsolutePath());
						return PDType0Font.load(doc, is);
					}
				}
			}

			String sysPath = fontProperties.getSystemFallbackPath();
			if (sysPath != null && !sysPath.isBlank()) {
				File sysFile = new File(sysPath);
				if (sysFile.exists()) {
					try (InputStream is = new FileInputStream(sysFile)) {
						log.debug("PDFBox 視覺簽章成功由系統字體庫載入: {}", sysFile.getAbsolutePath());
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
