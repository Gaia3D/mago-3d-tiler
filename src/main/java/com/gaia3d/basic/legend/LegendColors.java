package com.gaia3d.basic.legend;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Getter
@Setter
public class LegendColors {
    private TreeMap<Double,GaiaColor> colorMap = new TreeMap<>();

    public LegendColors() {
    }

    public void clear() {
        colorMap.clear();
    }

    public void setValueAndColor(double value, GaiaColor color) {
        if (colorMap.containsKey(value)) {
            log.warn("Value {} already exists in the color map, updating color.", value);
        }
        colorMap.put(value, color);
    }

    public void setValueAndColor(double value, double r, double g, double b, double a) {
        GaiaColor color = new GaiaColor((float)r, (float)g, (float)b, (float)a);
        setValueAndColor(value, color);
    }

    public GaiaColor getColorLinearInterpolation(double value) {
        if (colorMap.isEmpty()) {
            log.warn("Color map is empty, returning default color.");
            return new GaiaColor(0.0f, 0.0f, 0.0f, 1.0f); // Default color
        }

        Map.Entry<Double, GaiaColor> lowerEntry = colorMap.lowerEntry(value);
        Map.Entry<Double, GaiaColor> higherEntry = colorMap.higherEntry(value);

        if (lowerEntry == null) {
            return higherEntry.getValue(); // Return the first color if no lower entry exists
        } else if (higherEntry == null) {
            return lowerEntry.getValue(); // Return the last color if no higher entry exists
        } else {
            double lowerValue = lowerEntry.getKey();
            double higherValue = higherEntry.getKey();
            GaiaColor lowerColor = lowerEntry.getValue();
            GaiaColor higherColor = higherEntry.getValue();

            double ratio = (value - lowerValue) / (higherValue - lowerValue);
            float r = (float) (lowerColor.getRed() + ratio * (higherColor.getRed() - lowerColor.getRed()));
            float g = (float) (lowerColor.getGreen() + ratio * (higherColor.getGreen() - lowerColor.getGreen()));
            float b = (float) (lowerColor.getBlue() + ratio * (higherColor.getBlue() - lowerColor.getBlue()));
            float a = (float) (lowerColor.getAlpha() + ratio * (higherColor.getAlpha() - lowerColor.getAlpha()));

            return new GaiaColor(r, g, b, a);
        }
    }
}
