package com.gaia3d.basic.types;

import lombok.Getter;

/**
 * Enumerates the types of attributes.
 * @Author znkim
 * @Since 1.0.0
 * @See GaiaBuffer, GltfWriter, GaiaNode, GaiaPrimitive
 */
@Getter
public enum AttributeType {
    NONE("NONE", "NONE"),
    INDICE("INDICE", "INDICES"),
    POSITION("POSITION3", "POSITION"),
    NORMAL("NORMAL3", "NORMAL"),
    TEXCOORD("TEXCOORD2", "TEXCOORD_0"),
    COLOR("COLOR4", "COLOR_0"),
    BATCHID("OBJECTID", "_BATCHID");

    final String gaia;
    final String accessor;
    AttributeType(String gaia, String accessor) {
        this.gaia = gaia;
        this.accessor = accessor;
    }

    public static AttributeType getGaiaAttribute(String gaia) {
        for (AttributeType type : AttributeType.values()) {
            if (type.gaia.equals(gaia)) {
                return type;
            }
        }
        return AttributeType.NONE;
    }
}
