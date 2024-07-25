package com.gaia3d.basic.geometry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * GaiaColor is a class to store the color of a geometry.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GaiaColor implements Serializable {
    private float r;
    private float g;
    private float b;
    private float a;
}

