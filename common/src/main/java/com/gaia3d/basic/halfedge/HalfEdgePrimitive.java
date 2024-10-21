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
public class HalfEdgePrimitive implements Serializable {
    private Integer accessorIndices = -1;
    private Integer materialIndex = -1;
    private List<HalfEdgeSurface> surfaces = new ArrayList<>();
    private List<HalfEdgeVertex> vertices = new ArrayList<>(); // vertices of all surfaces.***
    private GaiaBoundingBox boundingBox = null;

    public void doTrianglesReduction() {
        for (HalfEdgeSurface surface : surfaces) {
            surface.doTrianglesReduction();
        }

        // Remake vertices.***
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
        if(resultBBox == null) {
            resultBBox = new GaiaBoundingBox();
        }
        int surfacesCount = this.surfaces.size();
        for(int i = 0; i < surfacesCount; i++) {
            HalfEdgeSurface surface = this.surfaces.get(i);
            GaiaBoundingBox boundingBox = surface.getBoundingBox();
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

    public void classifyFacesIdByPlane(PlaneType planeType, Vector3d planePosition)
    {
        for (HalfEdgeSurface surface : surfaces) {
            surface.classifyFacesIdByPlane(planeType, planePosition);
        }
    }

    public void removeDeletedObjects()
    {
        for (HalfEdgeSurface surface : surfaces) {
            surface.removeDeletedObjects();
        }
    }

    public void setObjectIdsInList()
    {
        for (HalfEdgeSurface surface : surfaces) {
            surface.setObjectIdsInList();
        }
    }

    public void writeFile(ObjectOutputStream outputStream) {
        /*
        private Integer accessorIndices = -1;
        private Integer materialIndex = -1;
        private List<HalfEdgeSurface> surfaces = new ArrayList<>();
        private List<HalfEdgeVertex> vertices = new ArrayList<>(); // vertices of all surfaces.***
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    public void extractSurfaces(List<HalfEdgeSurface> resultHalfEdgeSurfaces) {
        resultHalfEdgeSurfaces.addAll(surfaces);
    }
}
