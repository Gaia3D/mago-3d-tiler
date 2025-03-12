package com.gaia3d.renderer.renderable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RenderableObject {
    protected int status; // 0 = interior, 1 = exterior, -1 = unknown
    protected int colorCode; // 36-bit RGBA color
}
