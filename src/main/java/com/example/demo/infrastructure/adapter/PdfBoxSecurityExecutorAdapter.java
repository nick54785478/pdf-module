package com.example.demo.infrastructure.adapter;

import com.example.demo.application.port.out.PdfSecurityExecutorPort;
import com.example.demo.application.shared.command.DecryptPdfCommand;
import com.example.demo.application.shared.command.EncryptPdfCommand;
import com.example.demo.application.shared.command.SignPdfCommand;
import com.example.demo.application.shared.security.PdfCertificationLevel;
import com.example.demo.infrastructure.openpdf.template.PdfDocumentTemplate;
import com.example.demo.infrastructure.pdfbox.security.BouncyCastlePdfBoxSignatureInterface;
import com.example.demo.infrastructure.pdfbox.security.policy.PdfSignaturePolicy;
import com.example.demo.infrastructure.pdfbox.security.customizer.PdfVisualSignatureCustomizer;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Apache PDFBox 安全防護與數位簽章適配器 (Outbound Adapter)
 * <p>
 * 遵循 Clean Architecture 與六角形架構規範，實作應用層 {@link PdfSecurityExecutorPort} 輸出端口，
 * 並標註為 {@link Primary} 作為系統標準 PDF 安全與簽章執行核心。
 * 基於 Apache PDFBox 3.0.8 與 BouncyCastle 提供高自定義之安全能力：
 * 1. 256-bit AES 與細粒度 AccessPermission 權限控管。
 * 2. 一鍵安全解除之無損解密 (setAllSecurityToBeRemoved)。
 * 3. 策略驅動之 PKCS#7 增量數位簽署 (saveIncremental) 與自定義外觀繪製。
 * 4. 支援針對個別 {@link PdfDocumentTemplate} 專屬定義簽章規範與視覺印章，未指定或無定義時默認使用系統預設簽章。
 * 遵循命名規範 3-9：Outbound Adapter 一律以 Adapter 結尾。
 * </p>
 */
@Slf4j
@Primary
@Component("pdfSecurityExecutorPort")
public class PdfBoxSecurityExecutorAdapter implements PdfSecurityExecutorPort {

	private final PdfSignaturePolicy defaultSignaturePolicy;
	private final PdfVisualSignatureCustomizer defaultVisualCustomizer;
	private final List<PdfDocumentTemplate<?>> documentTemplates;

	@Autowired
	public PdfBoxSecurityExecutorAdapter(
			PdfSignaturePolicy defaultSignaturePolicy,
			PdfVisualSignatureCustomizer defaultVisualCustomizer,
			@Autowired(required = false) List<PdfDocumentTemplate<?>> documentTemplates) {
		this.defaultSignaturePolicy = defaultSignaturePolicy;
		this.defaultVisualCustomizer = defaultVisualCustomizer;
		this.documentTemplates = documentTemplates != null ? documentTemplates : Collections.emptyList();
	}

	public PdfBoxSecurityExecutorAdapter(
			PdfSignaturePolicy defaultSignaturePolicy,
			PdfVisualSignatureCustomizer defaultVisualCustomizer) {
		this(defaultSignaturePolicy, defaultVisualCustomizer, Collections.emptyList());
	}

	/**
	 * 對 PDF 檔案進行標準 256-bit AES 安全加密與細部權限控管
	 *
	 * @param command 加密命令物件 (包含原始 PDF 位元組、使用者/擁有者密碼與權限配置)
	 * @return 加密後受保護之 PDF 二進位 byte 陣列
	 */
	@Override
	public byte[] encryptPdf(EncryptPdfCommand command) {
		if (command == null || command.fileBytes() == null || command.fileBytes().length == 0) {
			log.error("PDFBox 加密失敗：命令物件或 PDF 檔案位元組為空");
			throw new IllegalArgumentException("待加密之 PDF 檔案位元組不可為空");
		}

		log.info("【PDFBox 引擎】開始對 PDF 進行安全加密: userPwdSet={}, ownerPwdSet={}, allowPrint={}, allowCopy={}",
				command.userPassword() != null && !command.userPassword().isEmpty(),
				command.ownerPassword() != null && !command.ownerPassword().isEmpty(),
				command.allowPrinting(), command.allowCopy());

		try (PDDocument doc = Loader.loadPDF(command.fileBytes())) {
			AccessPermission permissions = new AccessPermission();
			permissions.setCanPrint(command.allowPrinting());
			permissions.setCanExtractContent(command.allowCopy());
			permissions.setCanExtractForAccessibility(true);

			String userPwd = command.userPassword() != null ? command.userPassword() : "";
			String ownerPwd = command.ownerPassword() != null && !command.ownerPassword().isEmpty()
					? command.ownerPassword()
					: (!userPwd.isEmpty() ? userPwd : "defaultPdfBoxOwnerKey");

			StandardProtectionPolicy policy = new StandardProtectionPolicy(ownerPwd, userPwd, permissions);
			policy.setEncryptionKeyLength(256); // 採用高強度 256-bit AES

			doc.protect(policy);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			doc.save(baos);

			byte[] encryptedPdf = baos.toByteArray();
			log.info("【PDFBox 引擎】PDF 加密保護完成: outputSize={} bytes", encryptedPdf.length);
			return encryptedPdf;
		} catch (Exception e) {
			log.error("【PDFBox 引擎】PDF 加密過程發生異常: {}", e.getMessage(), e);
			throw new RuntimeException("PDF 加密處理失敗: " + e.getMessage(), e);
		}
	}

	/**
	 * 對受密碼保護之 PDF 檔案進行解密並完全移除安全限制
	 *
	 * @param command 解密命令物件 (包含受保護之 PDF 位元組與密碼)
	 * @return 解密後無安全限制之標準 PDF 二進位 byte 陣列
	 */
	@Override
	public byte[] decryptPdf(DecryptPdfCommand command) {
		if (command == null || command.fileBytes() == null || command.fileBytes().length == 0) {
			log.error("PDFBox 解密失敗：命令物件或 PDF 檔案位元組為空");
			throw new IllegalArgumentException("待解密之 PDF 檔案位元組不可為空");
		}

		String password = command.password() != null ? command.password() : "";
		log.info("【PDFBox 引擎】開始對 PDF 進行解密驗證: passwordProvided={}", !password.isEmpty());

		try (PDDocument doc = Loader.loadPDF(command.fileBytes(), password)) {
			// 一鍵徹底清除文檔的所有安全與權限限制
			doc.setAllSecurityToBeRemoved(true);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			doc.save(baos);

			byte[] decryptedPdf = baos.toByteArray();
			log.info("【PDFBox 引擎】PDF 解密完成: outputSize={} bytes", decryptedPdf.length);
			return decryptedPdf;
		} catch (InvalidPasswordException ipe) {
			log.warn("【PDFBox 引擎】解密失敗：密碼錯誤或未授權存取");
			throw new IllegalArgumentException("PDF 解密失敗：密碼錯誤或未授權存取", ipe);
		} catch (IllegalArgumentException iae) {
			throw iae;
		} catch (Exception e) {
			// 防禦性檢查例外原因鏈是否包含 InvalidPasswordException
			Throwable cause = e;
			while (cause != null) {
				if (cause instanceof InvalidPasswordException) {
					log.warn("【PDFBox 引擎】解密失敗：偵測到底層密碼錯誤 (from cause chain)");
					throw new IllegalArgumentException("PDF 解密失敗：密碼錯誤或未授權存取", cause);
				}
				cause = cause.getCause();
			}
			log.error("【PDFBox 引擎】PDF 解密過程發生異常: {}", e.getMessage(), e);
			throw new RuntimeException("PDF 解密處理失敗: " + e.getMessage(), e);
		}
	}

	/**
	 * 對 PDF 檔案進行數位簽署，支援範本專屬簽章規範與預設簽章 fallback
	 *
	 * @param command 數位簽章命令物件 (包含 PDF、PKCS#12 金鑰庫位元組、密碼、可選 templateName 與簽名設定)
	 * @return 完成數位簽署之 PDF 二進位 byte 陣列
	 */
	@Override
	public byte[] signPdf(SignPdfCommand command) {
		PdfDocumentTemplate<?> matchedTemplate = resolveTemplate(command != null ? command.templateName() : null);
		return signPdfWithTemplate(command, matchedTemplate);
	}

	/**
	 * 綁定特定單據範本直接進行數位簽署 (支援程式化傳入範本)
	 *
	 * @param command  數位簽章命令物件
	 * @param template 具體單據範本物件 (可為 null)
	 * @return 完成數位簽署之 PDF 二進位 byte 陣列
	 */
	public byte[] signPdf(SignPdfCommand command, PdfDocumentTemplate<?> template) {
		return signPdfWithTemplate(command, template);
	}

	private byte[] signPdfWithTemplate(SignPdfCommand command, PdfDocumentTemplate<?> template) {
		if (command == null || command.fileBytes() == null || command.fileBytes().length == 0) {
			log.error("PDFBox 數位簽章失敗：命令物件或 PDF 檔案位元組為空");
			throw new IllegalArgumentException("待簽章之 PDF 檔案位元組不可為空");
		}
		if (command.keyStoreBytes() == null || command.keyStoreBytes().length == 0) {
			log.error("PDFBox 數位簽章失敗：PKCS#12 金鑰庫位元組為空");
			throw new IllegalArgumentException("PKCS#12 憑證金鑰庫位元組不可為空");
		}
		if (command.keyStorePassword() == null) {
			log.error("PDFBox 數位簽章失敗：金鑰庫密碼為 null");
			throw new IllegalArgumentException("金鑰庫解鎖密碼不可為 null");
		}

		// 核心策略：針對個別範本定義簽章，如果沒有就默認使用預設簽章
		PdfSignaturePolicy effectivePolicy = (template != null && template.getSignaturePolicy() != null)
				? template.getSignaturePolicy()
				: this.defaultSignaturePolicy;

		PdfVisualSignatureCustomizer effectiveVisualCustomizer = (template != null && template.getVisualSignatureCustomizer() != null)
				? template.getVisualSignatureCustomizer()
				: this.defaultVisualCustomizer;

		String effectiveReason = (command.reason() != null && !command.reason().isBlank())
				? command.reason()
				: (template != null && template.getDefaultSignatureReason() != null
						? template.getDefaultSignatureReason()
						: "核准正式文件");

		String effectiveLocation = (command.location() != null && !command.location().isBlank())
				? command.location()
				: (template != null && template.getDefaultSignatureLocation() != null
						? template.getDefaultSignatureLocation()
						: "台北總部");

		log.info("【PDFBox 引擎】開始對 PDF 進行數位簽署: template={}, policy={}, reason={}, location={}, subFilter={}",
				template != null ? template.getTemplateName() : "default",
				effectivePolicy.getClass().getSimpleName(),
				effectiveReason, effectiveLocation,
				command.subFilter() != null ? command.subFilter().getSubFilterName() : effectivePolicy.getSubFilter());

		try {
			// 1. 載入金鑰庫與私鑰
			KeyStore ks = KeyStore.getInstance("PKCS12");
			try (ByteArrayInputStream ksis = new ByteArrayInputStream(command.keyStoreBytes())) {
				ks.load(ksis, command.keyStorePassword().toCharArray());
			} catch (Exception e) {
				log.warn("【PDFBox 引擎】金鑰庫載入失敗: {}", e.getMessage());
				throw new IllegalArgumentException("金鑰庫載入失敗：密碼錯誤或格式非合法 PKCS#12", e);
			}

			String alias = command.keyAlias();
			if (alias == null || alias.isBlank()) {
				Enumeration<String> aliases = ks.aliases();
				if (aliases.hasMoreElements()) {
					alias = aliases.nextElement();
				} else {
					throw new IllegalArgumentException("PKCS#12 金鑰庫中查無任何憑證別名 (Alias)");
				}
			}

			PrivateKey privateKey = (PrivateKey) ks.getKey(alias, command.keyStorePassword().toCharArray());
			if (privateKey == null) {
				throw new IllegalArgumentException("找不到別名 [" + alias + "] 對應之私鑰");
			}

			Certificate[] chain = ks.getCertificateChain(alias);
			if (chain == null || chain.length == 0) {
				throw new IllegalArgumentException("找不到別名 [" + alias + "] 對應之憑證鏈");
			}

			// 2. 載入 PDF 文件並建立簽章字典
			try (PDDocument doc = Loader.loadPDF(command.fileBytes())) {
				PDSignature signature = new PDSignature();
				signature.setFilter(COSName.getPDFName(effectivePolicy.getFilter()));
				String subFilter = (template != null && template.getSignaturePolicy() != null)
						? effectivePolicy.getSubFilter()
						: (command.subFilter() != null ? command.subFilter().getSubFilterName() : effectivePolicy.getSubFilter());
				signature.setSubFilter(COSName.getPDFName(subFilter));
				signature.setName(alias);
				signature.setReason(effectiveReason);
				signature.setLocation(effectiveLocation);
				if (command.contact() != null && !command.contact().isBlank()) {
					signature.setContactInfo(command.contact());
				}
				signature.setSignDate(Calendar.getInstance());

				// 3. 處理認證層級 (MDP Certification Level)
				PdfCertificationLevel certLevel = (template != null && template.getSignaturePolicy() != null)
						? effectivePolicy.getCertificationLevel()
						: ((command.certificationLevel() != null && command.certificationLevel() != PdfCertificationLevel.NOT_CERTIFIED)
								? command.certificationLevel()
								: effectivePolicy.getCertificationLevel());

				if (certLevel != PdfCertificationLevel.NOT_CERTIFIED) {
					setCertificationLevel(doc, signature, certLevel.getCode());
				}

				// 4. 自定義視覺防偽印章外觀繪製 (若範本有定義專屬坐標且呼叫端未覆寫，優先套用範本坐標)
				if (command.visualSignature()) {
					int pageIndex = Math.max(0, command.page() - 1);
					float effX = (template != null && template.getDefaultSignatureX() != null && command.x() == 360f)
							? template.getDefaultSignatureX() : command.x();
					float effY = (template != null && template.getDefaultSignatureY() != null && command.y() == 50f)
							? template.getDefaultSignatureY() : command.y();
					float effW = (template != null && template.getDefaultSignatureWidth() != null && command.width() == 180f)
							? template.getDefaultSignatureWidth() : command.width();
					float effH = (template != null && template.getDefaultSignatureHeight() != null && command.height() == 60f)
							? template.getDefaultSignatureHeight() : command.height();

					SignPdfCommand effectiveVisualCommand = new SignPdfCommand(
							command.fileBytes(), command.keyStoreBytes(), command.keyStorePassword(),
							command.keyAlias(), effectiveReason, effectiveLocation, command.contact(),
							command.visualSignature(), command.page(), effX, effY, effW, effH,
							command.templateName(), certLevel, command.subFilter()
					);
					effectiveVisualCustomizer.customizeAppearance(doc, pageIndex, effectiveVisualCommand, signature);
				}

				// 5. 調用 BouncyCastle 簽章介面增量簽署
				BouncyCastlePdfBoxSignatureInterface signatureInterface =
						new BouncyCastlePdfBoxSignatureInterface(privateKey, chain, effectivePolicy.getSignatureAlgorithm());

				SignatureOptions options = new SignatureOptions();
				doc.addSignature(signature, signatureInterface, options);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				doc.saveIncremental(baos); // 關鍵：增量儲存以維持不可否認性簽章完整性

				byte[] signedPdf = baos.toByteArray();
				log.info("【PDFBox 引擎】PDF 數位簽署成功完成: template={}, outputSize={} bytes",
						template != null ? template.getTemplateName() : "default", signedPdf.length);
				return signedPdf;
			}
		} catch (IllegalArgumentException iae) {
			throw iae;
		} catch (Exception e) {
			log.error("【PDFBox 引擎】數位簽署發生異常: {}", e.getMessage(), e);
			throw new RuntimeException("PDF 數位簽署失敗: " + e.getMessage(), e);
		}
	}

	/**
	 * 依範本識別字串尋找已註冊之單據範本
	 */
	private PdfDocumentTemplate<?> resolveTemplate(String templateName) {
		if (templateName == null || templateName.isBlank() || documentTemplates == null || documentTemplates.isEmpty()) {
			return null;
		}
		return documentTemplates.stream()
				.filter(t -> templateName.equalsIgnoreCase(t.getTemplateName())
						|| templateName.equalsIgnoreCase(t.getClass().getSimpleName()))
				.findFirst()
				.orElse(null);
	}

	/**
	 * 設定 PDF 認證層級字典 (DocMDP transform parameters)
	 */
	private void setCertificationLevel(PDDocument doc, PDSignature signature, int accessPermissions) {
		COSDictionary transformParameters = new COSDictionary();
		transformParameters.setItem(COSName.TYPE, COSName.getPDFName("TransformParams"));
		transformParameters.setInt(COSName.P, accessPermissions);
		transformParameters.setName(COSName.V, "1.2");

		COSDictionary reference = new COSDictionary();
		reference.setItem(COSName.TYPE, COSName.getPDFName("SigRef"));
		reference.setItem(COSName.getPDFName("TransformMethod"), COSName.getPDFName("DocMDP"));
		reference.setItem(COSName.getPDFName("TransformParams"), transformParameters);

		COSDictionary lockDictionary = new COSDictionary();
		lockDictionary.setItem(COSName.TYPE, COSName.getPDFName("SigFieldLock"));
		lockDictionary.setItem(COSName.getPDFName("Action"), COSName.getPDFName("All"));

		signature.getCOSObject().setItem(COSName.getPDFName("Reference"), reference);
		signature.getCOSObject().setItem(COSName.getPDFName("Lock"), lockDictionary);

		doc.getDocumentCatalog().getCOSObject().setInt(COSName.getPDFName("DocMDP"), accessPermissions);
	}
}
