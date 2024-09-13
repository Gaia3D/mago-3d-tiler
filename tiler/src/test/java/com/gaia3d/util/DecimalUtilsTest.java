package com.gaia3d.util;

import com.gaia3d.command.Configurator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class DecimalUtilsTest {

    @Test
    void testGermanyDecimalSeparator() {
        Configurator.initConsoleLogger();

        double original = 1234.12345678;
        DecimalFormat decimalFormat = new DecimalFormat("0.000");
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMANY));

        try {
            double result = Double.parseDouble(decimalFormat.format(original));
        } catch (NumberFormatException e) {
            log.error("Fail Case: {}", e.getMessage());
        }

        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        double result = Double.parseDouble(decimalFormat.format(original));
        log.info("Success Case: {}", result);
    }
}