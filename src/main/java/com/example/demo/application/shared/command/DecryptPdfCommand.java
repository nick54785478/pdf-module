package com.example.demo.application.shared.command;

/**
 * 對加密 PDF 檔案進行解密命令物件 (Command)
 * <p>
 * 遵循 Clean Architecture 規範，作為純資料載體 (Pure Data Carrier) 傳遞業務意圖，
 * 嚴禁依賴 Spring、HTTP 協定或底層 PDF 框架技術。
 * 遵循命名規範 3-4：動詞 + 名詞 + Command。
 * </p>
 *
 * @param fileBytes 待解密之受保護 PDF 檔案原始二進位位元組陣列 (不可為空)
 * @param password  解密密碼 (可為使用者開啟密碼或擁有者密碼，若為 null 則嘗試以空密碼開啟)
 */
public record DecryptPdfCommand(
		byte[] fileBytes,
		String password
) {
	/**
	 * 防禦性驗證
	 */
	public DecryptPdfCommand {
		if (fileBytes == null || fileBytes.length == 0) {
			throw new IllegalArgumentException("待解密之 PDF 檔案二進位位元組不可為空");
		}
	}
}
