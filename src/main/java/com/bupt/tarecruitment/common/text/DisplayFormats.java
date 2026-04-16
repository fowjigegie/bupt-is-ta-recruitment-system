package com.bupt.tarecruitment.common.text;

import java.math.BigDecimal;

/**
 * 提供界面展示和文本落盘会复用的数字格式化工具。
 */
public final class DisplayFormats {
    private DisplayFormats() {
    }

    public static String formatDecimal(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
