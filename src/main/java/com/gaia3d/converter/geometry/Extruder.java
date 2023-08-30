package com.gaia3d.converter.geometry;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class Extruder {

    private final Tessellator tessellator;

    public Extrusion extrude(List<Vector3d> positions, double roofHeight, double floorHeight) {
        List<GaiaTriangle> resultTriangles = new ArrayList<>();
        List<Vector3d> resultPositions = new ArrayList<>();
        Extrusion result = new Extrusion(resultTriangles, resultPositions);

        List<Vector3d> roofPositions = resetPositionHeight(positions, roofHeight);
        //List<Vector3d> roofPositions = positions;
        List<Vector3d> floorPositions = resetPositionHeight(positions, floorHeight);

        List<GaiaTriangle> triangleRoof = tessellator.tessellate(roofPositions);
        //List<GaiaTriangle> triangleRoof = new ArrayList<>(triangles);
        //triangleRoof = resetHeight(triangleRoof, roofHeight, true);
        //Collections.reverse(triangleRoof);

        List<GaiaTriangle> triangleFloor = tessellator.tessellate(floorPositions);
        //List<GaiaTriangle> triangleFloor = new ArrayList<>(triangles);
        //triangleFloor = resetHeight(triangleFloor, floorHeight, false);
        //Collections.reverse(triangleFloor);

        List<Vector3d> wallPositions = createWallPositions(roofPositions, floorPositions);
        List<GaiaTriangle> wallTriangles = createWallTriangles(wallPositions);

        resultTriangles.addAll(wallTriangles);
        resultTriangles.addAll(triangleRoof);
        //resultTriangles.addAll(triangleFloor);

        resultPositions.addAll(wallPositions);
        resultPositions.addAll(roofPositions);
        //resultPositions.addAll(floorPositions);

        return result;
    }

    private List<Vector3d> createWallPositions(List<Vector3d> roofPosition, List<Vector3d> floorPosition) {
        List<Vector3d> result = new ArrayList<>();
        int size = roofPosition.size();
        for (int index = 0; index < size; index++) {
            int crntIndex = index % size;
            int nextIndex = (index + 1) % size;
            Vector3d roofTriangle = roofPosition.get(crntIndex);
            Vector3d roofTriangleNext = roofPosition.get(nextIndex);
            Vector3d floorTriangle = floorPosition.get(crntIndex);
            Vector3d floorTriangleNext = floorPosition.get(nextIndex);

            /*result.add(roofTriangle);
            result.add(roofTriangleNext);
            result.add(floorTriangle);
            result.add(floorTriangleNext);

            Vector3d test = new Vector3d(roofTriangle);
            boolean isSame = test == roofTriangle;
            log.info("{}", isSame);*/

            result.add(new Vector3d(roofTriangle));
            result.add(new Vector3d(roofTriangleNext));
            result.add(new Vector3d(floorTriangle));
            result.add(new Vector3d(floorTriangleNext));
        }
        return result;
    }

    private List<GaiaTriangle> createWallTriangles(List<Vector3d> wallPositions) {
        List<GaiaTriangle> result = new ArrayList<>();
        for (int index = 0; index < wallPositions.size(); index+=4) {
            Vector3d positionA = wallPositions.get(index);
            Vector3d positionB = wallPositions.get(index + 1);
            Vector3d positionC = wallPositions.get(index + 2);
            Vector3d positionD = wallPositions.get(index + 3);

            GaiaTriangle wallTriangle1 = new GaiaTriangle(positionA, positionC, positionD);
            GaiaTriangle wallTriangle2 = new GaiaTriangle(positionA, positionD, positionB);

            result.add(wallTriangle1);
            result.add(wallTriangle2);
        }
        return result;
    }

    /*private List<GaiaTriangle> createWall(List<Vector3d> roofPosition, List<Vector3d> floorPosition) {
        List<GaiaTriangle> result = new ArrayList<>();
        int size = roofPosition.size();
        for (int index = 0; index < size; index++) {
            int nextIndex = (index + 1) % size;
            log.info("index: {}, nextIndex: {}", index, nextIndex);

            Vector3d roofTriangle = roofPosition.get(index);
            Vector3d roofTriangleNext = roofPosition.get(nextIndex);
            Vector3d floorTriangle = floorPosition.get(index);
            Vector3d floorTriangleNext = floorPosition.get(nextIndex);

            GaiaTriangle wallTriangle1 = new GaiaTriangle(roofTriangle, floorTriangle, floorTriangleNext);
            GaiaTriangle wallTriangle2 = new GaiaTriangle(roofTriangle, floorTriangleNext, roofTriangleNext);

            result.add(wallTriangle1);
            result.add(wallTriangle2);
        }
        return result;
    }*/

    private List<Vector3d> resetPositionHeight(List<Vector3d> positions, double height) {
        return positions.stream().map((gaiaTriangle) -> {
            return new Vector3d(gaiaTriangle.x, gaiaTriangle.y, height);
        }).collect(Collectors.toList());
    }

    private List<GaiaTriangle> resetHeight(List<GaiaTriangle> triangles, double height, boolean reverse) {
        return triangles.stream().map((gaiaTriangle) -> {
            Vector3d positionA = new Vector3d(gaiaTriangle.getPositions()[0]);
            Vector3d positionB = gaiaTriangle.getPositions()[1];
            Vector3d positionC = gaiaTriangle.getPositions()[2];

            Vector3d positionD = new Vector3d(positionA.x, positionA.y, height);
            Vector3d positionE = new Vector3d(positionB.x, positionB.y, height);
            Vector3d positionF = new Vector3d(positionC.x, positionC.y, height);

            if (reverse) {
                return new GaiaTriangle(positionD, positionE, positionF);
            }
            return new GaiaTriangle(positionD, positionF, positionE);
        }).collect(Collectors.toList());
    }
}
