package geometry.exchangable;

import geometry.basic.GaiaBoundingBox;
import geometry.structure.GaiaMaterial;
import geometry.structure.GaiaTexture;
import geometry.types.AttributeType;
import geometry.types.TextureType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.lwjgl.opengl.GL20;
import util.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class GaiaBatcher {
    GaiaSet batched = new GaiaSet();

    List<GaiaBufferDataSet> batchedBufferDatas = new ArrayList<>();
    List<GaiaMaterial> batchedMaterials = new ArrayList<>();

    public GaiaBatcher() {

    }

    // GaiaSets를 하나로
    public GaiaSet batch(List<GaiaSet> gaiaSets) {
        GaiaSet batched = new GaiaSet();
        batched.setProjectName("batched");
        List<GaiaBufferDataSet> batchedBufferDatas = new ArrayList<>();
        List<GaiaMaterial> batchedMaterials = new ArrayList<>();
        //List<GaiaBoundingBox> boundingBoxes = new ArrayList<>();
        GaiaBoundingBox globalBoundingBox = null;

        for (GaiaSet gaiaSet : gaiaSets) {
            List<GaiaBufferDataSet> bufferDatas = gaiaSet.getBufferDatas();
            List<GaiaMaterial> materials = gaiaSet.getMaterials();

            int batchedMaterialId = batchedMaterials.size();
            for (GaiaBufferDataSet gaiaBufferDataSet : bufferDatas) {
                int originMaterialId = gaiaBufferDataSet.getMaterialId();
                gaiaBufferDataSet.setTransformMatrix(gaiaSet.getTransformMatrix());
                gaiaBufferDataSet.setMaterialId(batchedMaterialId + originMaterialId);

                GaiaBoundingBox boundingBox = getBoundingBox(gaiaBufferDataSet, gaiaSet.getTransformMatrix());
                //boundingBoxes.add(boundingBox);

                if (globalBoundingBox == null) {
                    globalBoundingBox = boundingBox;
                } else {
                    globalBoundingBox.addBoundingBox(boundingBox);
                }
            }
            for (GaiaMaterial gaiaMaterial : materials) {
                int originMaterialId = gaiaMaterial.getId();
                gaiaMaterial.setId(batchedMaterialId + originMaterialId);
            }
            batchedBufferDatas.addAll(bufferDatas);
            batchedMaterials.addAll(materials);
        }

        Vector3d translation = globalBoundingBox.getCenter();
        translation.negate();

        List<GaiaMaterial> filterdMaterials = batchedMaterials.stream().filter((material) -> {
            int materialId = material.getId();
            GaiaMaterial sameMaterial = getSameMaterial(material, batchedMaterials);
            if (sameMaterial != null) {
                for (GaiaBufferDataSet gaiaBufferDataSet : batchedBufferDatas) {
                    int usedMaterialId = gaiaBufferDataSet.getMaterialId();
                    if (usedMaterialId == materialId) {
                        gaiaBufferDataSet.setMaterialId(sameMaterial.getId());
                    }
                }
                if (materialId > sameMaterial.getId()) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());

        batchedBufferDatas.forEach((batchedBufferData) -> {
            for (int i = 0; i < filterdMaterials.size(); i++) {
                GaiaMaterial material = filterdMaterials.get(i);
                if (material.getId() == batchedBufferData.getMaterialId()) {
                    batchedBufferData.setMaterialId(i);
                    break;
                }
            }
        });

        LinkedHashMap<Integer, List<GaiaBufferDataSet>> batchedBufferDatasMap = new LinkedHashMap<>();
        for (int i = 0; i < batchedBufferDatas.size(); i++) {
            GaiaBufferDataSet batchedBufferData = batchedBufferDatas.get(i);
            int materialId = batchedBufferData.getMaterialId();
            List<GaiaBufferDataSet> bufferDataSetDataList = batchedBufferDatasMap.get(materialId);
            if (bufferDataSetDataList == null) {
                bufferDataSetDataList = new ArrayList<>();
                batchedBufferDatasMap.put(materialId, bufferDataSetDataList);
            }
            bufferDataSetDataList.add(batchedBufferData);
            translate(batchedBufferData, translation);
        }

        List<GaiaBufferDataSet> filterdBufferDatas = new ArrayList<>();
        batchedBufferDatasMap.forEach((materialId, bufferDataSetDataList) -> {
            GaiaBufferDataSet batchedBufferData = batchVertices(bufferDataSetDataList);
            batchedBufferData.setMaterialId(materialId);
            filterdBufferDatas.add(batchedBufferData);
        });

        for (int i = 0; i < filterdMaterials.size(); i++) {
            GaiaMaterial material = filterdMaterials.get(i);
            material.setId(i);
        }

        batched.setBufferDatas(filterdBufferDatas);
        batched.setMaterials(filterdMaterials);
        return batched;
    }

    private GaiaBufferDataSet batchVertices(List<GaiaBufferDataSet> bufferDataSets) {
        GaiaBufferDataSet batchedBufferData = new GaiaBufferDataSet();
        int totalPositionCount = 0;
        int totalNormalCount = 0;
        int totalTexCoordCount = 0;
        int totalIndicesCount = 0;

        List<Float> positions = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Short> indices = new ArrayList<>();
        int totalIndicesMax = 0;

        for (GaiaBufferDataSet bufferDataSet : bufferDataSets) {
            LinkedHashMap<AttributeType, GaiaBuffer> buffers = bufferDataSet.getBuffers();
            GaiaBuffer positionBuffer = buffers.get(AttributeType.POSITION);
            if (positionBuffer != null) {
                totalPositionCount += positionBuffer.getFloats().length;
                for (float position : positionBuffer.getFloats()) {
                    positions.add(position);
                }
            }

            GaiaBuffer normalBuffer = buffers.get(AttributeType.NORMAL);
            if (normalBuffer != null) {
                totalNormalCount += normalBuffer.getFloats().length;
                for (float normal : normalBuffer.getFloats()) {
                    normals.add(normal);
                }
            }

            GaiaBuffer texCoordBuffer = buffers.get(AttributeType.TEXCOORD);
            if (texCoordBuffer != null) {
                totalTexCoordCount += texCoordBuffer.getFloats().length;
                for (float texCoord : texCoordBuffer.getFloats()) {
                    texCoords.add(texCoord);
                }
            }

            GaiaBuffer indicesBuffer = buffers.get(AttributeType.INDICE);
            if (indicesBuffer != null) {
                int indicesMax = 0;
                for (short indice : indicesBuffer.getShorts()) {
                    indicesMax = Math.max(indicesMax, indice);
                    int value = totalIndicesMax + indice;
                    indices.add((short) value);
                }
                totalIndicesMax = totalIndicesMax + (indicesMax + 1);
                totalIndicesCount += indicesBuffer.getShorts().length;
            }
        }

        if (totalPositionCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_FLOAT);
            buffer.setElementsCount(totalPositionCount / 3);
            buffer.setGlDimension((byte) 3);
            buffer.setFloats(ArrayUtils.convertFloatArrayToArrayList(positions));
            batchedBufferData.getBuffers().put(AttributeType.POSITION, buffer);
        }

        if (totalNormalCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_FLOAT);
            buffer.setElementsCount(totalNormalCount / 3);
            buffer.setGlDimension((byte) 3);
            buffer.setFloats(ArrayUtils.convertFloatArrayToArrayList(normals));
            batchedBufferData.getBuffers().put(AttributeType.NORMAL, buffer);
        }

        if (totalTexCoordCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_FLOAT);
            buffer.setElementsCount(totalTexCoordCount / 2);
            buffer.setGlDimension((byte) 2);
            buffer.setFloats(ArrayUtils.convertFloatArrayToArrayList(texCoords));
            batchedBufferData.getBuffers().put(AttributeType.TEXCOORD, buffer);
        }

        if (totalIndicesCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ELEMENT_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_UNSIGNED_SHORT);
            buffer.setElementsCount(totalIndicesCount);
            buffer.setGlDimension((byte) 1);
            buffer.setShorts(ArrayUtils.convertShortArrayToArrayList(indices));
            batchedBufferData.getBuffers().put(AttributeType.INDICE, buffer);
        }


        return batchedBufferData;
    }

    //translation
    private void translate(GaiaBufferDataSet batchedBufferData, Vector3d translations) {
        GaiaBoundingBox boundingBox = null;
        LinkedHashMap<AttributeType, GaiaBuffer> buffers = batchedBufferData.getBuffers();
        GaiaBuffer positionBuffer = buffers.get(AttributeType.POSITION);

        Matrix4d transform = batchedBufferData.getTransformMatrix();
        Vector3d translatedPosition = transform.transformPosition(translations, new Vector3d());
        //Vector3d translatedPosition = new Vector3d(position);

        if (positionBuffer != null) {
            float[] positions = positionBuffer.getFloats();
            for (int i = 0; i < positions.length; i += 3) {
                float x = positions[i];
                float y = positions[i + 1];
                float z = positions[i + 2];
                Vector3d position = new Vector3d(x, y, z);
                //Vector3d translatedPosition = new Vector3d(position);
                position.add(translatedPosition);
                positions[i] = (float) position.x;
                positions[i + 1] = (float) position.y;
                positions[i + 2] = (float) position.z;
            }
        }
    }

    // getBoundingBox
    private GaiaBoundingBox getBoundingBox(GaiaBufferDataSet batchedBufferData, Matrix4d transform) {
        GaiaBoundingBox boundingBox = null;
        LinkedHashMap<AttributeType, GaiaBuffer> buffers = batchedBufferData.getBuffers();
        GaiaBuffer positionBuffer = buffers.get(AttributeType.POSITION);
        if (positionBuffer != null) {
            float[] positions = positionBuffer.getFloats();
            for (int i = 0; i < positions.length; i += 3) {
                float x = positions[i];
                float y = positions[i + 1];
                float z = positions[i + 2];
                Vector3d position = new Vector3d(x, y, z);
                Vector3d transformedPosition = new Vector3d(position);
                if (transform != null) {
                    transform.transformPosition(position, transformedPosition);
                }
                if (boundingBox == null) {
                    boundingBox = new GaiaBoundingBox();
                    boundingBox.setInit(transformedPosition);
                } else {
                    boundingBox.addPoint(transformedPosition);
                }
            }
        }
        return boundingBox;
    }

    private GaiaMaterial getSameMaterial(GaiaMaterial material, List<GaiaMaterial> materials) {
        Vector4d diffuseColor = material.getDiffuseColor();
        LinkedHashMap<TextureType, List<GaiaTexture>> textures = material.getTextures();
        List<GaiaTexture> diffuseTextureList = textures.get(TextureType.DIFFUSE);
        GaiaTexture diffuseTexture = null;
        if (diffuseTextureList != null && diffuseTextureList.size() > 0) {
            diffuseTexture = diffuseTextureList.get(0);
        }
        for (GaiaMaterial searchMaterial : materials) {
            Vector4d searchDiffuseColor = searchMaterial.getDiffuseColor();
            LinkedHashMap<TextureType, List<GaiaTexture>> searchTextures = searchMaterial.getTextures();
            List<GaiaTexture> searchDiffuseTextureList = searchTextures.get(TextureType.DIFFUSE);
            GaiaTexture searchDiffuseTexture = null;
            if (searchDiffuseTextureList != null && searchDiffuseTextureList.size() > 0) {
                searchDiffuseTexture = searchDiffuseTextureList.get(0);
            }

            if (material.getId() == searchMaterial.getId()) {
                continue;
            }
            if (diffuseTexture == null && searchDiffuseTexture == null) {
                if (diffuseColor.equals(searchDiffuseColor)) {
                    return searchMaterial;
                }
            } else if (diffuseTexture != null && searchDiffuseTexture != null) {
                File diffuseTextureFile = new File(diffuseTexture.getParentPath() + File.separator + diffuseTexture.getPath());
                File searchDiffuseTextureFile = new File(searchDiffuseTexture.getParentPath() + File.separator + searchDiffuseTexture.getPath());
                //log.info("test");
                if (diffuseTexture.getPath().equals(searchDiffuseTexture.getPath())) {
                    //log.info("텍스쳐 경로가 같음");
                    return searchMaterial;
                } else if (diffuseTextureFile.length() == searchDiffuseTextureFile.length()) {
                    //log.info(diffuseTextureFile.length() + "," + searchDiffuseTextureFile.length());
                    //og.info("텍스쳐 길이가 같음");
                    if (isEqual(diffuseTextureFile, searchDiffuseTextureFile)) {
                        //log.info("같은 텍스쳐: " + diffuseTexture.getPath() + " -> " + searchDiffuseTexture.getPath());
                        return searchMaterial;
                    }
                } /*else if (Math.abs(diffuseTextureFile.length() - searchDiffuseTextureFile.length()) < 200) {
                    // 텍스쳐 사이즈가 다르지만 200byte밖에 차이 나지 않음
                    log.info(diffuseTextureFile.getPath() + "," + searchDiffuseTextureFile.getPath());
                    log.info(diffuseTextureFile.length() + "," + searchDiffuseTextureFile.length());
                    //log.info(String.valueOf(Math.abs(diffuseTextureFile.length() - searchDiffuseTextureFile.length())));
                    return searchMaterial;
                    //log.info("사이즈, 경로 모두 같지 않음");
                }*/
            }
        }
        return null;
    }

    private boolean isEqual(File firstFile, File secondFile) {
        try {
            return FileUtils.contentEquals(firstFile, secondFile);
        } catch (IOException e)
        {
            log.error(e.getMessage());
            return false;
        }
    }
}
