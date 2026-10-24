package com.example.demo.application.port.in;

import com.example.demo.application.shared.command.EncryptPdfCommand;

/**
 * 對 PDF 檔案進行安全加密用例 (Inbound Port / UseCase)
 * <p>
 * 依據 Clean Architecture 與六角形架構規範，代表應用層對外開放的 PDF 加密業務契約。
 * 支援設定使用者密碼 (開啟閱讀限制) 與擁有者密碼 (防竄改與授權控制)，並可細緻調控列印與文字複製權限。
 * 遵循命名規範 3-8：Inbound Port 一律以 UseCase 結尾。
 * </p>
 */
public interface EncryptPdfUseCase {

	/**
	 * 依據傳入之加密命令對 PDF 進行加密保護
	 *
	 * @param command 加密命令物件 (包含 PDF 位元組陣列、密碼與權限配置，不可為 null)
	 * @return 加密後受保護之 PDF 二進位 byte 陣列
	 */
	byte[] encryptPdf(EncryptPdfCommand command);
}
