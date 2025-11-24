package com.gaia3d.basic.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GeometryElement {
    SCENE,
    NODE,
    MESH,
    PRIMITIVE,
    VERTEX,
    FACE,
    SURFACE,
    EDGE
}
