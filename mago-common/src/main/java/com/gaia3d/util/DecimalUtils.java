package com.gaia3d.util;

import lombok.experimental.UtilityClass;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@UtilityClass
public class DecimalUtils {
    public static double cutFast(double value) {
        return Math.floor(value * 1e8) / 1e8;
    }

    public static double cut(double value) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00000000");
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return Double.parseDouble(decimalFormat.format(value));
    }

    public static double cut(double value, int digit) {
        DecimalFormat decimalFormat = new DecimalFormat("0." + "0".repeat(Math.max(0, digit)));
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return Double.parseDouble(decimalFormat.format(value));
    }

    public static float cut(float value) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00000000");
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return Float.parseFloat(decimalFormat.format(value));
    }

    public static float cut(float value, int digit) {
        DecimalFormat decimalFormat = new DecimalFormat("0." + "0".repeat(Math.max(0, digit)));
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return Float.parseFloat(decimalFormat.format(value));
    }

    public static String byteCountToDisplaySize(long size) {
        String displaySize;
        if (size / 1073741824L > 0L) {
            displaySize = size / 1073741824L + "GB";
        } else if (size / 1048576L > 0L) {
            displaySize = size / 1048576L + "MB";
        } else if (size / 1024L > 0L) {
            displaySize = size / 1024L + "KB";
        } else {
            displaySize = size + "bytes";
        }
        return displaySize;
    }

    public static String millisecondToDisplayTime(long millis) {
        String displayTime = "";

        long hour = millis / 3600000L;
        if (millis / 3600000L > 0L) {
            displayTime += hour + "h ";
        }
        millis %= 3600000L;

        long min = millis / 60000L;
        if (millis / 60000L > 0L) {
            displayTime += millis / 60000L + "m ";
        }
        millis %= 60000L;

        long sec = millis / 1000L;
        if (millis / 1000L > 0L) {
            displayTime += millis / 1000L + "s ";
        }
        millis %= 1000L;

        displayTime += millis % 1000L + "ms";
        return displayTime;
    }
}
