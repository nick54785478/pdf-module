/**
 * 輸入端口 (Inbound Ports / Use Cases)
 * <p>
 * 定義應用層對外開放的業務合約介面，接收 Command/Query 並回傳 DTO/View。
 * 命名規範：以 {@code UseCase} 結尾 (例如：{@code GeneratePdfUseCase})。
 * 嚴禁在此介面方法中使用任何技術類物件 (如 HttpServletRequest、Entity 等)。
 */
package com.example.demo.application.port.in;
