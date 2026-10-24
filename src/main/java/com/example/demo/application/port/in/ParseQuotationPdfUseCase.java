package com.example.demo.application.port.in;

import com.example.demo.application.shared.command.ParseQuotationPdfCommand;
import com.example.demo.application.shared.dto.QuotationGottenResult;

/**
 * 解析報價單 PDF 文件用例 (Inbound Port / UseCase)
 * <p>
 * 依據 Clean Architecture 與六角形架構規範，代表應用層對外開放的業務契約。
 * 接收包含 PDF 二進位資料之 {@link ParseQuotationPdfCommand} 命令物件，
 * 調度基礎設施層之輸出端口萃取文字與結構化欄位，並轉換為強型別之業務 DTO {@link QuotationGottenResult}。
 * 遵循命名規範 3-8：Inbound Port 一律以 UseCase 結尾。
 * </p>
 */
public interface 	ParseQuotationPdfUseCase {

	/**
	 * 依據上傳之 PDF 二進位資料解析並萃取報價單完整資訊
	 *
	 * @param command 解析報價單 PDF 命令物件 (包含 byte[] 檔案與檔名)
	 * @return 報價單完整資訊資料傳輸物件 (QuotationGottenResult)
	 */
	QuotationGottenResult parseQuotationPdf(ParseQuotationPdfCommand command);
}
