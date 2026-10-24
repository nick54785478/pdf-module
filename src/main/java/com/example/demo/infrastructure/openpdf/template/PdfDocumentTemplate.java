package com.example.demo.infrastructure.openpdf.template;

import java.io.OutputStream;

/**
 * PDF 業務範本通用抽象契約介面 (Template Contract)
 * <p>
 * 定義具體業務單據範本（如物料清單報價單、出貨單、工單憑證等）統一的排版渲染合約，
 * 規範將強型別業務 DTO 資料排版並寫入目標輸出串流的標準行為。
 * </p>
 *
 * @param <T> 業務資料物件型別 (DTO)
 */
public interface PdfDocumentTemplate<T> {

	/**
	 * 執行版面排版並將產出的 PDF 文件寫入指定輸出串流
	 *
	 * @param data         業務資料物件
	 * @param outputStream PDF 二進位輸出串流
	 * @throws Exception 當排版、資料格式化或串流寫入過程發生例外時拋出
	 */
	void render(T data, OutputStream outputStream) throws Exception;
}
