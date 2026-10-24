package com.example.demo.application.shared.security;

/**
 * PDF 數位簽章認證授權層級 (Certification Level)
 * <p>
 * 依據 ISO 32000-1 (PDF 1.7) 與 PDF Advanced Electronic Signatures 規範：
 * 控管文件簽署後的後續防竄改與授權變更限制。
 * </p>
 */
public enum PdfCertificationLevel {
	/**
	 * 普通核准簽章 (Approval Signature)：不鎖定文件結構
	 */
	NOT_CERTIFIED(0, "非認證簽章 (一般核准簽章)"),

	/**
	 * 最高安全等級認證：簽名後嚴格禁止任何變更 (No changes permitted)
	 */
	CERTIFIED_NO_CHANGES_ALLOWED(1, "嚴格認證：禁止後續任何變更"),

	/**
	 * 表單認證：允許後續填寫表單欄位與再次簽署 (Form filling permitted)
	 */
	CERTIFIED_FORM_FILLING(2, "表單認證：僅允許填寫表單"),

	/**
	 * 註解認證：允許後續填寫表單與添加註解 (Form filling and annotations permitted)
	 */
	CERTIFIED_FORM_FILLING_AND_ANNOTATIONS(3, "註解認證：允許填寫表單與添加註解");

	private final int code;
	private final String description;

	PdfCertificationLevel(int code, String description) {
		this.code = code;
		this.description = description;
	}

	public int getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}
}
