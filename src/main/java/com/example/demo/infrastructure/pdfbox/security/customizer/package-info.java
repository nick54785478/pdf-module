/**
 * Apache PDFBox 3.x 數位簽章視覺印章客製化子套件 (Visual Signature Customizer Layer)
 * <p>
 * 遵循六角形架構與策略模式 (Strategy Pattern)，負責繪製各種企業外觀樣式之防偽視覺電子鋼印：
 * 1. {@link com.example.demo.infrastructure.pdfbox.security.customizer.PdfVisualSignatureCustomizer}：視覺印章繪製抽象合約。
 * 2. {@link com.example.demo.infrastructure.pdfbox.security.customizer.impl.DefaultPdfBoxVisualSignatureCustomizer}：預設通用微軟正黑體雙線防偽鋼印。
 * 3. {@link com.example.demo.infrastructure.pdfbox.security.customizer.impl.BomQuotationVisualSignatureCustomizer}：BOM 原物料報價單專屬尊榮硃砂朱紅/金銅防偽鋼印。
 * </p>
 */
package com.example.demo.infrastructure.pdfbox.security.customizer;
