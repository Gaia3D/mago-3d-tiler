package renderable;

import assimp.DataLoader;
import geometry.structure.*;
import geometry.types.TextureType;
import org.joml.*;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AssimpObject extends RenderableObject {
    List<RenderableBuffer> renderableBuffers;
    TextureBuffer textureBuffer;

    int[] vbos;
    boolean dirty;
    File file;
    Path path;

    public AssimpObject(String filePath) {
        super();
        this.file = new File(filePath);
        this.path = file.toPath();
        this.setPosition(0.0f, 0.0f, -1.0f);
        this.setRotation(0.0f, 0.0f, 0.0f);
    }

    @Override
    public void render(int program) {
        RenderableBuffer testRenderable = this.getBuffer(); // test
        List<RenderableBuffer> renderableBuffers = this.renderableBuffers;
        TextureBuffer textureBuffer = this.textureBuffer;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            Matrix4f objectTransformMatrix = getTransformMatrix();
            int uObjectTransformMatrix = GL20.glGetUniformLocation(program, "uObjectTransformMatrix");
            int uTextureType = GL20.glGetUniformLocation(program, "uTextureType");

            int aVertexPosition = GL20.glGetAttribLocation(program, "aVertexPosition");
            int aVertexColor = GL20.glGetAttribLocation(program, "aVertexColor");
            int aVertexNormal = GL20.glGetAttribLocation(program, "aVertexNormal");
            int aVertexTextureCoordinate = GL20.glGetAttribLocation(program, "aVertexTextureCoordinate");

            float[] objectTransformMatrixBuffer = new float[16];

            if (textureBuffer != null) {
                GL20.glUniform1i(uTextureType, 1);
                textureBuffer.setTextureBind(textureBuffer.getTextureVbo());
            } else {
                GL20.glUniform1i(uTextureType, 0);
            }
            renderableBuffers.stream().forEach((renderableBuffer) -> {
                Matrix4d nodeTransformMatrix = renderableBuffer.getTransformMatrix();
                nodeTransformMatrix.get(objectTransformMatrixBuffer);
                GL20.glUniformMatrix4fv(uObjectTransformMatrix, false, objectTransformMatrixBuffer);

                renderableBuffer.setIndiceBind(renderableBuffer.getIndicesVbo());
                renderableBuffer.setAttribute(renderableBuffer.getPositionVbo(), aVertexPosition, 3, 0);
                renderableBuffer.setAttribute(renderableBuffer.getColorVbo(), aVertexColor, 4, 0);
                renderableBuffer.setAttribute(renderableBuffer.getNormalVbo(), aVertexNormal, 3, 0);
                renderableBuffer.setAttribute(renderableBuffer.getTextureCoordinateVbo(), aVertexTextureCoordinate, 2, 0);
                GL20.glDrawElements(GL20.GL_TRIANGLES, renderableBuffer.getIndicesLength(), GL20.GL_UNSIGNED_SHORT, 0);
            });

            if (textureBuffer != null) {
                GL20.glUniform1i(uTextureType, 0);
                textureBuffer.setTextureUnbind();
            }
        }
    }

    @Override
    public RenderableBuffer getBuffer() {
        if (this.renderableBuffers == null) {
            this.renderableBuffers = new ArrayList<>();
            GaiaScene scene = DataLoader.load(file.getAbsolutePath(), null);
            //scene = FileUtil.sampleScene();

            GaiaNode rootNode = scene.getNodes().get(0);
            List<GaiaNode> children = rootNode.getChildren();
            children.stream().forEach((node) -> {
                System.out.println(node.getName());

                List<Short> indicesList = new ArrayList<Short>();
                List<Float> positionList = new ArrayList<Float>();
                List<Float> colorList = new ArrayList<Float>();
                List<Float> normalList = new ArrayList<>();
                List<Float> textureCoordinateLsit = new ArrayList<Float>();

                List<GaiaMesh> meshes = node.getMeshes();
                meshes.stream().forEach((mesh) -> {

                    List<GaiaPrimitive> primitives = mesh.getPrimitives();
                    primitives.stream().forEach((primitive) -> {
                        GaiaMaterial material = primitive.getMaterial();
                        List<GaiaTexture> textures = material.getTextures().get(TextureType.DIFFUSE);

                        List<GaiaVertex> primitiveVerticesList = primitive.getVertices();
                        List<Integer> primitiveIndicesList = primitive.getIndices();
                        primitiveVerticesList.stream().forEach((vertices) -> {

                            if (vertices.getPosition() != null) {
                                Vector3d position = vertices.getPosition();
                                positionList.add((float) position.x);
                                positionList.add((float) position.y);
                                positionList.add((float) position.z);
                            }

                            if (vertices.getNormal() != null) {
                                Vector3d normal = vertices.getNormal();
                                normalList.add((float) normal.x);
                                normalList.add((float) normal.y);
                                normalList.add((float) normal.z);
                            }

                            if (vertices.getTextureCoordinates() != null) {
                                Vector2d textureCoordinate = vertices.getTextureCoordinates();
                                textureCoordinateLsit.add((float) textureCoordinate.x);
                                textureCoordinateLsit.add((float) textureCoordinate.y);
                            }

                            Vector4d diffuseColor = material.getDiffuseColor();
                            colorList.add((float) diffuseColor.x);
                            colorList.add((float) diffuseColor.y);
                            colorList.add((float) diffuseColor.z);
                            colorList.add((float) diffuseColor.w);
                        });

                        List<Short> shortIndicesList = primitiveIndicesList.stream()
                                .map(Integer::shortValue)
                                .collect(Collectors.toList());
                        indicesList.addAll(shortIndicesList);

                        if (textures.size() > 0) {
                            // 임시 singleTexture
                            GaiaTexture texture = textures.get(0);
                            TextureBuffer textureBuffer = new TextureBuffer();
                            texture.setFormat(GL20.GL_RGB);
                            texture.setParentPath(path.getParent());
                            texture.readImage();
                            texture.loadBuffer();
                            int textureVbo = textureBuffer.createGlTexture(texture);
                            textureBuffer.setTextureVbo(textureVbo);
                            this.textureBuffer = textureBuffer;
                        }
                    });

                    RenderableBuffer renderableBuffer = new RenderableBuffer();
                    renderableBuffer.setTransformMatrix(node.getPreMultipliedTransformMatrix());

                    int indicesVbo = renderableBuffer.createIndicesBuffer(indicesList);
                    int positionVbo = renderableBuffer.createBuffer(positionList);
                    int colorVbo = renderableBuffer.createBuffer(colorList);
                    int normalVbo = renderableBuffer.createBuffer(normalList);
                    int textureCoordinateVbo = renderableBuffer.createBuffer(textureCoordinateLsit);

                    renderableBuffer.setPositionVbo(positionVbo);
                    renderableBuffer.setColorVbo(colorVbo);
                    renderableBuffer.setNormalVbo(normalVbo);
                    renderableBuffer.setTextureCoordinateVbo(textureCoordinateVbo);
                    renderableBuffer.setIndicesVbo(indicesVbo);
                    renderableBuffer.setIndicesLength(indicesList.size());

                    this.renderableBuffers.add(renderableBuffer);
                });
            });
        }
        return null;
    }
}
