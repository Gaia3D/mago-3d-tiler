package com.gaia3d.basic.geometry;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * A class that represents a VBO (Vertex Buffer Object) for a Gaia object.
 * It is used for opengl rendering.
 * It contains the indices, positions, normals, colors, texture coordinates and texture.
 */
@Setter
@Getter
public class GaiaVBO implements Serializable {
    private int indicesLength;
    private int indicesVbo;
    private int positionVbo;
    private int normalVbo;
    private int colorVbo;
    private int textureCoordinateVbo;
    private int textureVbo;
}
