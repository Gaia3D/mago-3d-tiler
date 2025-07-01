package com.gaia3d.process.postprocess.pointcloud;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gaia3d.process.postprocess.ComponentType;
import com.gaia3d.process.postprocess.DataType;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ByteAddress {
    @JsonProperty("byteOffset")
    private int byteOffset;
    // "BYTE", "UNSIGNED_BYTE", "SHORT", "UNSIGNED_SHORT", "INT", "UNSIGNED_INT", "FLOAT", "DOUBLE"
    @JsonProperty("componentType")
    private String componentType;
    // "SCALAR", "VEC2", "VEC3" "VEC4"
    @JsonProperty("type")
    private String type;

    /**
     * Constructor for byteOffset
     * default componentType is "FLOAT" and type is "VEC3"
     * @param byteOffset the byte offset
     */
    public ByteAddress(int byteOffset) {
        this.byteOffset = byteOffset;
        this.componentType = ComponentType.FLOAT;
        this.type = DataType.VEC3;
    }

    /**
     * Constructor for byteOffset, componentType and type
     * @param byteOffset the byte offset
     * @param componentType the component type
     * @param type the type
     */
    public ByteAddress(int byteOffset, String componentType, String type) {
        this.byteOffset = byteOffset;
        this.componentType = componentType;
        this.type = type;
    }
}
