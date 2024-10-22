package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class HalfEdgeNode implements Serializable {
    private HalfEdgeNode parent = null;
    private Matrix4d transformMatrix = new Matrix4d();
    private Matrix4d preMultipliedTransformMatrix = new Matrix4d();
    private List<HalfEdgeMesh> meshes = new ArrayList<>();
    private List<HalfEdgeNode> children = new ArrayList<>();
    private GaiaBoundingBox boundingBox = null;

    public void doTrianglesReduction() {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.doTrianglesReduction();
        }
        for (HalfEdgeNode child : children) {
            child.doTrianglesReduction();
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

        if(finalMatrix.equals(identity)) {
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

    public List<HalfEdgeSurface> extractSurfaces(List<HalfEdgeSurface> resultHalfEdgeSurfaces)
    {
        if(resultHalfEdgeSurfaces == null) {
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
        if(resultBBox == null) {
            resultBBox = new GaiaBoundingBox();
        }
        for (HalfEdgeMesh mesh : meshes) {
            resultBBox = mesh.calculateBoundingBox(resultBBox);
        }
        for (HalfEdgeNode child : children) {
            resultBBox = child.calculateBoundingBox(resultBBox);
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
        for (HalfEdgeMesh mesh : meshes) {
            mesh.classifyFacesIdByPlane(planeType, planePosition);
        }
        for (HalfEdgeNode child : children) {
            child.classifyFacesIdByPlane(planeType, planePosition);
        }
    }

    public void writeFile(ObjectOutputStream outputStream) {
        try {
            /*
            private HalfEdgeNode parent = null;
            private Matrix4d transformMatrix = new Matrix4d();
            private Matrix4d preMultipliedTransformMatrix = new Matrix4d();
            private List<HalfEdgeMesh> meshes = new ArrayList<>();
            private List<HalfEdgeNode> children = new ArrayList<>();
            private GaiaBoundingBox boundingBox = null;
             */
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }
}
