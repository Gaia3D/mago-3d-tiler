package com.gaia3d.basic.model;

import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.structure.SceneStructure;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Matrix4d;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that represents a scene of a Gaia object.
 * The largest unit of the 3D file.
 * It contains the nodes and materials.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaScene extends SceneStructure implements Serializable {
    private Path originalPath;
    private GaiaBoundingBox gaiaBoundingBox;
    private GaiaAttribute attribute;

    public GaiaScene(GaiaSet gaiaSet) {
        List<GaiaBufferDataSet> bufferDataSets = gaiaSet.getBufferDataList();
        List<GaiaBufferDataSet> bufferDataSetsCopy = new ArrayList<>();
        for (GaiaBufferDataSet bufferDataSet : bufferDataSets) {
            bufferDataSetsCopy.add(bufferDataSet.clone());
        }
        List<GaiaMaterial> materials = gaiaSet.getMaterials();

        Matrix4d transformMatrix = new Matrix4d();
        transformMatrix.identity();

        GaiaNode rootNode = new GaiaNode();
        rootNode.setName("BatchedRootNode");
        rootNode.setTransformMatrix(transformMatrix);
        this.materials = materials;
        this.nodes.add(rootNode);
        this.attribute = gaiaSet.getAttribute();

        bufferDataSetsCopy.forEach((bufferDataSet) -> rootNode.getChildren().add(new GaiaNode(bufferDataSet)));
    }

    public GaiaBoundingBox getBoundingBox() {
        this.gaiaBoundingBox = new GaiaBoundingBox();
        for (GaiaNode node : this.getNodes()) {
            GaiaBoundingBox boundingBox = node.getBoundingBox(null);
            if (boundingBox != null) {
                gaiaBoundingBox.addBoundingBox(boundingBox);
            }
        }
        return this.gaiaBoundingBox;
    }

    public void clear() {
        this.nodes.forEach(GaiaNode::clear);
        this.materials.forEach(GaiaMaterial::clear);
        this.originalPath = null;
        this.gaiaBoundingBox = null;
        this.nodes.clear();

        int materialsCount = this.materials.size();
        for (int i = 0; i < materialsCount; i++) {
            GaiaMaterial material = this.materials.get(i);
            material.clear();
        }
        this.materials.clear();
    }

    public GaiaScene clone() {
        GaiaScene clone = new GaiaScene();
        for (GaiaNode node : this.nodes) {
            clone.getNodes().add(node.clone());
        }
        for (GaiaMaterial material : this.materials) {
            clone.getMaterials().add(material.clone());
        }
        clone.setOriginalPath(this.originalPath);
        clone.setGaiaBoundingBox(this.gaiaBoundingBox);

        // attribute is a reference type.
        GaiaAttribute attribute = this.attribute.getCopy();
        clone.setAttribute(attribute);
        return clone;
    }

    public long calcTriangleCount() {
        long triangleCount = 0;
        for (GaiaNode node : this.nodes) {
            triangleCount += node.getTriangleCount();
        }
        return triangleCount;
    }

    public void makeTriangleFaces()
    {
        for (GaiaNode node : this.nodes) {
            node.makeTriangleFaces();
        }
    }

    public void weldVertices(double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        for (GaiaNode node : this.nodes) {
            node.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
        }
    }

    public void unWeldVertices() {
        for (GaiaNode node : this.nodes) {
            node.unWeldVertices();
        }
    }

    public List<GaiaFace> extractGaiaFaces(List<GaiaFace> resultFaces) {
        if(resultFaces == null) {
            resultFaces = new ArrayList<>();
        }
        for (GaiaNode node : this.nodes) {
            node.extractGaiaFaces(resultFaces);
        }
        return resultFaces;
    }

    public void joinAllSurfaces() {
        GaiaNode rootNode = this.nodes.get(0);

        GaiaMesh meshMaster = new GaiaMesh();
        GaiaPrimitive primitiveMaster = new GaiaPrimitive();
        GaiaSurface surfaceMaster = new GaiaSurface();
        primitiveMaster.getSurfaces().add(surfaceMaster);
        meshMaster.getPrimitives().add(primitiveMaster);

        List<GaiaPrimitive> allPrimitives = this.extractPrimitives(null);
        for (GaiaPrimitive primitive : allPrimitives) {
            primitiveMaster.addPrimitive(primitive);
        }

        List<GaiaNode> children = rootNode.getChildren();
        for (GaiaNode child : children) {
            child.getMeshes().clear();
        }
        children.clear();

        GaiaNode node = new GaiaNode();
        node.getMeshes().add(meshMaster);

        children.add(node);
    }

    public List<GaiaPrimitive> extractPrimitives(List<GaiaPrimitive> resultPrimitives) {
        if(resultPrimitives == null) {
            resultPrimitives = new ArrayList<>();
        }
        for (GaiaNode node : this.nodes) {
            node.extractPrimitives(resultPrimitives);
        }
        return resultPrimitives;
    }

    public void doNormalLengthUnitary() {
        for (GaiaNode node : this.nodes) {
            node.doNormalLengthUnitary();
        }
    }

    public void deleteNormals()
    {
        for (GaiaNode node : this.nodes) {
            node.deleteNormals();
        }
    }

    public void deleteDegeneratedFaces() {
        for (GaiaNode node : this.nodes) {
            node.deleteDegeneratedFaces();
        }
    }

    public void spendTranformMatrix() {
        for (GaiaNode node : this.nodes) {
            node.spendTranformMatrix();
        }
    }

    public void makeTriangularFaces() {
        for (GaiaNode node : this.nodes) {
            node.makeTriangularFaces();
        }
    }
}
