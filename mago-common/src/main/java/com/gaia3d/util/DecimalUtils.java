package com.gaia3d.util;

import lombok.experimental.UtilityClass;

import java.text.DecimalFormat;

@UtilityClass
public class DecimalUtils {
    public static double cut(double value) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00000000");
        return Double.parseDouble(decimalFormat.format(value));
    }

    public static double cut(double value, int digit) {
        String format = "0.";
        for (int i = 0; i < digit; i++) {
            format += "0";
        }
        DecimalFormat decimalFormat = new DecimalFormat(format);
        return Double.parseDouble(decimalFormat.format(value));
    }

    public static float cut(float value) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00000000");
        return Float.parseFloat(decimalFormat.format(value));
    }

    public static float cut(float value, int digit) {
        String format = "0.";
        for (int i = 0; i < digit; i++) {
            format += "0";
        }
        DecimalFormat decimalFormat = new DecimalFormat(format);
        return Float.parseFloat(decimalFormat.format(value));
    }
}
