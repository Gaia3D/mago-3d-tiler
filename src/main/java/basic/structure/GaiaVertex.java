package basic.structure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;

/**
 * A class that represents a vertex of a Gaia object.
 * It contains the texture coordinates, position, normal, color, and batchId.
 * @author znkim
 * @since 1.0.0
 * @see <a href="https://en.wikipedia.org/wiki/Vertex_(geometry)">Vertex (geometry)</a>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaVertex {
    private Vector2d texcoords;
    private Vector3d position;
    private Vector3d normal;
    private byte[] color;
    private float batchId;
}
