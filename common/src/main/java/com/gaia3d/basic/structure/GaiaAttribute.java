package com.gaia3d.basic.structure;

import com.gaia3d.basic.structure.interfaces.AttributeStructure;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class GaiaAttribute extends AttributeStructure implements Serializable {

    public GaiaAttribute getCopy() {
        GaiaAttribute gaiaAttribute = new GaiaAttribute();
        gaiaAttribute.setIdentifier(this.getIdentifier());
        gaiaAttribute.setFileName(this.getFileName());
        gaiaAttribute.setNodeName(this.getNodeName());
        Map<String, String> attributes = this.getAttributes();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            gaiaAttribute.getAttributes().put(entry.getKey(), entry.getValue());
        }
        return gaiaAttribute;
    }
}