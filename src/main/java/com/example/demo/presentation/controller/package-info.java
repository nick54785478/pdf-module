/**
 * 表現層控制器 (REST Controllers / Inbound Adapters)
 * <p>
 * 接收外部 HTTP Request，透過 Assembler 轉換為 Command/Query 後呼叫 Application UseCase，
 * 並將處理結果透過 Assembler 封裝為 Response Resource 回傳。
 * 命名規範：以 {@code Controller} 結尾 (例如：{@code PdfController})。
 */
package com.example.demo.presentation.controller;
