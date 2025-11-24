package com.gaia3d.converter;

import com.gaia3d.util.VectorUtils;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PolygonFilter {

    public List<Vector2d> filter(List<Vector2d> input) {
        /* Remove last point if it is same as first point */
        if (input.get(0).equals(input.get(input.size() - 1))) {
            input.remove(input.size() - 1);
        }

        List<Vector2d> result = new ArrayList<>();
        int length = input.size();
        for (int i = 0; i < length; i++) {
            int index1 = (i - 1) % length < 0 ? length - 1 : (i - 1) % length;
            int index2 = i % length;
            int index3 = (i + 1) % length;
            Vector2d prev = input.get(index1);
            Vector2d crnt = input.get(index2);
            Vector2d next = input.get(index3);

            double cross = VectorUtils.cross(prev, crnt, next);
            if (crnt.equals(prev) || crnt.equals(next)) {
                continue;
            } else if (Double.isNaN(cross)) {
                continue;
            } else if (cross == 0) {
                continue;
            } else if (Math.abs(cross) < 0.000001) {
                continue;
            }
            result.add(crnt);
        }
        if (!result.isEmpty()) {
            result.add(new Vector2d(result.get(0)));
        }
        return result;
    }
}