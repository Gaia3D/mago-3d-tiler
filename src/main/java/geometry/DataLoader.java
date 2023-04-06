package geometry;

import org.joml.Vector2d;
import org.joml.Vector3d;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryUtil;
import util.FileUtil;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

/**
 * Loads a scene from a file
 */
public class DataLoader {

    /**
     * Default flags for Assimp import process
     */
    final static int DEFAULT_FLAGS = Assimp.aiProcess_GenNormals
            |Assimp.aiProcess_Triangulate
            |Assimp.aiProcess_JoinIdenticalVertices
            |Assimp.aiProcess_CalcTangentSpace
            |Assimp.aiProcess_SortByPType
            //|Assimp.aiProcess_FixInfacingNormals 이거 아님\
            |Assimp.aiProcess_FlipWindingOrder;

    /** Loads a scene from a filePath
     * @param filePath 파일 경로
     * @param hint 파일 확장자
     * @return
     */
    public static GaiaScene load(String filePath, String hint) {
        return load(new File(filePath), hint);
    }

    /**
     * Loads a scene from a file
     * @param file 파일
     * @param hint 파일 확장자
     * @return
     */
    public static GaiaScene load(File file, String hint) {
        String path = file.getAbsolutePath().replace(file.getName(), "");

        ByteBuffer byteBuffer = FileUtil.readFile(file);
        hint = (hint != null) ? hint : FileUtil.getExtension(file.getName());

        //AIScene aiScene = Assimp.aiImportFile(path, flags);
        AIScene aiScene = Assimp.aiImportFileFromMemory(byteBuffer, DEFAULT_FLAGS, hint);
        GaiaScene gaiaScene = convertScene(aiScene, path);

        aiScene.free();
        //MemoryUtil.memFree(byteBuffer);
        return gaiaScene;
    }

    /**
     * Converts an Assimp scene to a Gaia scene
     * @param aiScene Assimp scene
     * @param filePath 서브파일 경로
     * @return Gaia scene
     */
    public static GaiaScene convertScene(AIScene aiScene, String filePath) {
        GaiaScene scene = new GaiaScene();
        AINode aiNode = aiScene.mRootNode();

        int numMaterials = aiScene.mNumMaterials();
        PointerBuffer aiMaterials = aiScene.mMaterials();
        for (int i = 0; i < numMaterials; i++) {
            AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(i));
            scene.getMaterials().add(processMaterial(aiMaterial, filePath));
        }
        GaiaNode node = processNode(aiScene, aiNode, null);
        scene.getNodes().add(node);
        return scene;
    }

    /**
     * Converts an Assimp node to a Gaia node
     * @param aiMaterial Assimp material
     * @param path 서브파일 경로
     * @return Gaia node
     */
    private static GaiaMaterial processMaterial(AIMaterial aiMaterial, String path) {
        //PointerBuffer aiTextures = aiScene.mTextures();
        GaiaMaterial material = new GaiaMaterial();

        AIString diffPath = AIString.calloc();
        Assimp.aiGetMaterialTexture(aiMaterial, Assimp.aiTextureType_DIFFUSE, 0, diffPath, (IntBuffer) null, null, null, null, null, null);
        String diffTexPath = diffPath.dataString();
        //System.out.println(diffTexPath);

        GaiaTexture texture  = null;
        if (diffTexPath != null && diffTexPath.length() > 0) {
            //TextureCache textureCache = TextureCache.getInstance();
            //diffuseTexture = new TextureImage2D(path + "/" + diffTexPath, SamplerFilter.Trilinear);
        }

        int numProperties = aiMaterial.mNumProperties();
        PointerBuffer pointerBuffer = aiMaterial.mProperties();
        for (int i = 0; i < numProperties; i++) {
            AIMaterialProperty aiMaterialProperty = AIMaterialProperty.create(pointerBuffer.get(i));
            aiMaterialProperty.mData();
            //System.out.println(aiMaterialProperty.mKey().dataString());
            //System.out.println(aiMaterialProperty.mType());
        }
        return material;
    }

    /**
     * Converts an Assimp node to a Gaia node
     * @param aiScene
     * @param aiNode
     * @param parentNode
     * @return Gaia node
     */
    private static GaiaNode processNode(AIScene aiScene, AINode aiNode, GaiaNode parentNode) {
        PointerBuffer aiMeshes = aiScene.mMeshes();
        GaiaNode node = new GaiaNode();
        node.setParent(parentNode);

        int numMeshes = aiNode.mNumMeshes();
        for (int i = 0; i < numMeshes; i++) {
            AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
            GaiaMesh mesh = processMesh(aiMesh);
            node.getMeshes().add(mesh);
        }

        int numChildren = aiNode.mNumChildren();
        PointerBuffer childrenBuffer = aiNode.mChildren();
        for (int i = 0; i < numChildren; i++) {
            AINode aiChildNode = AINode.create(childrenBuffer.get(i));
            GaiaNode childNode = processNode(aiScene, aiChildNode, node);
            node.getChildren().add(childNode);
        }
        return node;
    }

    /**
     * Converts an Assimp mesh to a Gaia mesh
     * @param aiMesh Assimp mesh
     * @return Gaia mesh
     */
    private static GaiaMesh processMesh(AIMesh aiMesh) {
        GaiaPrimitive primitive = processPrimitive(aiMesh);
        GaiaMesh mesh = new GaiaMesh();
        mesh.getPrimitives().add(primitive);
        return mesh;
    }

    /**
     * Converts an Assimp surface to a Gaia surface
     * @param aiMesh Assimp mesh
     * @return Gaia surface
     */
    private static GaiaPrimitive processPrimitive(AIMesh aiMesh) {
        GaiaSurface surface = processSurface();

        GaiaPrimitive primitive = new GaiaPrimitive();
        primitive.getSurfaces().add(surface);

        int numFaces = aiMesh.mNumFaces();
        AIFace.Buffer facesBuffer = aiMesh.mFaces();
        for (int i = 0; i < numFaces; i++) {
            AIFace aiFace = facesBuffer.get(i);
            GaiaFace face = processFace(aiFace);
            surface.getFaces().add(face);

            //ArrayList<Integer> indices = face.getIndices();
            /*if (indices.size() == 3) {
                System.out.println(indices);
                primitive.getIndices().add(indices.get(0));
                primitive.getIndices().add(indices.get(1));
                primitive.getIndices().add(indices.get(2));
            }*/

            face.getIndices().stream().forEach((indices) -> {
                primitive.getIndices().add(indices);
            });
        }


        int mNumVertices = aiMesh.mNumVertices();
        AIVector3D.Buffer verticesBuffer = aiMesh.mVertices();
        AIVector3D.Buffer normalsBuffer = aiMesh.mNormals();
        AIVector3D.Buffer textCoords = aiMesh.mTextureCoords(0);

//        System.out.println("mNumVertices: " + mNumVertices);
//        System.out.println("verticesBuffer: " + verticesBuffer.remaining());
//        System.out.println("normalsBuffer: " + normalsBuffer.remaining());
//        System.out.println("textCoords: " + textCoords.remaining());

        /*int numTextCoords = textCoords != null ? textCoords.remaining() : 0;
        for (int i = 0; i < numTextCoords; i++) {
            AIVector3D textCoord = textCoords.get();
        }*/
        for (int i = 0; i < mNumVertices; i++) {
            AIVector3D aiVertice = verticesBuffer.get(i);
            AIVector3D aiNormal = normalsBuffer.get(i);
            AIVector3D textCoord = textCoords.get(i);
            GaiaVertex vertex = new GaiaVertex();
            vertex.setPosition(new Vector3d((double) aiVertice.x(), (double) aiVertice.z(), (double) aiVertice.y()));
            vertex.setNormal(new Vector3d((double) aiNormal.x(), (double) aiNormal.z(), (double) aiNormal.y()));
            vertex.setTextureCoordinates(new Vector2d((double) textCoord.x(), 1.0 - (double) textCoord.y()));
            //System.out.println(vertex.getTextureCoordinates().x + ", " + vertex.getTextureCoordinates().y);
            primitive.getVertices().add(vertex);
        }

        Rectangle2D rectangle2D = getTextureCoordsBoundingRect(primitive);

        return primitive;
    }

    // getTextureCoordsBoundingRect
    private static Rectangle2D getTextureCoordsBoundingRect(GaiaPrimitive primitive) {
        Rectangle2D rect = new Rectangle2D.Double();
        for (GaiaVertex vertex : primitive.getVertices()) {
            Vector2d textureCoordinates = vertex.getTextureCoordinates();
            if (textureCoordinates.x < rect.getMinX()) {
                rect.setRect(textureCoordinates.x, rect.getMinY(), rect.getWidth(), rect.getHeight());
            }
            if (textureCoordinates.y < rect.getMinY()) {
                rect.setRect(rect.getMinX(), textureCoordinates.y, rect.getWidth(), rect.getHeight());
            }
            if (textureCoordinates.x > rect.getMaxX()) {
                rect.setRect(rect.getMinX(), rect.getMinY(), textureCoordinates.x, rect.getHeight());
            }
            if (textureCoordinates.y > rect.getMaxY()) {
                rect.setRect(rect.getMinX(), rect.getMinY(), rect.getWidth(), textureCoordinates.y);
            }
        }
        return rect;
    }

    /**
     * Converts an Assimp surface to a Gaia surface
     * @return
     */
    private static GaiaSurface processSurface() {
        GaiaSurface surface = new GaiaSurface();
        return surface;
    }

    /**
     * Converts an Assimp face to a Gaia face
     * @param aiFace
     * @return
     */
    private static GaiaFace processFace(AIFace aiFace) {
        GaiaFace face = new GaiaFace();
        int numIndices = aiFace.mNumIndices();
        IntBuffer indicesBuffer = aiFace.mIndices();
        for (int i = 0; i < numIndices; i++) {
            int indices = indicesBuffer.get(i);
            face.getIndices().add(indices);
        }
        return face;
    }
}
