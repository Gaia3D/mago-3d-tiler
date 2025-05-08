package com.gaia3d.converter.geometry;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;

@Slf4j
@NoArgsConstructor
public class ForestModelHelper {
    public static boolean isForest = false;
    
    /**
     * Calculate the number of trees in a polygon based on the area and tree density.
     * @param polygon
     * @param treeProportion density of trees in the polygon (0.0 to 1.0)
     * @param treeDiameter diameter of the trees (in square meters)
     * @return
     */
    public static int calcTreeCount(Geometry polygon, double treeProportion, double treeDiameter) {
        if (treeProportion <= 0) {
            return 0;
        }
        double area = polygon.getArea();
        double forestArea = area * treeProportion;
        double treeDensity = treeDiameter * treeDiameter;

        double count = forestArea / treeDensity;

        int castCount = (int) count;
        System.out.println("count: " + castCount);
        return castCount;
    }

    public static double convertTreeHeightForEntity(String heightString) {
        double defaultHeight = 1.0;

        if (heightString == null) {
            log.error("Height is null.");
            return defaultHeight;
        }

        try {
            return Double.parseDouble(heightString);
        } catch (NumberFormatException e) {
            log.error("Height is not valid.");
            log.error("Height : {}", heightString);
            return defaultHeight;
        }
    }

    public static double convertTreeHeightForESD(String heightString) {
        double defaultHeight = 5.0;

        if (heightString == null) {
            log.error("Height is null.");
            return defaultHeight;
        } else if (heightString.contains("임분고")) {
            heightString = heightString.replace("임분고", "");
            heightString = heightString.replace(" ", "");
            heightString = heightString.replace("m", "");

            boolean isUnder = heightString.contains("미만");
            boolean isOver = heightString.contains("이상");

            if (isUnder && isOver) {
                String[] split = heightString.split("이상");
                if (split.length != 2) {
                    log.error("Height is not valid.");
                    log.error("Height : {}", heightString);
                    return defaultHeight;
                } else {
                    heightString = split[1].replace("미만", "");
                }
            } else if (isUnder) {
                heightString = heightString.replace("미만", "");
            } else if (isOver) {
                heightString = heightString.replace("이상", "");
            }
        }

        try {
            return Double.parseDouble(heightString);
        } catch (NumberFormatException e) {
            log.error("Height is not valid.");
            log.error("Height : {}", heightString);
            return defaultHeight;
        }
    }
}
