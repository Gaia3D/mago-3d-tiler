package com.gaia3d.basic.structure.interfaces;

import com.gaia3d.basic.structure.GaiaTexture;
import com.gaia3d.basic.types.TextureType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public abstract class MaterialStructure implements Serializable {
    protected Map<TextureType, List<GaiaTexture>> textures = new HashMap<>();
}
