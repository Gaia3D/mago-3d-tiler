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

@Setter
@Getter
@Slf4j
public class HalfEdgeMesh implements Serializable {
    private List<HalfEdgePrimitive> primitives = new ArrayList<>();
    private GaiaBoundingBox boundingBox = null;

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

    public void removeDeletedObjects() {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.removeDeletedObjects();
        }
    }

    public GaiaBoundingBox calculateBoundingBox(GaiaBoundingBox resultBBox) {
        if (resultBBox == null) {
            resultBBox = new GaiaBoundingBox();
        }
        for (HalfEdgePrimitive primitive : primitives) {
            GaiaBoundingBox primitiveBBox = primitive.calculateBoundingBox(null);
            if (primitiveBBox != null) {
                resultBBox.addBoundingBox(primitiveBBox);
            }
        }
        return resultBBox;
    }

    public GaiaBoundingBox getBoundingBox() {
        if (boundingBox == null) {
            boundingBox = calculateBoundingBox(null);
        }
        return boundingBox;
    }

    public void calculateNormals() {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.calculateNormals();
        }
    }

    public void classifyFacesIdByPlane(PlaneType planeType, Vector3d planePosition) {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.classifyFacesIdByPlane(planeType, planePosition);
        }
    }

    public void writeFile(ObjectOutputStream outputStream) {
        try {
            outputStream.writeInt(primitives.size());
            for (HalfEdgePrimitive primitive : primitives) {
                primitive.writeFile(outputStream);
            }
        } catch (Exception e) {
            log.error("[ERROR] Error Log : ", e);
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
            log.error("[ERROR] Error Log : ", e);
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

    public HalfEdgeMesh cloneByClassifyId(int classifyId) {
        HalfEdgeMesh clonedMesh = null;
        for (HalfEdgePrimitive primitive : primitives) {
            HalfEdgePrimitive clonedPrimitive = primitive.cloneByClassifyId(classifyId);
            if (clonedPrimitive != null) {
                if (clonedMesh == null) {
                    clonedMesh = new HalfEdgeMesh();
                }
                clonedMesh.primitives.add(clonedPrimitive);
            }
        }
        return clonedMesh;
    }

    public HalfEdgeMesh clone() {
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

    public void scissorTexturesByMotherScene(List<GaiaMaterial> thisMaterials, List<GaiaMaterial> motherMaterials) {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.scissorTexturesByMotherScene(thisMaterials, motherMaterials);
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

    public void decimate(DecimateParameters decimateParameters) {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.decimate(decimateParameters);
        }
    }

    public void splitFacesByBestObliqueCameraDirectionToProject() {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.splitFacesByBestObliqueCameraDirectionToProject();
        }
    }

    public void extractPrimitives(List<HalfEdgePrimitive> resultPrimitives) {
        resultPrimitives.addAll(primitives);
    }

    public void getWestEastSouthNorthVertices(GaiaBoundingBox bbox, List<HalfEdgeVertex> westVertices,
                                              List<HalfEdgeVertex> eastVertices,
                                              List<HalfEdgeVertex> southVertices,
                                              List<HalfEdgeVertex> northVertices, double error) {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.getWestEastSouthNorthVertices(bbox, westVertices, eastVertices, southVertices, northVertices, error);
        }
    }


    public double calculateArea() {
        double area = 0;
        for (HalfEdgePrimitive primitive : primitives) {
            area += primitive.calculateArea();
        }
        return area;
    }


    public int deleteDegeneratedFaces() {
        int deletedFacesCount = 0;
        for (HalfEdgePrimitive primitive : primitives) {
            deletedFacesCount += primitive.deleteDegeneratedFaces();
        }
        return deletedFacesCount;
    }

    public void translateTexCoordsToPositiveQuadrant() {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.translateTexCoordsToPositiveQuadrant();
        }
    }

    public void updateVerticesList() {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.updateVerticesList();
        }
    }

    public void updateFacesList() {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.updateFacesList();
        }
    }

    public int getFacesCount() {
        int facesCount = 0;
        for (HalfEdgePrimitive primitive : primitives) {
            facesCount += primitive.getFacesCount();
        }
        return facesCount;
    }

    public void getIntersectedFacesByPlane(PlaneType planeType, Vector3d planePosition, List<HalfEdgeFace> resultFaces, double error) {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.getIntersectedFacesByPlane(planeType, planePosition, resultFaces, error);
        }
    }
}
