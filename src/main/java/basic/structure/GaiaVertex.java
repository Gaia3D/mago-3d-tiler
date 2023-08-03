package basic.structure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaVertex {
    private Vector2d texcoords;
    private Vector3d position;
    private Vector3d normal;
    private Vector4d color;
    private float batchId;
}
