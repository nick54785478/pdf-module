package com.example.demo.application.shared.dto;

import java.util.Base64;

/**
 * PDF 抽取之圖片資料傳輸載體 (DTO)
 * <p>
 * 遵循六角形架構規範：
 * 1. 位於應用層 {@code application.shared.dto}。
 * 2. 嚴格作為純資料載體，封裝抽取的圖片二進位資料、圖片格式與頁碼等中繼資訊。
 * 3. 命名遵循 {@code N + Gotten + Result} 規範。
 * </p>
 *
 * @param data       圖片二進位資料 (byte[])
 * @param format     圖片格式 (如 "png", "jpg")
 * @param pageNumber 所屬頁碼 (從 1 開始)
 * @param imageName  圖片物件名稱 (如 "Im1", "image_p1_1.png")
 */
public record PdfImageGottenResult(
		byte[] data,
		String format,
		int pageNumber,
		String imageName
) {

	/**
	 * 取得圖片之 Base64 編碼字串
	 *
	 * @return Base64 字串，若無資料則為空字串
	 */
	public String toBase64() {
		return data != null && data.length > 0 ? Base64.getEncoder().encodeToString(data) : "";
	}

	/**
	 * 取得建議之檔案名稱 (包含副檔名)
	 *
	 * @return 檔案名稱 (如 "image_p1_1.png")
	 */
	public String getFileName() {
		String ext = (format != null && !format.isBlank()) ? format.toLowerCase() : "png";
		if (imageName != null && !imageName.isBlank()) {
			return imageName.contains(".") ? imageName : imageName + "." + ext;
		}
		return "image_p" + pageNumber + "." + ext;
	}
}
