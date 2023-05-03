package geometry.types;

import lombok.Getter;

@Getter
public enum AttributeType {
    INDICE("INDICE"),
    POSITION("POSITION3"),
    NORMAL("NORMAL3"),
    TEXCOORD_0("TEXCOORD2"),
    COLOR_0("COLOR4"),
    _BATHCHID("OBJECTID");

    String name;
    AttributeType(String name) {
        this.name = name;
    }
}
