package com.example.demo.infrastructure.openpdf.core;

import java.awt.Color;

/**
 * OpenPDF 企業標準排版調色盤與視覺幾何常數 (Style Palette)
 * <p>
 * 集中管理企業報表標準色彩與視覺規範，避免魔法數值（Magic Numbers）散落於各業務範本中。
 * </p>
 */
public final class OpenPdfStylePalette {

	private OpenPdfStylePalette() {
		// 工具常數類禁止實例化
	}

	/** 主品牌深藍 (用於文件主標題、表格主標頭) */
	public static final Color PRIMARY_NAVY = new Color(26, 54, 93);

	/** 企業輔助裝飾藍 (用於強調標題、主題分隔線) */
	public static final Color ACCENT_BLUE = new Color(49, 130, 206);

	/** 淺灰底色 (用於資訊標籤背景) */
	public static final Color BG_LIGHT_GRAY = new Color(237, 242, 247);

	/** 表格斑馬紋交替行底色 */
	public static final Color ROW_ALT_BG = new Color(248, 250, 252);

	/** 標準邊界淺灰線色 */
	public static final Color BORDER_GRAY = new Color(203, 213, 225);

	/** 主要文字深黑灰 */
	public static final Color TEXT_DARK = new Color(30, 41, 59);

	/** 次要文字淡黑灰 (用於備註、補充說明、地址) */
	public static final Color TEXT_MUTED = new Color(100, 116, 139);

	/** 金額總計與警示紅 */
	public static final Color HIGHLIGHT_RED = new Color(197, 48, 48);

	/** 輕量卡片背景淺灰 */
	public static final Color CARD_BG_LIGHT = new Color(250, 250, 250);

	/** 總計列柔和底色 */
	public static final Color TOTAL_BG_LIGHT = new Color(254, 242, 242);
}
