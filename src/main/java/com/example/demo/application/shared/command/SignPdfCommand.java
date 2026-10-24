package com.example.demo.application.shared.command;

import com.example.demo.application.shared.security.PdfCertificationLevel;
import com.example.demo.application.shared.security.PdfSignatureSubFilter;

/**
 * 對 PDF 檔案進行數位簽章命令物件 (Command)
 * <p>
 * 遵循 Clean Architecture 規範，作為純資料載體 (Pure Data Carrier) 傳遞業務意圖，
 * 封裝 PKCS#12 金鑰庫、私鑰資訊、簽章詮釋資料 (理由、地點、聯絡人)、指定範本 (templateName)、
 * 簽章規範 (認證等級、子過濾器) 與可選視覺印章配置。
 * 遵循命名規範 3-4：動詞 + 名詞 + Command。
 * </p>
 *
 * @param fileBytes          待簽章之 PDF 原始二進位位元組陣列 (不可為空)
 * @param keyStoreBytes      PKCS#12 (.p12 / .pfx) 金鑰庫二進位位元組陣列 (不可為空)
 * @param keyStorePassword   金鑰庫解鎖密碼 (不可為空)
 * @param keyAlias           私鑰別名 (若為空則預設取金鑰庫內第一個別名)
 * @param reason             簽章理由說明 (例如 "核准報價單", "Approved by Manager")
 * @param location           簽章簽署地點 (例如 "台北市", "Taipei, Taiwan")
 * @param contact            簽署人聯絡資訊 (可選)
 * @param visualSignature    是否印製可見視覺印章方塊 (Visual Signature Stamp)
 * @param page               印製視覺印章之頁碼 (自 1 起算，預設第 1 頁)
 * @param x                  視覺印章左下角 X 座標點 (預設 360f)
 * @param y                  視覺印章左下角 Y 座標點 (預設 50f)
 * @param width              視覺印章寬度 (預設 180f)
 * @param height             視覺印章高度 (預設 60f)
 * @param templateName       指定套用之單據範本名稱 (可選，例如 "bom"，若為 null 則套用預設簽章規範)
 * @param certificationLevel 簽章認證等級 (可選，預設 NOT_CERTIFIED)
 * @param subFilter          簽章標準規格 (可選，預設 ADBE_PKCS7_DETACHED)
 */
public record SignPdfCommand(
		byte[] fileBytes,
		byte[] keyStoreBytes,
		String keyStorePassword,
		String keyAlias,
		String reason,
		String location,
		String contact,
		boolean visualSignature,
		int page,
		float x,
		float y,
		float width,
		float height,
		String templateName,
		PdfCertificationLevel certificationLevel,
		PdfSignatureSubFilter subFilter
) {
	/**
	 * 便利建構子：簡化呼叫，預設於第 1 頁右下角繪製可見視覺印章，使用系統預設標準簽章規範
	 */
	public SignPdfCommand(
			byte[] fileBytes,
			byte[] keyStoreBytes,
			String keyStorePassword,
			String reason,
			String location
	) {
		this(fileBytes, keyStoreBytes, keyStorePassword, null, reason, location, null, true, 1, 360f, 50f, 180f, 60f, null, PdfCertificationLevel.NOT_CERTIFIED, PdfSignatureSubFilter.ADBE_PKCS7_DETACHED);
	}

	/**
	 * 向後相容便利建構子 (不指定範本與進階規範參數)
	 */
	public SignPdfCommand(
			byte[] fileBytes,
			byte[] keyStoreBytes,
			String keyStorePassword,
			String keyAlias,
			String reason,
			String location,
			String contact,
			boolean visualSignature,
			int page,
			float x,
			float y,
			float width,
			float height
	) {
		this(fileBytes, keyStoreBytes, keyStorePassword, keyAlias, reason, location, contact, visualSignature, page, x, y, width, height, null, PdfCertificationLevel.NOT_CERTIFIED, PdfSignatureSubFilter.ADBE_PKCS7_DETACHED);
	}

	/**
	 * 指定單據範本之便利建構子
	 */
	public SignPdfCommand(
			byte[] fileBytes,
			byte[] keyStoreBytes,
			String keyStorePassword,
			String keyAlias,
			String reason,
			String location,
			String contact,
			boolean visualSignature,
			int page,
			float x,
			float y,
			float width,
			float height,
			String templateName
	) {
		this(fileBytes, keyStoreBytes, keyStorePassword, keyAlias, reason, location, contact, visualSignature, page, x, y, width, height, templateName, PdfCertificationLevel.NOT_CERTIFIED, PdfSignatureSubFilter.ADBE_PKCS7_DETACHED);
	}

	/**
	 * 指定簽章規範參數 (認證等級與子過濾器) 但未指定單據範本之便利建構子
	 */
	public SignPdfCommand(
			byte[] fileBytes,
			byte[] keyStoreBytes,
			String keyStorePassword,
			String keyAlias,
			String reason,
			String location,
			String contact,
			boolean visualSignature,
			int page,
			float x,
			float y,
			float width,
			float height,
			PdfCertificationLevel certificationLevel,
			PdfSignatureSubFilter subFilter
	) {
		this(fileBytes, keyStoreBytes, keyStorePassword, keyAlias, reason, location, contact, visualSignature, page, x, y, width, height, null, certificationLevel, subFilter);
	}

	/**
	 * 防禦性驗證
	 */
	public SignPdfCommand {
		if (fileBytes == null || fileBytes.length == 0) {
			throw new IllegalArgumentException("待簽章之 PDF 檔案二進位位元組不可為空");
		}
		if (keyStoreBytes == null || keyStoreBytes.length == 0) {
			throw new IllegalArgumentException("PKCS#12 金鑰庫二進位位元組不可為空");
		}
		if (keyStorePassword == null) {
			throw new IllegalArgumentException("金鑰庫解鎖密碼不可為 null");
		}
		if (page < 1) {
			page = 1;
		}
		if (certificationLevel == null) {
			certificationLevel = PdfCertificationLevel.NOT_CERTIFIED;
		}
		if (subFilter == null) {
			subFilter = PdfSignatureSubFilter.ADBE_PKCS7_DETACHED;
		}
	}
}
