package com.example.demo.application.shared.security;

/**
 * PDF 數位簽章標準子過濾器 (SubFilter 簽章類型規格)
 */
public enum PdfSignatureSubFilter {
	/**
	 * Adobe PKCS#7 Detached (業界最通用之數位簽章標準)
	 */
	ADBE_PKCS7_DETACHED("adbe.pkcs7.detached", "Adobe PKCS#7 脫鉤簽名"),

	/**
	 * ETSI CAdES / PAdES (歐盟高級電子簽章合規標準)
	 */
	ETSI_CADES_DETACHED("ETSI.CAdES.detached", "ETSI CAdES/PAdES 高級電子簽章");

	private final String subFilterName;
	private final String description;

	PdfSignatureSubFilter(String subFilterName, String description) {
		this.subFilterName = subFilterName;
		this.description = description;
	}

	public String getSubFilterName() {
		return subFilterName;
	}

	public String getDescription() {
		return description;
	}
}
