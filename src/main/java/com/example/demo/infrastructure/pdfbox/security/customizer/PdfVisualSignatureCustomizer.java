package com.example.demo.infrastructure.pdfbox.security.customizer;

import com.example.demo.application.shared.command.SignPdfCommand;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;

/**
 * PDFBox 數位簽章視覺印章自定義繪製介面 (Visual Signature Customizer)
 * <p>
 * 允許開發者自定義在 PDFBox 簽署流程中的視覺印章呈現方式（如企業印章、中文字體、邊框幾何、審批細節）。
 * </p>
 */
@FunctionalInterface
public interface PdfVisualSignatureCustomizer {

	/**
	 * 在 PDF 指定頁面上繪製可見防偽印章外觀
	 *
	 * @param document  目標 PDF 實體文件
	 * @param pageIndex 頁碼索引 (自 0 起算)
	 * @param command   簽章命令物件
	 * @param signature 簽章字典物件
	 */
	void customizeAppearance(PDDocument document, int pageIndex, SignPdfCommand command, PDSignature signature);
}
