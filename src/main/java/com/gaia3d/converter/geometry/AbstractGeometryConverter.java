package com.gaia3d.converter.geometry;

import com.gaia3d.basic.structure.*;
import com.gaia3d.basic.types.TextureType;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Slf4j
public abstract class AbstractGeometryConverter {

    abstract protected List<GaiaScene> convert(File file);

    protected GaiaScene initScene() {
        GaiaScene scene = new GaiaScene();
        GaiaMaterial material = new GaiaMaterial();
        material.setId(0);
        material.setName("extruded");
        material.setDiffuseColor(new Vector4d(0.5, 0.5, 0.5, 1));
        Map<TextureType, List<GaiaTexture>> textureTypeListMap = material.getTextures();
        textureTypeListMap.put(TextureType.DIFFUSE, new ArrayList<>());
        scene.getMaterials().add(material);

        GaiaNode rootNode = new GaiaNode();
        Matrix4d transformMatrix = new Matrix4d();
        transformMatrix.identity();
        rootNode.setTransformMatrix(transformMatrix);
        scene.getNodes().add(rootNode);
        return scene;
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
        primitive.setMaterial(material);
        primitive.setMaterialIndex(0);
        primitive.setSurfaces(surfaces);
        primitive.setVertices(vertices);

        GaiaSurface surface = new GaiaSurface();
        Vector3d[] normals = new Vector3d[positions.size()];
        for (int i = 0; i < normals.length; i++) {
            normals[i] = new Vector3d(0,0,0);
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
            if (normal == null) {
                log.error("why");
            }
            vertex.setNormal(normal);
            vertices.add(vertex);
        }
        surfaces.add(surface);
        return primitive;
    }

    protected int indexOf(List<Vector3d> positions, Vector3d item) {
        return IntStream.range(0, positions.size()).filter(i -> positions.get(i) == item).findFirst().orElse(-1);
    }

    protected double getHeight(SimpleFeature feature, String column, double minimumHeight) {
        double result = 0.0d;
        Object heightLower = feature.getAttribute(column);
        Object heightUpper = feature.getAttribute(column);
        Object heightObject = null;
        if (heightLower != null) {
            heightObject = heightLower;
        } else if (heightUpper != null) {
            heightObject = heightUpper;
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
            String heightString = (String) heightObject;
            result = Double.parseDouble(heightString);
        }

        if (result < minimumHeight) {
            result = minimumHeight;
        }
        return result;
    }

    protected String getAttribute(SimpleFeature feature, String column) {
        String result = "default";
        Object LowerObject = feature.getAttribute(column);
        Object UpperObject = feature.getAttribute(column);
        Object attributeObject = null;
        if (LowerObject != null) {
            attributeObject = LowerObject;
        } else if (UpperObject != null) {
            attributeObject = UpperObject;
        }

        if (attributeObject instanceof String) {
            String value = (String) attributeObject;
            result = value;
        } else if (attributeObject instanceof Integer) {
            result = String.valueOf((int) attributeObject);
        } else if (attributeObject instanceof Long) {
            result = String.valueOf((Long) attributeObject);
        } else if (attributeObject instanceof Double) {
            result = String.valueOf((double) attributeObject);
        } else if (attributeObject instanceof Short) {
            result = String.valueOf((short) attributeObject);
        }
        return result;
    }

    protected double getAltitude(SimpleFeature feature, String column, double absoluteAltitude) {
        double result = 0.0d;
        Object heightLower = feature.getAttribute(column);
        Object heightUpper = feature.getAttribute(column);
        Object heightObject = null;
        if (heightLower != null) {
            heightObject = heightLower;
        } else if (heightUpper != null) {
            heightObject = heightUpper;
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
            String heightString = (String) heightObject;
            result = Double.parseDouble(heightString);
        }
        return result;
    }
}
