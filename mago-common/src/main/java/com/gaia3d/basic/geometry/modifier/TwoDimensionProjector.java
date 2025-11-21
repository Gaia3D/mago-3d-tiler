package com.gaia3d.basic.geometry.modifier;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.List;

@Slf4j
public class TwoDimensionProjector {

    public void project(GaiaScene scene) {
        List<GaiaNode> nodes = scene.getNodes();
        for (GaiaNode node : nodes) {
            project(node);
        }
    }

    private GaiaNode project(GaiaNode node) {
        GaiaNode projectedNode = new GaiaNode();
        projectedNode.setName(node.getName() + "_2D");
        projectedNode.setTransformMatrix(new Matrix4d(node.getTransformMatrix()));
        projectedNode.setParent(null);

        Matrix4d transformMatrix = projectedNode.getTransformMatrix();

        for (GaiaNode child : node.getChildren()) {
            GaiaNode projectedChild = project(child);
            projectedChild.setParent(projectedNode);
            projectedNode.getChildren().add(projectedChild);
        }

        for (GaiaMesh mesh : node.getMeshes()) {

            GaiaMesh projectedMesh = new GaiaMesh();
            for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                GaiaBoundingBox gaiaBoundingBox = primitive.getBoundingBox(transformMatrix);
                Vector3d center = gaiaBoundingBox.getCenter();

                List<GaiaVertex> vertices = primitive.getVertices();
                List<Vector3d> normals = vertices.stream()
                        .map(GaiaVertex::getNormal)
                        .toList();
                Vector3d averageNormal = calcNormalAverage(normals);

                Vector3d dir = new Vector3d(averageNormal);
                Vector3d up = new Vector3d(0, 0, 1);
                if (isCeil(averageNormal)) {
                    // If the average normal is close to vertical, project to XY plane
                    dir.set(0, 0, 1);
                    up.set(0, 1, 0);
                }

                Matrix4d projectionMatrix = createProjectionMatrix(dir, up, center);
                Matrix4d inverseProjectionMatrix = new Matrix4d(projectionMatrix).invert();
                for (GaiaVertex vertex : vertices) {
                    Vector3d position = new Vector3d(vertex.getPosition());
                    position = position.mulPosition(inverseProjectionMatrix);
                    position.z = 0.0d; // Set Z to 0 for 2D projection
                    vertex.setPosition(position);
                }
            }
            projectedNode.getMeshes().add(projectedMesh);
        }

        return projectedNode;
    }

    public Matrix4d createProjectionMatrix(Vector3d dir, Vector3d up, Vector3d center) {
        Vector3d right = new Vector3d();
        // UP X DIR = RIGHT, RIGHT X UP = DIR2
        up.cross(dir, right).normalize();
        right.cross(up, dir).normalize();
        return new Matrix4d(
                right.x(), right.y(), right.z(), 0,
                up.x(), up.y(), up.z(), 0,
                dir.x(), dir.y(), dir.z(), 0,
                center.x(), center.y(), center.z(), 1
        );
    }

    public Vector3d calcNormalAverage(List<Vector3d> normals) {
        Vector3d averageNormal = new Vector3d(0, 0, 0);
        for (Vector3d normal : normals) {
            averageNormal.add(normal);
        }
        averageNormal.normalize();
        return averageNormal;
    }

    public boolean isCeil(Vector3d normal) {
        return normal.z() > 0.9;
    }
}
