package geometry;

import org.joml.Vector3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.ArrayList;

public class DataLoader {
    private static byte[] ioResourceToBytes(String path) {
        //Files file = new File(path);
        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(new File(path).toPath());
            //byteBuffer = ByteBuffer.wrap(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static Scene load(String path) {
        System.out.println("Scene");
        Scene scene = new Scene();
        scene.setNodes(new ArrayList<Node>());

        int flags = Assimp.aiProcess_GenNormals
                |Assimp.aiProcess_Triangulate
                |Assimp.aiProcess_JoinIdenticalVertices
                |Assimp.aiProcess_CalcTangentSpace
                |Assimp.aiProcess_SortByPType;
        byte[] bytes = ioResourceToBytes(path);

        ByteBuffer byteBuffer = MemoryUtil.memCalloc(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        //Assimp.aiTextureType_SPECULAR;

        AIScene aiScene = Assimp.aiImportFileFromMemory(byteBuffer, flags, "dae");
        //AIScene aiScene = Assimp.aiImportFile(path, flags);
        AINode aiNode = aiScene.mRootNode();
        Node node = processNode(aiScene, aiNode, null);
        aiNode.free();
        scene.getNodes().add(node);
        MemoryUtil.memFree(byteBuffer);
        return scene;
    }
    private static Node processNode(AIScene aiScene, AINode aiNode, Node parentNode) {
        System.out.println("Node");
        ArrayList<Mesh> meshes = new ArrayList<Mesh>();
        PointerBuffer aiMeshes = aiScene.mMeshes();

        Node node = new Node();
        node.setParent(parentNode);
        node.setChildren(new ArrayList<Node>());
        node.setMeshes(meshes);

        int numMeshes = aiNode.mNumMeshes();
        IntBuffer meshesBuffer = aiNode.mMeshes();
        for (int i = 0; i < numMeshes; i++) {
            AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
            meshes.add(processMesh(aiMesh));
            aiMesh.free();
        }

        int numChildren = aiNode.mNumChildren();
        PointerBuffer childrenBuffer = aiNode.mChildren();
        for (int i = 0; i < numChildren; i++) {
            AINode aiChildNode = AINode.create(childrenBuffer.get(i));
            Node childNode = processNode(aiScene, aiChildNode, node);
            node.getChildren().add(childNode);
            aiChildNode.free();
        }
        return node;
    }
    private static Mesh processMesh(AIMesh aiMesh) {
        System.out.println("Mesh");
        Primitive primitive = processPrimitive(aiMesh);
        Mesh mesh = new Mesh();
        mesh.setPrimitives(new ArrayList<Primitive>());
        mesh.getPrimitives().add(primitive);
        return mesh;
    }
    private static Primitive processPrimitive(AIMesh aiMesh) {
        System.out.println("Primitive");
        Surface surface = processSurface();

        Primitive primitive = new Primitive();
        primitive.setIndices(new ArrayList<Integer>());
        primitive.setVertices(new ArrayList<Vertex>());
        primitive.setSurfaces(new ArrayList<Surface>());
        primitive.getSurfaces().add(surface);

        int numFaces = aiMesh.mNumFaces();
        AIFace.Buffer facesBuffer = aiMesh.mFaces();
        for (int i = 0; i < numFaces; i++) {
            AIFace aiFace = facesBuffer.get(i);
            Face face = processFace(aiFace);
            surface.getFaces().add(face);
            face.getIndices().stream().forEach((indices) -> {
                primitive.getIndices().add(indices);
            });
        }

        int mNumVertices = aiMesh.mNumVertices();

        System.out.println(numFaces + "::" + mNumVertices);
        AIVector3D.Buffer verticesBuffer = aiMesh.mVertices();
        for (int i = 0; i < mNumVertices; i++) {
            AIVector3D aiVector3D = verticesBuffer.get(i);
            Vertex vertex = new Vertex();
            vertex.setPosition(new Vector3d((double) aiVector3D.x(), (double) aiVector3D.z(), (double) aiVector3D.y()));
            primitive.getVertices().add(vertex);
            //System.out.println(aiVector3D.x() + "," + aiVector3D.y() + "," + aiVector3D.z());
        }
        return primitive;
    }
    private static Surface processSurface() {
        System.out.println("Surface");
        Surface surface = new Surface();
        surface.setFaces(new ArrayList<Face>());
        return surface;
    }
    private static Face processFace(AIFace aiFace) {
        System.out.println("Face");
        Face face = new Face();
        face.setIndices(new ArrayList<Integer>());

        int numIndices = aiFace.mNumIndices();
        IntBuffer indicesBuffer = aiFace.mIndices();
        for (int i = 0; i < numIndices; i++) {
            int indices = indicesBuffer.get(i);
            face.getIndices().add(indices);
        }
        return face;
    }
}
