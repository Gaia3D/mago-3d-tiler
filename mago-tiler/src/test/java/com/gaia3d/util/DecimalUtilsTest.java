package com.gaia3d.util;

import com.gaia3d.command.LoggingConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class DecimalUtilsTest {

    @Test
    void testGermanyDecimalSeparator() {
        LoggingConfiguration.initConsoleLogger();

        double original = 1234.12345678;
        DecimalFormat decimalFormat = new DecimalFormat("0.000");
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.GERMANY));

        try {
            double result = Double.parseDouble(decimalFormat.format(original));
        } catch (NumberFormatException e) {
            log.error("[ERROR] Fail Case: ", e);
        }

        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        double result = Double.parseDouble(decimalFormat.format(original));
        log.info("Success Case: {}", result);
    }

    @Test
    void millisecondToDisplayTime() {
        long millis = 123456789;
        String displayTime = DecimalUtils.millisecondToDisplayTime(millis);
        assertEquals("34h 17m 36s 789ms", displayTime);
    }
}