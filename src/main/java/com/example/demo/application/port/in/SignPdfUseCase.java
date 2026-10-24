package com.example.demo.application.port.in;

import com.example.demo.application.shared.command.SignPdfCommand;

/**
 * 對 PDF 檔案進行數位簽章用例 (Inbound Port / UseCase)
 * <p>
 * 依據 Clean Architecture 與六角形架構規範，代表應用層對外開放的 PDF 數位簽署業務契約。
 * 使用 PKCS#12 金鑰庫與 X.509 憑證鏈，依據標準 PKCS#7 規範對 PDF 內容進行不可否認性之加密數位簽章，
 * 同時支援在指定頁面繪製防偽視覺印章。
 * 遵循命名規範 3-8：Inbound Port 一律以 UseCase 結尾。
 * </p>
 */
public interface SignPdfUseCase {

	/**
	 * 依據傳入之簽章命令為 PDF 進行數位簽署
	 *
	 * @param command 數位簽章命令物件 (包含 PDF 位元組陣列、PKCS#12 憑證金鑰庫、密碼與印章配置，不可為 null)
	 * @return 完成數位簽章之 PDF 二進位 byte 陣列
	 */
	byte[] signPdf(SignPdfCommand command);
}
