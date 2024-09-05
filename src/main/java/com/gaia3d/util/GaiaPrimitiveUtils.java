package com.gaia3d.util;

import com.gaia3d.basic.structure.GaiaFace;
import com.gaia3d.basic.structure.GaiaPrimitive;
import com.gaia3d.basic.structure.GaiaSurface;

public class GaiaPrimitiveUtils {

    public static void mergePrimitives(GaiaPrimitive primitiveMaster, GaiaPrimitive primitive) {
        // Merge the primitives
        // 1rst, check if the primitiveMaster has the same material as the primitive : TODO.***

        int vertexCountMaster = primitiveMaster.getVertices().size();

        primitiveMaster.getVertices().addAll(primitive.getVertices());
        int surfacesCount = primitive.getSurfaces().size();
        for(int i=0; i<surfacesCount; i++)
        {
            GaiaSurface surface = primitive.getSurfaces().get(i);
            GaiaSurface surfaceNew = new GaiaSurface();
            int facesCount = surface.getFaces().size();
            for(int j=0; j<facesCount; j++)
            {
                GaiaFace face = surface.getFaces().get(j);
                GaiaFace faceNew = new GaiaFace();
                int indices[] = face.getIndices();
                int indicesCount = indices.length;
                int indicesNew[] = new int[indicesCount];
                for(int k=0; k<indicesCount; k++)
                {
                    indicesNew[k] = indices[k] + vertexCountMaster;
                }
                faceNew.setIndices(indicesNew);
                surfaceNew.getFaces().add(faceNew);
            }

            primitiveMaster.getSurfaces().add(surfaceNew);
        }
    }
}
