package com.gaia3d.basic.halfedge;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WeldingParameters {
    private boolean checkNormals = false;
    private boolean checkColors = false;
    private boolean checkTexCoords = false;
    private boolean checkBatchIds = false;
    private double positionEpsilon = 1e-4;

    public boolean getCheckTexCoords() {
        return checkTexCoords;
    }

    public boolean getCheckColors() {
        return checkColors;
    }

    public boolean getCheckBatchIds() {
        return checkBatchIds;
    }

    public boolean getCheckNormals() {
        return checkNormals;
    }

    public WeldingParameters clone() {
        WeldingParameters clone = new WeldingParameters();
        clone.setCheckNormals(this.checkNormals);
        clone.setCheckColors(this.checkColors);
        clone.setCheckTexCoords(this.checkTexCoords);
        clone.setCheckBatchIds(this.checkBatchIds);
        clone.setPositionEpsilon(this.positionEpsilon);
        return clone;
    }
}
