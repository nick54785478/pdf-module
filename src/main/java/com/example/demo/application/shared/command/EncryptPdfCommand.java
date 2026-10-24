package com.example.demo.application.shared.command;

/**
 * 對 PDF 檔案進行安全加密命令物件 (Command)
 * <p>
 * 遵循 Clean Architecture 規範，作為純資料載體 (Pure Data Carrier) 傳遞業務意圖，
 * 嚴禁依賴 Spring、HTTP 協定或底層 PDF 框架技術。
 * 遵循命名規範 3-4：動詞 + 名詞 + Command。
 * </p>
 *
 * @param fileBytes     待加密之 PDF 檔案原始二進位位元組陣列 (不可為空)
 * @param userPassword  使用者閱讀開啟密碼 (可為空或空白，若為空則開啟不需密碼，但受限於權限限制)
 * @param ownerPassword 擁有者/管理者完整權限密碼 (若為空則系統自動指定預設或對齊 userPassword)
 * @param allowPrinting 是否允許列印文件 (預設 true)
 * @param allowCopy     是否允許複製文件內容 (預設 true)
 */
public record EncryptPdfCommand(
		byte[] fileBytes,
		String userPassword,
		String ownerPassword,
		boolean allowPrinting,
		boolean allowCopy
) {
	/**
	 * 便利建構子：以單一閱讀密碼快速建立命令 (預設允許列印與複製)
	 */
	public EncryptPdfCommand(byte[] fileBytes, String userPassword) {
		this(fileBytes, userPassword, null, true, true);
	}

	/**
	 * 防禦性驗證
	 */
	public EncryptPdfCommand {
		if (fileBytes == null || fileBytes.length == 0) {
			throw new IllegalArgumentException("待加密之 PDF 檔案二進位位元組不可為空");
		}
	}
}
