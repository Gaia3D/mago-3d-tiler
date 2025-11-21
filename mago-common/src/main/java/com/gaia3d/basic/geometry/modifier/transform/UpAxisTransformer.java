package com.gaia3d.basic.geometry.modifier.transform;

import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import org.joml.Matrix4d;

import java.util.List;

public class UpAxisTransformer {
    /**
     * Rotate the scene around the X-axis to transform it to Z-Up orientation.
     * @param scene the GaiaScene
     * @param radian angle in radians
     */
    public static void rotateDegreeX(GaiaScene scene, double radian) {
        rotateXAxis(scene, Math.toRadians(radian));
    }

    /**
     * Rotate the scene around the X-axis to transform it to Z-Up orientation.
     * @param scene the GaiaScene
     * @param radian angle in radians
     */
    public static void rotateXAxis(GaiaScene scene, double radian) {
        List<GaiaNode> nodes = scene.getNodes();
        for (GaiaNode node : nodes) {
            Matrix4d transform = node.getTransformMatrix();
            Matrix4d rotation = new Matrix4d().identity();
            rotation.rotateX(radian);
            transform.mul(rotation);
        }
        scene.updateBoundingBox();
    }

    public static void transformToZUp(GaiaScene gaiaScene) {
        // Rotate the scene around the X-axis by 90 degrees (π/2 radians)
        rotateXAxis(gaiaScene, Math.PI / 2);
    }

    public static void transformToYUp(GaiaScene gaiaScene) {
        // Rotate the scene around the X-axis by -90 degrees (-π/2 radians)
        rotateXAxis(gaiaScene, -Math.PI / 2);
    }
}
