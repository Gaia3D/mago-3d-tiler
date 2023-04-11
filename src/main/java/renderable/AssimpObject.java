package renderable;

import assimp.DataLoader;
import geometry.structure.*;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AssimpObject extends RenderableObject {
    RenderableBuffer renderableBuffer;
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
        RenderableBuffer renderableBuffer = this.getBuffer();
        TextureBuffer textureBuffer = this.textureBuffer;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            Matrix4f objectRotationMatrix = getTransformMatrix();
            int uObjectRotationMatrix = GL20.glGetUniformLocation(program, "uObjectRotationMatrix");
            int uTextureType = GL20.glGetUniformLocation(program, "uTextureType");

            float[] objectRotationMatrixBuffer = new float[16];
            objectRotationMatrix.get(objectRotationMatrixBuffer);

            GL20.glUniformMatrix4fv(uObjectRotationMatrix, false, objectRotationMatrixBuffer);

            int aVertexPosition = GL20.glGetAttribLocation(program, "aVertexPosition");
            int aVertexColor = GL20.glGetAttribLocation(program, "aVertexColor");
            int aVertexNormal = GL20.glGetAttribLocation(program, "aVertexNormal");
            int aVertexTextureCoordinate = GL20.glGetAttribLocation(program, "aVertexTextureCoordinate");

            GL20.glUniform1i(uTextureType, 1);
            int uTexture = GL20.glGetUniformLocation(program, "uTexture");
            textureBuffer.setTextureBind(textureBuffer.getTextureVbo());

            renderableBuffer.setIndiceBind(renderableBuffer.getIndicesVbo());
            renderableBuffer.setAttribute(renderableBuffer.getPositionVbo(), aVertexPosition, 3, 0);
            renderableBuffer.setAttribute(renderableBuffer.getColorVbo(), aVertexColor, 4, 0);
            renderableBuffer.setAttribute(renderableBuffer.getNormalVbo(), aVertexNormal, 3, 0);
            renderableBuffer.setAttribute(renderableBuffer.getTextureCoordinateVbo(), aVertexTextureCoordinate, 2, 0);

            GL20.glDrawElements(GL20.GL_TRIANGLES, renderableBuffer.getIndicesLength(), GL20.GL_UNSIGNED_SHORT, 0);
            GL20.glUniform1i(uTextureType, 0);
            textureBuffer.setTextureUnbind();
        }
    }
    @Override
    public RenderableBuffer getBuffer() {
        if (this.renderableBuffer == null) {
            ArrayList<Short> indicesList = new ArrayList<Short>();
            ArrayList<Float> positionList = new ArrayList<Float>();
            ArrayList<Float> colorList = new ArrayList<Float>();
            ArrayList<Float> normalList = new ArrayList<Float>();
            ArrayList<Float> textureCoordinateLsit = new ArrayList<Float>();

            GaiaScene scene = DataLoader.load(file.getAbsolutePath(), null);
            //scene = FileUtil.sampleScene();

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
                            Vector3d position = vertices.getPosition();
                            positionList.add((float) position.x);
                            positionList.add((float) position.z);
                            positionList.add((float) position.y);

                            Vector3d normal = vertices.getNormal();
                            normalList.add((float) normal.x);
                            normalList.add((float) normal.y);
                            normalList.add((float) normal.z);

                            Vector2d textureCoordinate = vertices.getTextureCoordinates();
                            textureCoordinateLsit.add((float) textureCoordinate.x);
                            textureCoordinateLsit.add((float) textureCoordinate.y);

                            colorList.add(0.4f);
                            colorList.add(0.4f);
                            colorList.add(0.8f);
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
            TextureBuffer textureBuffer = new TextureBuffer();

            int indicesVbo = renderableBuffer.createIndicesBuffer(indicesList);
            int positionVbo = renderableBuffer.createBuffer(positionList);
            int colorVbo = renderableBuffer.createBuffer(colorList);
            int normalVbo = renderableBuffer.createBuffer(normalList);
            int textureCoordinateVbo = renderableBuffer.createBuffer(textureCoordinateLsit);



            //String diffusePath = gaiaTexture.getPath();

            //String imagePath = "C:\\data\\sample\\grid.jpg";
            //String imagePath = path.getParent() + File.separator + diffusePath;
            //System.out.println("DIFFUSE_PATH : " + imagePath);

//            GaiaMaterial material = scene.getMaterials().get(0);
//            GaiaTexture gaiaTexture = material.getTextures().get(GaiaMaterial.MaterialType.DIFFUSE);
//            gaiaTexture.setFormat(GL20.GL_RGB);
//            gaiaTexture.readImage(path.getParent());
//            gaiaTexture.loadBuffer();
//            int textureVbo = textureBuffer.createGlTexture(gaiaTexture);


            //BufferedImage image = FileUtil.readImage(imagePath);
            //int textureVbo = textureBuffer.makeTexture(image, GL20.GL_RGBA);
            //int textureVbo = textureBuffer.createTexture(image);

            /*IntBuffer x = BufferUtils.createIntBuffer(1);
            IntBuffer y = BufferUtils.createIntBuffer(1);
            IntBuffer channels = BufferUtils.createIntBuffer(1);
            ByteBuffer image = STBImage.stbi_load(imagePath, x, y, channels, STBImage.STBI_rgb_alpha);
            if (image == null) {
                System.err.println("Could not decode image file ["+ imagePath +"]: ["+ STBImage.stbi_failure_reason() +"]");
            }*/



            //int textureVbo = textureBuffer.createTexture(image, x.get(), y.get(), GL20.GL_RGBA8, GL20.GL_RGBA);

            //ByteBuffer imageByteBuffer = FileUtil.convertByteBufferToBufferdImage(image);

            //int textureVbo = textureBuffer.createTexture(byteBuffer, image.getWidth(), image.getHeight(), GL20.GL_RGBA, GL20.GL_RGBA);
            //System.out.println("textureVbo: " + textureVbo);

            renderableBuffer.setPositionVbo(positionVbo);
            renderableBuffer.setColorVbo(colorVbo);
            renderableBuffer.setNormalVbo(normalVbo);
            renderableBuffer.setTextureCoordinateVbo(textureCoordinateVbo);
            renderableBuffer.setIndicesVbo(indicesVbo);
            renderableBuffer.setIndicesLength(indicesList.size());
            //textureBuffer.setTextureVbo(textureVbo);
            this.renderableBuffer = renderableBuffer;
            this.textureBuffer = textureBuffer;
        }
        return this.renderableBuffer;
    }
}
