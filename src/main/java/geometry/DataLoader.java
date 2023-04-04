package geometry;

import org.joml.Vector3d;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;

public class DataLoader {

    final static int DEFAULT_FLAGS = Assimp.aiProcess_GenNormals
            |Assimp.aiProcess_Triangulate
            |Assimp.aiProcess_JoinIdenticalVertices
            |Assimp.aiProcess_CalcTangentSpace
            |Assimp.aiProcess_SortByPType;

    private static byte[] readBytes(File file) {
        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static GaiaScene load(String filePath) {
        GaiaScene scene = new GaiaScene();

        File file = new File(filePath);
        String path = file.getAbsolutePath().replace(file.getName(), "");
        //System.out.println(path);

        byte[] bytes = readBytes(file);
        ByteBuffer byteBuffer = MemoryUtil.memCalloc(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();

        //Assimp.aiTextureType_SPECULAR;
        //AIScene aiScene = Assimp.aiImportFile(path, flags);
        AIScene aiScene = Assimp.aiImportFileFromMemory(byteBuffer, DEFAULT_FLAGS, "3ds");
        AINode aiNode = aiScene.mRootNode();

        int numMaterials = aiScene.mNumMaterials();
        PointerBuffer aiMaterials = aiScene.mMaterials();
        for (int i = 0; i < numMaterials; i++) {
            AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(i));
            scene.getMaterials().add(processMaterial(aiMaterial, path));
        }

        GaiaNode node = processNode(aiScene, aiNode, null);
        scene.getNodes().add(node);

        aiNode.free();
        MemoryUtil.memFree(byteBuffer);
        return scene;
    }

    private static GaiaMaterial processMaterial(AIMaterial aiMaterial, String path) {
        //ointerBuffer aiTextures = aiScene.mTextures();
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

    private static GaiaNode processNode(AIScene aiScene, AINode aiNode, GaiaNode parentNode) {
        PointerBuffer aiMeshes = aiScene.mMeshes();
        GaiaNode node = new GaiaNode();
        node.setParent(parentNode);

        int numMeshes = aiNode.mNumMeshes();
        for (int i = 0; i < numMeshes; i++) {
            AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
            node.getMeshes().add(processMesh(aiMesh));
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
    private static GaiaMesh processMesh(AIMesh aiMesh) {
        GaiaPrimitive primitive = processPrimitive(aiMesh);
        GaiaMesh mesh = new GaiaMesh();
        mesh.getPrimitives().add(primitive);
        return mesh;
    }
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
            face.getIndices().stream().forEach((indices) -> {
                primitive.getIndices().add(indices);
            });
            //aiFace.free();
        }

        int mNumVertices = aiMesh.mNumVertices();
        AIVector3D.Buffer verticesBuffer = aiMesh.mVertices();
        for (int i = 0; i < mNumVertices; i++) {
            AIVector3D aiVector3D = verticesBuffer.get(i);
            GaiaVertex vertex = new GaiaVertex();
            vertex.setPosition(new Vector3d((double) aiVector3D.x(), (double) aiVector3D.z(), (double) aiVector3D.y()));
            primitive.getVertices().add(vertex);
            //aiVector3D.free();
        }
        //System.out.println(numFaces + "::" + mNumVertices);
        return primitive;
    }
    private static GaiaSurface processSurface() {
        GaiaSurface surface = new GaiaSurface();
        return surface;
    }
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
