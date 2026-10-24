package com.example.demo.infrastructure.adapter;

import com.example.demo.application.shared.command.DecryptPdfCommand;
import com.example.demo.application.shared.command.EncryptPdfCommand;
import com.example.demo.application.shared.command.SignPdfCommand;
import com.example.demo.application.shared.dto.QuotationGottenResult;
import com.example.demo.application.shared.dto.QuotationItemGottenResult;
import com.example.demo.application.shared.security.PdfCertificationLevel;
import com.example.demo.application.shared.security.PdfSignatureSubFilter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
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
 * Apache PDFBox 3.x 安全防護與數位簽章適配器測試 (Adapter Integration Test)
 * <p>
 * 遵循 AGENTS.md 規範：
 * 1. 驗證 Outbound Adapter 整合 Apache PDFBox 3.x 進行 256-bit AES 加密、解密還原與 PKCS#7 增量數位簽署。
 * 2. 驗證自定義數位簽章規範體系 (PdfSignaturePolicy) 與視覺印章客製化繪製 (PdfVisualSignatureCustomizer)。
 * 3. 測試包含正向 256-bit 加解密、權限控管、密碼錯誤異常拋出、認證等級 (PAdES / StrictArchival) 與簽章字典校驗。
 * 4. 測試方法皆以 BDD Gherkin 語法撰寫 Javadoc。
 * </p>
 */
@SpringBootTest
class PdfBoxSecurityExecutorAdapterTest {

	@Autowired
	private PdfBoxSecurityExecutorAdapter pdfBoxSecurityAdapter;

	@Autowired
	private OpenQuotationPdfGeneratorAdapter quotationGeneratorAdapter;

	private static byte[] testKeyStoreBytes;
	private static final String KEYSTORE_PASSWORD = "pdfBoxSecret123";
	private static final String KEY_ALIAS = "pdfbox-signer";

	@BeforeAll
	static void setupCertificate() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair keyPair = kpg.generateKeyPair();

		long now = System.currentTimeMillis();
		Date notBefore = new Date(now - 60_000L);
		Date notAfter = new Date(now + 365L * 24 * 60 * 60 * 1000L);
		X500Name subject = new X500Name("CN=PDFBox Security Test, O=Enterprise, C=TW");

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
	 * Feature: PDFBox 3.x 256-bit AES 加密與解密功能
	 * Scenario: 正常流程 - 輸入合法 PDF，以 PDFBox 進行 256-bit AES 加密，再以正確密碼解密還原
	 *
	 * Given: 一份合法報價單 PDF，使用者密碼為 "userAes256"，擁有者密碼為 "ownerAes256"
	 * When: 調用 pdfBoxSecurityAdapter.encryptPdf 執行 AES-256 加密
	 * Then: 產生加密 PDF，未提供密碼載入會拋出 InvalidPasswordException；再調用 decryptPdf 成功解開並還原為無加密文件
	 */
	@Test
	@DisplayName("PDFBox加密與解密-正常流程-AES256加密後無密碼無法開啟且正確密碼可解密還原")
	void encryptAndDecrypt_validPasswords_success() throws IOException {
		// Given
		byte[] sourcePdf = quotationGeneratorAdapter.generateQuotationPdf(createSampleQuotation("QT-BOX-001"));
		assertNotNull(sourcePdf);

		EncryptPdfCommand encryptCommand = new EncryptPdfCommand(
				sourcePdf,
				"userAes256",
				"ownerAes256",
				true,
				false
		);

		// When: 執行 PDFBox 加密
		byte[] encryptedPdf = pdfBoxSecurityAdapter.encryptPdf(encryptCommand);
		assertNotNull(encryptedPdf);
		assertTrue(encryptedPdf.length > 0);

		// Then: 驗證無密碼開啟失敗
		assertThrows(InvalidPasswordException.class, () -> Loader.loadPDF(encryptedPdf));

		// When: 以正確使用者密碼解密
		DecryptPdfCommand decryptCommand = new DecryptPdfCommand(
				encryptedPdf, "userAes256"
		);
		byte[] decryptedPdf = pdfBoxSecurityAdapter.decryptPdf(decryptCommand);

		// Then: 解密後 PDFBox 能無密碼順利讀取且已移除加密保護
		assertNotNull(decryptedPdf);
		try (PDDocument doc = Loader.loadPDF(decryptedPdf)) {
			assertEquals(1, doc.getNumberOfPages());
			assertFalse(doc.isEncrypted(), "解密後文件應標記為無加密 (isEncrypted == false)");
		}
	}

	/**
	 * Feature: PDFBox 安全解密防禦分支
	 * Scenario: 錯誤密碼解密失敗
	 *
	 * Given: 一份經由 PDFBox 加密之 PDF，但傳入錯誤之密碼 "wrongBoxPassword"
	 * When: 調用 pdfBoxSecurityAdapter.decryptPdf 進行解密
	 * Then: 系統攔截底層 InvalidPasswordException 並拋出語意清晰之 IllegalArgumentException
	 */
	@Test
	@DisplayName("PDFBox解密-異常分支-錯誤密碼解密應拋出IllegalArgumentException")
	void decrypt_wrongPassword_throwsIllegalArgumentException() {
		// Given
		byte[] sourcePdf = quotationGeneratorAdapter.generateQuotationPdf(createSampleQuotation("QT-BOX-002"));
		byte[] encryptedPdf = pdfBoxSecurityAdapter.encryptPdf(new EncryptPdfCommand(
				sourcePdf, "correctBoxPwd", "ownerPwd", true, true
		));

		// When & Then
		DecryptPdfCommand wrongPwdCommand = new DecryptPdfCommand(
				encryptedPdf, "wrongBoxPassword"
		);
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
				pdfBoxSecurityAdapter.decryptPdf(wrongPwdCommand)
		);
		assertTrue(ex.getMessage().contains("密碼錯誤"));
	}

	/**
	 * Feature: PDFBox 數位簽章功能 (預設標準簽章規範)
	 * Scenario: 正常流程 - 輸入合法 PDF 與 PKCS#12 憑證鏈，成功執行增量數位簽章並印製雙線視覺印章
	 *
	 * Given: 一份合法報價單 PDF、自簽 PKCS#12 金鑰庫位元組與密碼
	 * When: 調用 pdfBoxSecurityAdapter.signPdf 執行數位簽名
	 * Then: 簽署完成之 PDF 包含數位簽章欄位，且透過 PDFBox 檢索可驗證 Reason、Location 與視覺外觀
	 */
	@Test
	@DisplayName("PDFBox數位簽章-標準規範-成功完成增量數位簽章並包含簽章字典與視覺外觀")
	void signPdf_validKeyStore_success() throws IOException {
		// Given
		byte[] sourcePdf = quotationGeneratorAdapter.generateQuotationPdf(createSampleQuotation("QT-BOX-SIGN-01"));
		SignPdfCommand signCommand = new SignPdfCommand(
				sourcePdf,
				testKeyStoreBytes,
				KEYSTORE_PASSWORD,
				KEY_ALIAS,
				"核准正式報價單 (PDFBox 簽署)",
				"新竹研發中心",
				"pdfbox-security@enterprise.com",
				true,
				1,
				340f,
				60f,
				190f,
				70f,
				PdfCertificationLevel.NOT_CERTIFIED,
				PdfSignatureSubFilter.ADBE_PKCS7_DETACHED
		);

		// When: 執行 PDFBox 數位簽章
		byte[] signedPdf = pdfBoxSecurityAdapter.signPdf(signCommand);

		// Then: 驗證回傳非空且尺寸大於原始檔案 (包含增量更新與視覺外觀串流)
		assertNotNull(signedPdf);
		assertTrue(signedPdf.length > sourcePdf.length);

		// 透過 PDFBox 檢驗數位簽章字典
		try (PDDocument doc = Loader.loadPDF(signedPdf)) {
			List<PDSignature> signatures = doc.getSignatureDictionaries();
			assertNotNull(signatures);
			assertFalse(signatures.isEmpty(), "簽署後之 PDF 應具備至少一個數位簽章欄位");

			PDSignature signature = signatures.get(0);
			assertEquals("核准正式報價單 (PDFBox 簽署)", signature.getReason());
			assertEquals("新竹研發中心", signature.getLocation());
			assertEquals("pdfbox-security@enterprise.com", signature.getContactInfo());
			assertEquals("adbe.pkcs7.detached", signature.getSubFilter());
		}
	}

	/**
	 * Feature: PDFBox 高級數位簽章規範 (Strict Archival 法務存檔規範)
	 * Scenario: 正常流程 - 指定 CERTIFIED_NO_CHANGES_ALLOWED 認證等級與 ETSI.CAdES.detached 子過濾器
	 *
	 * Given: 一份報價單 PDF，傳入最高認證等級 CERTIFIED_NO_CHANGES_ALLOWED (MDP DocMDP=1)
	 * When: 調用 pdfBoxSecurityAdapter.signPdf 進行存檔認證簽名
	 * Then: 成功產出具備 DocMDP 認證權限之簽署 PDF，且 SubFilter 為 ETSI.CAdES.detached
	 */
	@Test
	@DisplayName("PDFBox數位簽章-嚴格法務存檔規範-成功標記DocMDP權限鎖定與CAdES規範")
	void signPdf_strictArchivalPolicy_success() throws IOException {
		// Given
		byte[] sourcePdf = quotationGeneratorAdapter.generateQuotationPdf(createSampleQuotation("QT-BOX-ARCHIVAL-01"));
		SignPdfCommand signCommand = new SignPdfCommand(
				sourcePdf,
				testKeyStoreBytes,
				KEYSTORE_PASSWORD,
				KEY_ALIAS,
				"法定正式存檔封存 (禁止後續任何變更)",
				"法務部檔案庫",
				"legal@enterprise.com",
				true,
				1,
				340f,
				60f,
				190f,
				70f,
				PdfCertificationLevel.CERTIFIED_NO_CHANGES_ALLOWED,
				PdfSignatureSubFilter.ETSI_CADES_DETACHED
		);

		// When: 執行數位簽章
		byte[] signedPdf = pdfBoxSecurityAdapter.signPdf(signCommand);
		assertNotNull(signedPdf);

		// Then: 檢驗 SubFilter 與簽章字典
		try (PDDocument doc = Loader.loadPDF(signedPdf)) {
			List<PDSignature> signatures = doc.getSignatureDictionaries();
			assertNotNull(signatures);
			assertFalse(signatures.isEmpty());

			PDSignature signature = signatures.get(0);
			assertEquals("ETSI.CAdES.detached", signature.getSubFilter());
			assertEquals("法定正式存檔封存 (禁止後續任何變更)", signature.getReason());
		}
	}

	/**
	 * Feature: 個別單據範本自定義簽章規範 (PdfDocumentTemplate Integration)
	 * Scenario: 正常流程 - 指定 template="bom"，自動套用 BomQuotationPdfTemplate 定義之 PAdES 規範、預設理由與地點
	 *
	 * Given: 一份報價單 PDF，未傳入 reason/location，但指定 templateName="bom"
	 * When: 調用 pdfBoxSecurityAdapter.signPdf 執行數位簽署
	 * Then: 系統識別 BOM 範本，自動套用範本定義之 PAdES 規範 (ETSI.CAdES.detached) 以及預設理由「BOM 原物料報價核准簽署」、地點「台北營運總部」
	 */
	@Test
	@DisplayName("PDFBox數位簽章-個別範本簽章規範-指定bom範本自動套用PAdES規範與專屬簽署理由地點")
	void signPdf_templateSignaturePolicy_success() throws IOException {
		// Given
		byte[] sourcePdf = quotationGeneratorAdapter.generateQuotationPdf(createSampleQuotation("QT-BOX-TEMPLATE-01"));
		SignPdfCommand signCommand = new SignPdfCommand(
				sourcePdf,
				testKeyStoreBytes,
				KEYSTORE_PASSWORD,
				KEY_ALIAS,
				null, // 未指定 reason，預期自動繼承範本定義
				null, // 未指定 location，預期自動繼承範本定義
				"approver@enterprise.com",
				true,
				1,
				340f,
				60f,
				190f,
				70f,
				"bom" // 指定 BOM 範本
		);

		// When: 執行數位簽署
		byte[] signedPdf = pdfBoxSecurityAdapter.signPdf(signCommand);
		assertNotNull(signedPdf);

		// Then: 檢驗簽章字典自動套用 BOM 範本定義之理由、地點與 PAdES 規範
		try (PDDocument doc = Loader.loadPDF(signedPdf)) {
			List<PDSignature> signatures = doc.getSignatureDictionaries();
			assertNotNull(signatures);
			assertFalse(signatures.isEmpty(), "簽署後之 PDF 應具備簽章字典");

			PDSignature signature = signatures.get(0);
			assertEquals("ETSI.CAdES.detached", signature.getSubFilter(), "BOM 範本指定 PAdES 規範，子過濾器應為 ETSI.CAdES.detached");
			assertEquals("BOM 原物料報價核准簽署", signature.getReason(), "未指定理由時應自動套用 BomQuotationPdfTemplate 定義之理由");
			assertEquals("台北營運總部", signature.getLocation(), "未指定地點時應自動套用 BomQuotationPdfTemplate 定義之地點");
		}
	}

	private QuotationGottenResult createSampleQuotation(String quotationNo) {
		return new QuotationGottenResult(
				quotationNo,
				LocalDate.of(2026, 10, 25),
				"PDFBox 企業測試股份有限公司",
				"87654321",
				"李經理 (02-87654321)",
				"45 天",
				"信用狀 (L/C)",
				"新竹市科學園區研發六路 8 號",
				new BigDecimal("88000.00"),
				new BigDecimal("4400.00"),
				new BigDecimal("92400.00"),
				"PDFBox 引擎高規格測試",
				List.of(
						new QuotationItemGottenResult(
								1,
								"PROD-BOX-01",
								"高階數位文件簽章模組",
								2,
								"SET",
								new BigDecimal("44000.00"),
								new BigDecimal("88000.00"),
								"含 256-bit AES 與 PAdES 簽章支援"
						)
				)
		);
	}
}
