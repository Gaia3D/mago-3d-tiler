package geometry.exchangable;

import geometry.types.AttributeType;
import lombok.Getter;
import lombok.Setter;
import util.BinaryUtils;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class GaiaBufferDataSet<T> {
    private LinkedHashMap<AttributeType, GaiaBuffer> buffers;
    private int id = -1;
    private String guid = "no_guid";
    private int materialId;

    public GaiaBufferDataSet() {
        this.buffers = new LinkedHashMap<>();
    }
    public void write(DataOutputStream stream) throws IOException {
        BinaryUtils.writeInt(stream, id);
        BinaryUtils.writeText(stream, guid);
        BinaryUtils.writeInt(stream, materialId);
        BinaryUtils.writeInt(stream,  buffers.size());
        for (Map.Entry<AttributeType, GaiaBuffer> entry : buffers.entrySet()) {
            AttributeType attributeType = entry.getKey();
            GaiaBuffer buffer = entry.getValue();
            BinaryUtils.writeText(stream, attributeType.getName());
            buffer.writeBuffer(stream);
        }
    }
}
