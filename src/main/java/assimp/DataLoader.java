package assimp;

import de.javagl.jgltf.impl.v2.Material;
import geometry.basic.GaiaBoundingBox;
import geometry.structure.*;
import org.joml.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import util.FileUtils;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.lang.Math;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
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
            |Assimp.aiProcess_FixInfacingNormals
            |Assimp.aiProcess_FlipWindingOrder
            //Assimp.aiProcess_ConvertToLeftHanded
            |Assimp.aiProcess_SortByPType;


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

        ByteBuffer byteBuffer = FileUtils.readFile(file);
        hint = (hint != null) ? hint : FileUtils.getExtension(file.getName());

        //AIScene aiScene = Assimp.aiImportFile(path, flags);
        AIScene aiScene = Assimp.aiImportFileFromMemory(byteBuffer, DEFAULT_FLAGS, hint);
        GaiaScene gaiaScene = convertScene(aiScene, path);

        aiScene.free();
        //MemoryUtil.memFree(byteBuffer);
        return gaiaScene;
    }

    // convertMatrix4dFromAIMatrix4x4
    private static Matrix4d convertMatrix4dFromAIMatrix4x4(AIMatrix4x4 aiMatrix4x4) {
        Matrix4d matrix4 = new Matrix4d();
        matrix4.m00(aiMatrix4x4.a1());
        matrix4.m01(aiMatrix4x4.b1());
        matrix4.m02(aiMatrix4x4.c1());
        matrix4.m03(aiMatrix4x4.d1());
        matrix4.m10(aiMatrix4x4.a2());
        matrix4.m11(aiMatrix4x4.b2());
        matrix4.m12(aiMatrix4x4.c2());
        matrix4.m13(aiMatrix4x4.d2());
        matrix4.m20(aiMatrix4x4.a3());
        matrix4.m21(aiMatrix4x4.b3());
        matrix4.m22(aiMatrix4x4.c3());
        matrix4.m23(aiMatrix4x4.d3());
        matrix4.m30(aiMatrix4x4.a4());
        matrix4.m31(aiMatrix4x4.b4());
        matrix4.m32(aiMatrix4x4.c4());
        matrix4.m33(aiMatrix4x4.d4());
        return matrix4;
    }


    /**
     * Converts an Assimp scene to a Gaia scene
     * @param aiScene Assimp scene
     * @param filePath 서브파일 경로
     * @return Gaia scene
     */
    public static GaiaScene convertScene(AIScene aiScene, String filePath) {
        GaiaScene gaiaScene = new GaiaScene();
        AINode aiNode = aiScene.mRootNode();

        int numMaterials = aiScene.mNumMaterials();
        PointerBuffer aiMaterials = aiScene.mMaterials();
        for (int i = 0; i < numMaterials; i++) {
            AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(i));
            gaiaScene.getMaterials().add(processMaterial(aiMaterial, filePath));
        }

        GaiaNode node = processNode(gaiaScene, aiScene, aiNode, null);
        Matrix4d rootTransform = node.getTransformMatrix();
        Vector4d pos = rootTransform.transform(0,0,0,1.0, new Vector4d());

        GaiaBoundingBox boundingBox = node.getBoundingBox(null);
        Vector3d boundingBoxCenter = boundingBox.getCenter();
        Vector3d translation = new Vector3d(-boundingBoxCenter.x, -boundingBoxCenter.y, -boundingBoxCenter.z);

        //rootTransform.identity();
        System.out.println(rootTransform);
        //rootTransform.scale(0.001);

        Matrix4d rotationMatrix = new Matrix4d();
        rotationMatrix.rotateX(Math.toRadians(90));
        Matrix4d translateMatrix = new Matrix4d();
        translateMatrix.translate(translation, translateMatrix);
        Matrix4d rotatedTranslatedMatrix = new Matrix4d();

        //rootTransform.translate(translation, rootTransform);
        //rootTransform.rotateX(Math.toRadians(90), rootTransform);

        rootTransform.mul(translateMatrix);
        rootTransform.mul(rotationMatrix);
        node.setTransformMatrix(rootTransform);

        System.out.println(rootTransform);

        node.recalculateTransform();

        System.out.println(node.getPreMultipliedTransformMatrix());

        //GaiaBoundingBox boundingBox = node.getBoundingBox(node, null);
        //System.out.println(node.getTransformMatrix());
        //System.out.println(node.recalculateTransform(node));
        gaiaScene.getNodes().add(node);
        return gaiaScene;
    }

    /**
     * Converts an Assimp node to a Gaia node
     * @param aiMaterial Assimp material
     * @param path 서브파일 경로
     * @return Gaia node
     */
    private static GaiaMaterial processMaterial(AIMaterial aiMaterial, String path) {
        GaiaMaterial material = new GaiaMaterial();

        AIColor4D color = AIColor4D.create();
        Vector4d vector4d;
        int result = Assimp.aiGetMaterialColor(aiMaterial, Assimp.AI_MATKEY_COLOR_DIFFUSE, Assimp.aiTextureType_NONE, 0, color);
        if (result == 0) {
            vector4d = new Vector4d(color.r(), color.g(), color.b(), color.a());
            material.setDiffuseColor(vector4d);
        }

        AIString diffPath = AIString.calloc();
        Assimp.aiGetMaterialTexture(aiMaterial, Assimp.aiTextureType_DIFFUSE, 0, diffPath, (IntBuffer) null, null, null, null, null, null);
        String diffTexPath = diffPath.dataString();

        //AIString otherPath = AIString.calloc();
        //Assimp.aiGetMaterialTexture(aiMaterial, Assimp.aiTextureType_NORMALS, 0, otherPath, (IntBuffer) null, null, null, null, null, null);
        //String otherTexPath = otherPath.dataString();

        Path parentPath = new File(path).toPath();

        if (diffTexPath != null && diffTexPath.length() > 0) {
            GaiaTexture texture = new GaiaTexture();
            texture.setPath(diffTexPath);
            texture.setType(GaiaMaterialType.DIFFUSE);
            texture.setParentPath(parentPath);
            //texture.readImage();
            material.getTextures().put(texture.getType(), texture);
        }

        /*int numProperties = aiMaterial.mNumProperties();
        PointerBuffer pointerBuffer = aiMaterial.mProperties();
        for (int i = 0; i < numProperties; i++) {
            AIMaterialProperty aiMaterialProperty = AIMaterialProperty.create(pointerBuffer.get(i));
            ByteBuffer byteBuffer = aiMaterialProperty.mData();
            //System.out.println(aiMaterialProperty.mKey().dataString());
            //System.out.println(aiMaterialProperty.mType());
        }*/
        return material;
    }

    /**
     * Converts an Assimp node to a Gaia node
     * @param aiScene
     * @param aiNode
     * @param parentNode
     * @return Gaia node
     */
    private static GaiaNode processNode(GaiaScene gaiaScene, AIScene aiScene, AINode aiNode, GaiaNode parentNode) {
        AIMatrix4x4 transformation = aiNode.mTransformation();
        Matrix4d transfrom = convertMatrix4dFromAIMatrix4x4(transformation);

        String name = aiNode.mName().dataString();
        int numMeshes = aiNode.mNumMeshes();
        int numChildren = aiNode.mNumChildren();

        if (numMeshes < 1 && numChildren < 1) {
            return null;
        }

        GaiaNode node = new GaiaNode();
        node.setName(name);
        node.setParent(parentNode);
        node.setTransformMatrix(transfrom);
        PointerBuffer aiMeshes = aiScene.mMeshes();

        IntBuffer nodeMeshes = aiNode.mMeshes();
        int nodeNum = -1;
        if (nodeMeshes != null && nodeMeshes.capacity() > 0) {
            nodeNum = nodeMeshes.get(0);
        }

        //System.out.println("Node name: " + name);
        //System.out.println("Node meshes num: " + nodeMeshesNum);
        //System.out.println("Node num: " + nodeNum);
        for (int i = 0; i < numMeshes; i++) {
            AIMesh aiMesh = AIMesh.create(aiMeshes.get(nodeNum));
            GaiaMesh mesh = processMesh(aiMesh, gaiaScene.getMaterials());
            node.getMeshes().add(mesh);
        }

        PointerBuffer childrenBuffer = aiNode.mChildren();
        for (int i = 0; i < numChildren; i++) {
            AINode aiChildNode = AINode.create(childrenBuffer.get(i));
            GaiaNode childNode = processNode(gaiaScene, aiScene, aiChildNode, node);
            if (childNode != null) {
                node.getChildren().add(childNode);
            }
        }
        return node;
    }

    /**
     * Converts an Assimp mesh to a Gaia mesh
     * @param aiMesh Assimp mesh
     * @return Gaia mesh
     */
    private static GaiaMesh processMesh(AIMesh aiMesh, ArrayList<GaiaMaterial> materials) {
        int materialIndex = aiMesh.mMaterialIndex();
        GaiaMaterial material = materials.get(materialIndex);
        GaiaPrimitive primitive = processPrimitive(aiMesh, material);
        GaiaMesh mesh = new GaiaMesh();
        mesh.getPrimitives().add(primitive);
        return mesh;
    }

    /**
     * Converts an Assimp surface to a Gaia surface
     * @param aiMesh Assimp mesh
     * @return Gaia surface
     */
    private static GaiaPrimitive processPrimitive(AIMesh aiMesh, GaiaMaterial material) {
        GaiaSurface surface = processSurface();

        GaiaPrimitive primitive = new GaiaPrimitive();
        primitive.getSurfaces().add(surface);
        primitive.setMaterial(material);

        int numFaces = aiMesh.mNumFaces();
        AIFace.Buffer facesBuffer = aiMesh.mFaces();
        for (int i = 0; i < numFaces; i++) {
            AIFace aiFace = facesBuffer.get(i);
            GaiaFace face = processFace(aiFace);
            surface.getFaces().add(face);

            face.getIndices().stream().forEach((indices) -> {
                primitive.getIndices().add(indices);
            });
        }

        int mNumVertices = aiMesh.mNumVertices();
        AIVector3D.Buffer verticesBuffer = aiMesh.mVertices();
        AIVector3D.Buffer normalsBuffer = aiMesh.mNormals();
        AIVector3D.Buffer textCoords = aiMesh.mTextureCoords(0);

        for (int i = 0; i < mNumVertices; i++) {
            AIVector3D aiVertice = verticesBuffer.get(i);
            AIVector3D aiNormal = normalsBuffer.get(i);
            GaiaVertex vertex = new GaiaVertex();
            vertex.setPosition(new Vector3d((double) aiVertice.x(), (double) aiVertice.y(), (double) aiVertice.z()));

            if (!(aiNormal.x() == 0.0 && aiNormal.y() == 0.0 && aiNormal.z() == 0.0)) {
                vertex.setNormal(new Vector3d((double) aiNormal.x(), (double) aiNormal.y(), (double) aiNormal.z()));
            }

            if (textCoords != null) {
                AIVector3D textCoord = textCoords.get(i);
                vertex.setTextureCoordinates(new Vector2d((double) textCoord.x(), 1.0 - (double) textCoord.y()));
            }
            primitive.getVertices().add(vertex);
        }

        //Rectangle2D rectangle2D = getTextureCoordsBoundingRect(primitive);
        primitive.genNormals();
        return primitive;
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
