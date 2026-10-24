package com.example.demo.application.assembler;

import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.dto.QuotationItemGottenResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 報價單報表參數與資料集防腐組裝器 (Application Assembler)
 * <p>
 * 遵循 Clean Architecture 規範：
 * 1. 位於應用層 {@code application.assembler}，負責將領域/應用層 DTO ({@link QuotationGottenResult})
 *    轉換為基礎設施報表引擎 (BIRT) 所需之報表參數 (Parameters Map) 與腳本資料上下文 (DataContext Map)。
 * 2. 嚴守單一職責，避免資料轉換邏輯侵入 DTO 或 Outbound Port。
 * 3. 處理金額數值千分位格式化與空值防禦。
 * </p>
 */
@Component
public class QuotationReportAssembler {

	private static final String CURRENCY_PATTERN = "#,##0";

	/**
	 * 將報價單 DTO 組裝為報表單頭與匯總參數 Map
	 *
	 * @param quotation 報價單資訊資料傳輸物件
	 * @return 報表參數 Map (供 BIRT Report Parameters 取用)
	 */
	public Map<String, Object> toParameters(QuotationGottenResult quotation) {
		if (quotation == null) {
			return Collections.emptyMap();
		}

		DecimalFormat df = new DecimalFormat(CURRENCY_PATTERN);
		Map<String, Object> params = new HashMap<>();

		params.put("quotationNo", defaultString(quotation.quotationNo()));
		params.put("quotationDate", quotation.quotationDate() != null ? quotation.quotationDate().toString() : "");
		params.put("customerName", defaultString(quotation.customerName()));
		params.put("customerTaxId", defaultString(quotation.customerTaxId()));
		params.put("contactPerson", defaultString(quotation.contactPerson()));
		params.put("validityPeriod", defaultString(quotation.validityPeriod()));
		params.put("paymentTerm", defaultString(quotation.paymentTerm()));
		params.put("deliveryPlace", defaultString(quotation.deliveryPlace()));
		params.put("subtotalAmount", formatBigDecimal(quotation.subtotalAmount(), df));
		params.put("taxAmount", formatBigDecimal(quotation.taxAmount(), df));
		params.put("totalAmount", formatBigDecimal(quotation.totalAmount(), df));
		params.put("terms", defaultString(quotation.terms()));

		return params;
	}

	/**
	 * 將報價單 DTO 之明細清單組裝為報表腳本資料集上下文 Map
	 *
	 * @param quotation 報價單資訊資料傳輸物件
	 * @return 資料上下文 Map (包含鍵值 {@code items} 之明細 Map 清單)
	 */
	public Map<String, Object> toDataContext(QuotationGottenResult quotation) {
		if (quotation == null || quotation.items() == null) {
			Map<String, Object> context = new HashMap<>();
			context.put("items", Collections.emptyList());
			return context;
		}

		DecimalFormat df = new DecimalFormat(CURRENCY_PATTERN);
		List<Map<String, Object>> itemsList = new ArrayList<>();

		for (QuotationItemGottenResult item : quotation.items()) {
			Map<String, Object> itemMap = new HashMap<>();
			itemMap.put("itemNo", item.itemNo());
			itemMap.put("materialNo", defaultString(item.materialNo()));
			itemMap.put("description", defaultString(item.description()));
			itemMap.put("quantity", item.quantity());
			itemMap.put("unit", defaultString(item.unit()));
			itemMap.put("unitPrice", formatBigDecimal(item.unitPrice(), df));
			itemMap.put("subtotal", formatBigDecimal(item.subtotal(), df));
			itemMap.put("remark", defaultString(item.remark()));
			itemsList.add(itemMap);
		}

		Map<String, Object> context = new HashMap<>();
		context.put("items", itemsList);
		return context;
	}

	private String defaultString(String val) {
		return val != null ? val : "";
	}

	private String formatBigDecimal(BigDecimal amount, DecimalFormat df) {
		return amount != null ? df.format(amount) : "0";
	}
}
