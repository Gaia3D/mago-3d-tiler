package com.gaia3d.converter;

import com.gaia3d.basic.geometry.GaiaTriangle;
import com.gaia3d.basic.geometry.network.modeler.TopologicalNetwork;
import com.gaia3d.basic.geometry.tessellator.GaiaExtrusionSurface;
import com.gaia3d.basic.geometry.tessellator.GaiaTessellator;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.pipe.GaiaPipeLineString;
import com.gaia3d.basic.pipe.Modeler3D;
import com.gaia3d.basic.pipe.PipeElbow;
import com.gaia3d.basic.pipe.PipeType;
import com.gaia3d.basic.types.Classification;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.util.GeometryUtils;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.feature.simple.SimpleFeature;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector4d;

import java.io.File;
import java.util.*;
import java.util.stream.IntStream;

@Slf4j
public abstract class AbstractGeometryConverter {

    protected abstract List<GaiaScene> convert(File file);

    protected GaiaScene initScene(File file) {
        GaiaScene scene = new GaiaScene();

        GaiaAttribute attribute = new GaiaAttribute();
        attribute.setIdentifier(UUID.randomUUID());
        attribute.setFileName(file.getName());
        String ROOT_NODE_NAME = "Extruded Root Node";
        attribute.setNodeName(ROOT_NODE_NAME);
        scene.setAttribute(attribute);

        Vector4d color = new Vector4d(0.8, 0.8, 0.8, 1);
        GaiaMaterial defaultMaterial = createMaterial(0, color);
        GaiaMaterial doorMaterial = createMaterial(1, Classification.DOOR.getColor());
        GaiaMaterial windowMaterial = createMaterial(2, Classification.WINDOW.getColor());
        GaiaMaterial floorMaterial = createMaterial(2, Classification.FLOOR.getColor());
        GaiaMaterial roofMaterial = createMaterial(3, Classification.ROOF.getColor());
        GaiaMaterial waterMaterial = createMaterial(4, Classification.WATER.getColor());
        GaiaMaterial groundMaterial = createMaterial(5, Classification.GROUND.getColor());

        scene.getMaterials().add(defaultMaterial);
        scene.getMaterials().add(doorMaterial);
        scene.getMaterials().add(windowMaterial);
        scene.getMaterials().add(floorMaterial);
        scene.getMaterials().add(roofMaterial);
        scene.getMaterials().add(waterMaterial);
        scene.getMaterials().add(groundMaterial);

        GaiaNode rootNode = new GaiaNode();
        Matrix4d transformMatrix = new Matrix4d();
        transformMatrix.identity();
        rootNode.setTransformMatrix(transformMatrix);
        rootNode.setName(ROOT_NODE_NAME);
        scene.getNodes().add(rootNode);

        return scene;
    }

    protected GaiaMaterial createMaterial(int id, Vector4d color) {
        GaiaMaterial material = new GaiaMaterial();
        material.setId(id);
        material.setName("extrusion-model-material");
        material.setDiffuseColor(color);
        Map<TextureType, List<GaiaTexture>> textureTypeListMap = material.getTextures();
        textureTypeListMap.put(TextureType.DIFFUSE, new ArrayList<>());
        return material;
    }

    protected GaiaNode createNode(GaiaMaterial material, List<Vector3d> positions, List<GaiaTriangle> triangles) {
        GaiaNode node = new GaiaNode();
        node.setTransformMatrix(new Matrix4d().identity());
        GaiaMesh mesh = new GaiaMesh();
        GaiaPrimitive primitive = createPrimitives(material, positions, triangles);
        mesh.getPrimitives().add(primitive);
        node.getMeshes().add(mesh);
        return node;
    }

    protected GaiaPrimitive createPrimitives(GaiaMaterial material, List<Vector3d> positions, List<GaiaTriangle> triangles) {
        GaiaPrimitive primitive = new GaiaPrimitive();
        List<GaiaSurface> surfaces = new ArrayList<>();
        List<GaiaVertex> vertices = new ArrayList<>();
        primitive.setMaterialIndex(material.getId());
        primitive.setSurfaces(surfaces);
        primitive.setVertices(vertices);

        GaiaSurface surface = new GaiaSurface();
        Vector3d[] normals = new Vector3d[positions.size()];
        for (int i = 0; i < normals.length; i++) {
            normals[i] = new Vector3d(0, 0, 0);
        }

        for (GaiaTriangle triangle : triangles) {
            GaiaFace face = new GaiaFace();
            Vector3d[] trianglePositions = triangle.getPositions();
            int[] indices = new int[trianglePositions.length];

            indices[0] = indexOf(positions, trianglePositions[0]);
            indices[1] = indexOf(positions, trianglePositions[1]);
            indices[2] = indexOf(positions, trianglePositions[2]);

            normals[indices[0]] = triangle.getNormal();
            normals[indices[1]] = triangle.getNormal();
            normals[indices[2]] = triangle.getNormal();

            face.setIndices(indices);
            surface.getFaces().add(face);
        }

        for (int i = 0; i < positions.size(); i++) {
            Vector3d position = positions.get(i);
            Vector3d normal = normals[i];

            GaiaVertex vertex = new GaiaVertex();
            vertex.setPosition(new Vector3d(position.x, position.y, position.z));
            vertex.setNormal(normal);
            vertices.add(vertex);
        }
        surfaces.add(surface);
        return primitive;
    }

    protected int indexOf(List<Vector3d> positions, Vector3d item) {
        //return positions.indexOf(item);
        IntStream intStream = IntStream.range(0, positions.size());
        int result = intStream.filter(i -> positions.get(i) == item).findFirst().orElse(-1);
        intStream.close();
        return result;
    }

    protected double getHeight(SimpleFeature feature, String column, double minimumHeight) {
        double result = 0.0d;
        Object heightLower = feature.getAttribute(column);
        Object heightUpper = feature.getAttribute(column.toUpperCase());
        Object heightObject = null;
        if (heightLower != null) {
            heightObject = heightLower;
        } else if (heightUpper != null) {
            heightObject = heightUpper;
        } else {
            return minimumHeight;
        }

        if (heightObject instanceof Short) {
            result = result + (short) heightObject;
        } else if (heightObject instanceof Integer) {
            result = result + (int) heightObject;
        } else if (heightObject instanceof Long) {
            result = result + (Long) heightObject;
        } else if (heightObject instanceof Double) {
            result = result + (double) heightObject;
        } else if (heightObject instanceof String) {
            result = Double.parseDouble((String) heightObject);
        }

        if (result < minimumHeight) {
            result = minimumHeight;
        }
        return result;
    }

    protected String getAttributeValue(SimpleFeature feature, String column) {
        String result = "default";
        Object LowerObject = feature.getAttribute(column);
        Object UpperObject = feature.getAttribute(column.toUpperCase());
        Object attributeObject = null;
        if (LowerObject != null) {
            attributeObject = LowerObject;
        } else if (UpperObject != null) {
            attributeObject = UpperObject;
        }

        if (attributeObject instanceof String) {
            result = (String) attributeObject;
        } else if (attributeObject instanceof Integer) {
            result = String.valueOf((int) attributeObject);
        } else if (attributeObject instanceof Long) {
            result = String.valueOf(attributeObject);
        } else if (attributeObject instanceof Double) {
            result = String.valueOf((double) attributeObject);
        } else if (attributeObject instanceof Short) {
            result = String.valueOf((short) attributeObject);
        }
        return result;
    }

    protected String castStringFromObject(Object object, String defaultValue) {
        String result;
        if (object == null) {
            result = defaultValue;
        } else if (object instanceof String) {
            result = (String) object;
        } else if (object instanceof Integer) {
            result = String.valueOf((int) object);
        } else if (object instanceof Long) {
            result = String.valueOf(object);
        } else if (object instanceof Double) {
            result = String.valueOf((double) object);
        } else if (object instanceof Short) {
            result = String.valueOf((short) object);
        } else {
            result = object.toString();
        }
        return result;
    }

    protected String getAttributeValueOfDefault(SimpleFeature feature, String column, String defaultValue) {
        String result = defaultValue;
        Object LowerObject = feature.getAttribute(column);
        Object UpperObject = feature.getAttribute(column.toUpperCase());
        Object attributeObject = null;
        if (LowerObject != null) {
            attributeObject = LowerObject;
        } else if (UpperObject != null) {
            attributeObject = UpperObject;
        } else {
            return result;
        }

        if (attributeObject instanceof String) {
            result = (String) attributeObject;
        } else if (attributeObject instanceof Integer) {
            result = String.valueOf((int) attributeObject);
        } else if (attributeObject instanceof Long) {
            result = String.valueOf(attributeObject);
        } else if (attributeObject instanceof Double) {
            result = String.valueOf((double) attributeObject);
        } else if (attributeObject instanceof Short) {
            result = String.valueOf((short) attributeObject);
        }
        return result;
    }

    protected double getNumberAttribute(SimpleFeature feature, String column) {
        double result = 0.0d;
        Object attributeLower = feature.getAttribute(column);
        Object attributeUpper = feature.getAttribute(column.toUpperCase());
        Object attributeObject = null;
        if (attributeLower != null) {
            attributeObject = attributeLower;
        } else if (attributeUpper != null) {
            attributeObject = attributeUpper;
        }

        if (attributeObject instanceof Short) {
            result = result + (short) attributeObject;
        } else if (attributeObject instanceof Integer) {
            result = result + (int) attributeObject;
        } else if (attributeObject instanceof Long) {
            result = result + (Long) attributeObject;
        } else if (attributeObject instanceof Double) {
            result = result + (double) attributeObject;
        } else if (attributeObject instanceof String) {
            result = Double.parseDouble((String) attributeObject);
        }
        return result;
    }

    protected double getAltitude(SimpleFeature feature, String column) {
        double result = 0.0d;
        Object heightLower = feature.getAttribute(column);
        Object heightUpper = feature.getAttribute(column.toUpperCase());
        Object heightObject = null;
        if (heightLower != null) {
            heightObject = heightLower;
        } else if (heightUpper != null) {
            heightObject = heightUpper;
        } else {
            return result;
        }

        if (heightObject instanceof Short) {
            result = result + (short) heightObject;
        } else if (heightObject instanceof Integer) {
            result = result + (int) heightObject;
        } else if (heightObject instanceof Long) {
            result = result + (Long) heightObject;
        } else if (heightObject instanceof Double) {
            result = result + (double) heightObject;
        } else if (heightObject instanceof String) {
            result = Double.parseDouble((String) heightObject);
        }
        return result;
    }

    protected double getDiameter(SimpleFeature feature, String column) {
        double result = 10.0d;
        Object attributeLower = feature.getAttribute(column);
        Object attributeUpper = feature.getAttribute(column.toUpperCase());
        Object attributeObject = null;
        if (attributeLower != null) {
            attributeObject = attributeLower;
        } else if (attributeUpper != null) {
            attributeObject = attributeUpper;
        } else {
            return result;
        }

        if (attributeObject instanceof Short) {
            result = result + (short) attributeObject;
        } else if (attributeObject instanceof Integer) {
            result = result + (int) attributeObject;
        } else if (attributeObject instanceof Long) {
            result = result + (Long) attributeObject;
        } else if (attributeObject instanceof Double) {
            result = result + (double) attributeObject;
        } else if (attributeObject instanceof String) {
            result = Double.parseDouble((String) attributeObject);
        }

        return result;
    }

    protected GaiaPrimitive createSurfaceFromExteriorAndInteriorPolygons(List<Vector3d> ExteriorPolygon, List<List<Vector3d>> interiorPolygons) {
        GaiaTessellator tessellator = new GaiaTessellator();

        GaiaPrimitive primitive = new GaiaPrimitive();
        GaiaSurface surface = new GaiaSurface();
        List<GaiaVertex> vertexList = new ArrayList<>();

        Vector3d normal = new Vector3d();
        tessellator.calculateNormal3D(ExteriorPolygon, normal);

        List<Integer> resultTrianglesIndices = new ArrayList<>();
        List<Vector3d> resultPolygonPoints = new ArrayList<>();
        tessellator.tessellate3D(ExteriorPolygon, interiorPolygons, resultPolygonPoints, resultTrianglesIndices);

        for (Vector3d point : resultPolygonPoints) {
            GaiaVertex vertex = new GaiaVertex();
            vertex.setPosition(point);
            vertex.setNormal(normal);
            vertexList.add(vertex);
        }
        primitive.setVertices(vertexList); // total vertex list
        primitive.getSurfaces().add(surface);

        int idx1Local = -1;
        int idx2Local = -1;
        int idx3Local = -1;
        int indicesCount = resultTrianglesIndices.size();
        int trianglesCount = indicesCount / 3;
        for (int n = 0; n < trianglesCount; n++) {
            idx1Local = resultTrianglesIndices.get(n * 3);
            idx2Local = resultTrianglesIndices.get(n * 3 + 1);
            idx3Local = resultTrianglesIndices.get(n * 3 + 2);

            GaiaFace face = new GaiaFace();
            int[] indicesArray = new int[3];
            indicesArray[0] = idx1Local;
            indicesArray[1] = idx2Local;
            indicesArray[2] = idx3Local;
            face.setIndices(indicesArray);
            face.setFaceNormal(normal);
            surface.getFaces().add(face);
        }

        primitive.deleteNoUsedVertices();

        return primitive;
    }

    protected GaiaPrimitive createPrimitiveFromPolygons(List<List<Vector3d>> polygons) {
        GaiaTessellator tessellator = new GaiaTessellator();

        GaiaPrimitive primitive = new GaiaPrimitive();
        List<GaiaVertex> vertexList = new ArrayList<>();
        Map<Vector3d, Integer> pointsMap = new HashMap<>();

        List<List<Vector3d>> polygonsClean = new ArrayList<>();
        double error = 1e-10;

        int polygonCount = polygons.size();
        for (List<Vector3d> polygon : polygons) {

            // check uroborus
            List<Vector3d> cleanPolygon = new ArrayList<>();
            cleanPolygon = GeometryUtils.getCleanPoints3dArray(polygon, cleanPolygon, error);
            polygonsClean.add(cleanPolygon);

            Vector3d normal = new Vector3d();
            tessellator.calculateNormal3D(cleanPolygon, normal);

            for (Vector3d vector3d : cleanPolygon) {
                GaiaVertex vertex = new GaiaVertex();
                vertex.setPosition(vector3d);
                vertex.setNormal(normal);
                vertexList.add(vertex);
            }
        }

        int vertexCount = vertexList.size();
        for (int m = 0; m < vertexCount; m++) {
            GaiaVertex vertex = vertexList.get(m);
            pointsMap.put(vertex.getPosition(), m);
        }

        primitive.setVertices(vertexList); // total vertex list

        List<Integer> resultTrianglesIndices = new ArrayList<>();

        for (int m = 0; m < polygonCount; m++) {
            GaiaSurface surface = new GaiaSurface();
            primitive.getSurfaces().add(surface);

            int idx1Local = -1;
            int idx2Local = -1;
            int idx3Local = -1;

            List<Vector3d> polygon = polygonsClean.get(m);
            resultTrianglesIndices.clear();

            // Note : in "tessellator.tessellate3D(polygon, resultTrianglesIndices);" is possible to loss some points (deleting collinear points)
            // So, in the end of this method, delete no used vertices
            tessellator.tessellate3D(polygon, resultTrianglesIndices);

            int indicesCount = resultTrianglesIndices.size();
            int trianglesCount = indicesCount / 3;
            for (int n = 0; n < trianglesCount; n++) {
                idx1Local = resultTrianglesIndices.get(n * 3);
                idx2Local = resultTrianglesIndices.get(n * 3 + 1);
                idx3Local = resultTrianglesIndices.get(n * 3 + 2);

                Vector3d point1 = polygon.get(idx1Local);
                Vector3d point2 = polygon.get(idx2Local);
                Vector3d point3 = polygon.get(idx3Local);

                int idx1 = pointsMap.get(point1);
                int idx2 = pointsMap.get(point2);
                int idx3 = pointsMap.get(point3);

                GaiaFace face = new GaiaFace();
                int[] indicesArray = new int[3];
                indicesArray[0] = idx1;
                indicesArray[1] = idx2;
                indicesArray[2] = idx3;
                face.setIndices(indicesArray);
                surface.getFaces().add(face);
            }
        }

        // now, delete no used vertices (bcos possible loss of some points)
        primitive.deleteNoUsedVertices();

        return primitive;
    }

    protected GaiaNode createPrimitiveFromPipeLineString(GaiaPipeLineString pipeLineString) {
        GaiaNode resultGaiaNode = null;
        int pointsCount = pipeLineString.getPositions().size();
        if (pointsCount < 2) {
            return null;
        }

        PipeType profileType = pipeLineString.getProfileType();
        if (profileType == PipeType.CIRCULAR) {
            // circular pipe.
            float pipeRadius = (float) (pipeLineString.getDiameter() / 2000.0f); // convert to meters from millimeters.

            // 1rst create elbows.
            float elbowRadius = pipeRadius * 1.5f; // test value
            List<PipeElbow> pipeElbows = new ArrayList<>();

            for (int i = 0; i < pointsCount; i++) {
                Vector3d point = pipeLineString.getPositions().get(i);
                PipeElbow pipeElbow = new PipeElbow(new Vector3d(point), profileType, elbowRadius);
                pipeElbow.setPipeRadius(pipeRadius);
                pipeElbow.setProfileType(profileType);
                pipeElbows.add(pipeElbow);
            }

            Modeler3D modeler3D = new Modeler3D();
            TopologicalNetwork tNetwork = modeler3D.getPipeNetworkFromPipeElbows(pipeElbows);
            resultGaiaNode = modeler3D.makeGeometry(tNetwork);
        } else if (profileType == PipeType.RECTANGULAR) {
            // rectangular pipe.
            float[] pipeRectangularSize = pipeLineString.getRectangularSize();
            float pipeWidth = pipeRectangularSize[0];
            float pipeHeight = pipeRectangularSize[1];

            // 1rst create elbows.
            float elbowRadius = Math.max(pipeWidth, pipeHeight) * 1.5f; // test value
            List<PipeElbow> pipeElbows = new ArrayList<>();

            for (int i = 0; i < pointsCount; i++) {
                Vector3d point = pipeLineString.getPositions().get(i);
                PipeElbow pipeElbow = new PipeElbow(new Vector3d(point), profileType, elbowRadius);
                pipeElbow.setPipeRectangularSize(new float[]{pipeWidth, pipeHeight});
                pipeElbow.setProfileType(profileType);
                pipeElbows.add(pipeElbow);
            }

            Modeler3D modeler3D = new Modeler3D();
            TopologicalNetwork tNetwork = modeler3D.getPipeNetworkFromPipeElbows(pipeElbows);
            resultGaiaNode = modeler3D.makeGeometry(tNetwork);
        }
        return resultGaiaNode;
    }

    protected GaiaPrimitive createPrimitiveFromGaiaExtrusionSurfaces(List<GaiaExtrusionSurface> surfaces) {
        GaiaTessellator tessellator = new GaiaTessellator();
        GaiaPrimitive primitive = new GaiaPrimitive();
        List<GaiaVertex> vertexList = new ArrayList<>();
        Map<Vector3d, Integer> pointsMap = new HashMap<>();

        for (GaiaExtrusionSurface extrusionSurface : surfaces) {
            List<Vector3d> polygon = extrusionSurface.getVertices();

            Vector3d normal = new Vector3d();
            tessellator.calculateNormal3D(polygon, normal);

            for (Vector3d vector3d : polygon) {
                GaiaVertex vertex = new GaiaVertex();
                vertex.setPosition(vector3d);
                vertex.setNormal(normal);
                vertexList.add(vertex);
            }
        }

        int vertexCount = vertexList.size();
        for (int m = 0; m < vertexCount; m++) {
            GaiaVertex vertex = vertexList.get(m);
            pointsMap.put(vertex.getPosition(), m);
        }

        primitive.setVertices(vertexList);

        List<Integer> resultTrianglesIndices = new ArrayList<>();

        for (GaiaExtrusionSurface extrusionSurface : surfaces) {
            List<Vector3d> polygon = extrusionSurface.getVertices();
            GaiaSurface gaiaSurface = new GaiaSurface();
            primitive.getSurfaces().add(gaiaSurface);

            int idx1Local = -1;
            int idx2Local = -1;
            int idx3Local = -1;

            resultTrianglesIndices.clear();
            tessellator.tessellate3D(polygon, resultTrianglesIndices);

            int indicesCount = resultTrianglesIndices.size();
            int trianglesCount = indicesCount / 3;
            for (int n = 0; n < trianglesCount; n++) {
                idx1Local = resultTrianglesIndices.get(n * 3);
                idx2Local = resultTrianglesIndices.get(n * 3 + 1);
                idx3Local = resultTrianglesIndices.get(n * 3 + 2);

                Vector3d point1 = polygon.get(idx1Local);
                Vector3d point2 = polygon.get(idx2Local);
                Vector3d point3 = polygon.get(idx3Local);

                int idx1 = pointsMap.get(point1);
                int idx2 = pointsMap.get(point2);
                int idx3 = pointsMap.get(point3);

                GaiaFace face = new GaiaFace();
                int[] indicesArray = new int[3];
                indicesArray[0] = idx1;
                indicesArray[1] = idx2;
                indicesArray[2] = idx3;
                face.setIndices(indicesArray);
                gaiaSurface.getFaces().add(face);
            }
        }

        return primitive;
    }


    protected GaiaMaterial getMaterialByClassification(List<GaiaMaterial> gaiaMaterials, Classification classification) {
        if (classification.equals(Classification.DOOR)) {
            return gaiaMaterials.get(1);
        } else if (classification.equals(Classification.WINDOW)) {
            return gaiaMaterials.get(2);
        } else if (classification.equals(Classification.CEILING)) {
            return gaiaMaterials.get(3);
        } else if (classification.equals(Classification.STAIRS)) {
            return gaiaMaterials.get(3);
        } else if (classification.equals(Classification.ROOF)) {
            return gaiaMaterials.get(3);
        } else if (classification.equals(Classification.WATER)) {
            return gaiaMaterials.get(4);
        } else if (classification.equals(Classification.GROUND)) {
            return gaiaMaterials.get(5);
        } else if (classification.equals(Classification.FURNITURE)) {
            return gaiaMaterials.get(6);
        } else if (classification.equals(Classification.INSTALLATION)) {
            return gaiaMaterials.get(7);
        } else if (classification.equals(Classification.INFRASTRUCTURE)) {
            return gaiaMaterials.get(8);
        } else {
            return gaiaMaterials.get(0);
        }
    }
}
