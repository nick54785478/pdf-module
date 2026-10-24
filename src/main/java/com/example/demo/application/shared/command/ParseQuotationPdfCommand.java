package com.example.demo.application.shared.command;

/**
 * 解析報價單 PDF 命令物件 (CQRS Command Object - Pure Data Carrier)
 * <p>
 * 遵循命名規範 3-4：V (Parse) + N (QuotationPdf) + Command。
 * 作為應用層用例之輸入載體，封裝上傳之二進位 PDF 檔案資料與檔案名稱。
 * 遵循六角形架構規範，內部使用純 Java 原生資料型別 (byte[] 與 String)，
 * 絕不引入 Spring MultipartFile 等表現層技術物件，確保應用層的技術純粹性。
 * </p>
 *
 * @param fileBytes PDF 檔案二進位位元組陣列 (不可為空)
 * @param fileName  原始上傳檔案名稱
 */
public record ParseQuotationPdfCommand(
		byte[] fileBytes,
		String fileName
) {
}
