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
    float r;
    float g;
    float b;
    float a;
}

