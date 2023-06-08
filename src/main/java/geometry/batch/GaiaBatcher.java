package geometry.batch;

import geometry.basic.GaiaBoundingBox;
import geometry.basic.GaiaRectangle;
import geometry.exchangable.GaiaBuffer;
import geometry.exchangable.GaiaBufferDataSet;
import geometry.exchangable.GaiaSet;
import geometry.exchangable.GaiaUniverse;
import geometry.structure.GaiaMaterial;
import geometry.structure.GaiaTexture;
import geometry.types.AttributeType;
import geometry.types.TextureType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.lwjgl.opengl.GL20;
import util.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GaiaBatcher {
    private final GaiaSet bachedSet;
    private GaiaBoundingBox globalBoundingBox = new GaiaBoundingBox();

    public GaiaBatcher() {
        this.bachedSet = new GaiaSet();
        this.bachedSet.setProjectName("GaiaBatchedProject");
        this.globalBoundingBox = new GaiaBoundingBox();
    }

    public void excute(GaiaUniverse universe) {

    }

    public GaiaSet batchAltas(Path imagesPath, List<GaiaMaterial> materials, List<GaiaBufferDataSet> bufferDataSets) {
        GaiaTextureCoordinator textureCoordinator = new GaiaTextureCoordinator("", materials, bufferDataSets);
        textureCoordinator.batchTextures();
        textureCoordinator.writeBatchedImage(imagesPath);

        List<List<GaiaBufferDataSet>> splitedLimitList = splitIndicesLimit(bufferDataSets);
        List<GaiaBufferDataSet> finalBatchedDataSet = splitedLimitList.stream().map((splitedLimit) -> {
            GaiaBufferDataSet batchedBufferData = batchVertices(splitedLimit);
            batchedBufferData.setMaterialId(0);
            return batchedBufferData;
        }).collect(Collectors.toList());

        return this.bachedSet;
    }

    public GaiaSet batch(List<GaiaSet> gaiaSets, Path imagesPath) {
        List<GaiaBufferDataSet> batchedBufferDatas = new ArrayList<>();
        List<GaiaMaterial> batchedMaterials = new ArrayList<>();
        for (GaiaSet gaiaSet : gaiaSets) {
            List<GaiaBufferDataSet> bufferDatas = gaiaSet.getBufferDatas();
            List<GaiaMaterial> materials = gaiaSet.getMaterials();

            int batchedMaterialId = batchedMaterials.size();
            for (GaiaBufferDataSet gaiaBufferDataSet : bufferDatas) {
                int originMaterialId = gaiaBufferDataSet.getMaterialId();
                gaiaBufferDataSet.setTransformMatrix(gaiaSet.getTransformMatrix());
                gaiaBufferDataSet.setMaterialId(batchedMaterialId + originMaterialId);
                GaiaBoundingBox boundingBox = getBoundingBox(gaiaBufferDataSet, gaiaSet.getTransformMatrix());
                if (globalBoundingBox.isInit()) {
                    globalBoundingBox.addBoundingBox(boundingBox);
                } else {
                    globalBoundingBox = boundingBox;
                }
            }
            for (GaiaMaterial gaiaMaterial : materials) {
                int originMaterialId = gaiaMaterial.getId();
                gaiaMaterial.setId(batchedMaterialId + originMaterialId);
            }
            batchedBufferDatas.addAll(bufferDatas);
            batchedMaterials.addAll(materials);
        }

        assert globalBoundingBox != null;
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
                return materialId <= sameMaterial.getId();
            }
            return true;
        }).collect(Collectors.toList());

        // MaterialId를 재정렬
        batchedBufferDatas.forEach((batchedBufferData) -> {
            for (int i = 0; i < filterdMaterials.size(); i++) {
                GaiaMaterial material = filterdMaterials.get(i);
                if (material.getId() == batchedBufferData.getMaterialId()) {
                    batchedBufferData.setMaterialId(i);
                    break;
                }
            }
        });
        for (int i = 0; i < filterdMaterials.size(); i++) {
            GaiaMaterial material = filterdMaterials.get(i);
            material.setId(i);
        }

        LinkedHashMap<Integer, List<GaiaBufferDataSet>> batchedBufferDatasMap = new LinkedHashMap<>();
        for (GaiaBufferDataSet batchedBufferData : batchedBufferDatas) {
            int materialId = batchedBufferData.getMaterialId();
            List<GaiaBufferDataSet> bufferDataSetDataList = batchedBufferDatasMap.computeIfAbsent(materialId, k -> new ArrayList<>());
            bufferDataSetDataList.add(batchedBufferData);
            translate(batchedBufferData, translation);
        }

        List<GaiaBufferDataSet> filterdBufferDatas = new ArrayList<>();
        batchedBufferDatasMap.forEach((materialId, bufferDataSetDataList) -> {
            GaiaBufferDataSet batchedBufferData = batchVertices(bufferDataSetDataList);
            batchedBufferData.setMaterialId(materialId);
            filterdBufferDatas.add(batchedBufferData);
        });

        for (GaiaBufferDataSet bufferDataSet : filterdBufferDatas) {
            int materialId = bufferDataSet.getMaterialId();
            GaiaMaterial material = filterdMaterials.get(materialId);
            if (material != null) {
                LinkedHashMap<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
                List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
                if (textures == null || textures.size() == 0) {
                    LinkedHashMap<AttributeType, GaiaBuffer> buffers = bufferDataSet.getBuffers();
                    buffers.remove(AttributeType.TEXCOORD);
                }
                material.setRepeat(checkRepeat(material, bufferDataSet));
            }
        }

        List<GaiaMaterial> nonRepeatMaterials = filterdMaterials.stream()
                .filter((material) -> !material.isRepeat())
                .collect(Collectors.toList());
        List<GaiaBufferDataSet> nonRepeatBufferDatas = filterdBufferDatas.stream().filter((bufferDataSet) -> {
            int materialId = bufferDataSet.getMaterialId();
            GaiaMaterial material = findMaterial(filterdMaterials, materialId);
            return !material.isRepeat();
        }).collect(Collectors.toList());

        List<GaiaMaterial> repeatMaterials = filterdMaterials.stream()
                .filter(GaiaMaterial::isRepeat)
                .collect(Collectors.toList());
        List<GaiaBufferDataSet> repeatBufferDatas = filterdBufferDatas.stream().filter((bufferDataSet) -> {
            int materialId = bufferDataSet.getMaterialId();
            GaiaMaterial material = findMaterial(filterdMaterials, materialId);
            return material.isRepeat();
        }).collect(Collectors.toList());

        log.info("nonRepeatMaterials : " + nonRepeatMaterials.size());
        log.info("nonRepeatBufferDatas : " + nonRepeatBufferDatas.size());
        log.info("repeatMaterials : " + repeatMaterials.size());
        log.info("repeatBufferDatas : " + repeatBufferDatas.size());

        GaiaTextureCoordinator textureCoordinator = new GaiaTextureCoordinator("", nonRepeatMaterials, nonRepeatBufferDatas);
        textureCoordinator.batchTextures();
        textureCoordinator.writeBatchedImage(imagesPath);

        List<List<GaiaBufferDataSet>> splitedIndicesLimits = splitIndicesLimit(nonRepeatBufferDatas);
        List<GaiaBufferDataSet> finalBatchedDataSet = splitedIndicesLimits.stream().map((splitedIndicesLimit) -> {
            GaiaBufferDataSet batchedBufferData = batchVertices(splitedIndicesLimit);
            batchedBufferData.setMaterialId(0);
            return batchedBufferData;
        }).collect(Collectors.toList());

        nonRepeatMaterials.removeIf((nonRepeatMaterial) -> {
            return nonRepeatMaterial.getId() > 0;
        });

        List<GaiaBufferDataSet> resultBufferDatas = new ArrayList<>();
        resultBufferDatas.addAll(finalBatchedDataSet);

        List<GaiaMaterial> resultMaterials = new ArrayList<>();
        resultMaterials.addAll(nonRepeatMaterials);

        this.bachedSet.setBufferDatas(resultBufferDatas);
        this.bachedSet.setMaterials(resultMaterials);
        return this.bachedSet;
    }


    private List<List<GaiaBufferDataSet>> splitIndicesLimit(List<GaiaBufferDataSet> bufferDataSets) {
        final int SHORT_LIMIT = 65535;

        int count = 0;
        List<List<GaiaBufferDataSet>> result = new ArrayList<>();
        List<GaiaBufferDataSet> splited = new ArrayList<>();
        result.add(splited);

        for (GaiaBufferDataSet bufferDataSet : bufferDataSets) {
            LinkedHashMap<AttributeType, GaiaBuffer> buffers = bufferDataSet.getBuffers();

            GaiaBuffer indicesBuffer = buffers.get(AttributeType.INDICE);
            if (indicesBuffer != null) {
                int indicesLength = indicesBuffer.getShorts().length;

                if ((count + indicesLength) > SHORT_LIMIT) {
                    splited = new ArrayList<>();
                    result.add(splited);
                    count = indicesLength;
                } else {
                    count += indicesLength;
                }
                splited.add(bufferDataSet);
            }
        }
        return result;
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
        GaiaRectangle batchedBoundingRectangle = null;
        int totalIndicesMax = 0;

        for (GaiaBufferDataSet bufferDataSet : bufferDataSets) {
            LinkedHashMap<AttributeType, GaiaBuffer> buffers = bufferDataSet.getBuffers();

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
            GaiaRectangle boundingRectangle = bufferDataSet.getTexcoordBoundingRectangle();
            if (boundingRectangle != null) {
                if (batchedBoundingRectangle == null) {
                    batchedBoundingRectangle = boundingRectangle;
                } else {
                    batchedBoundingRectangle.addBoundingRectangle(boundingRectangle);
                }
            }
        }

        batchedBufferData.setTexcoordBoundingRectangle(batchedBoundingRectangle);
        if (totalPositionCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_FLOAT);
            buffer.setElementsCount(totalPositionCount / 3);
            buffer.setGlDimension((byte) 3);
            buffer.setFloats(ArrayUtils.convertFloatArrayToList(positions));
            batchedBufferData.getBuffers().put(AttributeType.POSITION, buffer);
        }
        if (totalNormalCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_FLOAT);
            buffer.setElementsCount(totalNormalCount / 3);
            buffer.setGlDimension((byte) 3);
            buffer.setFloats(ArrayUtils.convertFloatArrayToList(normals));
            batchedBufferData.getBuffers().put(AttributeType.NORMAL, buffer);
        }
        if (totalTexCoordCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_FLOAT);
            buffer.setElementsCount(totalTexCoordCount / 2);
            buffer.setGlDimension((byte) 2);
            buffer.setFloats(ArrayUtils.convertFloatArrayToList(texCoords));
            batchedBufferData.getBuffers().put(AttributeType.TEXCOORD, buffer);
        }
        if (totalIndicesCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ELEMENT_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_UNSIGNED_SHORT);
            buffer.setElementsCount(totalIndicesCount);
            buffer.setGlDimension((byte) 1);
            buffer.setShorts(ArrayUtils.convertShortArrayToList(indices));
            batchedBufferData.getBuffers().put(AttributeType.INDICE, buffer);
        }
        return batchedBufferData;
    }

    //translation
    private void translate(GaiaBufferDataSet batchedBufferData, Vector3d translations) {
        LinkedHashMap<AttributeType, GaiaBuffer> buffers = batchedBufferData.getBuffers();
        GaiaBuffer positionBuffer = buffers.get(AttributeType.POSITION);
        Matrix4d transform = batchedBufferData.getTransformMatrix();
        Vector3d translatedPosition = transform.transformPosition(translations, new Vector3d());
        if (positionBuffer != null) {
            float[] positions = positionBuffer.getFloats();
            for (int i = 0; i < positions.length; i += 3) {
                float x = positions[i];
                float y = positions[i + 1];
                float z = positions[i + 2];
                Vector3d position = new Vector3d(x, y, z);
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
                    //boundingBox.setInit(transformedPosition);
                    boundingBox.addPoint(transformedPosition);
                } else {
                    boundingBox.addPoint(transformedPosition);
                }
            }
        }
        return boundingBox;
    }

    private GaiaMaterial getSameMaterial(GaiaMaterial material, List<GaiaMaterial> materials) {
        for (GaiaMaterial searchMaterial : materials) {
            if (searchMaterial.compareTo(material)) {
                return searchMaterial;
            }
        }
        return null;
    }

    private boolean checkRepeat(GaiaMaterial material, GaiaBufferDataSet gaiaBufferDataSet) {
        if (material.isRepeat()) {
            return true;
        }
        LinkedHashMap<AttributeType, GaiaBuffer> buffers = gaiaBufferDataSet.getBuffers();
        GaiaBuffer texCoordBuffer = buffers.get(AttributeType.TEXCOORD);
        if (texCoordBuffer == null) {
            return false;
        }
        GaiaRectangle boundingRectangle = gaiaBufferDataSet.getTexcoordBoundingRectangle();
        if (boundingRectangle != null) {
            Vector2d range = boundingRectangle.getRange();
            return range.x > 1.00f || range.y > 1.001f;
        }
        return false;
    }

    private GaiaMaterial findMaterial(List<GaiaMaterial> materials, int materialId) {
        return materials.stream()
                .filter(material -> material.getId() == materialId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("not found material"));
    }
}
