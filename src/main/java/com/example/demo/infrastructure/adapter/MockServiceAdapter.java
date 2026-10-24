package com.example.demo.infrastructure.adapter;

import com.example.demo.application.port.out.MockServicePort;
import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.dto.QuotationItemGottenResult;
import com.example.demo.application.shared.query.GetQuotationQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 模擬資料提供適配器 (Outbound Adapter)
 * <p>
 * 實作應用層 {@link MockServicePort} 外向端口，提供物料清單報價單之預設模擬資料範本。
 * 依據業務需求「不必檢核，只要提供回傳資料就好」，直接依據傳入之 ID 產生對應的報價單範本資料。
 * 遵循命名規範 3-9：Outbound Adapter 一律以 Adapter 結尾。
 * </p>
 */
@Slf4j
@Component
public class MockServiceAdapter implements MockServicePort {

	private static final String DEFAULT_QUOTATION_ID = "QT-20261023-001";

	/**
	 * 依據查詢條件提供報價單模擬範本資料
	 *
	 * @param query 查詢條件物件 (包含 ID)
	 * @return 報價單完整資訊資料傳輸物件 (QuotationGottenResult)
	 */
	@Override
	public QuotationGottenResult getQuotation(GetQuotationQuery query) {
		String id = (query != null && query.id() != null && !query.id().trim().isEmpty())
				? query.id().trim()
				: DEFAULT_QUOTATION_ID;

		log.info("【MockServiceAdapter】提供報價單範本資料: quotationId={}", id);

		List<QuotationItemGottenResult> items = new ArrayList<>();
		items.add(new QuotationItemGottenResult(1, "MCU-STM32-H743", "嵌入式高性能核心控制器 ARM Cortex-M7 480MHz 2MB Flash", 100, "PCS", new BigDecimal("520"), new BigDecimal("52000"), "原廠出廠證明"));
		items.add(new QuotationItemGottenResult(2, "SEN-MEMS-6AXIS", "六軸高精度慣性測量單元 (IMU) 數位感測器 (含溫度補償)", 200, "PCS", new BigDecimal("180"), new BigDecimal("36000"), "附校準參數表"));
		items.add(new QuotationItemGottenResult(3, "PWR-DIN24-120W", "工業級導軌式開關電源模組 (輸入 100-240VAC，輸出 24V/5A)", 20, "台", new BigDecimal("1450"), new BigDecimal("29000"), "過載/過溫保護"));
		items.add(new QuotationItemGottenResult(4, "PCB-4L-FR4-TG", "四層高密度沉金印刷電路板 (120x80mm, TG170, 阻抗控制)", 100, "片", new BigDecimal("280"), new BigDecimal("28000"), "100% 飛針測試"));
		items.add(new QuotationItemGottenResult(5, "CON-M12-8P-IP67", "M12 工業防水訊號接頭 (8-Pin A-Code 遮蔽型傳輸線長 3m)", 80, "條", new BigDecimal("320"), new BigDecimal("25600"), "耐磨防油 PUR"));
		items.add(new QuotationItemGottenResult(6, "REL-SSR-25A-480", "固態繼電器單相交流控制 (DC 控 24-480VAC 25A，附鋁座)", 30, "組", new BigDecimal("480"), new BigDecimal("14400"), "光電隔離 4kV"));
		items.add(new QuotationItemGottenResult(7, "COMM-RS485-ISO", "雙向光電隔離 RS485 模組 (3.3V/5V 邏輯相容, 15kV ESD)", 50, "PCS", new BigDecimal("210"), new BigDecimal("10500"), "突波防護設計"));
		items.add(new QuotationItemGottenResult(8, "ENC-AL-IP65", "陽極氧化工業鋁合金外殼 (160x100x60mm，CNC 精密加工)", 20, "套", new BigDecimal("850"), new BigDecimal("17000"), "含防水密封墊"));
		//---
		items.add(new QuotationItemGottenResult(1, "MCU-STM32-H743", "嵌入式高性能核心控制器 ARM Cortex-M7 480MHz 2MB Flash", 100, "PCS", new BigDecimal("520"), new BigDecimal("52000"), "原廠出廠證明"));
		items.add(new QuotationItemGottenResult(2, "SEN-MEMS-6AXIS", "六軸高精度慣性測量單元 (IMU) 數位感測器 (含溫度補償)", 200, "PCS", new BigDecimal("180"), new BigDecimal("36000"), "附校準參數表"));
		items.add(new QuotationItemGottenResult(3, "PWR-DIN24-120W", "工業級導軌式開關電源模組 (輸入 100-240VAC，輸出 24V/5A)", 20, "台", new BigDecimal("1450"), new BigDecimal("29000"), "過載/過溫保護"));
		items.add(new QuotationItemGottenResult(4, "PCB-4L-FR4-TG", "四層高密度沉金印刷電路板 (120x80mm, TG170, 阻抗控制)", 100, "片", new BigDecimal("280"), new BigDecimal("28000"), "100% 飛針測試"));
		items.add(new QuotationItemGottenResult(5, "CON-M12-8P-IP67", "M12 工業防水訊號接頭 (8-Pin A-Code 遮蔽型傳輸線長 3m)", 80, "條", new BigDecimal("320"), new BigDecimal("25600"), "耐磨防油 PUR"));
		items.add(new QuotationItemGottenResult(6, "REL-SSR-25A-480", "固態繼電器單相交流控制 (DC 控 24-480VAC 25A，附鋁座)", 30, "組", new BigDecimal("480"), new BigDecimal("14400"), "光電隔離 4kV"));
		items.add(new QuotationItemGottenResult(7, "COMM-RS485-ISO", "雙向光電隔離 RS485 模組 (3.3V/5V 邏輯相容, 15kV ESD)", 50, "PCS", new BigDecimal("210"), new BigDecimal("10500"), "突波防護設計"));
		items.add(new QuotationItemGottenResult(8, "ENC-AL-IP65", "陽極氧化工業鋁合金外殼 (160x100x60mm，CNC 精密加工)", 20, "套", new BigDecimal("850"), new BigDecimal("17000"), "含防水密封墊"));
		items.add(new QuotationItemGottenResult(1, "MCU-STM32-H743", "嵌入式高性能核心控制器 ARM Cortex-M7 480MHz 2MB Flash", 100, "PCS", new BigDecimal("520"), new BigDecimal("52000"), "原廠出廠證明"));
		items.add(new QuotationItemGottenResult(2, "SEN-MEMS-6AXIS", "六軸高精度慣性測量單元 (IMU) 數位感測器 (含溫度補償)", 200, "PCS", new BigDecimal("180"), new BigDecimal("36000"), "附校準參數表"));
		items.add(new QuotationItemGottenResult(3, "PWR-DIN24-120W", "工業級導軌式開關電源模組 (輸入 100-240VAC，輸出 24V/5A)", 20, "台", new BigDecimal("1450"), new BigDecimal("29000"), "過載/過溫保護"));
//		items.add(new QuotationItemGottenResult(4, "PCB-4L-FR4-TG", "四層高密度沉金印刷電路板 (120x80mm, TG170, 阻抗控制)", 100, "片", new BigDecimal("280"), new BigDecimal("28000"), "100% 飛針測試"));
//		items.add(new QuotationItemGottenResult(4, "PCB-4L-FR4-TG", "四層高密度沉金印刷電路板 (120x80mm, TG170, 阻抗控制)", 100, "片", new BigDecimal("280"), new BigDecimal("28000"), "100% 飛針測試"));
//		items.add(new QuotationItemGottenResult(4, "PCB-4L-FR4-TG", "四層高密度沉金印刷電路板 (120x80mm, TG170, 阻抗控制)", 100, "片", new BigDecimal("280"), new BigDecimal("28000"), "100% 飛針測試"));
//		items.add(new QuotationItemGottenResult(4, "PCB-4L-FR4-TG", "四層高密度沉金印刷電路板 (120x80mm, TG170, 阻抗控制)", 100, "片", new BigDecimal("280"), new BigDecimal("28000"), "100% 飛針測試"));


		return new QuotationGottenResult(
				id,
				LocalDate.now(),
				"台灣智能科技股份有限公司",
				"12345678",
				"陳建銘 經理 / (03) 578-1234 #567",
				"30 天 (至 " + LocalDate.now().plusDays(30) + ")",
				"月結 30 天 (Net 30)",
				"新竹市科學園區研發六路 12 號 (DDP)",
				new BigDecimal("212500"),
				new BigDecimal("10625"),
				new BigDecimal("223125"),
				"1. 本報價單所列物料均符合 RoHS 2.0 及 REACH 環保規範要求。\n" +
				"2. 提供出貨後 12 個月原廠保固服務（人為操作不當或不可抗力因素除外）。\n" +
				"3. 貴公司如確認訂購，請於下方「客戶確認簽回」欄位用印簽署後回傳。",
				Collections.unmodifiableList(items)
		);
	}
}
