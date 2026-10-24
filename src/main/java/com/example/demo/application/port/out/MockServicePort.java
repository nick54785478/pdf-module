package com.example.demo.application.port.out;

import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.query.GetQuotationQuery;

/**
 * 模擬資料提供外向端口 (Mock Data Outbound Port)
 * <p>
 * 遵循 Clean Architecture 與六角形架構規範，代表應用層向基礎設施要求提供模擬範本資料之契約。
 * 接收包含 ID 條件的 {@link GetQuotationQuery} 查詢物件，回傳完整的 {@link QuotationGottenResult} 業務 DTO 範本。
 * 遵循命名規範 3-8：Outbound Port 一律以 Port 結尾。
 * </p>
 */
public interface MockServicePort {

	/**
	 * 依據查詢條件提供報價單模擬範本資料
	 *
	 * @param query 查詢條件物件 (包含 ID)
	 * @return 報價單完整資訊資料傳輸物件 (QuotationGottenResult)
	 */
	QuotationGottenResult getQuotation(GetQuotationQuery query);
}
