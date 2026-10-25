# PDF Module (pdf-module)

本專案為基於 **Spring Boot** 與 **Java 21** 開發的 PDF 模組，採用 **DDD (領域驅動設計)** 與 **Clean Architecture / 六角形架構 (Ports & Adapters)** 設計原則。

---

## 目錄導覽

- [專案簡介](#專案簡介)
- [核心技術堆疊](#核心技術堆疊)
- [架構規範與目錄結構](#架構規範與目錄結構)
  - [目錄結構樹狀圖](#目錄結構樹狀圖)
  - [分層職責說明](#分層職責說明)
- [技術設計與架構文檔導覽](#技術設計與架構文檔導覽)
  - [三大技術體系與專案文檔](#三大技術體系與專案文檔)
- [命名規範 (9 大類別)](#命名規範-9-大類別)
- [REST API 端點說明 (報價單生成與解析)](#rest-api-端點說明-報價單生成與解析)
  - [OpenAPI 3.0 與 Swagger UI 互動式文件](#openapi-30-與-swagger-ui-互動式文件)
- [開發與測試規範](#開發與測試規範)
- [建置與測試指令](#建置與測試指令)

---

## 專案簡介

`pdf-module` 定位為專門提供 PDF 處理能力之功能模組（包含 PDF 生成、操作、解析等），以高度內聚、低耦合的六角形架構隔離外部依賴，確保商業應用邏輯純淨且易於擴展。

---

## 核心技術堆疊

- **Java Version**：21
- **Framework**：Spring Boot 4.1.1 (包含 Spring WebMvc, Spring Validation)
- **Validation**：Jakarta Validation + Hibernate Validator
- **OpenAPI / API Docs**：SpringDoc OpenAPI 2.8.5 (Swagger UI 3.0)
- **PDF / Security Libraries**：
  - [OpenPDF](https://github.com/LibrePDF/OpenPDF) 2.0.4 (PDF 生成、蓋印浮水印、128-bit 權限加密、密碼驗證解鎖、PKCS#7 數位簽署)
  - [Apache PDFBox](https://pdfbox.apache.org/) 3.0.8 (二進位文字逆向抽取、256-bit AES 最高等級加密、安全限制解除、PKCS#7 增量數位簽署與自定義簽章規範體系)
  - [BouncyCastle](https://www.bouncycastle.org/) 1.80 (PKCS#12 金鑰庫解析、X.509 憑證鏈處理與密碼編譯演算法)
  - [Eclipse BIRT Runtime](https://eclipse.dev/birt/) 4.20.0 (視覺化報表設計範本渲染)
- **Persistence**：Spring Data JPA + H2 Database (in-memory)
- **Testing**：JUnit 5, Spring Boot Test, Mockito

---

## 架構規範與目錄結構

本模組嚴格遵循專案內的架構準則（參照 `.agents/AGENTS.md`）：

### 目錄結構樹狀圖

```text
src/main/java/com/example/demo/
├── PdfModuleApplication.java
│
├── domain/                                          # 領域層 (Domain Layer)
│   └── package-info.java                            # 純淨領域目錄，嚴禁依賴外部框架 (Spring/JPA/Lombok)
│
├── application/                                     # 應用層 (Application Layer)
│   ├── port/
│   │   ├── in/                                      # 輸入端口 (Inbound Ports / Use Cases，結尾為 UseCase)
│   │   │   ├── GenerateQuotationPdfUseCase.java     # 產出報價單 PDF 用例介面 (OpenPDF 程式化)
│   │   │   ├── GenerateTemplateQuotationPdfUseCase.java # 產出報價單 PDF 用例介面 (BIRT 範本驅動)
│   │   │   ├── ParseQuotationPdfUseCase.java        # 解析報價單 PDF 用例介面 (PDFBox 逆向解析)
│   │   │   ├── AddWatermarkUseCase.java             # 通用 PDF 加蓋浮水印用例介面
│   │   │   ├── EncryptPdfUseCase.java               # PDF 安全加密與權限控管用例介面
│   │   │   ├── DecryptPdfUseCase.java               # 受保護 PDF 密碼解密用例介面
│   │   │   ├── SignPdfUseCase.java                  # PDF PKCS#7 數位簽章用例介面
│   │   │   └── package-info.java
│   │   └── out/                                     # 輸出端口 (Outbound Ports，結尾為 Port)
│   │       ├── QuotationPdfGeneratorPort.java       # 報價單 PDF 產出業務輸出端口 (支援生成時單次印製浮水印)
│   │       ├── QuotationParserPort.java             # 報價單內容解析輸出端口
│   │       ├── MockServicePort.java                 # 報價單模擬資料提供輸出端口
│   │       ├── PdfExtractorPort.java                # PDF 抽取輸出端口 (基於 PDFBox)
│   │       ├── ReportGeneratorPort.java             # 報表生成輸出端口 (業務語意)
│   │       ├── PdfWatermarkExecutorPort.java        # PDF 通用浮水印蓋印輸出端口
│   │       ├── PdfSecurityExecutorPort.java         # PDF 安全加密、解密與數位簽章輸出端口
│   │       └── package-info.java
│   ├── service/                                     # 應用服務 (實作 UseCase 介面，協調流程)
│   │   └── QuotationApplicationService.java         # 報價單業務應用服務 (Inbound Adapter)
│   ├── shared/                                      # 跨業務共用資料模型與載體 (Shared Components)
│   │   ├── command/                                 # 業務命令物件 (V + N + Command)
│   │   │   ├── ParseQuotationPdfCommand.java        # 上傳並解析報價單 PDF 命令
│   │   │   ├── AddWatermarkCommand.java             # 上傳並加蓋浮水印 PDF 命令
│   │   │   ├── EncryptPdfCommand.java               # PDF 加密與權限設定命令 (全面採用 PDFBox 256-bit AES)
│   │   │   ├── DecryptPdfCommand.java               # 受保護 PDF 解密命令 (全面採用 PDFBox)
│   │   │   ├── SignPdfCommand.java                  # PKCS#7 數位簽章命令 (支援指定範本 templateName/認證等級/子過濾器)
│   │   │   └── package-info.java
│   │   ├── query/                                   # 業務查詢物件 (V + N + Query)
│   │   │   ├── GetQuotationQuery.java               # 取得報價單查詢物件 (包含 ID 與浮水印)
│   │   │   └── package-info.java
│   │   ├── security/                                # 安全與數位簽章共用領域枚舉 (純 Java)
│   │   │   ├── PdfCertificationLevel.java           # 簽章認證與修改許可等級 (DocMDP 0~3)
│   │   │   └── PdfSignatureSubFilter.java           # PKCS#7 / PAdES 子過濾器枚舉
│   │   ├── dto/                                     # 查詢資料傳輸物件 (純資料載體，禁止包含轉換邏輯)
│   │   │   ├── QuotationGottenResult.java           # 報價單完整資訊資料載體
│   │   │   ├── QuotationItemGottenResult.java       # 報價單物料明細項目載體
│   │   │   ├── PdfTextPositionGottenResult.java     # 文字座標查詢結果 DTO
│   │   │   ├── PdfColumnRect.java                   # 表格欄位區域定義 DTO
│   │   │   └── package-info.java
│   │   └── package-info.java
│   ├── view/                                        # 查詢投影物件 (純資料載體，提供唯讀投影)
│   └── assembler/                                   # 應用層組裝器 (負責領域物件與 DTO/View/報表參數互轉)
│       ├── QuotationReportAssembler.java            # BIRT 報表參數與腳本資料集防腐組裝器
│       └── package-info.java
│
├── infrastructure/                                  # 基礎設施層 (Infrastructure Layer - Outbound Adapters)
│   ├── persistence/
│   │   ├── entity/                                  # ORM 資料庫實體 (*JpaEntity)
│   │   └── repository/                              # Spring Data JPA 介面 (*JpaRepository)
│   ├── messaging/                                   # 訊息佇列介接模組 (*MqProducer, *MqConsumer)
│   ├── openpdf/                                     # OpenPDF 報表與文件範本生成子系統
│   │   ├── core/                                    # 【通用排版積木】
│   │   │   ├── OpenPdfFontProvider.java             # 字體單例快取 (MSJHL.TTC) 與階層樣式提供者
│   │   │   ├── OpenPdfStylePalette.java             # 企業排版調色盤與視覺幾何常數
│   │   │   ├── OpenPdfCellFactory.java              # 儲存格與行排版工廠
│   │   │   ├── OpenPdfWatermarkStyle.java           # 通用浮水印視覺樣式配置值物件 (提供通用預設樣式與 Builder)
│   │   │   └── OpenPdfPageEventHelper.java          # 頁首頁尾生命週期鈎子、雙階段總頁數填寫與排版度量事件基底
│   │   └── template/                                # 【業務範本層】
│   │       ├── PdfDocumentTemplate.java             # PDF 業務範本通用抽象契約 (提供簽章規範與浮水印 Hook)
│   │       └── impl/                                # 【具體範本實作】
│   │           └── BomQuotationPdfTemplate.java     # 物料清單 (BOM) 報價單專屬排版實作 (定義專屬頁碼、浮水印與簽章規範)
│   ├── pdfbox/                                      # Apache PDFBox 3.x 安全與簽章子系統
│   │   └── security/                                # 安全與數位簽章子系統
│   │       ├── policy/                              # 【自定義數位簽章規範體系 (Policy)】
│   │       │   ├── PdfSignaturePolicy.java          # 數位簽章規範抽象契約介面
│   │       │   ├── DefaultPdfSignaturePolicy.java   # @Primary 企業預設標準簽章規範 (Adobe.PPKLite)
│   │       │   ├── PAdESCompliantSignaturePolicy.java # 歐盟 PAdES / CAdES 高級電子簽章規範 (DocMDP=2)
│   │       │   └── StrictArchivalSignaturePolicy.java # 嚴格法務存檔封存規範 (DocMDP=1 禁止任何變更)
│   │       ├── customizer/                          # 【視覺印章外觀客製化繪製體系 (Customizer)】
│   │       │   ├── PdfVisualSignatureCustomizer.java # 自定義視覺印章繪製外觀介面
│   │       │   ├── DefaultPdfBoxVisualSignatureCustomizer.java # @Primary 微軟正黑體雙線防偽鋼印客製化器
│   │       │   └── BomQuotationVisualSignatureCustomizer.java # BOM 報價單專屬硃砂紅/金銅原物料放行鋼印
│   │       └── BouncyCastlePdfBoxSignatureInterface.java # PDFBox 增量 PKCS#7 簽名介面實作
│   └── adapter/                                     # 出境適配器 (實作 Application 層 Outbound Port)
│       ├── OpenQuotationPdfGeneratorAdapter.java    # 實作 QuotationPdfGeneratorPort 之 OpenPDF 適配器
│       ├── TextQuotationParserAdapter.java          # 實作 QuotationParserPort 之文字與語法剖析適配器
│       ├── MockServiceAdapter.java                  # 實作 MockServicePort 之模擬範本資料適配器
│       ├── PdfExtractorAdapter.java                 # 實作 PdfExtractorPort 之 Apache PDFBox 適配器
│       ├── BirtReportAdapter.java                   # 實作 ReportGeneratorPort 之 Eclipse BIRT 適配器
│       ├── OpenPdfWatermarkExecutorAdapter.java     # 實作 PdfWatermarkExecutorPort 之通用浮水印蓋印適配器
│       ├── OpenPdfSecurityExecutorAdapter.java      # 實作 PdfSecurityExecutorPort 之 OpenPDF 安全適配器
│       ├── PdfBoxSecurityExecutorAdapter.java       # @Primary 實作 PdfSecurityExecutorPort (全面接納 PDFBox 核心引擎)
│       └── package-info.java
│
├── presentation/                                    # 表現層 (Presentation Layer - Inbound Adapters)
│   ├── controller/                                  # REST Controller API 介面 (*Controller)
│   │   ├── QuotationController.java                 # 報價單 PDF 生成、加解密與簽章控制器 (支援 template 參數)
│   │   └── package-info.java
│   ├── exception/                                   # 表現層例外處理 (*ExceptionHandler)
│   │   └── GlobalExceptionHandler.java              # 表現層全域例外處理器 (@RestControllerAdvice)
│   ├── resource/
│   │   ├── in/                                      # 請求資料載體 (V + N + Resource，純資料載體，支援 Jakarta Validation)
│   │   │   ├── GetQuotationPdfResource.java         # 取得報價單 PDF 請求資料載體 (包含 @Size, @Pattern 檢核)
│   │   │   ├── GenerateQuotationPdfResource.java    # 產出報價單 PDF 請求資料載體 (包含 @NotBlank, @Valid 檢核)
│   │   │   └── QuotationItemResource.java           # 報價單明細項目資料載體 (包含 @Positive, @NotNull 檢核)
│   │   └── out/                                     # 回應資料載體 (N + Ved + Resource，純資料載體)
│   │       ├── QuotationParsedResource.java         # 報價單 PDF 解析回傳結構化資料載體
│   │       ├── QuotationItemParsedResource.java     # 報價單明細項目解析回傳載體
│   │       ├── ErrorOccurredResource.java           # 全域錯誤回應資料載體 (HTTP Status, 錯誤訊息, 欄位清單)
│   │       └── package-info.java
│   └── assembler/                                   # 表現層防腐組裝器 (*ResourceAssembler)
│       ├── QuotationResourceAssembler.java          # 報價單 Resource <-> DTO / Query 防腐層組裝器
│       └── package-info.java
│
└── config/                                          # 配置層 (Configuration Layer - The Dirtiest Layer)
    ├── OpenApiConfig.java                           # OpenAPI 3.0 與 Swagger UI 全域元資料配置 (@Configuration)
    ├── PdfFontProperties.java                       # PDF 字體外部化配置屬性類 (@ConfigurationProperties(prefix = "pdf.font"))
    └── package-info.java                            # Spring @Configuration、Bean 裝配與全域設定

doc/
├── design/                                          # 技術設計與架構規範 (依技術分目錄)
│   ├── README.md                                    # 技術設計文檔總覽導覽
│   ├── openpdf/                                     # 【OpenPDF 技術體系】
│   │   ├── openpdf_architecture_design.md           # OpenPDF 報表生成整體架構設計
│   │   ├── custom_openpdf_template_guide.md         # 自定義業務單據範本開發實戰指南 (6 步驟)
│   │   ├── openpdf_anti_orphan_pagination_guide.md  # 動態資料量智慧防孤兒區塊排版技術指南
│   │   └── openpdf_saas_evolution_roadmap.md        # OpenPDF SaaS 多租戶架構演進與擴充規劃技術手冊
│   ├── birt/                                        # 【Eclipse BIRT 技術體系】
│   │   └── birt_pdf_module_operations_guide.md      # Eclipse BIRT 報表架構與維運排障手冊
│   └── pdfbox/                                      # 【Apache PDFBox 技術體系】
│       ├── pdf_parser_development_guide.md          # PDF 結構化文字抽取與狀態機剖析開發指南
│       ├── pdfbox_security_signature_design.md      # PDFBox 3.x 256-bit 安全防護、數位簽章與範本自定義規範體系設計文檔
│       └── pdf_digital_signature_workflow_and_secret_guide.md # PDF 數位簽章完整流程與憑證密鑰管理實戰指南 (含 ByteRange 與 Secret 取得)
├── operations/                                      # 維運與生產部署手冊
│   └── birt_pdf_module_operations_guide.md          # 維運人員專屬操作手冊
└── test/                                            # 測試情境與驗證手冊
    ├── README.md                                    # 測試文件總覽導覽
    └── pdf_digital_signature_test_scenarios.md      # PDF 數位簽章與安全防護完整測試情境指南 (Swagger/cURL/Adobe驗證)

src/main/resources/
├── application.properties                           # 應用程式配置檔 (包含字符編碼、OpenAPI 路徑、pdf.font.* 字型路徑)
├── fontsConfig.xml                                  # BIRT 報表字體配置檔
├── fonts/
│   └── MSJHL.TTC                                    # 微軟正黑體 Light (微軟正黑 TrueType Collection 字體)
├── templates/
│   └── bom_quotation_template.rptdesign             # Eclipse BIRT 報價單報表設計範本
└── example/
    └── bom_quotation_template.pdf                   # 物料清單 (BOM) 報價單 PDF 範本

src/test/java/com/example/demo/
├── application/
│   └── service/
│       └── QuotationApplicationServiceTest.java     # 報價單 UseCase BDD 單元測試
├── config/
│   ├── OpenApiConfigTest.java                       # OpenAPI 全域配置 BDD 單元測試
│   └── PdfFontPropertiesTest.java                   # PDF 字型配置屬性 BDD 單元與整合測試
├── infrastructure/
│   └── adapter/
│       ├── OpenPdfQuotationAdapterTest.java         # OpenPDF 報價單適配器 BDD 整合測試
│       ├── TextQuotationParserAdapterTest.java      # 報價單文字剖析適配器 BDD 單元測試
│       ├── MockServiceAdapterTest.java              # MockService 適配器 BDD 單元測試
│       ├── PdfBoxAdapterTest.java                   # PDFBox 適配器 BDD 單元測試
│       ├── BirtReportAdapterTest.java               # BIRT 報表適配器 BDD 單元測試
│       ├── OpenPdfWatermarkAdapterTest.java         # OpenPDF 浮水印適配器 BDD 整合測試
│       ├── OpenPdfSecurityExecutorAdapterTest.java  # OpenPDF 128-bit 加解密與簽章適配器 BDD 測試
│       └── PdfBoxSecurityExecutorAdapterTest.java   # PDFBox 256-bit AES、範本自定義簽章規範與鋼印印製 BDD 測試
└── presentation/
    └── controller/
        ├── QuotationControllerTest.java             # 報價單 API 控制器 BDD 單元測試 (含單據範本簽章驗證)
        └── GlobalExceptionHandlerTest.java          # 全域例外處理器 BDD 單元測試

```

### 分層職責說明

1. **領域層 (`domain/`)**
   - 純 Java 實作，**嚴格禁止**引入 Spring、JPA、Hibernate、Lombok 等技術依賴，確保核心業務邏輯的技術無關性。
2. **應用層 (`application/`)**
   - 負責業務流程編排、Use Case 調度與資料傳遞。
   - **Port 原則**：所有 `port/in/` 與 `port/out/` 介面的傳參及回傳型別，**嚴禁**使用技術類或環境相依物件（如 ORM Entity、HttpServletRequest、ByteArrayResource）。
   - **純資料載體**：`shared/dto/` 與 `view/` 僅為資料傳遞，轉換邏輯由 `assembler/`（如 `*DtoAssembler`）獨立負責。
3. **基礎設施層 (`infrastructure/`)**
   - 包含資料庫存取 (`persistence/`)、訊息傳遞 (`messaging/`) 與適配器 (`adapter/`)。
   - `adapter/` 作為所有外部介接的匯聚點，實作 Application 層的 Outbound Port（如 `PdfExtractorAdapter`、`BirtReportAdapter`、`TextQuotationParserAdapter`、`OpenQuotationPdfGeneratorAdapter`）。
4. **表現層 (`presentation/`)**
   - REST Controller 負責接收請求並轉換為 Command/Query。
   - `resource/in/` 與 `resource/out/` 嚴格作為純資料載體，不寫 `toCommand` 或 `fromResult`；由 `assembler/`（如 `*ResourceAssembler`）擔任防腐層轉換。
5. **配置層 (`config/`)**
   - 集中管理全域技術配置（如 Spring Configuration、Security、Web 設定等），嚴禁將配置類散落於 Domain 或 Application 層中。

---

## 技術設計與架構文檔導覽

本專案提供高度完整的架構分析、範本擴展實戰、數位簽章原理與測試手冊，詳情請參閱 [技術設計文檔導覽 (doc/design/README.md)](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/README.md) 與 [測試情境總覽 (doc/test/README.md)](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/test/README.md)。

### 核心技術體系與專案文檔

1. **【OpenPDF 體系】程式化像素級高精確排版**
   - 🏛️ [OpenPDF 報表與文件範本生成整體架構設計](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/openpdf/openpdf_architecture_design.md)：說明 Clean Architecture 六角分層、五項通用排版積木 (`FontProvider`, `StylePalette`, `CellFactory`, `PageEventHelper`, `OpenPdfWatermarkStyle`)、**浮水印雙軌協同架構 (Dual-Track)** 與 **BOM 報價單專屬尊榮防偽雙線鋼印浮水印**。
   - 🛠️ [自定義業務單據範本開發實戰指南 (6 步驟)](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/openpdf/custom_openpdf_template_guide.md)：提供開發全新單據（出貨單、採購單等）之標準 6-Step 流程、排版積木套用範例與範本專屬浮水印樣式 (`getWatermarkStyle()`) 覆寫。
   - 📐 [動態資料量智慧防孤兒區塊排版技術指南](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/openpdf/openpdf_anti_orphan_pagination_guide.md)：解析雙層防孤兒排版機制（結尾複合區塊防拆原子化 + 錨點明細牽引換頁演算法），保證任意資料量絕不出現孤兒簽章頁。
   - 🚀 [OpenPDF SaaS 多租戶架構演進與擴充規劃技術手冊](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/openpdf/openpdf_saas_evolution_roadmap.md)：詳述六大 SaaS 演進維度（多租戶品牌主題、雲端字型池與雙層快取、宣告式 JSON 排版 DSL、非同步 Worker 佇列、動態 DLP 浮水印與 Cloud KMS 憑證安全、使用量計量與訂閱配額）與 5 階段落地藍圖。

2. **【Eclipse BIRT 體系】視覺化範本驅動報表引擎**
   - 📊 [Eclipse BIRT 報表架構、範本維護與維運排障手冊](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/birt/birt_pdf_module_operations_guide.md)：說明 Spring 單例託管 BIRT 引擎生命週期、`.rptdesign` 報表參數與腳本資料集、容器化字體配置，以及六大常見故障排障 SOP。

3. **【Apache PDFBox 體系】逆向文字抽取、256-bit 安全防護與數位簽章**
   - 🔍 [PDF 結構化文字抽取與狀態機剖析開發指南](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/pdfbox/pdf_parser_development_guide.md)：說明兩階段管線化剖析架構（`PdfExtractorPort` 全文幾何抽取 $\rightarrow$ `QuotationParserPort` 正則狀態機剖析），包含千分位去噪與防禦性容錯。
   - 🛡️ [PDFBox 3.x 256-bit 安全防護、數位簽章與範本自定義規範體系設計文檔](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/pdfbox/pdfbox_security_signature_design.md)：說明全面接納 PDFBox 核心引擎、單據範本感知與個別簽章定義 (`PdfDocumentTemplate` Hook)、平滑降級機制，以及自定義簽章規範體系與視覺印章繪製器。
   - 🔐 [PDF 數位簽章完整流程與憑證密鑰管理實戰指南](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/pdfbox/pdf_digital_signature_workflow_and_secret_guide.md)：深入剖析密碼學 ByteRange 挖洞計算原理、CMS/PKCS#7 簽署核心、本機 keytool 自簽產製與企業級 AATL / HSM Secret 管理架構，並提供 Quotation 端到端實例。

4. **【測試情境與驗證指南】**
   - 🧪 [PDF 數位簽章與安全防護完整測試情境指南 (doc/test/pdf_digital_signature_test_scenarios.md)](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/test/pdf_digital_signature_test_scenarios.md)：涵蓋 TC-01 ~ TC-09 測試矩陣、Swagger UI 互動式簽章、cURL 腳本自動化、異常邊界防禦與 Adobe Reader 本機信任清單匯入 SOP。

5. **【維運與生產部署】**
   - 🚀 [維運人員專屬操作手冊 (doc/operations/birt_pdf_module_operations_guide.md)](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/operations/birt_pdf_module_operations_guide.md)：提供生產環境佈署、中文字體安裝與系統維運之快速檢核指南。

---

## 命名規範 (9 大類別)

系統內各類別物件必須嚴格遵守以下命名規範：

| 類別 | 命名規範 | 範例 |
| :--- | :--- | :--- |
| **1. Request** | `V + N + Resource` | `GetQuotationPdfResource`, `GenerateQuotationPdfResource` |
| **2. Response** | `N + Ved + Resource` | `QuotationParsedResource`, `QuotationItemParsedResource`, `ErrorOccurredResource` |
| **3. Event** | `N + Ved + Event` | `PdfGeneratedEvent`, `PdfProcessedEvent` |
| **4. Command** | `V + N + Command` | `ParseQuotationPdfCommand`, `AddWatermarkCommand`, `EncryptPdfCommand`, `DecryptPdfCommand`, `SignPdfCommand` |
| **5. Query** | `V + N + Query` | `GetQuotationQuery`, `GetPdfQuery` |
| **6. Dto** | `N + Gotten/Searched + Result` | `QuotationGottenResult`, `QuotationItemGottenResult`, `PdfTextPositionGottenResult` |
| **7. Projection** | `N + Gotten/Searched + View` | `PdfGottenView`, `PdfSearchedView` |
| **8. Port** | In: `... + UseCase`<br>Out: `... + Port` | In: `GenerateQuotationPdfUseCase`, `GenerateTemplateQuotationPdfUseCase`, `ParseQuotationPdfUseCase`, `AddWatermarkUseCase`, `EncryptPdfUseCase`, `DecryptPdfUseCase`, `SignPdfUseCase`<br>Out: `QuotationPdfGeneratorPort`, `ReportGeneratorPort`, `QuotationParserPort`, `PdfExtractorPort`, `PdfWatermarkExecutorPort`, `PdfSecurityExecutorPort`, `MockServicePort` |
| **9. Adapter** | In: `... + ApplicationService`<br>Out: `... + Adapter` | In: `QuotationApplicationService`<br>Out: `OpenQuotationPdfGeneratorAdapter`, `BirtReportAdapter`, `TextQuotationParserAdapter`, `PdfExtractorAdapter`, `OpenPdfWatermarkExecutorAdapter`, `PdfBoxSecurityExecutorAdapter`, `OpenPdfSecurityExecutorAdapter`, `MockServiceAdapter` |

---

## Apache PDFBox 3.x 安全防護與單據範本自定義簽章規範體系

本模組全面接納高度自定義的 **Apache PDFBox 3.x** 作為安全加解密與數位簽章之核心專屬引擎，並結合六角形架構輸出端口 `PdfSecurityExecutorPort`，提供可高度客製化的 **單據範本自定義簽章規範體系 (`PdfSignaturePolicy`)** 與 **視覺印章客製化介面 (`PdfVisualSignatureCustomizer`)**：

### 1. 核心安全引擎特色 (Apache PDFBox 3.x)
- **256-bit AES 最高等級安全加密**：支援軍規級 AES-256 對稱加密演算法與細粒度 8 項操作權限控管（列印、文字複製、表單填寫、組裝等）。
- **無損密碼解密與限制解除**：驗證正確擁有者密碼後，無損解除安全限制，還原為乾淨標準 PDF。
- **不可否認性增量更新數位簽章 (`doc.saveIncremental`)**：嚴格遵循 RFC 5652 / PKCS#7 規範，以 ByteRange 挖洞機制保留原檔二進位雜湊完整性，於文件尾端追加簽章。

### 2. 單據範本感知與個別簽章定義 (`PdfDocumentTemplate` Hook)
- **具體單據範本專屬規範**：單據範本（如 `BomQuotationPdfTemplate`）可覆寫 `getSignaturePolicy()`、`getVisualSignatureCustomizer()`、`getDefaultSignatureReason()`、`getDefaultSignatureLocation()`，量身訂製該業務專屬的認證等級與視覺鋼印。
  - **BOM 報價單範本 (`bom`)**：自動綁定歐盟 `PAdESCompliantSignaturePolicy` (ETSI.CAdES.detached / DocMDP=2)、預設理由「BOM 原物料報價核准簽署」、地點「台北營運總部」，並加蓋晨星智造硃砂紅/金銅雙重防偽鋼印。
- **智慧平滑降級 (Fallback to Default Policy)**：呼叫端未指定單據範本（`template = null`）或指定之範本未覆寫規範時，系統自動無縫套用 `@Primary` 系統預設標準規範（`DefaultPdfSignaturePolicy` 與 `DefaultPdfBoxVisualSignatureCustomizer`），兼具高度業務擴展彈性與系統強健性。

### 3. 字型路徑配置外部化 (DDD & Hexagonal Architecture)
- 遵循架構規範：字型技術細節由配置層 `config/` 中的 `PdfFontProperties` 集中管理，外部化於 `application.properties`：
  ```properties
  pdf.font.path=fonts/MSJHL.TTC
  pdf.font.dev-fallback-path=src/main/resources/fonts/MSJHL.TTC
  pdf.font.system-fallback-path=C:/Windows/Fonts/msjh.ttc
  ```
- 基礎設施層（`OpenPdfFontProvider`、`DefaultPdfBoxVisualSignatureCustomizer`、`BomQuotationVisualSignatureCustomizer`）透過依賴注入取得字型配置，並實現多層階層式容錯載入，嚴格保證領域層與應用層 100% 純淨無技術污染。

---

## REST API 端點說明 (報價單生成、安全加解密、數位簽章與解析)

本模組提供程式化生成、範本驅動生成、結構化上傳解析、通用防偽浮水印蓋印，以及全面由 Apache PDFBox 3.x 驅動的企業級安全加解密與數位簽章能力：

| HTTP 方法 | URL 路徑 | 請求參數 / 請求體 | 功能說明 | 底層引擎 | 回傳格式 | 關鍵回應標頭 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/quotations/openpdf` | `id` (可選)<br>`watermark` (可選)<br>`download` (可選，預設 `true`) | **程式化高精確排版**：使用 OpenPDF 產生雙層防孤兒排版之報價單 PDF，支援生成時單次印製專屬防偽雙線鋼印浮水印 | `OpenPDF 2.0.4` | `application/pdf` | `X-Pdf-Engine: OpenPDF`<br>`Content-Disposition: attachment; filename="{id}_openpdf.pdf"` |
| **GET** | `/api/v1/quotations/template`<br>*(同義別名 `/birt`)* | `id` (可選)<br>`watermark` (可選)<br>`download` (可選，預設 `true`) | **視覺化範本驅動排版**：使用 Eclipse BIRT 報表設計範本 (`.rptdesign`) 渲染標準報價單 PDF，支援後製通用動態防偽浮水印 | `Eclipse BIRT 4.20.0` | `application/pdf` | `X-Pdf-Engine: BIRT (Template)`<br>`Content-Disposition: attachment; filename="{id}_template.pdf"` |
| **POST** | `/api/v1/quotations/watermark` | `multipart/form-data`<br>`file`: PDF 檔案 (二進位)<br>`watermark`: 浮水印文字 (必填)<br>`download` (可選，預設 `true`) | **通用 PDF 防偽浮水印蓋印**：上傳任意 PDF 檔案，由 OpenPdfWatermarkExecutorAdapter 在每一頁中心疊加 45 度傾斜半透明防偽字樣 (微軟正黑體) | `OpenPDF 2.0.4` | `application/pdf` | `X-Pdf-Engine: OpenPDF (Watermark)`<br>`Content-Disposition: attachment; filename="{base}_watermarked.pdf"` |
| **POST** | `/api/v1/quotations/encrypt` | `multipart/form-data`<br>`file`: PDF 檔案 (二進位)<br>`userPassword`: 開啟密碼 (可選)<br>`ownerPassword`: 管理密碼 (可選)<br>`allowPrinting`: 允許列印 (預設 `true`)<br>`allowCopy`: 允許複製 (預設 `true`)<br>`download` (可選，預設 `true`) | **PDF 安全加密與權限控管**：採用 PDFBox 256-bit AES 軍規級加密保護，精細控制檢視、列印與文字複製權限 | `Apache PDFBox 3.0.8` | `application/pdf` | `X-Pdf-Engine: PDFBox (Security)`<br>`Content-Disposition: attachment; filename="{base}_encrypted.pdf"` |
| **POST** | `/api/v1/quotations/decrypt` | `multipart/form-data`<br>`file`: 加密 PDF 檔案 (二進位)<br>`password`: 解密密碼 (可選)<br>`download` (可選，預設 `true`) | **PDF 密碼驗證解密與限制解除**：上傳受密碼保護之 PDF 檔案，驗證密碼後解除密碼保護與操作權限限制，回傳乾淨之標準 PDF | `Apache PDFBox 3.0.8` | `application/pdf` | `X-Pdf-Engine: PDFBox (Security)`<br>`Content-Disposition: attachment; filename="{base}_decrypted.pdf"` |
| **POST** | `/api/v1/quotations/sign` | `multipart/form-data`<br>`file`: PDF 檔案 (二進位)<br>`keystore`: PKCS#12 (.p12/.pfx) 金鑰庫<br>`password`: 金鑰庫密碼 (必填)<br>`alias`: 密鑰別名 (可選)<br>`reason`: 簽章理由 (可選，優先套用範本預設)<br>`location`: 簽章地點 (可選，優先套用範本預設)<br>`contact`: 聯絡資訊<br>`visual`: 是否印製視覺印章 (預設 `true`)<br>`page`: 簽章頁碼 (預設 `1`)<br>`template`: 指定套用範本 (如 `bom`，可選)<br>`download` (可選，預設 `true`) | **PKCS#7 / PAdES 數位簽章與防偽印章**：支援 PDFBox 增量數位簽章 (`doc.saveIncremental`)、單據範本感知專屬簽章規範與視覺印章，搭配繁體中文雙線防偽鋼印視覺外觀 | `Apache PDFBox 3.0.8` + `BouncyCastle 1.80` | `application/pdf` | `X-Pdf-Engine: PDFBox (Digital Signature)`<br>`Content-Disposition: attachment; filename="{base}_signed.pdf"` |
| **POST** | `/api/v1/quotations/parse` | `multipart/form-data`<br>`file`: PDF 檔案 (二進位) | **PDF 結構化逆向解析**：上傳 PDF，抽取純文字並透過正則狀態機剖析為結構化報價單 DTO | `Apache PDFBox 3.0.8` | `application/json` | `Content-Type: application/json`<br>(回傳 `QuotationParsedResource`) |

> 📌 **回應標頭與下載機制說明**：
> - `X-Pdf-Engine`：清楚標示底層渲染引擎（`PDFBox (Security)`、`PDFBox (Digital Signature)`、`OpenPDF`、`OpenPDF (Watermark)`、`BIRT (Template)`）。
> - `Content-Disposition: attachment; filename="..."`：預設以附件方式回傳，Swagger UI 會自動呈現「Download file」按鈕以供下載檢視。若傳入 `download=false` 則以 `inline` 模式回傳，可在瀏覽器分頁直接預覽。
> - **防腐層 (ACL) 設計原則**：表現層接收 Spring `MultipartFile` 後，一律透過 `QuotationResourceAssembler` 封裝為技術無關的純資料 Command 物件（如 `AddWatermarkCommand`, `EncryptPdfCommand`, `DecryptPdfCommand`, `SignPdfCommand`），絕不將框架物件傳入 Application Port。


### OpenAPI 3.0 與 Swagger UI 互動式文件

本專案引入 `springdoc-openapi-starter-webmvc-ui` (2.8.5)，啟動本機應用程式後即可存取互動式 API 文檔：

- **Swagger UI 視覺化介面**：`http://localhost:8080/swagger-ui.html`
- **OpenAPI 3.0 JSON 規格**：`http://localhost:8080/v3/api-docs`

#### 控制器與資料載體標註規範
- **`@Tag`**：標註於 Controller，分類與描述 API 領域功能模組。
- **`@Operation`**：標註於端點方法，詳細說明端點摘要、業務調度與處理流程。
- **`@Parameter`**：標註於請求參數，提供參數意義、校驗約束與示範數值。
- **`@ApiResponses` / `@ApiResponse`**：明確定義 HTTP 200 (PDF 二進位串流)、HTTP 400 (參數檢核失敗) 與 HTTP 500 (伺服器錯誤) 之 Schema。
- **`@Schema`**：標註於 Request/Response Resource 物件與欄位，提供完整的資料結構元數據與範例資料。

---

## 開發與測試規範

1. **清晰的 Javadoc 註解**：
   - 所有介面、類別、實體、DTO、Port 與 Adapter 皆需撰寫清晰 Javadoc，載明架構角色、職責、參數與例外情境。
2. **UseCase 測試強制要求**：
   - 每個 UseCase 均須具備對應的單元或整合測試，測試正常流程、邊界條件與異常分支。
3. **BDD 與 Gherkin 語法**：
   - 每個測試方法上方必須使用 Gherkin 結構撰寫 Javadoc 說明情境：
     ```java
     /**
      * Feature: PDF 產出功能
      * Scenario: 成功依照範本生成 PDF 檔案
      * Given: 提供正確的範本路徑與填入參數
      * When: 執行 GeneratePdfUseCase.execute(command)
      * Then: 成功產生對應的位元組陣列並回傳 PdfGottenResult
      */
     ```
4. **乾淨的 Import 規範**：
   - 程式碼中採用明確的單一 Class Import，避免使用行內全路徑類別名稱 (Fully Qualified Class Names)。

---

## 建置與測試指令

使用專案內建的 Maven Wrapper 執行建置與測試：

```bash
# 執行編譯與測試
./mvnw.cmd test

# 打包專案
./mvnw.cmd clean package

# 本地啟動 Spring Boot 應用程式
./mvnw.cmd spring-boot:run
```
