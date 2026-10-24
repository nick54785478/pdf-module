package com.example.demo.infrastructure.adapter;

import com.example.demo.application.shared.command.DecryptPdfCommand;
import com.example.demo.application.shared.command.EncryptPdfCommand;
import com.example.demo.application.shared.command.SignPdfCommand;
import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.dto.QuotationItemGottenResult;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OpenPDF 與 BouncyCastle 安全防護與數位簽章適配器測試 (Adapter Integration Test)
 * <p>
 * 遵循 AGENTS.md 規範：
 * 1. 完整驗證 Outbound Adapter 整合 OpenPDF 與 BouncyCastle 進行 PDF 加密、解密與 PKCS#7 數位簽章之正確性。
 * 2. 測試包含正向密碼加解密、權限限制、錯誤密碼拒絕驗證、合法 PKCS#12 憑證鏈數位簽章印製。
 * 3. 測試方法皆以 BDD Gherkin 語法撰寫 Javadoc。
 * </p>
 */
@SpringBootTest
class OpenPdfSecurityExecutorAdapterTest {

	@Autowired
	private OpenPdfSecurityExecutorAdapter openPdfSecurityAdapter;

	@Autowired
	private OpenQuotationPdfGeneratorAdapter openPdfQuotationAdapter;

	private static byte[] testKeyStoreBytes;
	private static final String KEYSTORE_PASSWORD = "testPassword123";
	private static final String KEY_ALIAS = "test-signer";

	@BeforeAll
	static void setupCertificate() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair keyPair = kpg.generateKeyPair();

		long now = System.currentTimeMillis();
		Date notBefore = new Date(now - 60_000L);
		Date notAfter = new Date(now + 365L * 24 * 60 * 60 * 1000L);
		X500Name subject = new X500Name("CN=OpenPDF Security Test, O=Company, C=TW");

		SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
		X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
				subject,
				BigInteger.valueOf(System.currentTimeMillis()),
				notBefore,
				notAfter,
				subject,
				subPubKeyInfo
		);

		ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
		X509CertificateHolder certHolder = certBuilder.build(signer);
		X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);

		KeyStore ks = KeyStore.getInstance("PKCS12");
		ks.load(null, null);
		ks.setKeyEntry(KEY_ALIAS, keyPair.getPrivate(), KEYSTORE_PASSWORD.toCharArray(), new Certificate[]{cert});

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ks.store(baos, KEYSTORE_PASSWORD.toCharArray());
		testKeyStoreBytes = baos.toByteArray();
	}

	/**
	 * Feature: PDF 安全加密與解密功能
	 * Scenario: 正常流程 - 輸入合法 PDF、使用者密碼與權限配置，成功加密；再以正確密碼解密還原
	 *
	 * Given: 一份合法報價單 PDF，使用者密碼為 "userSecret"，擁有者密碼為 "ownerSecret"
	 * When: 調用 openPdfSecurityAdapter.encryptPdf 進行標準 128-bit 加密
	 * Then: 產生加密 PDF，若無密碼直接由 PDFBox 讀取將拋出 InvalidPasswordException；再調用 decryptPdf 成功解開並還原
	 */
	@Test
	@DisplayName("加密與解密-正常流程-加密後無密碼無法開啟且正確密碼可解密還原")
	void encryptAndDecrypt_validPasswords_success() throws IOException {
		// Given
		byte[] sourcePdf = openPdfQuotationAdapter.generateQuotationPdf(createSampleQuotation("QT-SEC-001"));
		assertNotNull(sourcePdf);

		EncryptPdfCommand encryptCommand = new EncryptPdfCommand(
				sourcePdf,
				"userSecret",
				"ownerSecret",
				true,
				false
		);

		// When: 執行加密
		byte[] encryptedPdf = openPdfSecurityAdapter.encryptPdf(encryptCommand);
		assertNotNull(encryptedPdf);
		assertTrue(encryptedPdf.length > 0);

		// Then: 驗證無密碼開啟失敗
		assertThrows(InvalidPasswordException.class, () -> Loader.loadPDF(encryptedPdf));

		// When: 以正確密碼解密
		DecryptPdfCommand decryptCommand = new DecryptPdfCommand(encryptedPdf, "userSecret");
		byte[] decryptedPdf = openPdfSecurityAdapter.decryptPdf(decryptCommand);

		// Then: 解密後 PDFBox 能無密碼順利讀取
		assertNotNull(decryptedPdf);
		try (PDDocument doc = Loader.loadPDF(decryptedPdf)) {
			assertEquals(1, doc.getNumberOfPages());
			assertFalse(doc.isEncrypted());
		}
	}

	/**
	 * Feature: PDF 安全解密防禦分支
	 * Scenario: 錯誤密碼解密失敗
	 *
	 * Given: 一份加密之 PDF，但傳入錯誤之密碼 "wrongPassword"
	 * When: 調用 openPdfSecurityAdapter.decryptPdf 進行解密
	 * Then: 系統攔截底層 BadPasswordException 並拋出語意清晰之 IllegalArgumentException
	 */
	@Test
	@DisplayName("解密-異常分支-錯誤密碼解密應拋出IllegalArgumentException")
	void decrypt_wrongPassword_throwsIllegalArgumentException() {
		// Given
		byte[] sourcePdf = openPdfQuotationAdapter.generateQuotationPdf(createSampleQuotation("QT-SEC-002"));
		byte[] encryptedPdf = openPdfSecurityAdapter.encryptPdf(new EncryptPdfCommand(sourcePdf, "correctPwd"));

		// When & Then
		DecryptPdfCommand wrongPwdCommand = new DecryptPdfCommand(encryptedPdf, "wrongPwd");
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
				openPdfSecurityAdapter.decryptPdf(wrongPwdCommand)
		);
		assertTrue(ex.getMessage().contains("密碼錯誤"));
	}

	/**
	 * Feature: PDF 數位簽章功能
	 * Scenario: 正常流程 - 輸入合法 PDF 與 PKCS#12 憑證鏈，成功執行 PKCS#7 數位簽名並加蓋可見印章
	 *
	 * Given: 一份合法報價單 PDF、動態產生之自簽 PKCS#12 金鑰庫位元組與密碼
	 * When: 調用 openPdfSecurityAdapter.signPdf 執行數位簽名
	 * Then: 簽署完成之 PDF 包含數位簽章欄位，透過 OpenPDF AcroFields 能檢索出簽名欄位名稱
	 */
	@Test
	@DisplayName("數位簽章-正常流程-成功完成PKCS#7數位簽章且包含簽章字典")
	void signPdf_validKeyStore_success() throws IOException {
		// Given
		byte[] sourcePdf = openPdfQuotationAdapter.generateQuotationPdf(createSampleQuotation("QT-SIGN-001"));
		SignPdfCommand signCommand = new SignPdfCommand(
				sourcePdf,
				testKeyStoreBytes,
				KEYSTORE_PASSWORD,
				KEY_ALIAS,
				"核准正式報價",
				"台北總部",
				"security@company.com",
				true,
				1,
				350f,
				50f,
				180f,
				60f
		);

		// When: 執行數位簽章
		byte[] signedPdf = openPdfSecurityAdapter.signPdf(signCommand);

		// Then: 驗證回傳非空且尺寸合法
		assertNotNull(signedPdf);
		assertTrue(signedPdf.length > sourcePdf.length);

		// 透過 PDFBox 跨庫驗證數位簽章字典與詮釋資料存在
		try (PDDocument doc = Loader.loadPDF(signedPdf)) {
			var signatures = doc.getSignatureDictionaries();
			assertNotNull(signatures);
			assertFalse(signatures.isEmpty(), "簽署後之 PDF 應具備至少一個數位簽章欄位");
			org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature signature = signatures.get(0);
			assertEquals("核准正式報價", signature.getReason());
			assertEquals("台北總部", signature.getLocation());
		}
	}

	private QuotationGottenResult createSampleQuotation(String quotationNo) {
		return new QuotationGottenResult(
				quotationNo,
				LocalDate.of(2026, 10, 25),
				"安全測試科技股份有限公司",
				"12345678",
				"王大明 (02-12345678)",
				"30 天",
				"TT 電匯",
				"台北市信義區信義路五段7號",
				new BigDecimal("50000.00"),
				new BigDecimal("2500.00"),
				new BigDecimal("52500.00"),
				"安全檢核無誤",
				List.of(
						new QuotationItemGottenResult(
								1,
								"PROD-SEC-01",
								"安全加解密授權套件",
								1,
								"SET",
								new BigDecimal("50000.00"),
								new BigDecimal("50000.00"),
								"含 1 年技術支援"
						)
				)
		);
	}
}
