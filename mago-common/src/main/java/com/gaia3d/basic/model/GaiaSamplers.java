package com.gaia3d.basic.model;

import com.gaia3d.basic.remesher.GaiaFrontierExpander;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Slf4j
@Getter
@Setter
public class GaiaSamplers implements Serializable {
    private int wrapS = 10497; // GL_REPEAT
    private int wrapT = 10497; // GL_REPEAT
    private int minFilter = 9729; // GL_LINEAR
    private int magFilter = 9729; // GL_LINEAR

    public GaiaSamplers clone(){
        GaiaSamplers cloned = new GaiaSamplers();
        cloned.setMagFilter(this.magFilter);
        cloned.setMinFilter(this.minFilter);
        cloned.setWrapS(this.wrapS);
        cloned.setWrapT(this.wrapT);
        return cloned;
    }
}
