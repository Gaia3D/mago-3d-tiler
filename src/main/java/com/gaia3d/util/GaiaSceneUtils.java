package com.gaia3d.util;

import com.gaia3d.basic.structure.GaiaMesh;
import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.basic.structure.GaiaPrimitive;
import com.gaia3d.basic.structure.GaiaScene;

public class GaiaSceneUtils {
    public static GaiaScene getSceneRectangularNet(int numCols, int numRows, double width, double height, boolean calculateTexCoords)
    {
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
