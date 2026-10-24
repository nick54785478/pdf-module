package com.example.demo.application.port.in;

import com.example.demo.application.shared.command.AddWatermarkCommand;

/**
 * 加蓋 PDF 浮水印用例 (Inbound Port / UseCase)
 * <p>
 * 依據 Clean Architecture 與六角形架構規範，代表應用層對外（表現層或外部微服務）開放的通用浮水印處理使用案例契約。
 * 接收封裝 PDF 二進位資料與文字之 {@link AddWatermarkCommand} 命令物件，
 * 調度基礎設施層之浮水印適配器完成傾斜半透明防偽印製，並回傳加工後之 PDF 位元組陣列。
 * 遵循命名規範 3-8：Inbound Port 一律以 UseCase 結尾。
 * </p>
 */
public interface AddWatermarkUseCase {

	/**
	 * 於指定 PDF 文件的每一頁中央加蓋傾斜半透明防偽浮水印
	 *
	 * @param command 加蓋浮水印命令物件 (包含 PDF 二進位位元組與浮水印文字，不可為 null)
	 * @return 加蓋浮水印後之 PDF 二進位 byte 陣列
	 */
	byte[] addWatermark(AddWatermarkCommand command);
}
