package com.gaia3d.basic.geometry.voxel;

import com.gaia3d.basic.geometry.GaiaByteColor4;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter

public class Voxel {
    private GaiaByteColor4 color = new GaiaByteColor4(0, 0, 0, 0);

    public void setByteColor4(byte r, byte g, byte b, byte a) {
        this.color.setRGBA(r, g, b, a);
    }

    public byte getAlpha() {
        return color.getA();
    }

    public int getAlphaInt() {
        return color.getA() & 0xFF;
    }

    public float getAlphaFloat() {
        return (float) (color.getA() & 0xFF) / 255.0f;
    }
}
