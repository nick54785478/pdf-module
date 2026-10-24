package com.example.demo.infrastructure.pdfbox.security.policy.impl;

import com.example.demo.infrastructure.pdfbox.security.policy.PdfSignaturePolicy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 預設標準企業數位簽章規範 (Default Signature Policy)
 * <p>
 * 遵循標準 Adobe PKCS#7 脫鉤簽名 (adbe.pkcs7.detached) 與 SHA256withRSA 演算法，
 * 適用於一般內部審批、報價單簽回與業務核准。
 * </p>
 */
@Primary
@Component
public class DefaultPdfSignaturePolicy implements PdfSignaturePolicy {
	// 繼承預設值：Filter=Adobe.PPKLite, SubFilter=adbe.pkcs7.detached, SHA256withRSA, NOT_CERTIFIED
}
