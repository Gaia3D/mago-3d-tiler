package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaMaterial;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class HalfEdgeMesh implements Serializable {
    @Setter
    @Getter
    private List<HalfEdgePrimitive> primitives = new ArrayList<>();
    private GaiaBoundingBox boundingBox = null;

    public void doTrianglesReduction(DecimateParameters decimateParameters) {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.doTrianglesReduction(decimateParameters);
        }
    }

    public void deleteObjects() {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.deleteObjects();
        }
        primitives.clear();
    }

    public void checkSandClockFaces() {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.checkSandClockFaces();
        }
    }

    public void transformPoints(Matrix4d finalMatrix) {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.transformPoints(finalMatrix);
        }
    }

    public void cutByPlane(PlaneType planeType, Vector3d planePosition, double error) {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.cutByPlane(planeType, planePosition, error);
        }
    }

    public void removeDeletedObjects()
    {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.removeDeletedObjects();
        }
    }

    public GaiaBoundingBox calculateBoundingBox(GaiaBoundingBox resultBBox) {
        if(resultBBox == null) {
            resultBBox = new GaiaBoundingBox();
        }
        for (HalfEdgePrimitive primitive : primitives) {
            resultBBox = primitive.calculateBoundingBox(resultBBox);
        }
        return resultBBox;
    }

    public GaiaBoundingBox getBoundingBox() {
        if (boundingBox == null) {
            boundingBox = calculateBoundingBox(null);
        }
        return boundingBox;
    }

    public void calculateNormals()
    {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.calculateNormals();
        }
    }

    public void classifyFacesIdByPlane(PlaneType planeType, Vector3d planePosition)
    {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.classifyFacesIdByPlane(planeType, planePosition);
        }
    }

    public void writeFile(ObjectOutputStream outputStream) {
        /*
        private List<HalfEdgePrimitive> primitives = new ArrayList<>();
        private GaiaBoundingBox boundingBox = null;
         */

        try {
            outputStream.writeInt(primitives.size());
            for (HalfEdgePrimitive primitive : primitives) {
                primitive.writeFile(outputStream);
            }
        } catch (Exception e) {
            log.error("Error Log : ", e);
        }
    }

    public void readFile(ObjectInputStream inputStream) {
        try {
            int primitivesSize = inputStream.readInt();
            for (int i = 0; i < primitivesSize; i++) {
                HalfEdgePrimitive primitive = new HalfEdgePrimitive();
                primitive.readFile(inputStream);
                primitives.add(primitive);
            }
        } catch (Exception e) {
            log.error("Error Log : ", e);
        }
    }

    public List<HalfEdgeSurface> extractSurfaces(List<HalfEdgeSurface> resultHalfEdgeSurfaces) {
        if (resultHalfEdgeSurfaces == null) {
            resultHalfEdgeSurfaces = new ArrayList<>();
        }

        for (HalfEdgePrimitive primitive : primitives) {
            primitive.extractSurfaces(resultHalfEdgeSurfaces);
        }

        return resultHalfEdgeSurfaces;
    }

    public void deleteFacesWithClassifyId(int classifyId) {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.deleteFacesWithClassifyId(classifyId);
        }
    }

    public HalfEdgeMesh clone()
    {
        HalfEdgeMesh clonedMesh = new HalfEdgeMesh();
        for (HalfEdgePrimitive primitive : primitives) {
            clonedMesh.primitives.add(primitive.clone());
        }
        return clonedMesh;
    }

    public void scissorTextures(List<GaiaMaterial> materials) {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.scissorTextures(materials);
        }
    }

    public int getTrianglesCount() {
        int trianglesCount = 0;
        for (HalfEdgePrimitive primitive : primitives) {
            trianglesCount += primitive.getTrianglesCount();
        }
        return trianglesCount;
    }

    public void setBoxTexCoordsXY(GaiaBoundingBox box) {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.setBoxTexCoordsXY(box);
        }
    }

    public void getUsedMaterialsIds(List<Integer> resultMaterialsIds) {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.getUsedMaterialsIds(resultMaterialsIds);
        }
    }

    public void setMaterialId(int materialId) {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.setMaterialId(materialId);
        }
    }

    public void weldVertices(double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
        }
    }

    public void translate(Vector3d translation) {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.translate(translation);
        }
    }

    public void doTrianglesReductionOneIteration(DecimateParameters decimateParameters) {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.doTrianglesReductionOneIteration(decimateParameters);
        }
    }

    public void splitFacesByBestPlanesToProject() {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.splitFacesByBestPlanesToProject();
        }
    }

    public void extractPrimitives(List<HalfEdgePrimitive> resultPrimitives) {
        for (HalfEdgePrimitive primitive : primitives) {
            resultPrimitives.add(primitive);
        }
    }
}
