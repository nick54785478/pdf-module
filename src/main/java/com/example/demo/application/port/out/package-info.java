/**
 * 輸出端口 (Outbound Ports)
 * <p>
 * 定義應用層對外要求基礎設施層提供的合約介面 (如 Repository、訊息發布等)。
 * 命名規範：以 {@code Port} 結尾 (例如：{@code PdfRepositoryPort})。
 * 嚴禁在此介面方法中使用任何技術類或環境相依的物件 (如 JPA Entity、Connection 等)。
 */
package com.example.demo.application.port.out;
