package com.gaia3d.renderer.renderable;

import java.util.HashMap;
import java.util.Map;

public class SelectionColorManager {
    // 36-bit RGBA color.***
    public Map<Integer, RenderableObject> mapColorRenderable = new HashMap<>();
    private int currColor = 0;

    public SelectionColorManager() {
    }

    public int getAvailableColor()
    {
        int color = this.currColor;
        this.currColor = (this.currColor + 1);
        if (this.currColor == 2147483647) {
            this.currColor = 0;
        }
        return color;
    }

    public void getEncodedColor4(int color, byte[] encodedColor)
    {
        encodedColor[0] = (byte)(color & 0xFF);
        encodedColor[1] = (byte)(color >> 8 & 0xFF);
        encodedColor[2] = (byte)(color >> 16 & 0xFF);
        encodedColor[3] = (byte)(color >> 24 & 0xFF);
    }

    public int getDecodedColor4(byte[] encodedColor)
    {
        return (encodedColor[3] & 0xFF) << 24 | (encodedColor[2] & 0xFF) << 16 | (encodedColor[1] & 0xFF) << 8 | encodedColor[0] & 0xFF;
    }
}
