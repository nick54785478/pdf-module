package com.example.demo.presentation.assembler;

import com.example.demo.application.shared.command.AddWatermarkCommand;
import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.dto.QuotationItemGottenResult;
import com.example.demo.application.shared.query.GetQuotationQuery;
import com.example.demo.presentation.resource.in.GenerateQuotationPdfResource;
import com.example.demo.presentation.resource.in.GetQuotationPdfResource;
import com.example.demo.presentation.resource.in.QuotationItemResource;
import com.example.demo.presentation.resource.out.QuotationItemParsedResource;
import com.example.demo.presentation.resource.out.QuotationParsedResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 報價單表現層防腐組裝器 (Presentation Assembler / Anti-Corruption Layer)
 * <p>
 * 依據 Clean Architecture 規範，作為表現層與應用層之間的防腐層轉換器，
 * 負責將表現層請求載體 {@link GetQuotationPdfResource} 轉換為應用層查詢物件 {@link GetQuotationQuery}，
 * 或將 {@link GenerateQuotationPdfResource} 轉換為 {@link QuotationGottenResult}。
 * 保持 Resource 與 DTO / Query 的職責單一與技術解耦。
 * </p>
 */
@Component
public class QuotationResourceAssembler {

	/**
	 * 將取得報價單 PDF 請求載體轉換為應用層 Query 物件
	 *
	 * @param resource 請求資料載體 (包含 ID 與浮水印文字)
	 * @return 應用層 GetQuotationQuery 查詢物件
	 */
	public GetQuotationQuery toQuery(GetQuotationPdfResource resource) {
		String id = (resource != null) ? resource.id() : null;
		String watermark = (resource != null) ? resource.watermark() : null;
		return new GetQuotationQuery(id, watermark);
	}

	/**
	 * 將上傳之 PDF 檔案與浮水印文字轉換為應用層 AddWatermarkCommand
	 *
	 * @param file          表現層 MultipartFile 二進位檔案
	 * @param watermarkText 欲蓋印之浮水印文字
	 * @return 應用層 AddWatermarkCommand 命令物件
	 * @throws IOException 檔案讀取失敗例外
	 */
	public AddWatermarkCommand toWatermarkCommand(MultipartFile file, String watermarkText) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("上傳之 PDF 檔案不可為空");
		}
		return new AddWatermarkCommand(file.getBytes(), watermarkText);
	}

	/**
	 * 將表現層請求載體轉換為應用層 DTO
	 *
	 * @param resource 請求資料載體 (不可為 null)
	 * @return 應用層 QuotationGottenResult DTO
	 */
	public QuotationGottenResult toDto(GenerateQuotationPdfResource resource) {
		if (resource == null) {
			return null;
		}

		List<QuotationItemGottenResult> items = new ArrayList<>();
		if (resource.items() != null) {
			for (QuotationItemResource item : resource.items()) {
				items.add(new QuotationItemGottenResult(
						item.itemNo(),
						item.materialNo(),
						item.description(),
						item.quantity(),
						item.unit(),
						item.unitPrice(),
						item.subtotal(),
						item.remark()
				));
			}
		}

		return new QuotationGottenResult(
				resource.quotationNo(),
				resource.quotationDate() != null ? resource.quotationDate() : LocalDate.now(),
				resource.customerName(),
				resource.customerTaxId(),
				resource.contactPerson(),
				resource.validityPeriod(),
				resource.paymentTerm(),
				resource.deliveryPlace(),
				resource.subtotalAmount(),
				resource.taxAmount(),
				resource.totalAmount(),
				resource.terms(),
				Collections.unmodifiableList(items)
		);
	}

	/**
	 * 建立標準範例物料清單報價單 DTO (供示範與預覽 API 使用)
	 *
	 * @param quotationNo 指定報價單號
	 * @return 預設之範例報價單 DTO
	 */
	public QuotationGottenResult createSampleQuotationDto(String quotationNo) {
		List<QuotationItemGottenResult> items = new ArrayList<>();
		items.add(new QuotationItemGottenResult(1, "MCU-STM32-H743", "嵌入式高性能核心控制器 ARM Cortex-M7 480MHz 2MB Flash", 100, "PCS", new BigDecimal("520"), new BigDecimal("52000"), "原廠出廠證明"));
		items.add(new QuotationItemGottenResult(2, "SEN-MEMS-6AXIS", "六軸高精度慣性測量單元 (IMU) 數位感測器 (含溫度補償)", 200, "PCS", new BigDecimal("180"), new BigDecimal("36000"), "附校準參數表"));
		items.add(new QuotationItemGottenResult(3, "PWR-DIN24-120W", "工業級導軌式開關電源模組 (輸入 100-240VAC，輸出 24V/5A)", 20, "台", new BigDecimal("1450"), new BigDecimal("29000"), "過載/過溫保護"));
		items.add(new QuotationItemGottenResult(4, "PCB-4L-FR4-TG", "四層高密度沉金印刷電路板 (120x80mm, TG170, 阻抗控制)", 100, "片", new BigDecimal("280"), new BigDecimal("28000"), "100% 飛針測試"));
		items.add(new QuotationItemGottenResult(5, "CON-M12-8P-IP67", "M12 工業防水訊號接頭 (8-Pin A-Code 遮蔽型傳輸線長 3m)", 80, "條", new BigDecimal("320"), new BigDecimal("25600"), "耐磨防油 PUR"));
		items.add(new QuotationItemGottenResult(6, "REL-SSR-25A-480", "固態繼電器單相交流控制 (DC 控 24-480VAC 25A，附鋁座)", 30, "組", new BigDecimal("480"), new BigDecimal("14400"), "光電隔離 4kV"));
		items.add(new QuotationItemGottenResult(7, "COMM-RS485-ISO", "雙向光電隔離 RS485 模組 (3.3V/5V 邏輯相容, 15kV ESD)", 50, "PCS", new BigDecimal("210"), new BigDecimal("10500"), "突波防護設計"));
		items.add(new QuotationItemGottenResult(8, "ENC-AL-IP65", "陽極氧化工業鋁合金外殼 (160x100x60mm，CNC 精密加工)", 20, "套", new BigDecimal("850"), new BigDecimal("17000"), "含防水密封墊"));

		return new QuotationGottenResult(
				quotationNo != null && !quotationNo.trim().isEmpty() ? quotationNo : "QT-20261023-001",
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

	/**
	 * 將應用層報價單 DTO 轉換為表現層解析回應資源 (Anti-Corruption Layer)
	 *
	 * @param result 應用層 QuotationGottenResult (可為 null)
	 * @return 表現層 QuotationParsedResource (若輸入為 null 則回傳 null)
	 */
	public QuotationParsedResource toParsedResource(QuotationGottenResult result) {
		if (result == null) {
			return null;
		}

		List<QuotationItemParsedResource> itemResources = new ArrayList<>();
		if (result.items() != null) {
			for (QuotationItemGottenResult item : result.items()) {
				itemResources.add(new QuotationItemParsedResource(
						item.itemNo(),
						item.materialNo(),
						item.description(),
						item.quantity(),
						item.unit(),
						item.unitPrice(),
						item.subtotal(),
						item.remark()
				));
			}
		}

		return new QuotationParsedResource(
				result.quotationNo(),
				result.quotationDate(),
				result.customerName(),
				result.customerTaxId(),
				result.contactPerson(),
				result.validityPeriod(),
				result.paymentTerm(),
				result.deliveryPlace(),
				result.subtotalAmount(),
				result.taxAmount(),
				result.totalAmount(),
				result.terms(),
				Collections.unmodifiableList(itemResources)
		);
	}
}
