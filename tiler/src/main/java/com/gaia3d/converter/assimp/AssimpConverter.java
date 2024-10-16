package com.gaia3d.converter.assimp;

import com.gaia3d.basic.model.*;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.util.ImageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A class that converts a file to a GaiaScene object using Assimp.
 * @author znkim
 * @since 1.0.0
 * @see Converter
 */
@Slf4j
@RequiredArgsConstructor
public class AssimpConverter implements Converter {
    public final int DEFAULT_FLAGS =
            Assimp.aiProcess_GenNormals |
            Assimp.aiProcess_Triangulate|
            Assimp.aiProcess_JoinIdenticalVertices|
            Assimp.aiProcess_CalcTangentSpace|
            Assimp.aiProcess_SortByPType;

    public List<GaiaScene> load(String filePath) {
        return load(new File(filePath));
    }

    public List<GaiaScene> load(Path filePath) {
        return load(filePath.toFile());
    }

    public List<GaiaScene> load(File file) throws RuntimeException {
        if (!file.isFile() && !file.exists()) {
            log.error("File does not exist: {}", file.getAbsolutePath());
            throw new RuntimeException("File does not exist: " + file.getAbsolutePath());
        }

        String path = file.getAbsolutePath().replace(file.getName(), "");
        AIScene aiScene =  Assimp.aiImportFile(file.getAbsolutePath(), DEFAULT_FLAGS);

        assert aiScene != null;
        GaiaScene gaiaScene = convertScene(aiScene, path, file.getName());
        aiScene.free();
        gaiaScene.setOriginalPath(file.toPath());

        GaiaAttribute attribute = new GaiaAttribute();
        attribute.setIdentifier(UUID.randomUUID());
        attribute.setFileName(file.getName());
        attribute.setNodeName(gaiaScene.getNodes().get(0).getName());

        gaiaScene.setAttribute(attribute);

        List<GaiaScene> gaiaScenes = new ArrayList<>();
        gaiaScenes.add(gaiaScene);
        return gaiaScenes;
    }

    private Matrix4d convertMatrix4dFromAIMatrix4x4(AIMatrix4x4 aiMatrix4x4, GaiaNode parentNode, FormatType isYUpFormat) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        boolean isZeroOrigin = globalOptions.isZeroOrigin();
        boolean autoUpAxis = globalOptions.isAutoUpAxis();
        Matrix4d matrix4 = new Matrix4d();

        boolean isRootNode = parentNode == null;
//        log.debug(isRootNode ? "=======RootTrasformMatrix=======" : "=======TrasformMatrix=======");
//        log.debug("{} {} {} {}", aiMatrix4x4.a1(), aiMatrix4x4.b1(), aiMatrix4x4.c1(), aiMatrix4x4.d1());
//        log.debug("{} {} {} {}", aiMatrix4x4.a2(), aiMatrix4x4.b2(), aiMatrix4x4.c2(), aiMatrix4x4.d2());
//        log.debug("{} {} {} {}", aiMatrix4x4.a3(), aiMatrix4x4.b3(), aiMatrix4x4.c3(), aiMatrix4x4.d3());
//        log.debug("{} {} {} {}", aiMatrix4x4.a4(), aiMatrix4x4.b4(), aiMatrix4x4.c4(), aiMatrix4x4.d4());

        // getTransformMatrix
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

        if (isRootNode && autoUpAxis) {
            if (isYUpFormat.isYUpAxis()) {
                matrix4.m00(1.0d);
                matrix4.m01(0.0d);
                matrix4.m02(0.0d);
                matrix4.m03(0.0d);
                matrix4.m10(0.0d);
                matrix4.m11(1.0d);
                matrix4.m12(0.0d);
                matrix4.m13(0.0d);
                matrix4.m20(0.0d);
                matrix4.m21(0.0d);
                matrix4.m22(1.0d);
                matrix4.m23(0.0d);
            }
        }
        if (isRootNode && isZeroOrigin) {
            matrix4.m30(0.0d);
            matrix4.m31(0.0d);
            matrix4.m32(0.0d);
            matrix4.m33(0.0d);
        } else {
            matrix4.m30(aiMatrix4x4.a4());
            matrix4.m31(aiMatrix4x4.b4());
            matrix4.m32(aiMatrix4x4.c4());
            matrix4.m33(aiMatrix4x4.d4());
        }
        return matrix4;
    }

    private GaiaScene convertScene(AIScene aiScene, String filePath, String fileName) {
        FormatType formatType = FormatType.fromExtension(FilenameUtils.getExtension(fileName));

        /*
        //READ METADATA
        AIMetaData aiMetaData = aiScene.mMetaData();
        assert aiMetaData != null;
        if (aiMetaData != null) {
            // UnitScaleFactor
            var keys = aiMetaData.mKeys();
            var values = aiMetaData.mValues();
            int length = aiMetaData.mNumProperties();
            for (int i = 0; i < length; i++) {
                AIString keyAiString = keys.get(i);
                String key = keyAiString.dataString();
                AIMetaDataEntry dataEntry = values.get(i);
                int type = dataEntry.mType();
                ByteBuffer buffer = dataEntry.mData(0);
                log.info("Key: " + key);
            }
        }*/

        GaiaScene gaiaScene = new GaiaScene();
        AINode aiNode = aiScene.mRootNode();
        List<String> embeddedTextures = getEmbeddedTexturePath(aiScene, filePath, fileName);

        // convert materials
        int numMaterials = aiScene.mNumMaterials();
        PointerBuffer aiMaterials = aiScene.mMaterials();
        for (int i = 0; i < numMaterials; i++) {
            assert aiMaterials != null;
            AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(i));
            gaiaScene.getMaterials().add(processMaterial(aiMaterial, filePath, embeddedTextures));
        }

        assert aiNode != null;
        GaiaNode node = processNode(gaiaScene, aiScene, aiNode, null, formatType);

        assert node != null;
        Matrix4d rootTransform = node.getTransformMatrix();

        node.setTransformMatrix(rootTransform);
        node.recalculateTransform();
        gaiaScene.getNodes().add(node);
        return gaiaScene;
    }

    private List<String> getEmbeddedTexturePath(AIScene aiScene, String filePath, String fileName) {
        String EMBEDDED_TEXTURES_DIR = "embedded_textures";
        List<String> embeddedTextures = new ArrayList<>();
        // embedded textures
        PointerBuffer aiTextures = aiScene.mTextures();
        String fileNameWithoutExtension = FilenameUtils.removeExtension(fileName);
        int numTextures = aiScene.mNumTextures();
        for (int i = 0; i < numTextures; i++) {
            File path = new File(filePath, EMBEDDED_TEXTURES_DIR);
            path.mkdirs();

            assert aiTextures != null;
            AITexture aiTexture = AITexture.create(aiTextures.get(i));

            ByteBuffer buffer = aiTexture.pcDataCompressed();
            String ext = aiTexture.achFormatHintString();

            AIString pathName = aiTexture.mFilename();
            String filename = pathName.dataString();
            if (filename.isEmpty()) {
                filename = fileNameWithoutExtension + "_extracted_" + i + "." + ext;
            } else {
                File file = new File(filename);
                filename = file.getName();
            }

            try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(path, filename)))) {
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                stream.write(bytes);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
            embeddedTextures.add(filename);
            aiTexture.free();
        }
        return embeddedTextures;
    }

    private GaiaMaterial processMaterial(AIMaterial aiMaterial, String path, List<String> embeddedTextures) {
        GaiaMaterial material = new GaiaMaterial();

        // TODO: Check if the material has a name
        /*String materialName = "unnamed";
        AIString namePath = AIString.calloc();
        int materialNameString = Assimp.aiGetMaterialString(aiMaterial, Assimp.AI_MATKEY_NAME, 0, 0, namePath);
        if (materialNameString == 0) {
            materialName = namePath.dataString();
        }*/

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

        File parentPath = new File(path);
        if (!diffTexPath.isEmpty()) {
            material.setName(diffTexPath);

            List<GaiaTexture> textures = new ArrayList<>();
            GaiaTexture texture = new GaiaTexture();
            texture.setType(TextureType.DIFFUSE);
            texture.setParentPath(path);

            // embedded texture check
            if (diffTexPath.startsWith("*")) {
                String embeddedTexturePath = embeddedTextures.get(Integer.parseInt(diffTexPath.substring(1)));
                log.debug("Original Texture Path: " + diffTexPath);
                log.debug("Embedded Texture Path: " + embeddedTexturePath);
                diffTexPath = "embedded_textures" + File.separator + embeddedTexturePath;
            } else {
                File filePath = new File(diffTexPath);
                String fileName = filePath.getName();
                String embeddedTexturePath = "embedded_textures" + File.separator + fileName;
                File inputFile = new File(parentPath, embeddedTexturePath);
                if (inputFile.exists() && inputFile.isFile()) {
                    log.debug("Original Texture Path: " + diffTexPath);
                    log.debug("Corrected Texture Path: " + embeddedTexturePath);
                    diffTexPath = embeddedTexturePath;
                }
            }

            File file = ImageUtils.getChildFile(parentPath, diffTexPath);
            if (file != null && file.exists() && file.isFile()) {
                texture.setPath(diffTexPath);
                textures.add(texture);
                material.getTextures().put(texture.getType(), textures);
            } else {
                log.error("Diffuse Texture is not found: {}", diffTexPath);
            }

        } else {
            material.setName("NoTexture");
            List<GaiaTexture> textures = new ArrayList<>();
            material.getTextures().put(TextureType.DIFFUSE, textures);
        }

        List<GaiaTexture> textures;
        if (!ambientTexPath.isEmpty()) {
            textures = new ArrayList<>();
            GaiaTexture texture = new GaiaTexture();
            texture.setType(TextureType.AMBIENT);
            texture.setPath(ambientTexPath);
            texture.setParentPath(path);
            File file = new File(parentPath, ambientTexPath);
            if (!(file.exists() && file.isFile())) {
                log.error("Ambient Texture is not found: {}", file.getAbsolutePath());
            } else {
                textures.add(texture);
                material.getTextures().put(texture.getType(), textures);
            }
        } else {
            textures = new ArrayList<>();
            material.getTextures().put(TextureType.AMBIENT, textures);
        }

        if (!specularTexPath.isEmpty()) {
            textures = new ArrayList<>();
            GaiaTexture texture = new GaiaTexture();
            texture.setPath(specularTexPath);
            texture.setType(TextureType.SPECULAR);
            texture.setParentPath(path);
            File file = new File(parentPath, specularTexPath);
            if (!(file.exists() && file.isFile())) {
                log.error("Specular Texture is not found: {}", file.getAbsolutePath());
            } else {
                textures.add(texture);
                material.getTextures().put(texture.getType(), textures);
            }
        } else {
            textures = new ArrayList<>();
            material.getTextures().put(TextureType.SPECULAR, textures);
        }

        if (!shininessTexPath.isEmpty()) {
            textures = new ArrayList<>();
            GaiaTexture texture = new GaiaTexture();
            texture.setPath(shininessTexPath);
            texture.setType(TextureType.SHININESS);
            texture.setParentPath(path);
            File file = new File(parentPath, specularTexPath);
            if (!(file.exists() && file.isFile())) {
                log.error("Shininess Texture is not found: {}", file.getAbsolutePath());
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

    private GaiaNode processNode(GaiaScene gaiaScene, AIScene aiScene, AINode aiNode, GaiaNode parentNode, FormatType formatType) {
        String name = aiNode.mName().dataString();

        AIMatrix4x4 transformation = aiNode.mTransformation();
        Matrix4d transform = convertMatrix4dFromAIMatrix4x4(transformation, parentNode, formatType);

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

        for (int i = 0; i < numMeshes; i++) {
            assert aiMeshes != null;
            assert nodeMeshes != null;
            AIMesh aiMesh = AIMesh.create(aiMeshes.get(nodeMeshes.get(i)));
            GaiaMesh mesh = processMesh(aiMesh, gaiaScene.getMaterials());
            node.getMeshes().add(mesh);
        }

        PointerBuffer childrenBuffer = aiNode.mChildren();
        for (int i = 0; i < numChildren; i++) {
            assert childrenBuffer != null;
            AINode aiChildNode = AINode.create(childrenBuffer.get(i));
            GaiaNode childNode = processNode(gaiaScene, aiScene, aiChildNode, node, formatType);
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
        primitive.setMaterialIndex(material.getId());

        Vector4d diffuse = material.getDiffuseColor();
        byte[] diffuseColor = new byte[]{(byte) (diffuse.x * 255), (byte) (diffuse.y * 255), (byte) (diffuse.z * 255), (byte) (diffuse.w * 255)};

        int numFaces = aiMesh.mNumFaces();
        AIFace.Buffer facesBuffer = aiMesh.mFaces();
        for (int i = 0; i < numFaces; i++) {
            AIFace aiFace = facesBuffer.get(i);
            GaiaFace face = processFace(aiFace);
            surface.getFaces().add(face);
        }

        int mNumVertices = aiMesh.mNumVertices();
        AIVector3D.Buffer verticesBuffer = aiMesh.mVertices();
        AIVector3D.Buffer normalsBuffer = aiMesh.mNormals();
        AIVector3D.Buffer textureCoordiantesBuffer = aiMesh.mTextureCoords(0);
        //AIColor4D.Buffer colorsBuffer = aiMesh.mColors(0);

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
                    vertex.setTexcoords(new Vector2d(textureCoordinate.x(), textureCoordinate.y()));
                }
            } else {
                vertex.setTexcoords(new Vector2d());
            }

            diffuseColor[0] = (byte) (diffuse.x * 255);
            diffuseColor[1] = (byte) (diffuse.y * 255);
            diffuseColor[2] = (byte) (diffuse.z * 255);
            diffuseColor[3] = (byte) (diffuse.w * 255);
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
        int[] indicesArray = new int[numIndices];
        IntBuffer indicesBuffer = aiFace.mIndices();
        for (int i = 0; i < numIndices; i++) {
            indicesArray[i] = indicesBuffer.get(i);
        }
        face.setIndices(indicesArray);
        return face;
    }
}
