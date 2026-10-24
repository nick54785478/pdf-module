package com.example.demo.application.port.in;

import com.example.demo.application.shared.command.DecryptPdfCommand;

/**
 * 對加密 PDF 檔案進行解密用例 (Inbound Port / UseCase)
 * <p>
 * 依據 Clean Architecture 與六角形架構規範，代表應用層對外開放的 PDF 解密業務契約。
 * 接收使用者提供之密碼解開受保護之 PDF 文件，並產出無密碼保護之標準 PDF 位元組陣列。
 * 遵循命名規範 3-8：Inbound Port 一律以 UseCase 結尾。
 * </p>
 */
public interface DecryptPdfUseCase {

	/**
	 * 依據傳入之解密命令對 PDF 進行驗證解鎖並移除安全限制
	 *
	 * @param command 解密命令物件 (包含受保護之 PDF 位元組陣列與解密密碼，不可為 null)
	 * @return 解密後無安全限制之標準 PDF 二進位 byte 陣列
	 */
	byte[] decryptPdf(DecryptPdfCommand command);
}
