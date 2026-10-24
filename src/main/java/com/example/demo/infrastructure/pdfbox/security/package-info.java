/**
 * Apache PDFBox 3.x 安全防護與數位簽章子系統
 * <p>
 * 遵循六角形架構規範，模組化劃分兩大核心體系：
 * 1. {@link com.example.demo.infrastructure.pdfbox.security.policy}：數位簽章規範與標準協定子套件 (Policy)。
 * 2. {@link com.example.demo.infrastructure.pdfbox.security.customizer}：視覺印章外觀客製化繪製子套件 (Customizer)。
 * 3. {@link com.example.demo.infrastructure.pdfbox.security.BouncyCastlePdfBoxSignatureInterface}：PDFBox 增量數位簽章介面實作。
 * </p>
 */
package com.example.demo.infrastructure.pdfbox.security;
