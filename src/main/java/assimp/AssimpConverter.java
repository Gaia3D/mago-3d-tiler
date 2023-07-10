package assimp;

import command.ConverterMain;
import geometry.structure.*;
import geometry.types.TextureType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import util.ImageUtils;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AssimpConverter {
    private final CommandLine command;

    public AssimpConverter(CommandLine command) {
        if (command == null) {
            command = createDefaultCommand();
        }
        this.command = command;
    }

    private CommandLine createDefaultCommand() {
        Options options = ConverterMain.createOptions();
        CommandLineParser parser = new org.apache.commons.cli.DefaultParser();
        try {
            return parser.parse(options, new String[]{});
        } catch (org.apache.commons.cli.ParseException e) {
            throw new RuntimeException(e);
        }
    }

    final int DEFAULT_FLAGS =
            Assimp.aiProcess_GenNormals |
            Assimp.aiProcess_Triangulate|
            Assimp.aiProcess_JoinIdenticalVertices|
            Assimp.aiProcess_CalcTangentSpace|
            Assimp.aiProcess_SortByPType;

    public GaiaScene load(String filePath, String hint) {
        return load(new File(filePath), hint);
    }

    public GaiaScene load(Path filePath, String hint) {
        return load(filePath.toFile(), hint);
    }

    public GaiaScene load(File file, String hint) {
        if (file.isFile()) {
            String path = file.getAbsolutePath().replace(file.getName(), "");
            ByteBuffer byteBuffer = ImageUtils.readFile(file, true);
            hint = (hint != null) ? hint : FilenameUtils.getExtension(file.getName());

            assert byteBuffer != null;
            AIScene aiScene = Assimp.aiImportFileFromMemory(byteBuffer, DEFAULT_FLAGS, hint);
            assert aiScene != null;
            GaiaScene gaiaScene = convertScene(aiScene, path);
            gaiaScene.setOriginalPath(file.toPath());
            return gaiaScene;
        } else {
            return null;
        }
    }

    private Matrix4d convertMatrix4dFromAIMatrix4x4(AIMatrix4x4 aiMatrix4x4) {
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

    private GaiaScene convertScene(AIScene aiScene, String filePath) {
        GaiaScene gaiaScene = new GaiaScene();
        AINode aiNode = aiScene.mRootNode();

        int numMaterials = aiScene.mNumMaterials();
        PointerBuffer aiMaterials = aiScene.mMaterials();
        for (int i = 0; i < numMaterials; i++) {
            assert aiMaterials != null;
            AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(i));
            gaiaScene.getMaterials().add(processMaterial(aiMaterial, filePath));
        }
        assert aiNode != null;
        GaiaNode node = processNode(gaiaScene, aiScene, aiNode, null);

        assert node != null;
        Matrix4d rootTransform = node.getTransformMatrix();
        if (command.hasOption("swapYZ")) {
            rootTransform.rotateX(Math.toRadians(90), rootTransform);
        }
        node.setTransformMatrix(rootTransform);
        node.recalculateTransform();
        gaiaScene.getNodes().add(node);
        return gaiaScene;
    }

    private GaiaMaterial processMaterial(AIMaterial aiMaterial, String path) {
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

        Path parentPath = new File(path).toPath();
        if (diffTexPath.length() > 0) {
            material.setName(diffTexPath);

            List<GaiaTexture> textures = new ArrayList<>();
            GaiaTexture texture = new GaiaTexture();
            texture.setType(TextureType.DIFFUSE);
            texture.setPath(diffTexPath);
            texture.setParentPath(parentPath);
            File file = new File(parentPath.toFile(), diffTexPath);
            if (!(file.exists() && file.isFile())) {
                log.error("Diffuse Texture not found: " + file.getAbsolutePath());
            } else {
                textures.add(texture);
                material.getTextures().put(texture.getType(), textures);
            }
        } else {
            material.setName("NoTexture");
            List<GaiaTexture> textures = new ArrayList<>();
            material.getTextures().put(TextureType.DIFFUSE, textures);
        }

        List<GaiaTexture> textures;
        if (ambientTexPath.length() > 0) {
            textures = new ArrayList<>();
            GaiaTexture texture = new GaiaTexture();
            texture.setType(TextureType.AMBIENT);
            texture.setPath(ambientTexPath);
            texture.setParentPath(parentPath);
            File file = new File(parentPath.toFile(), ambientTexPath);
            if (!(file.exists() && file.isFile())) {
                log.error("Ambient Texture not found: " + file.getAbsolutePath());
            } else {
                textures.add(texture);
                material.getTextures().put(texture.getType(), textures);
            }
        } else {
            textures = new ArrayList<>();
            material.getTextures().put(TextureType.AMBIENT, textures);
        }

        if (specularTexPath.length() > 0) {
            textures = new ArrayList<>();
            GaiaTexture texture = new GaiaTexture();
            texture.setPath(specularTexPath);
            texture.setType(TextureType.SPECULAR);
            texture.setParentPath(parentPath);
            File file = new File(parentPath.toFile(), specularTexPath);
            if (!(file.exists() && file.isFile())) {
                log.error("Specular Texture not found: " + file.getAbsolutePath());
            } else {
                textures.add(texture);
                material.getTextures().put(texture.getType(), textures);
            }
        } else {
            textures = new ArrayList<>();
            material.getTextures().put(TextureType.SPECULAR, textures);
        }

        if (shininessTexPath.length() > 0) {
            textures = new ArrayList<>();
            GaiaTexture texture = new GaiaTexture();
            texture.setPath(shininessTexPath);
            texture.setType(TextureType.SHININESS);
            texture.setParentPath(parentPath);
            File file = new File(parentPath.toFile(), specularTexPath);
            if (!(file.exists() && file.isFile())) {
                log.error("Shininess Texture not found: " + file.getAbsolutePath());
            } else {
                textures.add(texture);
                material.getTextures().put(texture.getType(), textures);
            }
        } else {
            textures = new ArrayList<>();
            material.getTextures().put(TextureType.SHININESS, textures);
        }
        return material;
    }

    private GaiaNode processNode(GaiaScene gaiaScene, AIScene aiScene, AINode aiNode, GaiaNode parentNode) {
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
            assert aiMeshes != null;
            AIMesh aiMesh = AIMesh.create(aiMeshes.get(nodeNum));
            GaiaMesh mesh = processMesh(aiMesh, gaiaScene.getMaterials());
            node.getMeshes().add(mesh);
        }

        PointerBuffer childrenBuffer = aiNode.mChildren();
        for (int i = 0; i < numChildren; i++) {
            assert childrenBuffer != null;
            AINode aiChildNode = AINode.create(childrenBuffer.get(i));
            GaiaNode childNode = processNode(gaiaScene, aiScene, aiChildNode, node);
            if (childNode != null) {
                node.getChildren().add(childNode);
            }
        }
        return node;
    }

    private GaiaMesh processMesh(AIMesh aiMesh, List<GaiaMaterial> materials) {
        int materialIndex = aiMesh.mMaterialIndex();
        GaiaMaterial material = materials.get(materialIndex);
        material.setId(materialIndex);
        GaiaPrimitive primitive = processPrimitive(aiMesh, material);
        GaiaMesh mesh = new GaiaMesh();
        mesh.getPrimitives().add(primitive);
        return mesh;
    }

    private GaiaPrimitive processPrimitive(AIMesh aiMesh, GaiaMaterial material) {
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

            face.getIndices().forEach((indices) -> primitive.getIndices().add(indices));
        }

        int mNumVertices = aiMesh.mNumVertices();
        AIVector3D.Buffer verticesBuffer = aiMesh.mVertices();
        AIVector3D.Buffer normalsBuffer = aiMesh.mNormals();
        AIVector3D.Buffer textureCoordiantesBuffer = aiMesh.mTextureCoords(0);
        for (int i = 0; i < mNumVertices; i++) {
            GaiaVertex vertex = new GaiaVertex();
            AIVector3D aiVertice = verticesBuffer.get(i);
            if (Float.isNaN(aiVertice.x()) || Float.isNaN(aiVertice.y()) || Float.isNaN(aiVertice.z())) {
                vertex.setPosition(new Vector3d());
            } else {
                vertex.setPosition(new Vector3d(aiVertice.x(), aiVertice.y(), aiVertice.z()));
            }

            if (normalsBuffer != null) {
                AIVector3D aiNormal = normalsBuffer.get(i);
                if (Float.isNaN(aiNormal.x()) || Float.isNaN(aiNormal.y()) || Float.isNaN(aiNormal.z())) {
                    vertex.setNormal(new Vector3d());
                } else {
                    vertex.setNormal(new Vector3d(aiNormal.x(), aiNormal.y(), aiNormal.z()));
                }
            } else {
                vertex.setNormal(new Vector3d());
            }

            if (textureCoordiantesBuffer != null) {
                AIVector3D textureCoordinate = textureCoordiantesBuffer.get(i);
                if (Float.isNaN(textureCoordinate.x()) || Float.isNaN(textureCoordinate.y())) {
                    vertex.setTexcoords(new Vector2d());
                } else {
                    vertex.setTexcoords(new Vector2d(textureCoordinate.x(), 1.0 - textureCoordinate.y()));
                }
            }
            primitive.getVertices().add(vertex);
        }

        primitive.calculateNormal();
        return primitive;
    }

    private GaiaSurface processSurface() {
        return new GaiaSurface();
    }

    private GaiaFace processFace(AIFace aiFace) {
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
