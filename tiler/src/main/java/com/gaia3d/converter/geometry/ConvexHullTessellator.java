package com.gaia3d.converter.geometry;

import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ConvexHullTessellator {

    public List<GaiaTriangle> tessellate(List<Vector3d> positions) {
        List<GaiaTriangle> result = new ArrayList<>();
        int start = 1;
        int size = positions.size() - 1;
        Vector3d p0 = positions.get(0);
        for (int i = start; i < size; i ++) {
            Vector3d p1 = positions.get(i);
            Vector3d p2 = positions.get(i + 1);
            GaiaTriangle triangle = new GaiaTriangle(p0, p1, p2);
            result.add(triangle);
        }
        return result;
    }
}
