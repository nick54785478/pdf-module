package com.example.demo.infrastructure.pdfbox.security.policy.impl;

import com.example.demo.application.shared.security.PdfCertificationLevel;
import com.example.demo.infrastructure.pdfbox.security.policy.PdfSignaturePolicy;

/**
 * 嚴格法務存檔不可變更簽章規範 (Strict Archival Signature Policy)
 * <p>
 * 套用 CERTIFIED_NO_CHANGES_ALLOWED 認證等級，簽署後任何對該文件結構或文字的篡改皆會導致簽章失效。
 * </p>
 */
public class StrictArchivalSignaturePolicy implements PdfSignaturePolicy {

	@Override
	public PdfCertificationLevel getCertificationLevel() {
		return PdfCertificationLevel.CERTIFIED_NO_CHANGES_ALLOWED;
	}

	@Override
	public String getSignatureAlgorithm() {
		return "SHA384withRSA";
	}
}
