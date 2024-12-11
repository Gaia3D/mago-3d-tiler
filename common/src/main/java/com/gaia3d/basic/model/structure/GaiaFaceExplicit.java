package com.gaia3d.basic.model.structure;

import com.gaia3d.basic.model.GaiaVertex;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

@Getter
@Setter
public class GaiaFaceExplicit {
    private GaiaVertex vertex1;
    private GaiaVertex vertex2;
    private GaiaVertex vertex3;
    private Vector3d planeNormal;

    public void setVertices(GaiaVertex vertex1, GaiaVertex vertex2, GaiaVertex vertex3) {
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
        this.vertex3 = vertex3;
    }

    public void setPlaneNormal(Vector3d normal) {
        this.planeNormal = new Vector3d(normal);
    }
}
