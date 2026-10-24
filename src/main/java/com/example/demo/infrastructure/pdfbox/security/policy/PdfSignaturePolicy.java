package com.example.demo.infrastructure.pdfbox.security.policy;

import com.example.demo.application.shared.security.PdfCertificationLevel;
import com.example.demo.application.shared.security.PdfSignatureSubFilter;

/**
 * PDF 數位簽章規範自定義介面 (Signature Policy Interface)
 * <p>
 * 提供企業自定義之簽章標準規範契約，包含：
 * 1. 簽章過濾器與子過濾器 (Filter & SubFilter - 如 Adobe PKCS#7 或 歐盟 PAdES/CAdES)
 * 2. 雜湊與非對稱加密演算法 (如 SHA256withRSA, SHA384withRSA)
 * 3. 認證等級 (Certification Level - 是否鎖定文件禁止後續變更)
 * 4. RFC 3161 時間戳伺服器 (TSA) 連線策略
 * </p>
 */
public interface PdfSignaturePolicy {

	/**
	 * 取得簽章過濾器名稱 (Filter)
	 *
	 * @return 簽章過濾器 (預設為 Adobe.PPKLite)
	 */
	default String getFilter() {
		return "Adobe.PPKLite";
	}

	/**
	 * 取得簽章子過濾器名稱 (SubFilter)
	 *
	 * @return 簽章標準規格 (如 adbe.pkcs7.detached 或 ETSI.CAdES.detached)
	 */
	default String getSubFilter() {
		return PdfSignatureSubFilter.ADBE_PKCS7_DETACHED.getSubFilterName();
	}

	/**
	 * 取得非對稱簽署演算法名稱
	 *
	 * @return 演算法名稱 (如 SHA256withRSA)
	 */
	default String getSignatureAlgorithm() {
		return "SHA256withRSA";
	}

	/**
	 * 取得文件簽署後之防竄改認證等級
	 *
	 * @return 認證層級
	 */
	default PdfCertificationLevel getCertificationLevel() {
		return PdfCertificationLevel.NOT_CERTIFIED;
	}

	/**
	 * 是否啟用 RFC 3161 時間戳權威機構 (TSA) 驗證
	 *
	 * @return true 代表啟用時間戳記
	 */
	default boolean isTimestampEnabled() {
		return false;
	}

	/**
	 * 取得時間戳權威伺服器 URL (若啟用時間戳)
	 *
	 * @return TSA 服務端點 URL
	 */
	default String getTimestampServerUrl() {
		return null;
	}
}
