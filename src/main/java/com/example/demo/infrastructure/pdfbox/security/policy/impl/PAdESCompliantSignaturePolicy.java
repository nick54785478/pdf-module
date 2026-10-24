package com.example.demo.infrastructure.pdfbox.security.policy.impl;

import com.example.demo.application.shared.security.PdfCertificationLevel;
import com.example.demo.application.shared.security.PdfSignatureSubFilter;
import com.example.demo.infrastructure.pdfbox.security.policy.PdfSignaturePolicy;

/**
 * 歐盟 PAdES / CAdES 高級電子簽章合規規範
 * <p>
 * 遵循 ETSI EN 319 142 標準，採用 ETSI.CAdES.detached 子過濾器規格。
 * </p>
 */
public class PAdESCompliantSignaturePolicy implements PdfSignaturePolicy {

	@Override
	public String getSubFilter() {
		return PdfSignatureSubFilter.ETSI_CADES_DETACHED.getSubFilterName();
	}

	@Override
	public String getSignatureAlgorithm() {
		return "SHA256withRSA";
	}

	@Override
	public PdfCertificationLevel getCertificationLevel() {
		return PdfCertificationLevel.CERTIFIED_FORM_FILLING;
	}
}
