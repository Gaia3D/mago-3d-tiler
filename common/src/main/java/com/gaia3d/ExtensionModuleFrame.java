package com.gaia3d;

import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaTexture;

import java.util.List;
import java.util.Map;

public interface ExtensionModuleFrame {
    String getName();

    boolean isSupported();

    GaiaScene executePhotorealistic(GaiaScene gaiaScene, Map<String, Object> options);

    List<GaiaTexture> getRenderScene(List<GaiaScene> scene);
}
