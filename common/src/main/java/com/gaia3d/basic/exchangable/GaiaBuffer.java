package com.gaia3d.basic.exchangable;

import com.gaia3d.basic.types.AccessorType;
import com.gaia3d.basic.types.AttributeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * GaiaBuffer represents a buffer by attribute, which is a convenient form to convert to gltf.
 * @author znkim
 * @since 1.0.0
 * @see AttributeType , AccessorType, GaiaBufferDataSet
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaBuffer implements Serializable {
    private AttributeType attributeType;
    private AccessorType accessorType;

    private int elementsCount = -1;

    private byte glDimension;
    private int glType;
    private int glTarget;

    private float[] floats;
    private int[] ints;
    private short[] shorts;
    private byte[] bytes;

    public void clear() {
        floats = null;
        ints = null;
        shorts = null;
        bytes = null;
    }

    public GaiaBuffer clone() {
        GaiaBuffer clone = new GaiaBuffer();
        clone.setAttributeType(attributeType);
        clone.setAccessorType(accessorType);
        clone.setElementsCount(elementsCount);
        clone.setGlDimension(glDimension);
        clone.setGlType(glType);
        clone.setGlTarget(glTarget);
        if (floats != null) {
            clone.setFloats(floats.clone());
        }
        if (ints != null) {
            clone.setInts(ints.clone());
        }
        if (shorts != null) {
            clone.setShorts(shorts.clone());
        }
        if (bytes != null) {
            clone.setBytes(bytes.clone());
        }
        return clone;
    }
}
