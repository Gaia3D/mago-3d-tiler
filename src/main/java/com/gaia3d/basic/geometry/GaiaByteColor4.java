package com.gaia3d.basic.geometry;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
/**
 * Class representing a color with 4 components (RGBA) using byte values.
 * The values are expected to be in the range of 0-255.
 */
public class GaiaByteColor4 {
    private byte r;
    private byte g;
    private byte b;
    private byte a;

    public GaiaByteColor4() {
        this.r = 0;
        this.g = 0;
        this.b = 0;
        this.a = 0;
    }

    public GaiaByteColor4(byte r, byte g, byte b, byte a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public GaiaByteColor4(int r, int g, int b, int a) {
        this.r = (byte) r;
        this.g = (byte) g;
        this.b = (byte) b;
        this.a = (byte) a;
    }

    public void setRGBA(byte r, byte g, byte b, byte a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

}
