package com.gaia3d.basic.geometry.tessellator;

import com.gaia3d.util.GeometryUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class GaiaExtruder {
    public List<GaiaExtrusionSurface> extrude(List<Vector3d> positions, double roofHeight, double floorHeight) {

        // check uroborus and aligned points.******************************************
        List<Vector3d> cleanPositions = new ArrayList<>();
        double error = 1e-10;
        GeometryUtils.getCleanPoints3dArray(positions, cleanPositions, error);
        // End of check uroborus and aligned points.-----------------------------------

        // Check positions sense : CW or CCW.******************************************
        Vector3d normal = new Vector3d();
        GeometryUtils.calculateNormal3D(cleanPositions, normal);

        if(normal.z < 0.0)
        {
            // reverse cleanPositions.***
            Collections.reverse(cleanPositions);
        }


        List<GaiaExtrusionSurface> result = new ArrayList<>();
        List<Vector3d> roofPositions = resetHeight(cleanPositions, roofHeight);
        List<Vector3d> floorPositions = resetHeight(cleanPositions, floorHeight);

        GaiaExtrusionSurface roof = new GaiaExtrusionSurface(roofPositions);
        List<GaiaExtrusionSurface> wallPositions = createWallPositions(roofPositions, floorPositions);

        result.add(roof);
        result.addAll(wallPositions);
        return result;
    }

    private List<GaiaExtrusionSurface> createWallPositions(List<Vector3d> roofPosition, List<Vector3d> floorPosition) {
        List<GaiaExtrusionSurface> result = new ArrayList<>();
        int size = roofPosition.size();
        for (int index = 0; index < size; index++) {
            int crntIndex = index % size;
            int nextIndex = (index + 1) % size;
            Vector3d roofTriangle = roofPosition.get(crntIndex);
            Vector3d roofTriangleNext = roofPosition.get(nextIndex);
            Vector3d floorTriangle = floorPosition.get(crntIndex);
            Vector3d floorTriangleNext = floorPosition.get(nextIndex);

            List<Vector3d> wallPositions = new ArrayList<>();
            wallPositions.add(new Vector3dOnlyHashEquals(roofTriangle));
            wallPositions.add(new Vector3dOnlyHashEquals(floorTriangle));
            wallPositions.add(new Vector3dOnlyHashEquals(floorTriangleNext));
            wallPositions.add(new Vector3dOnlyHashEquals(roofTriangleNext));
            //wallPositions.add(new Vector3dOnlyHashEquals(roofTriangle));
            result.add(new GaiaExtrusionSurface(wallPositions));
        }
        return result;
    }

    private List<Vector3d> resetHeight(List<Vector3d> positions, double height) {
        return positions.stream().map(position -> new Vector3dOnlyHashEquals(new Vector3d(position.x, position.y, height))).collect(Collectors.toList());
    }
}
