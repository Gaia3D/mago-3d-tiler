package com.gaia3d.basic.types;

import java.io.Serializable;

/**
 * Enumerates the types of accessors.
 * @author znkim
 * @since 1.0.0
 */
public enum AccessorType implements Serializable {
    SCALAR,
    VEC2,
    VEC3,
    VEC4,
    MAT3,
    MAT4
}
