/**
 * Apache PDFBox 3.x 數位簽章規範子套件 (Signature Policy Layer)
 * <p>
 * 遵循六角形架構與策略模式 (Strategy Pattern)，封裝各種國際標準與企業客製化之數位簽章規範：
 * 1. {@link com.example.demo.infrastructure.pdfbox.security.policy.PdfSignaturePolicy}：簽章規範抽象合約。
 * 2. {@link com.example.demo.infrastructure.pdfbox.security.policy.impl.DefaultPdfSignaturePolicy}：企業標準 Adobe PKCS#7 脫鉤簽名。
 * 3. {@link com.example.demo.infrastructure.pdfbox.security.policy.impl.PAdESCompliantSignaturePolicy}：歐盟 PAdES / CAdES 高級電子簽名 (DocMDP=2)。
 * 4. {@link com.example.demo.infrastructure.pdfbox.security.policy.impl.StrictArchivalSignaturePolicy}：嚴格法務存檔封存規範 (DocMDP=1 鎖定禁止任何變更)。
 * </p>
 */
package com.example.demo.infrastructure.pdfbox.security.policy;
