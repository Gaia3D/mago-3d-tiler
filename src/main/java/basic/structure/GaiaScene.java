package basic.structure;

import basic.geometry.GaiaBoundingBox;
import basic.exchangable.GaiaBufferDataSet;
import basic.exchangable.GaiaSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that represents a scene of a Gaia object.
 * The largest unit of the 3D file.
 * It contains the nodes and materials.
 * @author znkim
 * @since 1.0.0
 * @see <a href="https://en.wikipedia.org/wiki/3D_computer_graphics">3D computer graphics</a>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaScene {
    private List<GaiaNode> nodes = new ArrayList<>();
    private List<GaiaMaterial> materials = new ArrayList<>();
    private Path originalPath;
    private GaiaBoundingBox gaiaBoundingBox;

    public GaiaScene(GaiaSet gaiaSet) {
        List<GaiaBufferDataSet> bufferDataSets = gaiaSet.getBufferDatas();
        List<GaiaMaterial> materials = gaiaSet.getMaterials();

        //Matrix4d transformMatrix = gaiaSet.getTransformMatrix();
        Matrix4d transformMatrix = new Matrix4d();
        transformMatrix.identity();
        //transformMatrix.rotateX(Math.toRadians(-90)); // y and z axis swap

        GaiaNode rootNode = new GaiaNode();
        rootNode.setName("BatchedRootNode");
        rootNode.setTransformMatrix(transformMatrix);
        this.materials = materials;
        this.nodes.add(rootNode);

        bufferDataSets.forEach((bufferDataSet) -> rootNode.getChildren().add(new GaiaNode(bufferDataSet)));
    }

    public GaiaBoundingBox getBoundingBox() {
        this.gaiaBoundingBox = new GaiaBoundingBox();
        for (GaiaNode node : this.getNodes()) {
            GaiaBoundingBox boundingBox = node.getBoundingBox(null);
            if(boundingBox != null) {
                gaiaBoundingBox.addBoundingBox(boundingBox);
            }
        }
        return this.gaiaBoundingBox;
    }
}
