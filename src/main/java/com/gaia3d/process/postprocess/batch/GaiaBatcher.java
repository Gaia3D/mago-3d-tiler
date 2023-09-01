package com.gaia3d.process.postprocess.batch;

import com.gaia3d.basic.exchangable.GaiaBuffer;
import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.structure.GaiaMaterial;
import com.gaia3d.basic.structure.GaiaTexture;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.LevelOfDetail;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector4d;
import org.lwjgl.opengl.GL20;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class GaiaBatcher implements Batcher {
    private final static int SHORT_LIMIT = 65535;
    private final CommandLine command;

    public GaiaBatcher(CommandLine command) {
        this.command = command;
    }

    private void reassignMaterialsToGaiaBufferDataSetWithSameMaterial(List<GaiaBufferDataSet> dataSets, LevelOfDetail lod) {
        int datasetsCount = dataSets.size();
        for (int i = 0; i < datasetsCount; i++) {
            GaiaBufferDataSet dataSet = dataSets.get(i);
            GaiaMaterial material = dataSet.material;
            for (int j = i + 1; j < datasetsCount; j++) {
                GaiaBufferDataSet dataSet2 = dataSets.get(j);
                if (dataSet == dataSet2) continue;

                if (dataSet2.material == material) {
                    continue;
                }

                if (GaiaMaterial.areEqualMaterials(dataSet2.material, material, lod.getTextureScale())) {
                    //dataSet2.material.deleteTextures();
                    dataSet2.material = material;
                }
            }
        }
    }

    private void setBatchId(List<GaiaSet> sets) {
        for (int i = 0; i < sets.size(); i++) {
            GaiaSet set = sets.get(i);
            List<GaiaBufferDataSet> dataSets = set.getBufferDatas();
            for (GaiaBufferDataSet dataSet : dataSets) {
                Map<AttributeType, GaiaBuffer> buffers = dataSet.getBuffers();
                GaiaBuffer positionBuffer = buffers.get(AttributeType.POSITION);
                int elementsCount = positionBuffer.getElementsCount();
                float[] batchIdList = new float[elementsCount];
                Arrays.fill(batchIdList, i);

                GaiaBuffer batchIdBuffer = new GaiaBuffer();
                batchIdBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
                batchIdBuffer.setGlType(GL20.GL_FLOAT);
                batchIdBuffer.setElementsCount(elementsCount);
                batchIdBuffer.setGlDimension((byte) 1);
                batchIdBuffer.setFloats(batchIdList);

                buffers.put(AttributeType.BATCHID, batchIdBuffer);
            }
        }
    }

    public ContentInfo run(ContentInfo contentInfo) {
        List<TileInfo> tileInfos = contentInfo.getTileInfos();
        List<GaiaSet> sets = tileInfos.stream()
                .map(TileInfo::getSet)
                .collect(Collectors.toList());
        setBatchId(sets);

        List<GaiaBufferDataSet> batchedDataSets = new ArrayList<>();
        List<GaiaMaterial> batchedMaterials = new ArrayList<>();
        sets.forEach((set) -> {
            List<GaiaBufferDataSet> dataSets = set.getBufferDatas();
            List<GaiaMaterial> materials = set.getMaterials();
            int materialIdOffset = batchedMaterials.size();
            dataSets.forEach((dataSet) -> {
                int materialId = dataSet.getMaterialId();
                dataSet.setTransformMatrix(set.getTransformMatrix());
                dataSet.setMaterialId(materialIdOffset + materialId);
            });
            materials.forEach((material) -> {
                int materialId = material.getId();
                material.setId(materialIdOffset + materialId);
            });
            batchedDataSets.addAll(dataSets);
            batchedMaterials.addAll(materials);
        });

        // check if exist equal materials.***
        reassignMaterialsToGaiaBufferDataSetWithSameMaterial(batchedDataSets, contentInfo.getLod());
        List<GaiaMaterial> filteredMaterials = new ArrayList<>();
        getMaterialslIstOfBufferDataSet(batchedDataSets, filteredMaterials);

        // batch dataSets with same material.***
        List<GaiaBufferDataSet> filteredDataSets = batchDataSetsWithTheSameMaterial(batchedDataSets);
        setMaterialsIndexInList(filteredMaterials, filteredDataSets);
        checkIsRepeatMaterial(filteredDataSets);

        List<GaiaMaterial> colorMaterials = filteredMaterials.stream().filter((material) -> {
            Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
            List<GaiaTexture> diffuseTextures = textures.get(TextureType.DIFFUSE);
            return diffuseTextures.isEmpty();
        }).collect(Collectors.toList());
        List<GaiaBufferDataSet> colorDataSet = filteredDataSets.stream().filter((bufferDataSet) -> {
            GaiaMaterial material = bufferDataSet.material;
            Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
            List<GaiaTexture> diffuseTextures = textures.get(TextureType.DIFFUSE);
            createColorBuffer(bufferDataSet);
            return diffuseTextures.isEmpty();
        }).collect(Collectors.toList());
        setMaterialsIndexInList(colorMaterials, colorDataSet);

        List<GaiaMaterial> textureMaterials = filteredMaterials.stream().filter((material) -> {
            Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
            List<GaiaTexture> diffuseTextures = textures.get(TextureType.DIFFUSE);
            return !diffuseTextures.isEmpty();
        }).collect(Collectors.toList());
        List<GaiaBufferDataSet> textureDataSet = filteredDataSets.stream().filter((bufferDataSet) -> {
            GaiaMaterial material = bufferDataSet.material;
            Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
            List<GaiaTexture> diffuseTextures = textures.get(TextureType.DIFFUSE);
            return !diffuseTextures.isEmpty();
        }).collect(Collectors.toList());
        setMaterialsIndexInList(textureMaterials, textureDataSet);

        List<GaiaMaterial> clampMaterials = textureMaterials.stream().filter((material) -> !material.isRepeat()).collect(Collectors.toList());
        List<GaiaBufferDataSet> clampDataSets = textureDataSet.stream().filter((bufferDataSet) -> {
            GaiaMaterial material = bufferDataSet.material;
            return !material.isRepeat();
        }).collect(Collectors.toList());
        setMaterialsIndexInList(clampMaterials, clampDataSets);

        List<GaiaMaterial> repeatMaterials = textureMaterials.stream().filter(GaiaMaterial::isRepeat).collect(Collectors.toList());
        List<GaiaBufferDataSet> repeatDataSets = textureDataSet.stream().filter((bufferDataSet) -> {
            GaiaMaterial material = bufferDataSet.material;
            return material.isRepeat();
        }).collect(Collectors.toList());
        setMaterialsIndexInList(repeatMaterials, repeatDataSets);

        List<GaiaBufferDataSet> resultBufferDatas = new ArrayList<>();
        List<GaiaMaterial> resultMaterials = new ArrayList<>();
        if (!clampDataSets.isEmpty() && !clampMaterials.isEmpty()) {
            atlasTextures(contentInfo.getLod(), contentInfo.getNodeCode(), clampDataSets, clampMaterials);
            List<List<GaiaBufferDataSet>> splitedDataSets = divisionByMaxIndices(clampDataSets);
            List<GaiaBufferDataSet> batchedClampDataSets = batchClampMaterial(splitedDataSets);
            clampMaterials.removeIf((clampMaterial) -> {
                return clampMaterial.getId() > 0;
            });
            clampMaterials.get(0).setName("ATLAS");
            resultMaterials.addAll(clampMaterials);
            resultBufferDatas.addAll(batchedClampDataSets);
        }

        if (!repeatDataSets.isEmpty() && !repeatMaterials.isEmpty()) {
            rearrangeRepeatMaterial(repeatDataSets, repeatMaterials, resultMaterials.size());
            resultMaterials.addAll(repeatMaterials);
            resultBufferDatas.addAll(repeatDataSets);
        }
        if (!colorMaterials.isEmpty() && !colorDataSet.isEmpty()) {
            rearrangeRepeatMaterial(colorDataSet, colorMaterials, resultMaterials.size());
            resultMaterials.addAll(colorMaterials);
            resultBufferDatas.addAll(colorDataSet);
        }

        GaiaSet batchedSet = new GaiaSet();
        batchedSet.setProjectName(contentInfo.getName());
        batchedSet.setBufferDatas(resultBufferDatas);
        batchedSet.setMaterials(resultMaterials);

        if (resultBufferDatas.isEmpty() || resultMaterials.isEmpty()) {
            log.error("Batched Set is empty");
        }

        Matrix4d transform = new Matrix4d();
        transform.identity();

        batchedSet.setTransformMatrix(transform);

        contentInfo.setBatchedSet(batchedSet);
        return contentInfo;
    }

    public void getMaterialslIstOfBufferDataSet(List<GaiaBufferDataSet> bufferDataSets, List<GaiaMaterial> materials) {
        // first, make a map to avoid duplicate materials
        Map<GaiaMaterial, GaiaMaterial> materialMap = new WeakHashMap<>();
        for (GaiaBufferDataSet bufferDataSet : bufferDataSets) {
            materialMap.put(bufferDataSet.getMaterial(), bufferDataSet.getMaterial());
        }
        // second, make a list from the map
        materials.addAll(materialMap.values());
    }

    private void setMaterialsIndexInList(List<GaiaMaterial> materials, List<GaiaBufferDataSet> dataSets) {
        for (int i = 0; i < materials.size(); i++) {
            GaiaMaterial material = materials.get(i);
            material.setId(i);
        }

        dataSets.forEach((dataSet) -> {
            int materialId = dataSet.material.getId();
            dataSet.setMaterialId(materialId);
        });
    }

    // Material Id 재정렬
    private void rearrangeRepeatMaterial(List<GaiaBufferDataSet> dataSets, List<GaiaMaterial> materials, int offset) {
        dataSets.forEach((batchedBufferData) -> {
            for (int i = 0; i < materials.size(); i++) {
                GaiaMaterial material = materials.get(i);
                if (material.getId() == batchedBufferData.getMaterialId()) {
                    batchedBufferData.setMaterialId(i + offset);
                    break;
                }
            }
        });
        for (int i = 0; i < materials.size(); i++) {
            GaiaMaterial material = materials.get(i);
            material.setId(i + offset);
        }
    }

    // Indices 최대 값 만큼 객체를 나눔
    private List<List<GaiaBufferDataSet>> divisionByMaxIndices(List<GaiaBufferDataSet> dataSets) {
        int count = 0;
        List<List<GaiaBufferDataSet>> result = new ArrayList<>();
        List<GaiaBufferDataSet> splitList = new ArrayList<>();
        result.add(splitList);
        for (GaiaBufferDataSet dataSet : dataSets) {
            Map<AttributeType, GaiaBuffer> buffers = dataSet.getBuffers();
            GaiaBuffer indicesBuffer = buffers.get(AttributeType.INDICE);
            if (indicesBuffer != null) {
                int indicesLength = indicesBuffer.getInts().length;
                if ((count + indicesLength) >= SHORT_LIMIT) {
                    if (!splitList.isEmpty()) {
                        splitList = new ArrayList<>();
                    }
                    result.add(splitList);
                    count = indicesLength;
                } else {
                    count += indicesLength;
                }
                splitList.add(dataSet);
            }
        }
        return result;
    }

    // 객체의 Indices와 Vertices를 하나로 배칭
    /*rivate List<GaiaBufferDataSet> batchDataSets(List<GaiaBufferDataSet> dataSets, Vector3d translation) {
        Map<Integer, List<GaiaBufferDataSet>> dataSetsMap = new Map<>();
        List<GaiaBufferDataSet> filterdBufferDataList = new ArrayList<>();
        for (GaiaBufferDataSet dataSet : dataSets) {
            int materialId = dataSet.getMaterialId();
            List<GaiaBufferDataSet> bufferDataSetDataList = dataSetsMap.computeIfAbsent(materialId, k -> new ArrayList<>());
            bufferDataSetDataList.add(dataSet);
            translateOrigin(dataSet, translation);
        }
        dataSetsMap.forEach((materialId, bufferDataSetDataList) -> {
            GaiaBufferDataSet batchedBufferData = batchVertices(bufferDataSetDataList);
            batchedBufferData.setMaterialId(materialId);
            filterdBufferDataList.add(batchedBufferData);
        });
        return filterdBufferDataList;
    }*/

    private List<GaiaBufferDataSet> batchDataSetsWithTheSameMaterial(List<GaiaBufferDataSet> dataSets) {
        List<GaiaBufferDataSet> filterdBufferDataList = new ArrayList<>();
        // make map : key = GaiaMaterial, value = array<GaiaBufferDataSet>.
        Map<GaiaMaterial, List<GaiaBufferDataSet>> dataSetsMap = new WeakHashMap<>();
        for (GaiaBufferDataSet dataSet : dataSets) {
            GaiaMaterial material = dataSet.getMaterial();
            List<GaiaBufferDataSet> bufferDataSetDataList = dataSetsMap.computeIfAbsent(material, k -> new ArrayList<>());
            bufferDataSetDataList.add(dataSet);
        }
        // make batched buffer data.
        dataSetsMap.forEach((material, bufferDataSetDataList) -> {
            List<List<GaiaBufferDataSet>> splitedDataSets = divisionByMaxIndices(bufferDataSetDataList);
            splitedDataSets.forEach((splitedDataSet) -> {
                GaiaBufferDataSet batchedBufferData = batchVertices(splitedDataSet);
                batchedBufferData.setMaterial(material);
                filterdBufferDataList.add(batchedBufferData);
            });
        });
        return filterdBufferDataList;
    }

    private void checkIsRepeatMaterial(List<GaiaBufferDataSet> dataSets) {
        for (GaiaBufferDataSet dataSet : dataSets) {
            GaiaMaterial material = dataSet.material;
            if (material != null) {
                Map<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
                List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
                if (textures == null || textures.isEmpty()) {
                    Map<AttributeType, GaiaBuffer> buffers = dataSet.getBuffers();
                    buffers.remove(AttributeType.TEXCOORD);
                }
                material.setRepeat(checkRepeat(material, dataSet));
            }
        }
    }

    // 스트레치 텍스쳐들 배칭
    private List<GaiaBufferDataSet> batchClampMaterial(List<List<GaiaBufferDataSet>> splitedDataSets) {
        return splitedDataSets.stream().map((splitedIndicesLimit) -> {
            GaiaBufferDataSet batchedBufferData = batchVertices(splitedIndicesLimit);
            batchedBufferData.setMaterialId(0);
            return batchedBufferData;
        }).collect(Collectors.toList());
    }

    // 각 Material의 Texture들을 하나의 이미지로 변경
    private void atlasTextures(LevelOfDetail lod, String codeName, List<GaiaBufferDataSet> dataSets, List<GaiaMaterial> materials) {
        GaiaTextureCoordinator textureCoordinator = new GaiaTextureCoordinator(codeName, materials, dataSets);
        textureCoordinator.batchTextures(lod, this.command);
    }

    private boolean checkRepeat(GaiaMaterial material, GaiaBufferDataSet dataSet) {
        if (material.isRepeat()) {
            return true;
        }
        Map<AttributeType, GaiaBuffer> buffers = dataSet.getBuffers();
        GaiaBuffer texCoordBuffer = buffers.get(AttributeType.TEXCOORD);
        if (texCoordBuffer == null) {
            return false;
        }
        GaiaRectangle boundingRectangle = dataSet.getTexcoordBoundingRectangle();
        if (boundingRectangle != null) {
            Vector2d range = boundingRectangle.getRange();
            return range.x > 1.1f || range.y > 1.1f;
        }
        return false;
    }

    private void createColorBuffer(GaiaBufferDataSet bufferDataSet) {
        GaiaMaterial material = bufferDataSet.material;
        Vector4d diffuseColor = material.getDiffuseColor();

        Map<AttributeType, GaiaBuffer> bufferMap = bufferDataSet.getBuffers();
        GaiaBuffer positionBuffer = bufferMap.get(AttributeType.POSITION);
        int elementsCount = positionBuffer.getElementsCount();
        int length = elementsCount * 4;
        byte[] colorList = new byte[length];
        for (int i = 0; i < length; i+=4) {
            colorList[i] = (byte) (diffuseColor.x * 255);
            colorList[i + 1] = (byte) (diffuseColor.y * 255);
            colorList[i + 2] = (byte) (diffuseColor.z * 255);
            colorList[i + 3] = (byte) (diffuseColor.w * 255);
        }

        GaiaBuffer colorBuffer = new GaiaBuffer();
        colorBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
        colorBuffer.setGlType(GL20.GL_UNSIGNED_BYTE);
        colorBuffer.setElementsCount(elementsCount);
        colorBuffer.setGlDimension((byte) 4);
        colorBuffer.setBytes(colorList);
        bufferDataSet.getBuffers().put(AttributeType.COLOR, colorBuffer);
    }

    private GaiaBufferDataSet batchVertices(List<GaiaBufferDataSet> bufferDataSets) {
        GaiaBufferDataSet dataSet = new GaiaBufferDataSet();
        int totalIndicesCount = 0;
        int totalPositionCount = 0;
        int totalNormalCount = 0;
        int totalColorCount = 0;
        int totalTexCoordCount = 0;
        int totalBatchIdCount = 0;

        int indicesIndex = 0;
        int positionIndex = 0;
        int normalIndex = 0;
        int colorIndex = 0;
        int texCoordIndex = 0;
        int batchIdIndex = 0;

        for (GaiaBufferDataSet bufferDataSet : bufferDataSets) {
            Map<AttributeType, GaiaBuffer> buffers = bufferDataSet.getBuffers();
            GaiaBuffer indicesBuffer = buffers.get(AttributeType.INDICE);
            GaiaBuffer positionBuffer = buffers.get(AttributeType.POSITION);
            GaiaBuffer normalBuffer = buffers.get(AttributeType.NORMAL);
            GaiaBuffer colorBuffer = buffers.get(AttributeType.COLOR);
            GaiaBuffer texCoordBuffer = buffers.get(AttributeType.TEXCOORD);
            GaiaBuffer batchIdBuffer = buffers.get(AttributeType.BATCHID);

            if (indicesBuffer != null) {
                totalIndicesCount += indicesBuffer.getInts().length;
            }
            if (positionBuffer != null) {
                totalPositionCount += positionBuffer.getFloats().length;
            }
            if (normalBuffer != null) {
                totalNormalCount += normalBuffer.getFloats().length;
            }
            if (colorBuffer != null) {
                totalColorCount += colorBuffer.getBytes().length;
            }
            if (texCoordBuffer != null) {
                totalTexCoordCount += texCoordBuffer.getFloats().length;
            }
            if (batchIdBuffer != null) {
                totalBatchIdCount += batchIdBuffer.getFloats().length;
            }
        }

        int[] indices = new int[totalIndicesCount];
        float[] positions = new float[totalPositionCount];
        float[] normals = new float[totalNormalCount];
        float[] texCoords = new float[totalTexCoordCount];
        float[] batchIds = new float[totalBatchIdCount];
        byte[] colors = new byte[totalColorCount];

        GaiaRectangle batchedBoundingRectangle = null;
        int totalIndicesMax = 0;
        for (GaiaBufferDataSet bufferDataSet : bufferDataSets) {
            Map<AttributeType, GaiaBuffer> buffers = bufferDataSet.getBuffers();

            GaiaBuffer indicesBuffer = buffers.get(AttributeType.INDICE);
            GaiaBuffer positionBuffer = buffers.get(AttributeType.POSITION);
            GaiaBuffer normalBuffer = buffers.get(AttributeType.NORMAL);
            GaiaBuffer colorBuffer = buffers.get(AttributeType.COLOR);
            GaiaBuffer texCoordBuffer = buffers.get(AttributeType.TEXCOORD);
            GaiaBuffer batchIdBuffer = buffers.get(AttributeType.BATCHID);

            if (indicesBuffer != null) {
                int indicesMax = 0;
                for (int indice : indicesBuffer.getInts()) {
                    int intIndice = indice < 0 ? indice + 65536 : indice;
                    indicesMax = Math.max(indicesMax, intIndice);
                    //short value = (short) (totalIndicesMax + intIndice);
                    int value = (totalIndicesMax + intIndice);
                    indices[indicesIndex++] = value;
                }
                totalIndicesMax = totalIndicesMax + (indicesMax + 1);
                totalIndicesCount += indicesBuffer.getInts().length;
            } else {
                log.error("indicesBuffer is null");
            }

            if (positionBuffer != null) {
                for (float position : positionBuffer.getFloats()) {
                    positions[positionIndex++] = position;
                }
            } else {
                log.error("positionBuffer is null");
            }

            if (normalBuffer != null) {
                for (float normal : normalBuffer.getFloats()) {
                    normals[normalIndex++] = normal;
                }
            }

            if (colorBuffer != null) {
                for (byte color : colorBuffer.getBytes()) {
                    colors[colorIndex++] = color;
                }
            }

            if (texCoordBuffer != null) {
                for (float texCoord : texCoordBuffer.getFloats()) {
                    texCoords[texCoordIndex++] = texCoord;
                }
            }

            if (batchIdBuffer != null) {
                for (float batchId : batchIdBuffer.getFloats()) {
                    batchIds[batchIdIndex++] = batchId;
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

        dataSet.setTexcoordBoundingRectangle(batchedBoundingRectangle);
        if (totalPositionCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_FLOAT);
            buffer.setElementsCount(totalPositionCount / 3);
            buffer.setGlDimension((byte) 3);
            buffer.setFloats(positions);
            dataSet.getBuffers().put(AttributeType.POSITION, buffer);
        }
        if (totalNormalCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_FLOAT);
            buffer.setElementsCount(totalNormalCount / 3);
            buffer.setGlDimension((byte) 3);
            buffer.setFloats(normals);
            dataSet.getBuffers().put(AttributeType.NORMAL, buffer);
        }
        if (totalColorCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_UNSIGNED_BYTE);
            buffer.setElementsCount(totalColorCount / 4);
            buffer.setGlDimension((byte) 4);
            buffer.setBytes(colors);
            dataSet.getBuffers().put(AttributeType.COLOR, buffer);
        }
        if (totalTexCoordCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_FLOAT);
            buffer.setElementsCount(totalTexCoordCount / 2);
            buffer.setGlDimension((byte) 2);
            buffer.setFloats(texCoords);
            dataSet.getBuffers().put(AttributeType.TEXCOORD, buffer);
        }
        if (totalBatchIdCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_FLOAT);
            buffer.setElementsCount(totalBatchIdCount);
            buffer.setGlDimension((byte) 1);
            buffer.setFloats(batchIds);
            dataSet.getBuffers().put(AttributeType.BATCHID, buffer);
        }
        if (totalIndicesCount > 0) {
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.setGlTarget(GL20.GL_ELEMENT_ARRAY_BUFFER);
            buffer.setGlType(GL20.GL_UNSIGNED_INT);
            buffer.setElementsCount(totalIndicesCount);
            buffer.setGlDimension((byte) 1);
            buffer.setInts(indices);
            dataSet.getBuffers().put(AttributeType.INDICE, buffer);
        }
        return dataSet;
    }
}