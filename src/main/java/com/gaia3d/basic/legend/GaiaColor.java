package com.gaia3d.basic.legend;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter

public class GaiaColor {
    private float red;
    private float green;
    private float blue;
    private float alpha;

    public GaiaColor() {
        this.red = 0.0f;
        this.green = 0.0f;
        this.blue = 0.0f;
        this.alpha = 1.0f; // Default alpha to fully opaque
    }

    public GaiaColor(float red, float green, float blue, float alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public byte[] getColorBytesArray() {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (red * 255.0);
        bytes[1] = (byte) (green * 255.0);
        bytes[2] = (byte) (blue * 255.0);
        bytes[3] = (byte) (alpha * 255.0);
        return bytes;
    }
}
