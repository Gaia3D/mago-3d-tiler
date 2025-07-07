package com.gaia3d.basic.types;

import lombok.Getter;

import java.io.Serializable;

/**
 * Enumerates the types of attributes.
 */
@Getter
public enum AttributeType implements Serializable {
    NONE("NONE", "NONE"),
    INDICE("INDICE", "INDICES"),
    POSITION("POSITION3", "POSITION"),
    NORMAL("NORMAL3", "NORMAL"),
    TEXCOORD("TEXCOORD2", "TEXCOORD_0"),
    COLOR("COLOR4", "COLOR_0"),

    BATCHID("BATCHID", "_BATCHID"),
    CLASSIFICATION("CLASSIFICATION", "_CLASSIFICATION"),
    INTENSITY("INTENSITY", "_INTENSITY"),

    FEATURE_ID_0("_FEATURE_ID_0", "_FEATURE_ID_0"),
    FEATURE_ID_1("_FEATURE_ID_1", "_FEATURE_ID_1");

    final String name;
    final String accessor;

    AttributeType(String name, String accessor) {
        this.name = name;
        this.accessor = accessor;
    }
}
