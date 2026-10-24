package com.example.demo.application.port.out;

import com.example.demo.application.shared.command.DecryptPdfCommand;
import com.example.demo.application.shared.command.EncryptPdfCommand;
import com.example.demo.application.shared.command.SignPdfCommand;

/**
 * PDF 安全防護與數位簽章輸出端口 (Outbound Port)
 * <p>
 * 依據 Clean Architecture 與六角形架構規範，代表應用層向基礎設施層所索求的 PDF 安全防護技術能力契約。
 * 涵蓋文件權限加密、安全密碼驗證解密以及標準 PKCS#7 數位簽署。
 * 遵循命名規範 3-8：Outbound Port 一律以 Port 結尾。
 * 方法參數與回傳型別嚴格遵守純 Java 基礎型別與純資料 Command 物件，嚴禁洩露底層 OpenPDF 或 BouncyCastle 框架物件。
 * </p>
 */
public interface PdfSecurityExecutorPort {

	/**
	 * 對 PDF 檔案進行安全加密與權限設定
	 *
	 * @param command 加密命令物件 (包含原始 PDF 位元組、密碼與權限配置，不可為 null)
	 * @return 加密後之 PDF 二進位 byte 陣列
	 */
	byte[] encryptPdf(EncryptPdfCommand command);

	/**
	 * 對受密碼保護之 PDF 檔案進行解密
	 *
	 * @param command 解密命令物件 (包含加密 PDF 位元組與密碼，不可為 null)
	 * @return 解密後無安全限制之 PDF 二進位 byte 陣列
	 */
	byte[] decryptPdf(DecryptPdfCommand command);

	/**
	 * 對 PDF 檔案進行 PKCS#7 數位簽章與防偽印章印製
	 *
	 * @param command 數位簽章命令物件 (包含原始 PDF、PKCS#12 金鑰庫位元組、密碼與簽名設置，不可為 null)
	 * @return 完成數位簽章之 PDF 二進位 byte 陣列
	 */
	byte[] signPdf(SignPdfCommand command);
}
