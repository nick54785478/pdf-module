# 測試情境與驗證指南目錄導覽 (Test Documentation Index)

> 🔗 回到 [專案根目錄 README.md](file:///d:/桌面/ModuleWorkSpace/pdf-module/README.md) 或 [技術設計文檔導覽 (doc/design/README.md)](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/README.md)

本目錄 (`doc/test/`) 收納本模組各核心業務功能與安全機制的**測試情境、操作手冊與自動化測試指引**。

---

## 測試文件清單

| 文件名稱 | 主要內容 | 涵蓋技術與工具 |
| :--- | :--- | :--- |
| **[pdf_digital_signature_test_scenarios.md](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/test/pdf_digital_signature_test_scenarios.md)** | **PDF 數位簽章與安全防護完整測試情境指南**：包含憑證產製、Swagger UI / cURL 操作、BOM 範本 PAdES 規範自動套用、異常攔截與 Adobe Reader 本機驗證指南。 | Apache PDFBox 3.x, PKCS#12, Swagger UI, cURL, Adobe Reader, JUnit 5 |

---

## 快速測試指令

```powershell
# 執行全模組所有 76 項單元與整合測試
./mvnw.cmd test

# 僅執行數位簽章與安全加解密整合測試
./mvnw.cmd test -Dtest=PdfBoxSecurityExecutorAdapterTest
```
