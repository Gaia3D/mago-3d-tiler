package geometry.exchangable;

import geometry.types.AttributeType;
import lombok.Getter;
import lombok.Setter;
import util.BinaryUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;

@Getter
@Setter
public class GaiaBufferDataSet<T> {
    private LinkedHashMap<AttributeType, GaiaBuffer> buffers;
    private int id = -1;
    private String guid = "NoGuid";
    private int materialId;

    public GaiaBufferDataSet() {
        this.buffers = new LinkedHashMap<>();
    }
    public void write(OutputStream stream) throws IOException {
        BinaryUtils.writeInt(stream, id);
        BinaryUtils.writeText(stream, guid);
        BinaryUtils.writeInt(stream, materialId);
        BinaryUtils.writeInt(stream,  buffers.size());
        buffers.forEach((attributeType, buffer) -> {
            // attributeType : length/string
            buffer.writeBuffer();
        });
    }
}
