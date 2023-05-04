package assimp;

import command.CommandOption;
import geometry.basic.GaiaBoundingBox;
import geometry.structure.*;
import geometry.types.TextureType;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import util.FileUtils;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.List;

/**
 * Loads a scene from a file
 */
public class DataLoader {

    /**
     * Default flags for Assimp import process
     */
    final static int DEFAULT_FLAGS =
            Assimp.aiProcess_GenNormals |
            //Assimp.aiProcess_FixInfacingNormals|
            Assimp.aiProcess_Triangulate|
            Assimp.aiProcess_JoinIdenticalVertices|
            Assimp.aiProcess_CalcTangentSpace|
            //Assimp.aiProcess_FlipWindingOrder|
            //=Assimp.aiProcess_FixInfacingNormals|
            //Assimp.aiProcess_ConvertToLeftHanded|
            Assimp.aiProcess_SortByPType;


    /** Loads a scene from a filePath
     * @param filePath 파일 경로
     * @return
     */
    public static GaiaScene load(String filePath, CommandOption option) {
        return load(new File(filePath), null, option);
    }

    public static GaiaScene load(Path filePath, CommandOption option) {
        return load(filePath.toFile(), null, option);
    }

    public static GaiaScene load(String filePath, String hint, CommandOption option) {
        return load(new File(filePath), hint, option);
    }

    /**
     * Loads a scene from a file
     * @param file 파일
     * @param hint 파일 확장자
     * @return
     */
    public static GaiaScene load(File file, String hint, CommandOption option) {
        if (file.isFile()) {
            String path = file.getAbsolutePath().replace(file.getName(), "");
            ByteBuffer byteBuffer = FileUtils.readFile(file);
            hint = (hint != null) ? hint : FileUtils.getExtension(file.getName());

            AIScene aiScene = Assimp.aiImportFileFromMemory(byteBuffer, DEFAULT_FLAGS, hint);
            GaiaScene gaiaScene = convertScene(aiScene, path);
            gaiaScene.setOriginalPath(file.toPath());
            aiScene.free();
            //MemoryUtil.memFree(byteBuffer);
            return gaiaScene;
        } else {
            return null;
        }
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
        //rootTransform.rotateX(Math.toRadians(90), rootTransform);

        GaiaBoundingBox boundingBox = node.getBoundingBox(null);
        Vector3d boundingBoxCenter = boundingBox.getCenter();

        Vector3d volume = boundingBox.getVolume();
        Vector3d translation = new Vector3d(-boundingBoxCenter.x, -boundingBoxCenter.y, -boundingBoxCenter.z);
        //CRSFactory factory = new CRSFactory();
        //CoordinateReferenceSystem epsg5186 = factory.createFromParameters("EPSG:5186", "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=600000 +ellps=GRS80 +units=m +no_defs");
        //CoordinateReferenceSystem epsg4326 = factory.createFromParameters("EPSG:4326", "+proj=longlat +datum=WGS84 +no_defs");
        //GeometryUtils.transform(epsg5186, epsg4326, 0.0d, 0.0d);

        //rootTransform.identity();
        //rootTransform.scale(0.1d);
        //rootTransform.translate(translation, rootTransform);
        node.setTransformMatrix(rootTransform);
        node.recalculateTransform();
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

        Vector4d diffVector4d;
        AIColor4D diffColor = AIColor4D.create();
        int diffResult = Assimp.aiGetMaterialColor(aiMaterial, Assimp.AI_MATKEY_COLOR_DIFFUSE, Assimp.aiTextureType_NONE, 0, diffColor);
        if (diffResult == 0) {
            diffVector4d = new Vector4d(diffColor.r(), diffColor.g(), diffColor.b(), diffColor.a());
            material.setDiffuseColor(diffVector4d);
        }

        Vector4d ambientVector4d;
        AIColor4D ambientColor = AIColor4D.create();
        int ambientResult = Assimp.aiGetMaterialColor(aiMaterial, Assimp.AI_MATKEY_COLOR_AMBIENT, Assimp.aiTextureType_NONE, 0, ambientColor);
        if (ambientResult == 0) {
            ambientVector4d = new Vector4d(ambientColor.r(), ambientColor.g(), ambientColor.b(), ambientColor.a());
            material.setAmbientColor(ambientVector4d);
        }

        Vector4d specVector4d;
        AIColor4D specColor = AIColor4D.create();
        int specResult = Assimp.aiGetMaterialColor(aiMaterial, Assimp.AI_MATKEY_COLOR_SPECULAR, Assimp.aiTextureType_NONE, 0, specColor);
        if (specResult == 0) {
            specVector4d = new Vector4d(specColor.r(), specColor.g(), specColor.b(), specColor.a());
            material.setSpecularColor(specVector4d);
        }

        AIString diffPath = AIString.calloc();
        Assimp.aiGetMaterialTexture(aiMaterial, Assimp.aiTextureType_DIFFUSE, 0, diffPath, (IntBuffer) null, null, null, null, null, null);
        String diffTexPath = diffPath.dataString();

        AIString ambientPath = AIString.calloc();
        Assimp.aiGetMaterialTexture(aiMaterial, Assimp.aiTextureType_AMBIENT, 0, ambientPath, (IntBuffer) null, null, null, null, null, null);
        String ambientTexPath = ambientPath.dataString();

        AIString specularPath = AIString.calloc();
        Assimp.aiGetMaterialTexture(aiMaterial, Assimp.aiTextureType_SPECULAR, 0, specularPath, (IntBuffer) null, null, null, null, null, null);
        String specularTexPath = specularPath.dataString();

        AIString shininessPath = AIString.calloc();
        Assimp.aiGetMaterialTexture(aiMaterial, Assimp.aiTextureType_SHININESS, 0, shininessPath, (IntBuffer) null, null, null, null, null, null);
        String shininessTexPath = shininessPath.dataString();

        //AIString otherPath = AIString.calloc();
        //Assimp.aiGetMaterialTexture(aiMaterial, Assimp.aiTextureType_NORMALS, 0, otherPath, (IntBuffer) null, null, null, null, null, null);
        //String otherTexPath = otherPath.dataString();

        Path parentPath = new File(path).toPath();
        if (diffTexPath != null && diffTexPath.length() > 0) {
            List<GaiaTexture> textures = new ArrayList<>();
            GaiaTexture texture = new GaiaTexture();
            texture.setType(TextureType.DIFFUSE);
            texture.setPath(diffTexPath);
            texture.setParentPath(parentPath);
            File file = new File(parentPath.toFile(), diffTexPath);
            if (!(file.exists() && file.isFile())) {
                System.err.println("Diffuse Texture not found: " + file.getAbsolutePath());
            } else {
                textures.add(texture);
                material.getTextures().put(texture.getType(), textures);
            }
        } else {
            List<GaiaTexture> textures = new ArrayList<>();
            material.getTextures().put(TextureType.DIFFUSE, textures);
        }

        if (ambientTexPath != null && ambientTexPath.length() > 0) {
            List<GaiaTexture> textures = new ArrayList<>();
            GaiaTexture texture = new GaiaTexture();
            texture.setType(TextureType.AMBIENT);
            texture.setPath(ambientTexPath);
            texture.setParentPath(parentPath);
            File file = new File(parentPath.toFile(), ambientTexPath);
            if (!(file.exists() && file.isFile())) {
                System.err.println("Ambient Texture not found: " + file.getAbsolutePath());
            } else {
                textures.add(texture);
                material.getTextures().put(texture.getType(), textures);
            }
        } else {
            List<GaiaTexture> textures = new ArrayList<>();
            material.getTextures().put(TextureType.AMBIENT, textures);
        }

        if (specularTexPath != null && specularTexPath.length() > 0) {
            List<GaiaTexture> textures = new ArrayList<>();
            GaiaTexture texture = new GaiaTexture();
            texture.setPath(specularTexPath);
            texture.setType(TextureType.SPECULAR);
            texture.setParentPath(parentPath);
            File file = new File(parentPath.toFile(), specularTexPath);
            if (!(file.exists() && file.isFile())) {
                System.err.println("Specular Texture not found: " + file.getAbsolutePath());
            } else {
                textures.add(texture);
                material.getTextures().put(texture.getType(), textures);
            }
        } else {
            List<GaiaTexture> textures = new ArrayList<>();
            material.getTextures().put(TextureType.SPECULAR, textures);
        }

        if (shininessTexPath != null && shininessTexPath.length() > 0) {
            List<GaiaTexture> textures = new ArrayList<>();
            GaiaTexture texture = new GaiaTexture();
            texture.setPath(shininessTexPath);
            texture.setType(TextureType.SHININESS);
            texture.setParentPath(parentPath);
            File file = new File(parentPath.toFile(), specularTexPath);
            if (!(file.exists() && file.isFile())) {
                System.err.println("Shininess Texture not found: " + file.getAbsolutePath());
            } else {
                textures.add(texture);
                material.getTextures().put(texture.getType(), textures);
            }
        } else {
            List<GaiaTexture> textures = new ArrayList<>();
            material.getTextures().put(TextureType.SHININESS, textures);
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
    private static GaiaNode processNode(GaiaScene gaiaScene, AIScene aiScene, AINode aiNode, GaiaNode parentNode) {
        AIMatrix4x4 transformation = aiNode.mTransformation();
        Matrix4d transform = convertMatrix4dFromAIMatrix4x4(transformation);

        String name = aiNode.mName().dataString();
        int numMeshes = aiNode.mNumMeshes();
        int numChildren = aiNode.mNumChildren();

        if (numMeshes < 1 && numChildren < 1) {
            return null;
        }

        GaiaNode node = new GaiaNode();
        node.setName(name);
        node.setParent(parentNode);
        node.setTransformMatrix(transform);
        PointerBuffer aiMeshes = aiScene.mMeshes();

        IntBuffer nodeMeshes = aiNode.mMeshes();
        int nodeNum = -1;
        if (nodeMeshes != null && nodeMeshes.capacity() > 0) {
            nodeNum = nodeMeshes.get(0);
        }

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
    private static GaiaMesh processMesh(AIMesh aiMesh, List<GaiaMaterial> materials) {
        int materialIndex = aiMesh.mMaterialIndex();
        GaiaMaterial material = materials.get(materialIndex);
        material.setId(materialIndex);
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
        primitive.setMaterialIndex(material.getId());

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
        AIVector3D.Buffer textureCoordiantesBuffer = aiMesh.mTextureCoords(0);
        for (int i = 0; i < mNumVertices; i++) {
            GaiaVertex vertex = new GaiaVertex();
            if (verticesBuffer != null) {
                AIVector3D aiVertice = verticesBuffer.get(i);
                if (Float.isNaN(aiVertice.x()) || Float.isNaN(aiVertice.y()) || Float.isNaN(aiVertice.z())) {
                    vertex.setPosition(new Vector3d());
                } else {
                    vertex.setPosition(new Vector3d((double) aiVertice.x(), (double) aiVertice.y(), (double) aiVertice.z()));
                }
            } else {
                //vertex.setPosition(new Vector3d());
            }

            if (normalsBuffer != null) {
                AIVector3D aiNormal = normalsBuffer.get(i);
                if (Double.isNaN((double) aiNormal.x()) || Double.isNaN((double) aiNormal.y()) || Double.isNaN((double) aiNormal.z())) {
                    vertex.setNormal(new Vector3d());
                } else {
                    vertex.setNormal(new Vector3d((double) aiNormal.x(), (double) aiNormal.y(), (double) aiNormal.z()));
                }
            } else {
                vertex.setNormal(new Vector3d());
            }

            if (textureCoordiantesBuffer != null) {
                AIVector3D textureCoordiante = textureCoordiantesBuffer.get(i);
                if (Float.isNaN(textureCoordiante.x()) || Float.isNaN(textureCoordiante.y())) {
                    vertex.setTextureCoordinates(new Vector2d());
                } else {
                    vertex.setTextureCoordinates(new Vector2d((double) textureCoordiante.x(), 1.0 - (double) textureCoordiante.y()));
                }
            } else {
                //vertex.setTextureCoordinates(new Vector2d());
            }
            primitive.getVertices().add(vertex);
        }

        //Rectangle2D rectangle2D = getTextureCoordsBoundingRect(primitive);
        //primitive.genNormals();
        primitive.calculateNormal();
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
