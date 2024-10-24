package com.gaia3d.renderer.renderable;

public class RenderableObject {
    int status; // 0 = interior, 1 = exterior, -1 = unknown.***
    int colorCode; // 36-bit RGBA color.***

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getColorCode() {
        return colorCode;
    }

    public void setColorCode(int colorCode) {
        this.colorCode = colorCode;
    }
}
