package com.gaia3d.basic.structure.interfaces;

import com.gaia3d.basic.structure.GaiaFace;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public abstract class SurfaceStructure {
    protected List<GaiaFace> faces = new ArrayList<>();
}
