package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaPlane;
import com.gaia3d.basic.geometry.entities.GaiaTriangle;
import com.gaia3d.basic.model.GaiaFace;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaScene;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.joml.Vector4d;

@Slf4j
@Getter
@Setter
public class GaiaFaceData {
    // This class is used by GaiaOctree to store the face data.
    private GaiaFace face = null; // main data
    private GaiaScene sceneParent = null;
    private GaiaPrimitive primitiveParent = null;
    private GaiaBoundingBox boundingBox = null;
    private Vector3d centerPoint = null;
    private Vector4d primaryColor = null;
    private GaiaPlane plane = null;
    private GaiaTriangle triangle = null;

    public GaiaBoundingBox getBoundingBox() {
        if (boundingBox == null) {
            if (primitiveParent != null) {
                boundingBox = face.getBoundingBox(primitiveParent.getVertices(), new GaiaBoundingBox());
            } else {
                log.error("[ERROR][getBoundingBox] : primitiveParent is null.");
            }
        }
        return boundingBox;
    }

    public Vector3d getCenterPoint() {
        if (centerPoint == null) {
            if (boundingBox == null) {
                getBoundingBox();
            }
            centerPoint = boundingBox.getCenter();
        }
        return centerPoint;
    }

    public GaiaPlane getPlane() {
        if (plane == null) {
            Vector3d normal = face.getFaceNormal();
            if (normal == null) {
                face.calculateFaceNormal(primitiveParent.getVertices());
                normal = face.getFaceNormal();
            }
            Vector3d position = getCenterPoint();
            plane = new GaiaPlane(position, normal);
        }
        return plane;
    }
    
    public GaiaTriangle getTriangle() {
        if (triangle == null) {
            int[] indices = face.getIndices();
            Vector3d point1 = primitiveParent.getVertices().get(indices[0]).getPosition();
            Vector3d point2 = primitiveParent.getVertices().get(indices[1]).getPosition();
            Vector3d point3 = primitiveParent.getVertices().get(indices[2]).getPosition();
            triangle = new GaiaTriangle(point1, point2, point3);
        }
        return triangle;
    }
}
