package com.gaia3d.basic.structure;

import com.gaia3d.basic.structure.interfaces.AttributeStructure;
import com.gaia3d.util.io.BigEndianDataInputStream;
import com.gaia3d.util.io.BigEndianDataOutputStream;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class GaiaAttribute extends AttributeStructure implements Serializable {
    public void write(BigEndianDataOutputStream stream) throws IOException {
        stream.writeText(identifier.toString());
        stream.writeText(fileName);
        stream.writeText(nodeName);
        stream.writeInt(attributes.size());
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            stream.writeText(entry.getKey());
            stream.writeText(entry.getValue());
        }
    }

    public void read(BigEndianDataInputStream stream) throws IOException {
        setIdentifier(UUID.fromString(stream.readText()));
        setFileName(stream.readText());
        setNodeName(stream.readText());
        int size = stream.readInt();
        for (int i = 0; i < size; i++) {
            String key = stream.readText();
            String value = stream.readText();
            attributes.put(key, value);
        }
    }
}