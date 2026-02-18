package com.pallux.sandsim.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumberFormatter {

    public static String format(BigDecimal number) {
        if (number == null) return "0";
        boolean isNegative = number.compareTo(BigDecimal.ZERO) < 0;
        BigDecimal absNumber = number.abs();
        String result;

        if (absNumber.compareTo(BigDecimal.valueOf(1_000_000_000_000_000L)) >= 0) {
            result = formatWithSuffix(absNumber, BigDecimal.valueOf(1_000_000_000_000_000L), "Qa");
        } else if (absNumber.compareTo(BigDecimal.valueOf(1_000_000_000_000L)) >= 0) {
            result = formatWithSuffix(absNumber, BigDecimal.valueOf(1_000_000_000_000L), "T");
        } else if (absNumber.compareTo(BigDecimal.valueOf(1_000_000_000)) >= 0) {
            result = formatWithSuffix(absNumber, BigDecimal.valueOf(1_000_000_000), "B");
        } else if (absNumber.compareTo(BigDecimal.valueOf(1_000_000)) >= 0) {
            result = formatWithSuffix(absNumber, BigDecimal.valueOf(1_000_000), "M");
        } else if (absNumber.compareTo(BigDecimal.valueOf(1_000)) >= 0) {
            result = formatWithSuffix(absNumber, BigDecimal.valueOf(1_000), "K");
        } else {
            result = absNumber.setScale(0, RoundingMode.DOWN).toPlainString();
        }

        return isNegative ? "-" + result : result;
    }

    private static String formatWithSuffix(BigDecimal number, BigDecimal divisor, String suffix) {
        BigDecimal divided = number.divide(divisor, 2, RoundingMode.DOWN);
        return divided.stripTrailingZeros().toPlainString() + suffix;
    }

    public static String format(double number) { return format(BigDecimal.valueOf(number)); }
    public static String format(int number)    { return format(BigDecimal.valueOf(number)); }
    public static String format(long number)   { return format(BigDecimal.valueOf(number)); }
}
