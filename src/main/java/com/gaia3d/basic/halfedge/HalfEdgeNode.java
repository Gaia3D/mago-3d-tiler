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

@Getter
@Setter
@Slf4j
public class HalfEdgeNode implements Serializable {
    private HalfEdgeNode parent = null;
    private Matrix4d transformMatrix = new Matrix4d();
    private Matrix4d preMultipliedTransformMatrix = new Matrix4d();
    private List<HalfEdgeMesh> meshes = new ArrayList<>();
    private List<HalfEdgeNode> children = new ArrayList<>();
    private GaiaBoundingBox boundingBox = null;

    public void doTrianglesReduction(DecimateParameters decimateParameters) {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.doTrianglesReduction(decimateParameters);
        }
        for (HalfEdgeNode child : children) {
            child.doTrianglesReduction(decimateParameters);
        }
    }

    public void deleteObjects() {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.deleteObjects();
        }
        meshes.clear();
        for (HalfEdgeNode child : children) {
            child.deleteObjects();
        }
        children.clear();
    }

    public void checkSandClockFaces() {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.checkSandClockFaces();
        }
        for (HalfEdgeNode child : children) {
            child.checkSandClockFaces();
        }
    }

    public void calculateNormals() {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.calculateNormals();
        }
        for (HalfEdgeNode child : children) {
            child.calculateNormals();
        }
    }

    public Matrix4d getFinalTransformMatrix() {
        Matrix4d finalMatrix = new Matrix4d();
        finalMatrix.set(transformMatrix);
        if (parent != null) {
            finalMatrix.mul(parent.getFinalTransformMatrix());
        }
        return finalMatrix;
    }

    public void spendTransformationMatrix() {
        Matrix4d finalMatrix = getFinalTransformMatrix();
        Matrix4d identity = new Matrix4d();
        identity.identity();

        if (finalMatrix.equals(identity)) {
            return;
        }

        for (HalfEdgeMesh mesh : meshes) {
            mesh.transformPoints(finalMatrix);
        }
        for (HalfEdgeNode child : children) {
            child.spendTransformationMatrix();
        }

        // Clear the transform matrix.
        transformMatrix.identity();
    }

    public void cutByPlane(PlaneType planeType, Vector3d planePosition, double error) {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.cutByPlane(planeType, planePosition, error);
        }
        for (HalfEdgeNode child : children) {
            child.cutByPlane(planeType, planePosition, error);
        }
    }

    public void removeDeletedObjects() {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.removeDeletedObjects();
        }
        for (HalfEdgeNode child : children) {
            child.removeDeletedObjects();
        }
    }

    public List<HalfEdgeSurface> extractSurfaces(List<HalfEdgeSurface> resultHalfEdgeSurfaces) {
        if (resultHalfEdgeSurfaces == null) {
            resultHalfEdgeSurfaces = new ArrayList<>();
        }
        for (HalfEdgeMesh mesh : meshes) {
            resultHalfEdgeSurfaces = mesh.extractSurfaces(resultHalfEdgeSurfaces);
        }
        for (HalfEdgeNode child : children) {
            resultHalfEdgeSurfaces = child.extractSurfaces(resultHalfEdgeSurfaces);
        }
        return resultHalfEdgeSurfaces;
    }

    public GaiaBoundingBox calculateBoundingBox(GaiaBoundingBox resultBBox) {
//        if (resultBBox == null) {
//            resultBBox = new GaiaBoundingBox();
//        }
//        for (HalfEdgeMesh mesh : meshes) {
//            resultBBox = mesh.calculateBoundingBox(resultBBox);
//        }
//        for (HalfEdgeNode child : children) {
//            resultBBox = child.calculateBoundingBox(resultBBox);
//        }
//        return resultBBox;
        GaiaBoundingBox boundingBox = null;
//        Matrix4d transformMatrix = new Matrix4d(this.transformMatrix);
//        if (parentTransformMatrix != null) {
//            parentTransformMatrix.mul(transformMatrix, transformMatrix);
//        }
        for (HalfEdgeMesh mesh : this.getMeshes()) {
            GaiaBoundingBox meshBoundingBox = mesh.calculateBoundingBox(null);
            if (meshBoundingBox == null) {
                continue;
            }
            if (boundingBox == null) {
                boundingBox = meshBoundingBox;
            } else {
                boundingBox.addBoundingBox(meshBoundingBox);
            }
        }
        for (HalfEdgeNode child : this.getChildren()) {
            GaiaBoundingBox childBoundingBox = child.calculateBoundingBox(null);
            if (childBoundingBox == null) {
                continue;
            }
            if (boundingBox == null) {
                boundingBox = childBoundingBox;
            } else {
                boundingBox.addBoundingBox(childBoundingBox);
            }
        }
        return boundingBox;
    }

    public GaiaBoundingBox getBoundingBox() {
        if (boundingBox == null) {
            boundingBox = calculateBoundingBox(null);
        }
        return boundingBox;
    }

    public void classifyFacesIdByPlane(PlaneType planeType, Vector3d planePosition) {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.classifyFacesIdByPlane(planeType, planePosition);
        }
        for (HalfEdgeNode child : children) {
            child.classifyFacesIdByPlane(planeType, planePosition);
        }
    }

    public void deleteFacesWithClassifyId(int classifyId) {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.deleteFacesWithClassifyId(classifyId);
        }
        for (HalfEdgeNode child : children) {
            child.deleteFacesWithClassifyId(classifyId);
        }
    }

    public HalfEdgeNode cloneByClassifyId(int classifyId) {
        HalfEdgeNode clonedNode = null;

        for (HalfEdgeMesh mesh : meshes) {
            HalfEdgeMesh clonedMesh = mesh.cloneByClassifyId(classifyId);
            if (clonedMesh != null) {
                if (clonedNode == null) {
                    clonedNode = new HalfEdgeNode();
                    clonedNode.transformMatrix = new Matrix4d(transformMatrix);
                    clonedNode.preMultipliedTransformMatrix = new Matrix4d(preMultipliedTransformMatrix);
                }
                clonedNode.meshes.add(clonedMesh);
            }
        }
        for (HalfEdgeNode child : children) {
            HalfEdgeNode clonedChild = child.cloneByClassifyId(classifyId);
            if (clonedChild != null) {
                if (clonedNode == null) {
                    clonedNode = new HalfEdgeNode();
                    clonedNode.transformMatrix = new Matrix4d(transformMatrix);
                    clonedNode.preMultipliedTransformMatrix = new Matrix4d(preMultipliedTransformMatrix);
                }
                clonedChild.parent = clonedNode;
                clonedNode.children.add(clonedChild);
            }
        }
//        if (boundingBox != null && clonedNode != null) {
//            clonedNode.boundingBox = boundingBox.clone();
//        }
        return clonedNode;
    }

    public HalfEdgeNode clone() {
        HalfEdgeNode clonedNode = new HalfEdgeNode();
        clonedNode.transformMatrix = new Matrix4d(transformMatrix);
        clonedNode.preMultipliedTransformMatrix = new Matrix4d(preMultipliedTransformMatrix);
        for (HalfEdgeMesh mesh : meshes) {
            clonedNode.meshes.add(mesh.clone());
        }
        for (HalfEdgeNode child : children) {
            HalfEdgeNode clonedChild = child.clone();
            clonedChild.parent = clonedNode;
            clonedNode.children.add(clonedChild);
        }
        if (boundingBox != null) {
            clonedNode.boundingBox = boundingBox.clone();
        }
        return clonedNode;
    }

    public void writeFile(ObjectOutputStream outputStream) {
        try {
            // transformMatrix
            outputStream.writeObject(transformMatrix);
            // preMultipliedTransformMatrix
            outputStream.writeObject(preMultipliedTransformMatrix);
            // meshes
            outputStream.writeInt(meshes.size());
            for (HalfEdgeMesh mesh : meshes) {
                mesh.writeFile(outputStream);
            }

            // children
            outputStream.writeInt(children.size());
            for (HalfEdgeNode child : children) {
                child.writeFile(outputStream);
            }

        } catch (Exception e) {
            log.error("[ERROR] Error Log : ", e);
        }
    }

    public void readFile(ObjectInputStream inputStream) {
        try {
            // transformMatrix
            transformMatrix = (Matrix4d) inputStream.readObject();
            // preMultipliedTransformMatrix
            preMultipliedTransformMatrix = (Matrix4d) inputStream.readObject();
            // meshes
            int meshesSize = inputStream.readInt();
            for (int i = 0; i < meshesSize; i++) {
                HalfEdgeMesh mesh = new HalfEdgeMesh();
                mesh.readFile(inputStream);
                meshes.add(mesh);
            }

            // children
            int childrenSize = inputStream.readInt();
            for (int i = 0; i < childrenSize; i++) {
                HalfEdgeNode child = new HalfEdgeNode();
                child.readFile(inputStream);
                children.add(child);
            }

        } catch (Exception e) {
            log.error("[ERROR] Error Log : ", e);
        }
    }

    public void scissorTextures(List<GaiaMaterial> materials) {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.scissorTextures(materials);
        }
        for (HalfEdgeNode child : children) {
            child.scissorTextures(materials);
        }
    }

    public void scissorTexturesByMotherScene(List<GaiaMaterial> materials, List<GaiaMaterial> motherMaterials) {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.scissorTexturesByMotherScene(materials, motherMaterials);
        }
        for (HalfEdgeNode child : children) {
            child.scissorTexturesByMotherScene(materials, motherMaterials);
        }
    }

    public int getTrianglesCount() {
        int trianglesCount = 0;
        for (HalfEdgeMesh mesh : meshes) {
            trianglesCount += mesh.getTrianglesCount();
        }
        for (HalfEdgeNode child : children) {
            trianglesCount += child.getTrianglesCount();
        }
        return trianglesCount;
    }

    public void setBoxTexCoordsXY(GaiaBoundingBox box) {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.setBoxTexCoordsXY(box);
        }
        for (HalfEdgeNode child : children) {
            child.setBoxTexCoordsXY(box);
        }
    }

    public void getUsedMaterialsIds(List<Integer> resultMaterialsIds) {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.getUsedMaterialsIds(resultMaterialsIds);
        }
        for (HalfEdgeNode child : children) {
            child.getUsedMaterialsIds(resultMaterialsIds);
        }
    }

    public void setMaterialId(int materialId) {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.setMaterialId(materialId);
        }
        for (HalfEdgeNode child : children) {
            child.setMaterialId(materialId);
        }
    }

    public void weldVertices(double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
        }
        for (HalfEdgeNode child : children) {
            child.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
        }
    }

    public void translate(Vector3d translation) {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.translate(translation);
        }
        for (HalfEdgeNode child : children) {
            child.translate(translation);
        }
    }

    public void doTrianglesReductionOneIteration(DecimateParameters decimateParameters) {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.doTrianglesReductionOneIteration(decimateParameters);
        }
        for (HalfEdgeNode child : children) {
            child.doTrianglesReductionOneIteration(decimateParameters);
        }
    }

    public void splitFacesByBestObliqueCameraDirectionToProject() {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.splitFacesByBestObliqueCameraDirectionToProject();
        }
        for (HalfEdgeNode child : children) {
            child.splitFacesByBestObliqueCameraDirectionToProject();
        }
    }

    public void extractPrimitives(List<HalfEdgePrimitive> resultPrimitives) {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.extractPrimitives(resultPrimitives);
        }
        for (HalfEdgeNode child : children) {
            child.extractPrimitives(resultPrimitives);
        }
    }

    public void getWestEastSouthNorthVertices(GaiaBoundingBox bbox, List<HalfEdgeVertex> westVertices, List<HalfEdgeVertex> eastVertices, List<HalfEdgeVertex> southVertices, List<HalfEdgeVertex> northVertices, double error) {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.getWestEastSouthNorthVertices(bbox, westVertices, eastVertices, southVertices, northVertices, error);
        }

        for (HalfEdgeNode child : children) {
            child.getWestEastSouthNorthVertices(bbox, westVertices, eastVertices, southVertices, northVertices, error);
        }
    }


    public double calculateArea() {
        double area = 0;
        for (HalfEdgeMesh mesh : meshes) {
            area += mesh.calculateArea();
        }
        for (HalfEdgeNode child : children) {
            area += child.calculateArea();
        }
        return area;
    }


    public int deleteDegeneratedFaces() {
        int deletedFaces = 0;
        for (HalfEdgeMesh mesh : meshes) {
            deletedFaces += mesh.deleteDegeneratedFaces();
        }
        for (HalfEdgeNode child : children) {
            deletedFaces += child.deleteDegeneratedFaces();
        }
        return deletedFaces;
    }

    public void translateTexCoordsToPositiveQuadrant() {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.translateTexCoordsToPositiveQuadrant();
        }
        for (HalfEdgeNode child : children) {
            child.translateTexCoordsToPositiveQuadrant();
        }
    }

    public void updateVerticesList() {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.updateVerticesList();
        }
        for (HalfEdgeNode child : children) {
            child.updateVerticesList();
        }
    }

    public void updateFacesList() {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.updateFacesList();
        }
        for (HalfEdgeNode child : children) {
            child.updateFacesList();
        }
    }

    public int getFacesCount() {
        int facesCount = 0;
        for (HalfEdgeMesh mesh : meshes) {
            facesCount += mesh.getFacesCount();
        }
        for (HalfEdgeNode child : children) {
            facesCount += child.getFacesCount();
        }
        return facesCount;
    }

    public void getIntersectedFacesByPlane(PlaneType planeType, Vector3d planePosition, List<HalfEdgeFace> resultFaces, double error) {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.getIntersectedFacesByPlane(planeType, planePosition, resultFaces, error);
        }
        for (HalfEdgeNode child : children) {
            child.getIntersectedFacesByPlane(planeType, planePosition, resultFaces, error);
        }
    }
}
