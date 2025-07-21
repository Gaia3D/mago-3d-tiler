package com.gaia3d.basic.model.structure;

import com.gaia3d.basic.model.GaiaFace;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public abstract class SurfaceStructure implements Serializable {
    protected List<GaiaFace> faces = new ArrayList<>();
}
