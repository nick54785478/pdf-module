package com.example.demo.infrastructure.adapter;

import com.example.demo.application.port.out.PdfSecurityExecutorPort;
import com.example.demo.application.shared.command.DecryptPdfCommand;
import com.example.demo.application.shared.command.EncryptPdfCommand;
import com.example.demo.application.shared.command.SignPdfCommand;
import com.example.demo.infrastructure.openpdf.core.OpenPdfFontProvider;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.exceptions.BadPasswordException;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSignatureAppearance;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Enumeration;

/**
 * OpenPDF 與 BouncyCastle 安全防護與數位簽章適配器 (Outbound Adapter)
 * <p>
 * 遵循 Clean Architecture 與六角形架構規範，實作應用層 {@link PdfSecurityExecutorPort} 輸出端口。
 * 作為基礎設施層與底層 OpenPDF (2.0.4) 及 BouncyCastle 安全套件的橋樑，提供：
 * 1. 標準 128-bit 加密與閱讀/複製/列印細部權限控管。
 * 2. 密碼驗證解鎖與保護限制解除。
 * 3. 基於 PKCS#12 (PFX) 金鑰庫、X.509 憑證鏈的不可否認性 PKCS#7 數位簽章與視覺印章印製。
 * 遵循命名規範 3-9：Outbound Adapter 一律以 Adapter 結尾。
 * </p>
 */
@Slf4j
@Component("openPdfSecurityExecutorAdapter")
@RequiredArgsConstructor
public class OpenPdfSecurityExecutorAdapter implements PdfSecurityExecutorPort {

	private final OpenPdfFontProvider fontProvider;

	/**
	 * 對 PDF 檔案進行標準 128-bit 安全加密與權限控管
	 *
	 * @param command 加密命令物件 (包含原始 PDF 位元組、使用者/擁有者密碼與權限配置)
	 * @return 加密後受保護之 PDF 二進位 byte 陣列
	 */
	@Override
	public byte[] encryptPdf(EncryptPdfCommand command) {
		if (command == null || command.fileBytes() == null || command.fileBytes().length == 0) {
			log.error("PDF 加密失敗：命令物件或 PDF 檔案位元組為空");
			throw new IllegalArgumentException("待加密之 PDF 檔案位元組不可為空");
		}

		log.info("開始對 PDF 進行加密保護: userPasswordSet={}, ownerPasswordSet={}, allowPrint={}, allowCopy={}",
				command.userPassword() != null && !command.userPassword().isEmpty(),
				command.ownerPassword() != null && !command.ownerPassword().isEmpty(),
				command.allowPrinting(), command.allowCopy());

		PdfReader reader = null;
		PdfStamper stamper = null;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			reader = new PdfReader(command.fileBytes());
			stamper = new PdfStamper(reader, baos);

			byte[] userPwd = command.userPassword() != null ? command.userPassword().getBytes(StandardCharsets.UTF_8) : null;
			byte[] ownerPwd = command.ownerPassword() != null && !command.ownerPassword().isEmpty()
					? command.ownerPassword().getBytes(StandardCharsets.UTF_8)
					: (userPwd != null ? userPwd : "defaultOwnerKey".getBytes(StandardCharsets.UTF_8));

			int permissions = 0;
			if (command.allowPrinting()) {
				permissions |= PdfWriter.ALLOW_PRINTING;
			}
			if (command.allowCopy()) {
				permissions |= PdfWriter.ALLOW_COPY | PdfWriter.ALLOW_SCREENREADERS;
			}

			stamper.setEncryption(userPwd, ownerPwd, permissions, PdfWriter.STANDARD_ENCRYPTION_128);
			stamper.close();
			reader.close();

			byte[] encryptedPdf = baos.toByteArray();
			log.info("PDF 加密保護完成: outputSize={} bytes", encryptedPdf.length);
			return encryptedPdf;
		} catch (Exception e) {
			log.error("PDF 加密過程發生異常: {}", e.getMessage(), e);
			throw new RuntimeException("PDF 加密處理失敗: " + e.getMessage(), e);
		} finally {
			closeSilently(stamper, reader);
		}
	}

	/**
	 * 對受密碼保護之 PDF 檔案進行解密並解除安全限制
	 *
	 * @param command 解密命令物件 (包含受保護之 PDF 位元組與密碼)
	 * @return 解密後無安全限制之標準 PDF 二進位 byte 陣列
	 */
	@Override
	public byte[] decryptPdf(DecryptPdfCommand command) {
		if (command == null || command.fileBytes() == null || command.fileBytes().length == 0) {
			log.error("PDF 解密失敗：命令物件或 PDF 檔案位元組為空");
			throw new IllegalArgumentException("待解密之 PDF 檔案位元組不可為空");
		}

		log.info("開始對 PDF 進行解密驗證: passwordProvided={}",
				command.password() != null && !command.password().isEmpty());

		PdfReader reader = null;
		PdfStamper stamper = null;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			byte[] passwordBytes = command.password() != null ? command.password().getBytes(StandardCharsets.UTF_8) : null;

			try {
				reader = new PdfReader(command.fileBytes(), passwordBytes);
			} catch (BadPasswordException bpe) {
				log.warn("PDF 解密失敗：密碼錯誤或未提供正確憑據");
				throw new IllegalArgumentException("PDF 解密失敗：密碼錯誤或未授權存取", bpe);
			}

			stamper = new PdfStamper(reader, baos);
			stamper.close();
			reader.close();

			byte[] decryptedPdf = baos.toByteArray();
			log.info("PDF 解密成功完成: outputSize={} bytes", decryptedPdf.length);
			return decryptedPdf;
		} catch (IllegalArgumentException iae) {
			throw iae;
		} catch (Exception e) {
			log.error("PDF 解密過程發生異常: {}", e.getMessage(), e);
			throw new RuntimeException("PDF 解密處理失敗: " + e.getMessage(), e);
		} finally {
			closeSilently(stamper, reader);
		}
	}

	/**
	 * 對 PDF 檔案進行標準 PKCS#7 數位簽署與視覺印章印製
	 *
	 * @param command 數位簽章命令物件 (包含 PDF、PKCS#12 金鑰庫位元組、密碼與簽名設定)
	 * @return 完成數位簽署之 PDF 二進位 byte 陣列
	 */
	@Override
	public byte[] signPdf(SignPdfCommand command) {
		if (command == null || command.fileBytes() == null || command.fileBytes().length == 0) {
			log.error("PDF 數位簽章失敗：命令物件或 PDF 檔案位元組為空");
			throw new IllegalArgumentException("待簽章之 PDF 檔案位元組不可為空");
		}
		if (command.keyStoreBytes() == null || command.keyStoreBytes().length == 0) {
			log.error("PDF 數位簽章失敗：PKCS#12 金鑰庫位元組為空");
			throw new IllegalArgumentException("PKCS#12 憑證金鑰庫位元組不可為空");
		}
		if (command.keyStorePassword() == null) {
			log.error("PDF 數位簽章失敗：金鑰庫密碼為 null");
			throw new IllegalArgumentException("金鑰庫解鎖密碼不可為 null");
		}

		log.info("開始對 PDF 進行數位簽章: reason={}, location={}, visual={}",
				command.reason(), command.location(), command.visualSignature());

		PdfReader reader = null;
		PdfStamper stamper = null;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			// 1. 載入 PKCS#12 金鑰庫
			KeyStore ks = KeyStore.getInstance("PKCS12");
			try (ByteArrayInputStream ksis = new ByteArrayInputStream(command.keyStoreBytes())) {
				ks.load(ksis, command.keyStorePassword().toCharArray());
			} catch (Exception e) {
				log.warn("PKCS#12 金鑰庫載入失敗或密碼不符: {}", e.getMessage());
				throw new IllegalArgumentException("金鑰庫載入失敗：密碼錯誤或檔案格式非合法 PKCS#12", e);
			}

			// 2. 取得別名 (Alias)
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
				throw new IllegalArgumentException("於金鑰庫中找不到別名 [" + alias + "] 對應之私鑰");
			}

			Certificate[] chain = ks.getCertificateChain(alias);
			if (chain == null || chain.length == 0) {
				throw new IllegalArgumentException("於金鑰庫中找不到別名 [" + alias + "] 對應之憑證鏈");
			}

			// 3. 建立簽章外觀
			reader = new PdfReader(command.fileBytes());
			stamper = PdfStamper.createSignature(reader, baos, '\0');

			PdfSignatureAppearance sap = stamper.getSignatureAppearance();
			if (command.reason() != null && !command.reason().isBlank()) {
				sap.setReason(command.reason());
			}
			if (command.location() != null && !command.location().isBlank()) {
				sap.setLocation(command.location());
			}
			if (command.contact() != null && !command.contact().isBlank()) {
				sap.setContact(command.contact());
			}

			// 4. 設定可見視覺印章
			if (command.visualSignature()) {
				int targetPage = Math.min(command.page(), reader.getNumberOfPages());
				Rectangle rect = new Rectangle(
						command.x(),
						command.y(),
						command.x() + command.width(),
						command.y() + command.height()
				);
				sap.setVisibleSignature(rect, targetPage, null);

				if (fontProvider != null && fontProvider.getBaseFont() != null) {
					sap.setLayer2Font(new Font(fontProvider.getBaseFont(), 9, Font.NORMAL, Color.DARK_GRAY));
				}
			}

			// 5. 設定憑證與簽名加密演算法 (PKCS#7)
			sap.setCrypto(privateKey, chain, null, PdfSignatureAppearance.SELF_SIGNED);

			stamper.close();
			reader.close();

			byte[] signedPdf = baos.toByteArray();
			log.info("PDF 數位簽署成功完成: outputSize={} bytes", signedPdf.length);
			return signedPdf;
		} catch (IllegalArgumentException iae) {
			throw iae;
		} catch (Exception e) {
			log.error("PDF 數位簽署過程發生異常: {}", e.getMessage(), e);
			throw new RuntimeException("PDF 數位簽署失敗: " + e.getMessage(), e);
		} finally {
			closeSilently(stamper, reader);
		}
	}

	private void closeSilently(PdfStamper stamper, PdfReader reader) {
		if (stamper != null) {
			try {
				stamper.close();
			} catch (Exception ignored) {
			}
		}
		if (reader != null) {
			try {
				reader.close();
			} catch (Exception ignored) {
			}
		}
	}
}
