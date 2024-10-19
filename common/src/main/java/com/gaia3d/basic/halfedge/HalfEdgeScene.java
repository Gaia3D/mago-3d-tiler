package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaMaterial;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class HalfEdgeScene {
    private Path originalPath;
    private GaiaBoundingBox gaiaBoundingBox;
    private GaiaAttribute attribute;
    private List<HalfEdgeNode> nodes = new ArrayList<>();
    private List<GaiaMaterial> materials = new ArrayList<>();
    private GaiaBoundingBox boundingBox = null;

    public void doTrianglesReduction() {
        for (HalfEdgeNode node : nodes) {
            node.doTrianglesReduction();
        }
    }

    public void deleteObjects() {
        for (HalfEdgeNode node : nodes) {
            node.deleteObjects();
        }
        nodes.clear();
        materials.clear();
    }

    public void checkSandClockFaces() {
        for (HalfEdgeNode node : nodes) {
            node.checkSandClockFaces();
        }
    }

    public void spendTransformationMatrix() {
        for (HalfEdgeNode node : nodes) {
            node.spendTransformationMatrix();
        }
    }

    public boolean cutByPlane(PlaneType planeType, Vector3d planePosition, double error)
    {
        // 1rst check if the plane intersects the bbox.***
        GaiaBoundingBox bbox = getBoundingBox();

        if (bbox == null) {
            return false;
        }

        if (planeType == PlaneType.XZ) {
            if (planePosition.y < bbox.getMinY() || planePosition.y > bbox.getMaxY()) {
                return false;
            }
        } else if (planeType == PlaneType.YZ) {
            if (planePosition.x < bbox.getMinX() || planePosition.x > bbox.getMaxX()) {
                return false;
            }
        } else if (planeType == PlaneType.XY) {
            if (planePosition.z < bbox.getMinZ() || planePosition.z > bbox.getMaxZ()) {
                return false;
            }
        }

        for (HalfEdgeNode node : nodes) {
            node.cutByPlane(planeType, planePosition, error);
        }

        return true;
    }

    public GaiaBoundingBox calculateBoundingBox(GaiaBoundingBox resultBBox) {
        if(resultBBox == null) {
            resultBBox = new GaiaBoundingBox();
        }
        for (HalfEdgeNode node : nodes) {
            resultBBox = node.calculateBoundingBox(resultBBox);
        }
        return resultBBox;
    }

    public GaiaBoundingBox getBoundingBox() {
        if (boundingBox == null) {
            boundingBox = calculateBoundingBox(null);
        }
        return boundingBox;
    }

    public void classifyFacesIdByPlane(PlaneType planeType, Vector3d planePosition)
    {
        for (HalfEdgeNode node : nodes) {
            node.classifyFacesIdByPlane(planeType, planePosition);
        }
    }

}
