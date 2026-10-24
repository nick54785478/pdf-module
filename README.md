# PDF Module (pdf-module)

本專案為基於 **Spring Boot** 與 **Java 21** 開發的 PDF 模組，採用 **DDD (領域驅動設計)**、**Clean Architecture / 六角形架構 (Ports & Adapters)** 與 **CQRS** 設計原則。

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
- **PDF / Reporting Libraries**：
  - [OpenPDF](https://github.com/LibrePDF/OpenPDF) 3.0.5
  - [Apache PDFBox](https://pdfbox.apache.org/) 3.0.8
  - [Eclipse BIRT Runtime](https://eclipse.dev/birt/) 4.20.0
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
│   │   │   └── package-info.java
│   │   └── out/                                     # 輸出端口 (Outbound Ports，結尾為 Port)
│   │       ├── QuotationPdfGeneratorPort.java       # 報價單 PDF 產出業務輸出端口
│   │       ├── QuotationParserPort.java             # 報價單內容解析輸出端口
│   │       ├── MockServicePort.java                 # 報價單模擬資料提供輸出端口
│   │       ├── PdfExtractorPort.java                # PDF 抽取輸出端口 (基於 PDFBox)
│   │       ├── ReportGeneratorPort.java             # 報表生成輸出端口 (業務語意)
│   │       └── package-info.java
│   ├── service/                                     # 應用服務 (實作 UseCase 介面，協調流程)
│   │   └── QuotationApplicationService.java         # 報價單業務應用服務 (Inbound Adapter)
│   ├── shared/                                      # 跨業務共用資料模型與載體 (Shared Components)
│   │   ├── command/                                 # CQRS 命令物件 (V + N + Command)
│   │   │   ├── ParseQuotationPdfCommand.java        # 上傳並解析報價單 PDF 命令
│   │   │   └── package-info.java
│   │   ├── query/                                   # CQRS 查詢物件 (V + N + Query)
│   │   │   ├── GetQuotationQuery.java               # 取得報價單查詢物件 (包含 ID)
│   │   │   └── package-info.java
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
│   │   │   └── OpenPdfPageEventHelper.java          # 分頁與動態頁碼 (第 X 頁 / 共 Y 頁) 與浮水印事件
│   │   └── template/                                # 【業務專屬範本】
│   │       ├── PdfDocumentTemplate.java             # PDF 業務範本通用抽象契約介面
│   │       └── BomQuotationPdfTemplate.java         # 物料清單 (BOM) 報價單專屬排版實作
│   └── adapter/                                     # 出境適配器 (實作 Application 層 Outbound Port)
│       ├── OpenQuotationPdfGeneratorAdapter.java    # 實作 QuotationPdfGeneratorPort 之 OpenPDF 適配器
│       ├── TextQuotationParserAdapter.java          # 實作 QuotationParserPort 之文字與語法剖析適配器
│       ├── MockServiceAdapter.java                  # 實作 MockServicePort 之模擬範本資料適配器
│       ├── PdfExtractorAdapter.java                 # 實作 PdfExtractorPort 之 Apache PDFBox 適配器
│       ├── BirtReportAdapter.java                   # 實作 ReportGeneratorPort 之 Eclipse BIRT 適配器
│       └── package-info.java
│
├── presentation/                                    # 表現層 (Presentation Layer - Inbound Adapters)
│   ├── controller/                                  # REST Controller API 介面 (*Controller)
│   │   ├── QuotationController.java                 # 報價單 PDF 生成與解析控制器 (標註 OpenPDF/PDFBox)
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
    └── package-info.java                            # Spring @Configuration、Bean 裝配與全域設定

doc/
├── design/                                          # 技術設計與架構規範 (依技術分目錄)
│   ├── README.md                                    # 技術設計文檔總覽導覽
│   ├── openpdf/                                     # 【OpenPDF 技術體系】
│   │   ├── openpdf_architecture_design.md           # OpenPDF 報表生成整體架構設計
│   │   ├── custom_openpdf_template_guide.md         # 自定義業務單據範本開發實戰指南 (6 步驟)
│   │   └── openpdf_anti_orphan_pagination_guide.md  # 動態資料量智慧防孤兒區塊排版技術指南
│   ├── birt/                                        # 【Eclipse BIRT 技術體系】
│   │   └── birt_pdf_module_operations_guide.md      # Eclipse BIRT 報表架構與維運排障手冊
│   └── pdfbox/                                      # 【Apache PDFBox 技術體系】
│       └── pdf_parser_development_guide.md          # PDF 結構化文字抽取與狀態機剖析開發指南
└── operations/                                      # 維運與生產部署手冊
    └── birt_pdf_module_operations_guide.md          # 維運人員專屬操作手冊

src/main/resources/
├── application.properties                           # 應用程式配置檔 (包含 SpringDoc Swagger UI 設定)
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
│   └── OpenApiConfigTest.java                       # OpenAPI 全域配置 BDD 單元測試
├── infrastructure/
│   └── adapter/
│       ├── OpenPdfQuotationAdapterTest.java         # OpenPDF 報價單適配器 BDD 整合測試
│       ├── TextQuotationParserAdapterTest.java      # 報價單文字剖析適配器 BDD 單元測試
│       ├── MockServiceAdapterTest.java              # MockService 適配器 BDD 單元測試
│       ├── PdfBoxAdapterTest.java                   # PDFBox 適配器 BDD 單元測試
│       └── BirtReportAdapterTest.java               # BIRT 報表適配器 BDD 單元測試
└── presentation/
    └── controller/
        ├── QuotationControllerTest.java             # 報價單 API 控制器 BDD 單元測試
        └── GlobalExceptionHandlerTest.java          # 全域例外處理器 BDD 單元測試

```

### 分層職責說明

1. **領域層 (`domain/`)**
   - 純 Java 實作，**嚴格禁止**引入 Spring、JPA、Hibernate、Lombok 等技術依賴，確保核心業務邏輯的技術無關性。
2. **應用層 (`application/`)**
   - 負責業務流程編排、CQRS 派送與資料投影。
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

本專案提供高度完整的架構分析、範本擴展實戰與維運排障手冊，詳情請參閱 [技術設計文檔導覽 (doc/design/README.md)](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/README.md)。

### 三大技術體系與專案文檔

1. **【OpenPDF 體系】程式化像素級高精確排版**
   - 🏛️ [OpenPDF 報表與文件範本生成整體架構設計](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/openpdf/openpdf_architecture_design.md)：說明 Clean Architecture 六角分層、四項通用排版積木 (`FontProvider`, `StylePalette`, `CellFactory`, `PageEventHelper`)。
   - 🛠️ [自定義業務單據範本開發實戰指南 (6 步驟)](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/openpdf/custom_openpdf_template_guide.md)：提供開發全新單據（出貨單、採購單等）之標準 6-Step 流程與排版積木套用範例。
   - 📐 [動態資料量智慧防孤兒區塊排版技術指南](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/openpdf/openpdf_anti_orphan_pagination_guide.md)：解析雙層防孤兒排版機制（結尾複合區塊防拆原子化 + 錨點明細牽引換頁演算法），保證任意資料量絕不出現孤兒簽章頁。

2. **【Eclipse BIRT 體系】視覺化範本驅動報表引擎**
   - 📊 [Eclipse BIRT 報表架構、範本維護與維運排障手冊](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/birt/birt_pdf_module_operations_guide.md)：說明 Spring 單例託管 BIRT 引擎生命週期、`.rptdesign` 報表參數與腳本資料集、容器化字體配置，以及六大常見故障排障 SOP。

3. **【Apache PDFBox 體系】二進位逆向結構化文字抽取**
   - 🔍 [PDF 結構化文字抽取與狀態機剖析開發指南](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/pdfbox/pdf_parser_development_guide.md)：說明兩階段管線化剖析架構（`PdfExtractorPort` 全文幾何抽取 $\rightarrow$ `QuotationParserPort` 正則狀態機剖析），包含千分位去噪與防禦性容錯。

4. **【維運與生產部署】**
   - 🚀 [維運人員專屬操作手冊 (doc/operations/birt_pdf_module_operations_guide.md)](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/operations/birt_pdf_module_operations_guide.md)：提供生產環境佈署、中文字體安裝與系統維運之快速檢核指南。

---

## 命名規範 (9 大類別)

系統內各類別物件必須嚴格遵守以下命名規範：

| 類別 | 命名規範 | 範例 |
| :--- | :--- | :--- |
| **1. Request** | `V + N + Resource` | `GetQuotationPdfResource`, `GenerateQuotationPdfResource` |
| **2. Response** | `N + Ved + Resource` | `QuotationParsedResource`, `QuotationItemParsedResource`, `ErrorOccurredResource` |
| **3. Event** | `N + Ved + Event` | `PdfGeneratedEvent`, `PdfProcessedEvent` |
| **4. Command** | `V + N + Command` | `ParseQuotationPdfCommand`, `GeneratePdfCommand`, `MergePdfCommand` |
| **5. Query** | `V + N + Query` | `GetQuotationQuery`, `GetPdfQuery` |
| **6. Dto** | `N + Gotten/Searched + Result` | `QuotationGottenResult`, `QuotationItemGottenResult`, `PdfTextPositionGottenResult` |
| **7. Projection** | `N + Gotten/Searched + View` | `PdfGottenView`, `PdfSearchedView` |
| **8. Port** | In: `... + UseCase`<br>Out: `... + Port` | In: `GenerateQuotationPdfUseCase`, `GenerateTemplateQuotationPdfUseCase`, `ParseQuotationPdfUseCase`<br>Out: `QuotationPdfGeneratorPort`, `ReportGeneratorPort`, `QuotationParserPort`, `PdfExtractorPort`, `MockServicePort` |
| **9. Adapter** | In: `... + ApplicationService`<br>Out: `... + Adapter` | In: `QuotationApplicationService`<br>Out: `OpenQuotationPdfGeneratorAdapter`, `BirtReportAdapter`, `TextQuotationParserAdapter`, `PdfExtractorAdapter`, `MockServiceAdapter` |

---

## REST API 端點說明 (報價單生成與解析)

本模組提供程式化生成、範本驅動生成與結構化上傳解析三大核心能力：

| HTTP 方法 | URL 路徑 | 請求參數 / 請求體 | 功能說明 | 底層引擎 | 回傳格式 | 關鍵回應標頭 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/quotations/openpdf` | `id` (可選)<br>`download` (可選，預設 `true`) | **程式化高精確排版**：使用 OpenPDF 產生雙層防孤兒排版之報價單 PDF | `OpenPDF 3.0.5` | `application/pdf` | `X-Pdf-Engine: OpenPDF`<br>`Content-Disposition: attachment; filename="{id}_openpdf.pdf"` |
| **GET** | `/api/v1/quotations/template`<br>*(同義別名 `/birt`)* | `id` (可選)<br>`download` (可選，預設 `true`) | **視覺化範本驅動排版**：使用 Eclipse BIRT 報表設計範本 (`.rptdesign`) 渲染標準報價單 PDF | `Eclipse BIRT 4.20.0` | `application/pdf` | `X-Pdf-Engine: BIRT (Template)`<br>`Content-Disposition: attachment; filename="{id}_template.pdf"` |
| **POST** | `/api/v1/quotations/parse` | `multipart/form-data`<br>`file`: PDF 檔案 (二進位) | **PDF 結構化逆向解析**：上傳 PDF，抽取純文字並透過正則狀態機剖析為結構化報價單 DTO | `Apache PDFBox 3.0.8` | `application/json` | `Content-Type: application/json`<br>(回傳 `QuotationParsedResource`) |

> 📌 **回應標頭與下載機制說明**：
> - `X-Pdf-Engine`：清楚標示底層渲染引擎為 `OpenPDF` 或 `BIRT (Template)`。
> - `Content-Disposition: attachment; filename="..."`：預設以附件方式回傳，Swagger UI 會自動呈現「Download file」按鈕以供下載檢視。若傳入 `download=false` 則以 `inline` 模式回傳，可在瀏覽器分頁直接預覽。
> - `POST /api/v1/quotations/parse`：防腐層設計，表現層接收 Spring `MultipartFile` 後封裝為領域無關的 `ParseQuotationPdfCommand(byte[], fileName)`，絕不將框架物件傳入 Application Port。回傳 JSON 包含報價單號、客戶資訊、物料明細、小計稅額總計與交易條款。

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
