package com.example.demo.application.port.in;

import com.example.demo.application.shared.query.GetQuotationQuery;

/**
 * 產出報價單 PDF 文件用例 (Inbound Port / UseCase)
 * <p>
 * 依據 Clean Architecture 與六角形架構規範，代表應用層對外（表現層或外部呼叫端）開放的業務使用案例契約。
 * 接收包含 ID 條件之 {@link GetQuotationQuery} 查詢物件，由 MockService 提供資料範本並調度 OpenPDF 產出 PDF 二進位資料。
 * 遵循命名規範 3-8：Inbound Port 一律以 UseCase 結尾。
 * </p>
 */
public interface GenerateQuotationPdfUseCase {

	/**
	 * 依據查詢條件 (ID) 取得報價單資料並產出 PDF 檔案
	 *
	 * @param query 查詢條件物件 (包含 ID)
	 * @return PDF 檔案之二進位 byte 陣列
	 */
	byte[] generateQuotationPdf(GetQuotationQuery query);
}
