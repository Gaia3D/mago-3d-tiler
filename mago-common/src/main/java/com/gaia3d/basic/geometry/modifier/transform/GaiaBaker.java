package com.gaia3d.basic.geometry.modifier.transform;

import com.gaia3d.basic.geometry.modifier.Modifier;
import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.List;

/**
 * TransformBaker is responsible for baking the transformation matrices of a GaiaScene.
 * This process is typically used to optimize the scene for rendering or further processing.
 * It involves applying the transformations to the vertices of the scene's nodes and meshes,\
 * thus creating a final, baked version of the scene that is ready for use.
 */
@Slf4j
public class GaiaBaker extends Modifier {

    /**
     * Bakes the transformation of a GaiaScene.
     * @param scene the GaiaScene to bake
     */
    @Override
    public void apply(GaiaScene scene) {
        List<GaiaNode> rootNodes = scene.getNodes();
        for (GaiaNode node : rootNodes) {
            applyNode(new Matrix4d(), node);
        }
        initTransformMatrix(scene);
    }

    /**
     * Bakes the transformation of aGaiaNode.
     * @param node the GaiaNode to bake
     */
    public void apply(GaiaNode node) {
        applyNode(new Matrix4d(), node);

        Matrix4d localTransformMatrix = node.getTransformMatrix();
        List<GaiaNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (GaiaNode child : children) {
                applyNode(localTransformMatrix, child);
            }
        }
        initTransformMatrix(node);
    }

    @Override
    protected void applyVertex(Matrix4d productTransformMatrix, GaiaVertex vertex) {
        Matrix3d productRotationMatrix = new Matrix3d(productTransformMatrix);

        Vector3d position = vertex.getPosition();
        if (position != null) {
            Vector3d localizedPosition = productTransformMatrix.transformPosition(position, new Vector3d());
            vertex.setPosition(localizedPosition);
        }

        Vector3d normal = vertex.getNormal();
        if (normal != null) {
            Vector3d localizedNormal = productRotationMatrix.transform(normal, new Vector3d());
            localizedNormal.normalize();
            vertex.setNormal(localizedNormal);
        }
    }

    private void initTransformMatrix(GaiaScene scene) {
        List<GaiaNode> rootNodes = scene.getNodes();
        for (GaiaNode node : rootNodes) {
            initTransformMatrix(node);
        }
    }

    private void initTransformMatrix(GaiaNode node) {
        Matrix4d transformMatrix = new Matrix4d();
        transformMatrix.identity();
        node.setTransformMatrix(transformMatrix);

        List<GaiaNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (GaiaNode child : children) {
                initTransformMatrix(child);
            }
        }
    }
}
