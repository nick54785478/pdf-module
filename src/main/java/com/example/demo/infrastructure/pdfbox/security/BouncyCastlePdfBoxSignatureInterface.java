package com.example.demo.infrastructure.pdfbox.security;

import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * 基於 BouncyCastle 之 PDFBox 數位簽章介面實作 (SignatureInterface)
 * <p>
 * 遵循 PKCS#7 / CMS (Cryptographic Message Syntax) 規範，
 * 接收 PDFBox 串流計算雜湊並以 PrivateKey 進行簽署，封裝完整 X.509 憑證鏈。
 * </p>
 */
public class BouncyCastlePdfBoxSignatureInterface implements SignatureInterface {

	private final PrivateKey privateKey;
	private final Certificate[] certificateChain;
	private final String signatureAlgorithm;

	public BouncyCastlePdfBoxSignatureInterface(PrivateKey privateKey, Certificate[] certificateChain, String signatureAlgorithm) {
		this.privateKey = privateKey;
		this.certificateChain = certificateChain;
		this.signatureAlgorithm = (signatureAlgorithm != null && !signatureAlgorithm.isBlank()) ? signatureAlgorithm : "SHA256withRSA";
	}

	@Override
	public byte[] sign(InputStream content) throws IOException {
		try {
			List<Certificate> certList = Arrays.asList(certificateChain);
			JcaCertStore certStore = new JcaCertStore(certList);

			CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
			ContentSigner shaSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(privateKey);

			gen.addSignerInfoGenerator(
					new JcaSignerInfoGeneratorBuilder(
							new JcaDigestCalculatorProviderBuilder().build()
					).build(shaSigner, new X509CertificateHolder(certificateChain[0].getEncoded()))
			);
			gen.addCertificates(certStore);

			CMSProcessableInputStream msg = new CMSProcessableInputStream(content);
			CMSSignedData signedData = gen.generate(msg, false);
			return signedData.getEncoded();
		} catch (Exception e) {
			throw new IOException("PDFBox CMS/PKCS#7 數位簽署計算失敗: " + e.getMessage(), e);
		}
	}

	/**
	 * 將串流包裝為 CMSTypedData 供 CMS 簽章產生器直接消費
	 */
	private static class CMSProcessableInputStream implements CMSTypedData {
		private final InputStream in;
		private final ASN1ObjectIdentifier contentType;

		public CMSProcessableInputStream(InputStream in) {
			this(new ASN1ObjectIdentifier(CMSObjectIdentifiers.data.getId()), in);
		}

		public CMSProcessableInputStream(ASN1ObjectIdentifier type, InputStream in) {
			this.contentType = type;
			this.in = in;
		}

		@Override
		public ASN1ObjectIdentifier getContentType() {
			return contentType;
		}

		@Override
		public void write(OutputStream out) throws IOException, CMSException {
			in.transferTo(out);
		}

		@Override
		public Object getContent() {
			return in;
		}
	}
}
