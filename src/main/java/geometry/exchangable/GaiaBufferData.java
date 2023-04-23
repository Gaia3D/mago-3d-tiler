package geometry.exchangable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

@Getter
@Setter
@NoArgsConstructor
public class GaiaBufferData<T> {
    // ByteBuffer FloatBuffer
    private byte[] byteBuffer;

    public GaiaBufferData(byte[] bytes) {

    }

    public void writeBytes() {
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(this.byteBuffer.length);
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(new File("")))) {
            ;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
