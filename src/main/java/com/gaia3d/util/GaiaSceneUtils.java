package com.gaia3d.util;

import com.gaia3d.basic.model.GaiaMesh;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaScene;

public class GaiaSceneUtils {
    public static GaiaScene getSceneRectangularNet(int numCols, int numRows, double width, double height, boolean calculateTexCoords) {
        GaiaScene scene = new GaiaScene();
        GaiaNode rootNode = new GaiaNode();
        scene.getNodes().add(rootNode);

        GaiaNode node = new GaiaNode();
        rootNode.getChildren().add(node);

        GaiaMesh mesh = new GaiaMesh();
        node.getMeshes().add(mesh);

        GaiaPrimitive primitive = GaiaPrimitiveUtils.getRectangularNet(numCols, numRows, width, height, calculateTexCoords);
        mesh.getPrimitives().add(primitive);
        return scene;
    }
}
