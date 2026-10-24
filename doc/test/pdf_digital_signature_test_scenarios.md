# PDF 數位簽章與安全防護測試情境指南 (PDF Digital Signature Test Scenarios & Verification Guide)

> 🔗 **相關架構與實務文件導覽**：
> - [PDF 數位簽章完整流程與憑證密鑰管理實戰指南 (doc/design/pdfbox/pdf_digital_signature_workflow_and_secret_guide.md)](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/pdfbox/pdf_digital_signature_workflow_and_secret_guide.md)
> - [Apache PDFBox 安全防護與範本自定義規範設計文檔 (doc/design/pdfbox/pdfbox_security_signature_design.md)](file:///d:/桌面/ModuleWorkSpace/pdf-module/doc/design/pdfbox/pdfbox_security_signature_design.md)
> - [專案根目錄 README.md](file:///d:/桌面/ModuleWorkSpace/pdf-module/README.md)

---

## 1. 測試目標與前置準備 (Prerequisites & Test Setup)

本文件專為測試人員、開發者與系統整合人員提供 **PDF 數位簽章（Digital Signature）** 與安全防護功能的完整測試指引，涵蓋從憑證產製、Swagger UI 互動式測試、cURL 自動化腳本、異常邊界攔截，到 Adobe Acrobat Reader 終端驗證的完整驗證流程。

### 1.1 測試環境需求
- **執行環境**：Java 21+、Spring Boot 4.1.1
- **啟動專案**：在專案根目錄執行 `./mvnw.cmd spring-boot:run`
- **Swagger UI 介面**：`http://localhost:8080/swagger-ui.html`

### 1.2 金鑰庫憑證準備 (Keystore)
專案根目錄已具備測試用 PKCS#12 金鑰庫：
- **金鑰庫檔案**：[`enterprise_signer.p12`](file:///d:/桌面/ModuleWorkSpace/pdf-module/enterprise_signer.p12)
- **金鑰別名 (`keyAlias`)**：`pdfbox-signer`
- **金鑰庫密碼 (`password`)**：`SecretPassword123`

> 💡 **若需自行重新產製金鑰庫**：
> 請於 PowerShell 執行以下指令（引用 `JAVA_HOME` 避免環境變數缺漏）：
> ```powershell
> # 若已有舊檔案，先刪除再重新產生
> if (Test-Path enterprise_signer.p12) { Remove-Item enterprise_signer.p12 }
> 
> & "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -alias pdfbox-signer -keyalg RSA -keysize 2048 -validity 365 -keystore enterprise_signer.p12 -storetype PKCS12 -storepass SecretPassword123 -dname "CN=晨星智造系統核准章, OU=研發處, O=MorningStar Corp, C=TW"
> ```

### 1.3 待簽原始 PDF 準備
您可使用本模組的 GET 端點快速取得一份原始報價單：
- 瀏覽器開啟：`http://localhost:8080/api/v1/quotations/openpdf?id=QT-20261023-001&download=true` 下載 `QT-20261023-001.pdf` 作為測試樣本。

---

## 2. 測試情境矩陣總覽 (Test Scenarios Matrix)

| 編號 | 測試情境名稱 | 驗證重點 | 測試方式 | 預期結果 |
| :---: | :--- | :--- | :---: | :--- |
| **TC-01** | **金鑰庫有效性檢核** | 驗證 PKCS#12 包含 PrivateKeyEntry 與有效別名 | CLI (`keytool`) | 回傳包含 `pdfbox-signer`，狀態為 PrivateKeyEntry |
| **TC-02** | **Swagger UI 基礎簽署流程** | 驗證 Web UI 上傳 PDF 與憑證庫直接簽署 | Swagger UI | HTTP 200，產出附帶簽章之 PDF 檔案 |
| **TC-03** | **BOM 範本規範自動套用** | 帶入 `template=bom`，驗證 PAdES 規範與專屬鋼印 | Swagger / cURL | 自動套用 `ETSI.CAdES.detached`、專屬理由與硃砂紅放行鋼印 |
| **TC-04** | **系統預設規範平滑降級** | 不帶 `template`，驗證 Fallback 預設簽章政策 | Swagger / cURL | 自動套用 `adbe.pkcs7.detached` 與企業深藍雙線防偽框 |
| **TC-05** | **純背景隱形簽章測試** | 設定 `visual=false`，驗證無外觀純數位簽名 | Swagger / cURL | PDF 版面無印章方塊，但閱讀器簽名面板顯示有效簽署 |
| **TC-06** | **異常密碼防禦攔截** | 輸入錯誤的金鑰庫密碼 (如 `wrongSecret`) | REST API | HTTP 400，明確提示密碼錯誤，不洩漏系統例外堆疊 |
| **TC-07** | **空檔案與損毀檔案攔截** | 未上傳 PDF 或上傳偽造之損毀二進位資料 | REST API | HTTP 400，驗證防禦機制正常運作 |
| **TC-08** | **Adobe Reader 防偽不可否認性** | 本機閱讀器打開已簽署 PDF 檢驗雜湊完整性 | Adobe Acrobat | 顯示「自簽署後未曾修改過」，ByteRange 雜湊吻合 |
| **TC-09** | **全套自動化單元/整合測試** | 執行 Maven BDD 測試套件 | `./mvnw.cmd test` | 76/76 項測試全數 `BUILD SUCCESS` |

---

## 3. 情境 1：金鑰庫產製與有效性驗證 (TC-01)

### 測試步驟
於專案根目錄開啟 PowerShell，執行檢驗指令：

```powershell
& "$env:JAVA_HOME\bin\keytool.exe" -list -keystore enterprise_signer.p12 -storepass SecretPassword123
```

### 預期輸出
```text
金鑰庫類型: PKCS12
金鑰庫提供者: SUN

您的金鑰庫包含 1 個項目:
pdfbox-signer, 2026年10月25日, PrivateKeyEntry, 
憑證指紋 (SHA-256): 4F:11:E2:FB:74:C8:8E:1B:B8:3A:07:7E:35:DA:2C:6F:5C:0F:C0:85:81:5C:4C:AB:CF:1E:45:61:A2:63:D6:A1
```
> ✅ **驗證點**：確認存在項目名稱為 `pdfbox-signer`，且類型為 `PrivateKeyEntry`（代表內含非對稱私鑰）。

---

## 4. 情境 2：Swagger UI 互動式簽署報價單情境 (TC-02)

### 測試步驟
1. 開啟瀏覽器進入 Swagger UI：`http://localhost:8080/swagger-ui.html`
2. 展開 **`POST /api/v1/quotations/sign`**（對 PDF 檔案進行 PKCS#7 數位簽章與防偽印章印製）。
3. 點擊 **「Try it out」**，依序填入以下測試資料：
   - **`file`**：點擊「選擇檔案」，選取任一 PDF 文件（如 `QT-20261023-001.pdf`）。
   - **`keystore`**：點擊「選擇檔案」，選取專案根目錄下的 [`enterprise_signer.p12`](file:///d:/桌面/ModuleWorkSpace/pdf-module/enterprise_signer.p12)。
   - **`password`**：輸入 `SecretPassword123`
   - **`alias`**：輸入 `pdfbox-signer`（或留白自動抓取）
   - **`reason`**：輸入 `核准正式報價單放行`
   - **`location`**：輸入 `台北總部`
   - **`visual`**：選擇 `true`
   - **`page`**：輸入 `1`
   - **`download`**：選擇 `true`
4. 點擊藍色 **「Execute」** 按鈕發出請求。

### 預期結果
1. **HTTP Status Code**：`200 OK`
2. **Response Headers** 必須包含：
   - `Content-Disposition: attachment; filename="QT-20261023-001_signed.pdf"`
   - `X-Pdf-Engine: PDFBox (Digital Signature)`
3. 點擊 Swagger UI 回應區的 **「Download file」**，成功下載已完成簽名之 PDF 檔案。

---

## 5. 情境 3：單據範本專屬規範自動套用測試情境 (TC-03)

驗證本模組獨創之「**單據範本感知與個別簽章定義 (`PdfDocumentTemplate` Hook)**」機制。

### 測試方式 (cURL)
開啟終端機，執行以下 cURL 呼叫（傳入 `template=bom`，故意不傳 `reason` 與 `location`）：

```bash
curl -X POST "http://localhost:8080/api/v1/quotations/sign" \
  -H "accept: application/pdf" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@QT-20261023-001.pdf;type=application/pdf" \
  -F "keystore=@enterprise_signer.p12;type=application/x-pkcs12" \
  -F "password=SecretPassword123" \
  -F "template=bom" \
  -F "visual=true" \
  -F "page=1" \
  -F "download=true" \
  --output bom_pades_signed.pdf
```

### 預期驗證點
以 PDF 閱讀器打開 `bom_pades_signed.pdf`：
1. **簽章規格**：簽章字典屬性為 `ETSI.CAdES.detached`（PAdES-BES 標準）。
2. **自動代入範本預設理由**：理由自動顯示為 `BOM 原物料報價核准簽署`。
3. **自動代入範本預設地點**：地點自動顯示為 `台北營運總部`。
4. **專屬視覺印章外觀**：
   - 印章外框：晨星智造企業深藍 (`#1A365D`) + 輔助防偽金銅色雙線邊框。
   - 尊榮標題：硃砂紅 (`#B91C1C`)「★ 晨星智造 • 原物料放行審批 ★」。
   - 英文副標：包含「BOM APPROVAL SEAL • PAdES-BES VERIFIED」。

---

## 6. 情境 4：系統預設簽章規範平滑降級測試情境 (TC-04)

驗證當呼叫端**未指定單據範本（`template` 留空）**時的自動 Fallback 機制。

### 測試方式 (cURL)
```bash
curl -X POST "http://localhost:8080/api/v1/quotations/sign" \
  -H "accept: application/pdf" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@QT-20261023-001.pdf;type=application/pdf" \
  -F "keystore=@enterprise_signer.p12;type=application/x-pkcs12" \
  -F "password=SecretPassword123" \
  -F "reason=一般商務核准" \
  -F "location=新竹分部" \
  -F "visual=true" \
  --output default_signed.pdf
```

### 預期驗證點
1. **簽章規格**：自動採用系統 `@Primary` 預設標準規範 `adbe.pkcs7.detached`。
2. **視覺印章**：呈現標準企業深藍雙線框，標題為「✔ 數位簽署防偽認證 (Verified Signature)」。

---

## 7. 情境 5：純背景隱形數位簽章測試 (TC-05)

在許多高度機密的合約中，不希望版面上覆蓋印章圖形，但仍需要密碼學簽章的法律效力。

### 測試步驟
在 Swagger UI 中將參數 `visual` 設定為 `false`，其他參數正常填寫後執行。

### 預期驗證點
1. 下載簽署後的 PDF，版面完全保持乾淨，無任何印章或文字圖層。
2. 以 Adobe Acrobat Reader 開啟時，左側/頂部「簽名面板」依然顯示「已簽署且所有簽名均有效」，點擊可檢視完整的憑證與雜湊校驗。

---

## 8. 情境 6：異常分支與安全性阻絕情境 (TC-06 / TC-07)

### 案例 6.1：密碼錯誤測試 (Negative Case)
- **測試輸入**：`keystore` 上傳合法檔案，但 `password` 輸入 `WrongPassword123`。
- **預期結果**：
  - HTTP 狀態碼：`400 Bad Request`
  - Response Body JSON：
    ```json
    {
      "status": 400,
      "message": "金鑰庫載入失敗：密碼錯誤或格式非合法 PKCS#12",
      "timestamp": "..."
    }
    ```
  - ✅ **資安驗證**：不回傳 500 Internal Server Error，不洩漏任何 Java 原始碼堆疊（Stacktrace）。

### 案例 6.2：未上傳金鑰庫測試
- **測試輸入**：`file` 上傳 PDF，但 `keystore` 欄位留空。
- **預期結果**：
  - HTTP 狀態碼：`400 Bad Request`。

---

## 9. 情境 7：Adobe Acrobat Reader 本機終端驗證指南 (TC-08)

### 9.1 開啟簽章檔案
使用桌面版 **Adobe Acrobat Reader** 打開剛簽署完成的 PDF 檔案（例如 `bom_pades_signed.pdf`）。

### 9.2 檢驗簽名面板與防竄改雜湊
1. 點擊頂部或左側欄位之「🖊 簽名」按鈕展開簽章面板。
2. 檢視檢驗項目：
   - **文件完整性**：顯示「自簽署後未曾修改過 (Document has not been modified since it was certified)」。
   - **簽名時間**：顯示簽名時間戳（SignDate）。
   - **憑證主體**：點擊「簽署者詳細資訊」，確認使用者為 `CN=晨星智造系統核准章, OU=研發處, O=MorningStar Corp, C=TW`。

### 9.3 如何消除自簽憑證警告（將測試自簽憑證新增至本機受信任名單）
由於 `enterprise_signer.p12` 是由本機 `keytool` 產生的自簽憑證（非公開付費 CA），Adobe 初次打開可能會提示「⚠️ 簽名者的身分不明」：
1. 在簽名面板上的簽名項目點右鍵 -> 點擊 **「顯示簽名內容 (Show Signature Properties)」**。
2. 點擊 **「顯示簽署者的憑證 (Show Signer's Certificate)」**。
3. 切換至 **「信任 (Trust)」** 分頁 -> 點擊 **「新增至受信任的憑證 (Add to Trusted Certificates)」**。
4. 勾選 **「使用此憑證作為受信任的根 (Use this certificate as a trusted root)」** 與 **「已認證的文件 (Certified documents)」** -> 點擊「確定」。
5. 回到文件按下 `F5` 重新驗證，頂部即會點亮正式綠色勾勾標誌：
   > **`✔ 已認證，由 晨星智造系統核准章 簽署。自簽署後未曾變更過。`**

---

## 10. 情境 8：Maven 自動化單元與整合測試執行 (TC-09)

本模組的安全防護與數位簽章具備 100% 完整之 BDD Gherkin 單元與整合測試保護。

### 執行特定簽章整合測試
```powershell
./mvnw.cmd test -Dtest=PdfBoxSecurityExecutorAdapterTest
```

### 執行全模組所有測試
```powershell
./mvnw.cmd test
```

### 預期輸出
```text
[INFO] Running com.example.demo.infrastructure.adapter.PdfBoxSecurityExecutorAdapterTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.542 s -- in com.example.demo.infrastructure.adapter.PdfBoxSecurityExecutorAdapterTest
...
[INFO] Results:
[INFO] Tests run: 76, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```
所有 76 項測試案例涵蓋：
- `signPdf_defaultPolicy_success`：驗證預設簽章與增量儲存
- `signPdf_templateSignaturePolicy_success`：驗證 BOM 範本 PAdES 規範自動套用
- `signPdf_invalidKeystorePassword_throwsException`：驗證密碼錯誤異常防護
- `signPdf_withoutVisualSignature_success`：驗證隱形簽章
- `encryptPdf_aes256_success`：驗證 256-bit AES 加密防護
- `decryptPdf_withCorrectPassword_success`：驗證密碼解密還原
