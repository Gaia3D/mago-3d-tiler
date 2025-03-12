package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaScene;
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

@Getter
@Setter
@Slf4j
public class HalfEdgePrimitive implements Serializable {
    private Integer accessorIndices = -1;
    private Integer materialIndex = -1;
    private List<HalfEdgeSurface> surfaces = new ArrayList<>();
    private List<HalfEdgeVertex> vertices = new ArrayList<>(); // vertices of all surfaces
    private GaiaBoundingBox boundingBox = null;

    public void doTrianglesReduction(DecimateParameters decimateParameters) {
        for (HalfEdgeSurface surface : surfaces) {
            surface.doTrianglesReduction(decimateParameters);
        }

        // Remake vertices
        vertices.clear();
        for (HalfEdgeSurface surface : surfaces) {
            this.vertices.addAll(surface.getVertices());
        }
    }

    public List<HalfEdgeVertex> getVertices() {
        return calculateVertices();
    }

    public List<HalfEdgeVertex> calculateVertices() {
        vertices.clear();
        for (HalfEdgeSurface surface : surfaces) {
            vertices.addAll(surface.getVertices());
        }
        return vertices;
    }

    public void calculateNormals() {
        for (HalfEdgeSurface surface : surfaces) {
            surface.calculateNormals();
        }
    }

    public void deleteObjects() {
        for (HalfEdgeSurface surface : surfaces) {
            surface.deleteObjects();
        }
        surfaces.clear();
        vertices.clear();
    }

    public void checkSandClockFaces() {
        for (HalfEdgeSurface surface : surfaces) {
            surface.checkSandClockFaces();
        }
    }

    public void transformPoints(Matrix4d finalMatrix) {
        for (HalfEdgeSurface surface : surfaces) {
            surface.transformPoints(finalMatrix);
        }
    }

    public void cutByPlane(PlaneType planeType, Vector3d planePosition, double error) {
        for (HalfEdgeSurface surface : surfaces) {
            surface.cutByPlane(planeType, planePosition, error);
        }

        vertices.clear();
    }

    public GaiaBoundingBox calculateBoundingBox(GaiaBoundingBox resultBBox) {
        for (HalfEdgeSurface surface : this.surfaces) {
            GaiaBoundingBox boundingBox = surface.getBoundingBox();
            if (boundingBox == null) {
                continue;
            }

            if (resultBBox == null) {
                resultBBox = new GaiaBoundingBox();
            }
            resultBBox.addBoundingBox(boundingBox);
        }
        return resultBBox;
    }

    public GaiaBoundingBox getBoundingBox() {
        if (this.boundingBox == null) {
            this.boundingBox = calculateBoundingBox(null);
        }
        return this.boundingBox;
    }

    public void classifyFacesIdByPlane(PlaneType planeType, Vector3d planePosition) {
        for (HalfEdgeSurface surface : surfaces) {
            surface.classifyFacesIdByPlane(planeType, planePosition);
        }
    }

    public void removeDeletedObjects() {
        for (HalfEdgeSurface surface : surfaces) {
            surface.removeDeletedObjects();
        }
    }

    public void setObjectIdsInList() {
        for (HalfEdgeSurface surface : surfaces) {
            surface.setObjectIdsInList();
        }
    }

    public void writeFile(ObjectOutputStream outputStream) {
        /*
        private Integer accessorIndices = -1;
        private Integer materialIndex = -1;
        private List<HalfEdgeSurface> surfaces = new ArrayList<>();
        private List<HalfEdgeVertex> vertices = new ArrayList<>(); // vertices of all surfaces
        private GaiaBoundingBox boundingBox = null;
         */

        try {
            // accessorIndices
            outputStream.writeInt(accessorIndices);
            // materialIndex
            outputStream.writeInt(materialIndex);
            // surfaces
            outputStream.writeInt(surfaces.size());
            for (HalfEdgeSurface surface : surfaces) {
                surface.writeFile(outputStream);
            }
        } catch (Exception e) {
            log.error("[ERROR] Error Log : ", e);
        }
    }

    public void readFile(ObjectInputStream inputStream) {
        try {
            // accessorIndices
            accessorIndices = inputStream.readInt();
            // materialIndex
            materialIndex = inputStream.readInt();
            // surfaces
            int surfacesCount = inputStream.readInt();
            for (int i = 0; i < surfacesCount; i++) {
                HalfEdgeSurface surface = new HalfEdgeSurface();
                surface.readFile(inputStream);
                surfaces.add(surface);
            }
        } catch (Exception e) {
            log.error("[ERROR] Error Log : ", e);
        }
    }

    public void extractSurfaces(List<HalfEdgeSurface> resultHalfEdgeSurfaces) {
        resultHalfEdgeSurfaces.addAll(surfaces);
    }

    public void deleteFacesWithClassifyId(int classifyId) {
        for (HalfEdgeSurface surface : surfaces) {
            surface.deleteFacesWithClassifyId(classifyId);
        }
    }

    public void scissorTextures(List<GaiaMaterial> materials) {
        int matId = this.materialIndex;
        if (matId < 0 || matId >= materials.size()) {
            return;
        }

        GaiaMaterial material = materials.get(matId);
        if (material == null) {
            return;
        }

        for (HalfEdgeSurface surface : surfaces) {
            surface.scissorTextures(material);
        }

    }

    public void scissorTexturesByMotherScene(List<GaiaMaterial> thisMaterials, List<GaiaMaterial> motherMaterials) {
        int matId = this.materialIndex;
        if (matId < 0 || matId >= thisMaterials.size()) {
            return;
        }

        GaiaMaterial material = thisMaterials.get(matId);
        if (material == null) {
            return;
        }

        GaiaMaterial motherMaterial = motherMaterials.get(matId);
        if (motherMaterial == null) {
            return;
        }

        for (HalfEdgeSurface surface : surfaces) {
            surface.scissorTexturesByMotherScene(material, motherMaterial);
        }
    }

    public HalfEdgePrimitive cloneByClassifyId(int classifyId) {
        HalfEdgePrimitive cloned = null;
        for (HalfEdgeSurface surface : surfaces) {
            HalfEdgeSurface clonedSurface = surface.cloneByClassifyId(classifyId);
            if (clonedSurface != null) {
                if (cloned == null) {
                    cloned = new HalfEdgePrimitive();
                    cloned.setAccessorIndices(accessorIndices);
                    cloned.setMaterialIndex(materialIndex);
                }
                cloned.getSurfaces().add(clonedSurface);
            }
        }
        return cloned;
    }

    public HalfEdgePrimitive clone() {
        HalfEdgePrimitive cloned = new HalfEdgePrimitive();
        cloned.setAccessorIndices(accessorIndices);
        cloned.setMaterialIndex(materialIndex);
        for (HalfEdgeSurface surface : surfaces) {
            cloned.getSurfaces().add(surface.clone());
        }
        return cloned;
    }

    public int getTrianglesCount() {
        int trianglesCount = 0;
        for (HalfEdgeSurface surface : surfaces) {
            trianglesCount += surface.getTrianglesCount();
        }
        return trianglesCount;
    }

    public void setBoxTexCoordsXY(GaiaBoundingBox box) {
        for (HalfEdgeSurface surface : surfaces) {
            surface.setBoxTexCoordsXY(box);
        }
    }

    public void getUsedMaterialsIds(List<Integer> resultMaterialsIds) {
        if (resultMaterialsIds == null) {
            resultMaterialsIds = new ArrayList<>();
        }

        int matId = this.materialIndex;
        if (matId >= 0 && !resultMaterialsIds.contains(matId)) {
            resultMaterialsIds.add(matId);
        }
    }

    public void setMaterialId(int materialId) {
        this.materialIndex = materialId;
    }

    public void weldVertices(double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        for (HalfEdgeSurface surface : surfaces) {
            surface.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
        }
    }

    public void translate(Vector3d translation) {
        for (HalfEdgeSurface surface : surfaces) {
            surface.translate(translation);
        }
    }

    public void doTrianglesReductionOneIteration(DecimateParameters decimateParameters) {
        for (HalfEdgeSurface surface : surfaces) {
            surface.doTrianglesReductionOneIteration(decimateParameters);
        }
    }

    public void splitFacesByBestObliqueCameraDirectionToProject() {
        for (HalfEdgeSurface surface : surfaces) {
            surface.splitFacesByBestObliqueCameraDirectionToProject();
        }
    }

    public void getWestEastSouthNorthVertices(GaiaBoundingBox bbox, List<HalfEdgeVertex> westVertices, List<HalfEdgeVertex> eastVertices, List<HalfEdgeVertex> southVertices, List<HalfEdgeVertex> northVertices, double error) {
        for (HalfEdgeSurface surface : surfaces) {
            surface.getWestEastSouthNorthVertices(bbox, westVertices, eastVertices, southVertices, northVertices, error);
        }
    }


    public double calculateArea() {
        double area = 0;
        for (HalfEdgeSurface surface : surfaces) {
            area += surface.calculateArea();
        }
        return area;
    }


    public int deleteDegeneratedFaces() {
        int deletedFacesCount = 0;
        for (HalfEdgeSurface surface : surfaces) {
            deletedFacesCount += surface.deleteDegeneratedFaces();
        }
        return deletedFacesCount;
    }

    public GaiaRectangle getTexCoordinateBoundingRectangle(GaiaRectangle resultRectangle) {
        if (resultRectangle == null) {
            resultRectangle = new GaiaRectangle();
        }
        for (HalfEdgeSurface surface : surfaces) {
            resultRectangle = surface.getTexCoordinateBoundingRectangle(resultRectangle);
        }
        return resultRectangle;
    }

    public void translateTexCoordsToPositiveQuadrant() {
        GaiaRectangle texCoordBoundingRectangle = getTexCoordinateBoundingRectangle(null);
        if (texCoordBoundingRectangle == null) {
            return;
        }

        double rectWidth = texCoordBoundingRectangle.getWidth();
        double rectHeight = texCoordBoundingRectangle.getHeight();

        double minX = texCoordBoundingRectangle.getMinX();
        double minY = texCoordBoundingRectangle.getMinY();

        double maxX = texCoordBoundingRectangle.getMaxX();
        double maxY = texCoordBoundingRectangle.getMaxY();

        if (minX < 0 || minY < 0) {
            int hola = 0;
        }

        if (minX > 1 || minY > 1) {
            int hola = 0;
        }


        int hola = 0;
    }

    public void updateVerticesList() {
        vertices.clear();
        for (HalfEdgeSurface surface : surfaces) {
            surface.updateVerticesList();
            vertices.addAll(surface.getVertices());
        }
    }

    public void updateFacesList() {
        for (HalfEdgeSurface surface : surfaces) {
            surface.updateFacesList();
        }
    }

    public int getFacesCount() {
        int facesCount = 0;
        for (HalfEdgeSurface surface : surfaces) {
            facesCount += surface.getFacesCount();
        }
        return facesCount;
    }

    public void getIntersectedFacesByPlane(PlaneType planeType, Vector3d planePosition, List<HalfEdgeFace> resultFaces, double error) {
        for (HalfEdgeSurface surface : surfaces) {
            surface.getIntersectedFacesByPlane(planeType, planePosition, resultFaces, error);
        }
    }
}
