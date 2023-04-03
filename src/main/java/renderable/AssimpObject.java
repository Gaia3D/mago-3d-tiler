package renderable;

import geometry.*;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AssimpObject extends RenderableObject {
    RenderableBuffer renderableBuffer;
    int[] vbos;
    boolean dirty;
    String path;

    public AssimpObject(String path) {
        super();
        //path = "C:\\data\\cesium-ion-converted\\ws2-before\\a_bd001_d.dae";
        this.path = path;
        this.setPosition(0.0f, 0.0f, -1.0f);
        this.setRotation(0.0f, 0.0f, 0.0f);
    }
    @Override
    public void render(int program) {
        RenderableBuffer renderableBuffer = this.getBuffer();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            Matrix4f objectRotationMatrix = getTransformMatrix();
            int uObjectRotationMatrix = GL20.glGetUniformLocation(program, "uObjectRotationMatrix");
            float[] objectRotationMatrixBuffer = new float[16];
            objectRotationMatrix.get(objectRotationMatrixBuffer);

            GL20.glUniformMatrix4fv(uObjectRotationMatrix, false, objectRotationMatrixBuffer);

            int aVertexPosition = GL20.glGetAttribLocation(program, "aVertexPosition");
            int aVertexColor = GL20.glGetAttribLocation(program, "aVertexColor");

            renderableBuffer.setIndiceBind(renderableBuffer.getIndicesVbo());
            renderableBuffer.setAttribute(renderableBuffer.getPositionVbo(), aVertexPosition, 3, 0);
            renderableBuffer.setAttribute(renderableBuffer.getColorVbo(), aVertexColor, 4, 0);

            GL20.glDrawElements(GL20.GL_TRIANGLES, renderableBuffer.getIndicesLength(), GL20.GL_UNSIGNED_SHORT, 0);
        }
    }
    @Override
    public RenderableBuffer getBuffer() {
        if (this.renderableBuffer == null) {
            ArrayList<Short> indicesList = new ArrayList<Short>();
            ArrayList<Float> positionList = new ArrayList<Float>();
            ArrayList<Float> colorList = new ArrayList<Float>();

            GaiaScene scene = DataLoader.load(path);
            GaiaNode rootNode = scene.getNodes().get(0);
            ArrayList<GaiaNode> children = rootNode.getChildren();
            children.stream().forEach((node) -> {
                ArrayList<GaiaMesh> meshes = node.getMeshes();
                meshes.stream().forEach((mesh) -> {
                    ArrayList<GaiaPrimitive> primitives = mesh.getPrimitives();
                    primitives.stream().forEach((primitive) -> {
                        ArrayList<GaiaVertex> primitiveVerticesList = primitive.getVertices();
                        ArrayList<Integer> primitiveIndicesList = primitive.getIndices();
                        primitiveVerticesList.stream().forEach((vertices) -> {
                            Vector3d vector3d = vertices.getPosition();
                            positionList.add((float) vector3d.x);
                            positionList.add((float) vector3d.z);
                            positionList.add((float) vector3d.y);
                            colorList.add(0.6f);
                            colorList.add(0.6f);
                            colorList.add(0.6f);
                            colorList.add(1.0f);
                        });

                        List<Short> shortIndicesList = primitiveIndicesList.stream().map((indices) -> {
                            int indicesInt = indices;
                            short indicesShort = (short) indicesInt;
                            return Short.valueOf(indicesShort);
                        }).collect(Collectors.toList());
                        indicesList.addAll(shortIndicesList);
                    });
                });
            });

            RenderableBuffer renderableBuffer = new RenderableBuffer();

            int indicesVbo = renderableBuffer.createIndicesBuffer(indicesList);
            int positionVbo = renderableBuffer.createBuffer(positionList);
            int colorVbo = renderableBuffer.createBuffer(colorList);

            renderableBuffer.setPositionVbo(positionVbo);
            renderableBuffer.setColorVbo(colorVbo);
            renderableBuffer.setIndicesVbo(indicesVbo);
            renderableBuffer.setIndicesLength(indicesList.size());
            this.renderableBuffer = renderableBuffer;
        }
        return this.renderableBuffer;
    }
}
