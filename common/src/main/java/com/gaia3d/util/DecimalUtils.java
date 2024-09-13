package com.gaia3d.util;

import lombok.experimental.UtilityClass;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

@UtilityClass
public class DecimalUtils {
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
}
