package com.gaia3d.basic.structure;

import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
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
 * @author znkim
 * @since 1.0.0
 * @see <a href="https://en.wikipedia.org/wiki/3D_computer_graphics">3D computer graphics</a>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaScene implements Serializable {
    private List<GaiaNode> nodes = new ArrayList<>();
    private List<GaiaMaterial> materials = new ArrayList<>();
    private Path originalPath;
    private GaiaBoundingBox gaiaBoundingBox;

    public GaiaScene(GaiaSet gaiaSet) {
        List<GaiaBufferDataSet> bufferDataSets = gaiaSet.getBufferDatas();
        List<GaiaMaterial> materials = gaiaSet.getMaterials();

        Matrix4d transformMatrix = new Matrix4d();
        transformMatrix.identity();

        GaiaNode rootNode = new GaiaNode();
        rootNode.setName("BatchedRootNode");
        rootNode.setTransformMatrix(transformMatrix);
        this.materials = materials;
        this.nodes.add(rootNode);

        bufferDataSets.forEach((bufferDataSet) -> rootNode.getChildren().add(new GaiaNode(bufferDataSet)));
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
        return clone;
    }

    public long calcTriangleCount() {
        long triangleCount = 0;
        for (GaiaNode node : this.nodes) {
            triangleCount += node.getTriangleCount();
        }
        return triangleCount;
    }
}
