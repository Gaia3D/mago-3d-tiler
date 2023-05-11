package geometry.exchangable;

import geometry.structure.GaiaMaterial;
import geometry.structure.GaiaTexture;
import geometry.types.TextureType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joml.Vector4d;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        for (GaiaSet gaiaSet : gaiaSets) {
            List<GaiaBufferDataSet> bufferDatas = gaiaSet.getBufferDatas();
            List<GaiaMaterial> materials = gaiaSet.getMaterials();

            int batchedMaterialId = batchedMaterials.size();
            for (GaiaBufferDataSet gaiaBufferDataSet : bufferDatas) {
                int originMaterialId = gaiaBufferDataSet.getMaterialId();
                gaiaBufferDataSet.setMaterialId(batchedMaterialId + originMaterialId);
            }
            for (GaiaMaterial gaiaMaterial : materials) {
                int originMaterialId = gaiaMaterial.getId();
                gaiaMaterial.setId(batchedMaterialId + originMaterialId);
            }
            batchedBufferDatas.addAll(bufferDatas);
            batchedMaterials.addAll(materials);
        }

        List<GaiaMaterial> filterdMaterials = batchedMaterials.stream().filter((material) -> {
            int materialId = material.getId();
            GaiaMaterial sameMaterial = getSameMaterial(material, batchedMaterials);
            if (sameMaterial != null) {
                for (GaiaBufferDataSet gaiaBufferDataSet : batchedBufferDatas) {
                    int usedMaterialId = gaiaBufferDataSet.getMaterialId();
                    if (usedMaterialId == materialId) {
                        log.info(gaiaBufferDataSet.getMaterialId() + " -> " + sameMaterial.getId());
                        gaiaBufferDataSet.setMaterialId(sameMaterial.getId());
                    }
                }
                if (materialId > sameMaterial.getId()) {
                    return false;
                }
            }
            log.info("new material: " + materialId);
            return true;
        }).collect(Collectors.toList());

        batchedBufferDatas.forEach((batchedBufferData) -> {
            for (int i = 0; i < filterdMaterials.size(); i++) {
                GaiaMaterial material = filterdMaterials.get(i);
                if (material.getId() == batchedBufferData.getMaterialId()) {
                    log.info(material.getId() + "::" + batchedBufferData.getMaterialId());
                    batchedBufferData.setMaterialId(i);
                    break;
                }
            }
        });

        for (int i = 0; i < filterdMaterials.size(); i++) {
            GaiaMaterial material = filterdMaterials.get(i);
            material.setId(i);
        }

        batched.setBufferDatas(batchedBufferDatas);
        batched.setMaterials(filterdMaterials);
        return batched;
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
                log.info("test");
                if (diffuseTexture.getPath().equals(searchDiffuseTexture.getPath())) {
                    //todo texture size check
                    return searchMaterial;
                } else if (diffuseTextureFile.length() == searchDiffuseTextureFile.length() && isEqual(diffuseTextureFile, searchDiffuseTextureFile)) {
                    log.info("same texture: " + diffuseTexture.getPath() + " -> " + searchDiffuseTexture.getPath());
                    return searchMaterial;
                }
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
