package com.example.demo.infrastructure.adapter;

import com.example.demo.application.port.out.QuotationParserPort;
import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.dto.QuotationItemGottenResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基於正規表示式與狀態機之報價單文字剖析出境適配器 (Outbound Adapter)
 * <p>
 * 遵循 Clean Architecture 與六角形架構規範：
 * 1. 位於基礎設施層 {@code infrastructure.adapter}，實作應用層 Outbound Port {@link QuotationParserPort}。
 * 2. 封裝針對 PDF 萃取出的純文字資料進行報價單語法剖析之技術細節（包含單行/多行品項、金額換算與網格欄位特徵識別）。
 * 3. 遵從命名規範 3-9：Outbound Adapter 一律以 {@code Adapter} 結尾。
 * </p>
 */
@Slf4j
@Component
public class TextQuotationParserAdapter implements QuotationParserPort {

	private static final Pattern QUOTATION_NO_PATTERN = Pattern.compile("單號[：:]?\\s*([A-Za-z0-9_-]+)");
	private static final Pattern QUOTATION_DATE_PATTERN = Pattern.compile("日期[：:]?\\s*(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}日?)");
	private static final Pattern CUSTOMER_NAME_PATTERN = Pattern.compile("客戶名稱[：:]?\\s*([^\\r\\n]+?)(?=\\s+有效期限|\\r|\\n|$)");
	private static final Pattern VALIDITY_PERIOD_PATTERN = Pattern.compile("有效期限[：:]?\\s*([^\\r\\n]+?)(?=\\r|\\n|$)");
	private static final Pattern CUSTOMER_TAX_ID_PATTERN = Pattern.compile("統一編號[：:]?\\s*(\\d{8})\\s+付款條件");
	private static final Pattern PAYMENT_TERM_PATTERN = Pattern.compile("付款條件[：:]?\\s*([^\\r\\n]+?)(?=\\r|\\n|$)");
	private static final Pattern CONTACT_PERSON_PATTERN = Pattern.compile("聯絡人員[：:]?\\s*([^\\r\\n]+?)(?=\\s+交貨地點|\\r|\\n|$)");
	private static final Pattern DELIVERY_PLACE_PATTERN = Pattern.compile("交貨地點[：:]?\\s*([^\\r\\n]+?)(?=\\r|\\n|$)");

	private static final Pattern SUBTOTAL_PATTERN = Pattern.compile("未稅合計[^\\r\\n]*?(?:[:：]|NT\\$)\\s*(?:NT\\$\\s*)?([0-9,]+(?:\\.[0-9]+)?)");
	private static final Pattern TAX_PATTERN = Pattern.compile("營業稅[^\\r\\n]*?(?:[:：]|NT\\$)\\s*(?:NT\\$\\s*)?([0-9,]+(?:\\.[0-9]+)?)");
	private static final Pattern TOTAL_PATTERN = Pattern.compile("總計金額[^\\r\\n]*?(?:[:：]|NT\\$)\\s*(?:NT\\$\\s*)?([0-9,]+(?:\\.[0-9]+)?)");

	// 單行完整品項模式 (項次 料號 描述 數量 單位 單價 小計 [備註])
	private static final Pattern SINGLE_LINE_ITEM_PATTERN = Pattern.compile(
			"^(\\d{1,4})\\s+([A-Za-z0-9_-]+)\\s+(.+?)\\s+(\\d+)\\s+([A-Za-z\\u4e00-\\u9fa5]+)\\s+([0-9,]+(?:\\.[0-9]+)?)\\s+([0-9,]+(?:\\.[0-9]+)?)(?:\\s+(.*))?$"
	);

	// 多行品項首行模式 (項次 料號 [描述首段])
	private static final Pattern MULTI_LINE_HEADER_PATTERN = Pattern.compile(
			"^(\\d{1,4})\\s+([A-Za-z0-9_-]+)(?:\\s+(.*))?$"
	);

	// 多行品項數值尾行模式 ([描述末段] 數量 單位 單價 小計 [備註])
	private static final Pattern MULTI_LINE_NUMERIC_PATTERN = Pattern.compile(
			"^(?:(.*)\\s+)?(\\d+)\\s+([A-Za-z\\u4e00-\\u9fa5]+)\\s+([0-9,]+(?:\\.[0-9]+)?)\\s+([0-9,]+(?:\\.[0-9]+)?)(?:\\s+(.*))?$"
	);

	/**
	 * 解析 PDF 抽取文字並組裝為 QuotationGottenResult
	 *
	 * @param rawText 自 PDF 萃取出的全文文字字串
	 * @return 結構化報價單完整資訊 DTO
	 */
	@Override
	public QuotationGottenResult parse(String rawText) {
		if (rawText == null || rawText.trim().isEmpty()) {
			return new QuotationGottenResult(
					"UNKNOWN", LocalDate.now(), "", "", "", "", "", "",
					BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", Collections.emptyList()
			);
		}

		String quotationNo = extractMatch(QUOTATION_NO_PATTERN, rawText, "UNKNOWN");
		LocalDate quotationDate = extractDate(rawText);
		String customerName = extractMatch(CUSTOMER_NAME_PATTERN, rawText, "");
		String validityPeriod = extractMatch(VALIDITY_PERIOD_PATTERN, rawText, "");
		String customerTaxId = extractTaxId(rawText);
		String paymentTerm = extractMatch(PAYMENT_TERM_PATTERN, rawText, "");
		String contactPerson = extractMatch(CONTACT_PERSON_PATTERN, rawText, "");
		String deliveryPlace = extractMatch(DELIVERY_PLACE_PATTERN, rawText, "");

		BigDecimal subtotalAmount = extractAmount(SUBTOTAL_PATTERN, rawText);
		BigDecimal taxAmount = extractAmount(TAX_PATTERN, rawText);
		BigDecimal totalAmount = extractAmount(TOTAL_PATTERN, rawText);

		String terms = extractTerms(rawText);
		List<QuotationItemGottenResult> items = extractItems(rawText);

		return new QuotationGottenResult(
				quotationNo,
				quotationDate,
				customerName,
				customerTaxId,
				contactPerson,
				validityPeriod,
				paymentTerm,
				deliveryPlace,
				subtotalAmount,
				taxAmount,
				totalAmount,
				terms,
				Collections.unmodifiableList(items)
		);
	}

	private String extractMatch(Pattern pattern, String text, String defaultValue) {
		Matcher m = pattern.matcher(text);
		if (m.find()) {
			return m.group(1).trim();
		}
		return defaultValue;
	}

	private LocalDate extractDate(String text) {
		Matcher m = QUOTATION_DATE_PATTERN.matcher(text);
		if (m.find()) {
			String dateStr = m.group(1).trim().replace("年", "-").replace("月", "-").replace("日", "");
			try {
				return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-M-d"));
			} catch (Exception e) {
				try {
					return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
				} catch (Exception ignored) {
				}
			}
		}
		return LocalDate.now();
	}

	private String extractTaxId(String text) {
		Matcher m = CUSTOMER_TAX_ID_PATTERN.matcher(text);
		if (m.find()) {
			return m.group(1).trim();
		}
		// 若網格未匹配，尋找第二個 8 碼統一編號 (第一個為賣方公司統一編號)
		Pattern p = Pattern.compile("統一編號[：:]?\\s*(\\d{8})");
		Matcher mAll = p.matcher(text);
		String lastTaxId = "";
		int count = 0;
		while (mAll.find()) {
			count++;
			lastTaxId = mAll.group(1).trim();
			if (count == 2) {
				return lastTaxId;
			}
		}
		return lastTaxId;
	}

	private BigDecimal extractAmount(Pattern pattern, String text) {
		Matcher m = pattern.matcher(text);
		if (m.find()) {
			String raw = m.group(1).replace(",", "").trim();
			try {
				return new BigDecimal(raw);
			} catch (Exception e) {
				log.warn("解析金額失敗: raw={}", raw, e);
			}
		}
		return BigDecimal.ZERO;
	}

	private String extractTerms(String text) {
		int termsIndex = text.indexOf("【報價與交易條款說明】");
		if (termsIndex == -1) {
			termsIndex = text.indexOf("報價與交易條款說明");
		}
		if (termsIndex != -1) {
			String afterTerms = text.substring(termsIndex + "【報價與交易條款說明】".length());
			int endIndex = afterTerms.indexOf("未稅合計");
			if (endIndex == -1) {
				endIndex = afterTerms.indexOf("晨星智造");
			}
			if (endIndex != -1) {
				return afterTerms.substring(0, endIndex).trim();
			}
			return afterTerms.trim();
		}
		return "";
	}

	private List<QuotationItemGottenResult> extractItems(String text) {
		List<QuotationItemGottenResult> items = new ArrayList<>();
		String[] lines = text.split("\\r?\\n");

		boolean inTable = false;
		ItemBuilder currentItem = null;

		for (String rawLine : lines) {
			String line = rawLine.trim();
			if (line.isEmpty()) {
				continue;
			}

			// 偵測表頭起始
			if (line.contains("項次") && line.contains("物料料號")) {
				inTable = true;
				continue;
			}

			// 偵測表格結束標誌
			if (inTable && (line.contains("未稅合計") || line.contains("【報價與交易條款說明】") || line.contains("晨星智造"))) {
				if (currentItem != null && currentItem.isValid()) {
					items.add(currentItem.build());
					currentItem = null;
				}
				break;
			}

			if (!inTable) {
				continue;
			}

			// 嘗試單行完整匹配
			Matcher singleLineMatcher = SINGLE_LINE_ITEM_PATTERN.matcher(line);
			if (singleLineMatcher.matches()) {
				if (currentItem != null && currentItem.isValid()) {
					items.add(currentItem.build());
					currentItem = null;
				}
				items.add(new QuotationItemGottenResult(
						Integer.parseInt(singleLineMatcher.group(1)),
						singleLineMatcher.group(2).trim(),
						singleLineMatcher.group(3).trim(),
						Integer.parseInt(singleLineMatcher.group(4)),
						singleLineMatcher.group(5).trim(),
						parseBigDecimal(singleLineMatcher.group(6)),
						parseBigDecimal(singleLineMatcher.group(7)),
						singleLineMatcher.group(8) != null ? singleLineMatcher.group(8).trim() : ""
				));
				continue;
			}

			// 嘗試多行品項數值行 (例如：[描述末段] 100 PCS 520 52,000 原廠出廠證明) - 優先於新品項表頭判斷
			if (currentItem != null) {
				Matcher numericMatcher = MULTI_LINE_NUMERIC_PATTERN.matcher(line);
				if (numericMatcher.matches()) {
					String descSuffix = numericMatcher.group(1);
					if (descSuffix != null && !descSuffix.trim().isEmpty()) {
						currentItem.descriptionParts.add(descSuffix.trim());
					}
					currentItem.quantity = Integer.parseInt(numericMatcher.group(2));
					currentItem.unit = numericMatcher.group(3).trim();
					currentItem.unitPrice = parseBigDecimal(numericMatcher.group(4));
					currentItem.subtotal = parseBigDecimal(numericMatcher.group(5));
					currentItem.remark = numericMatcher.group(6) != null ? numericMatcher.group(6).trim() : "";

					items.add(currentItem.build());
					currentItem = null;
					continue;
				}
			}

			// 嘗試多行品項首行 (例如：1 MCU-STM32-H743 嵌入式高性能核心控制器 ARM Cortex-M7)
			Matcher headerMatcher = MULTI_LINE_HEADER_PATTERN.matcher(line);
			if (headerMatcher.matches() && isNumeric(headerMatcher.group(1))) {
				if (currentItem != null && currentItem.isValid()) {
					items.add(currentItem.build());
				}
				currentItem = new ItemBuilder();
				currentItem.itemNo = Integer.parseInt(headerMatcher.group(1));
				currentItem.materialNo = headerMatcher.group(2).trim();
				String initialDesc = headerMatcher.group(3);
				if (initialDesc != null && !initialDesc.trim().isEmpty()) {
					currentItem.descriptionParts.add(initialDesc.trim());
				}
				continue;
			}

			// 描述換行延續
			if (currentItem != null) {
				currentItem.descriptionParts.add(line);
			}
		}

		if (currentItem != null && currentItem.isValid()) {
			items.add(currentItem.build());
		}

		return items;
	}

	private BigDecimal parseBigDecimal(String str) {
		if (str == null) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(str.replace(",", "").trim());
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}

	private boolean isNumeric(String str) {
		if (str == null || str.isEmpty()) {
			return false;
		}
		for (char c : str.toCharArray()) {
			if (!Character.isDigit(c)) {
				return false;
			}
		}
		return true;
	}

	private static class ItemBuilder {
		Integer itemNo;
		String materialNo;
		List<String> descriptionParts = new ArrayList<>();
		Integer quantity;
		String unit;
		BigDecimal unitPrice;
		BigDecimal subtotal;
		String remark = "";

		boolean isValid() {
			return itemNo != null && materialNo != null;
		}

		QuotationItemGottenResult build() {
			String desc = String.join(" ", descriptionParts).trim();
			return new QuotationItemGottenResult(
					itemNo,
					materialNo,
					desc,
					quantity != null ? quantity : 0,
					unit != null ? unit : "",
					unitPrice != null ? unitPrice : BigDecimal.ZERO,
					subtotal != null ? subtotal : BigDecimal.ZERO,
					remark != null ? remark : ""
			);
		}
	}
}
