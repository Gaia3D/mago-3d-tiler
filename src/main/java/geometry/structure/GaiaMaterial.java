package geometry.structure;

import geometry.types.TextureType;
import io.LittleEndianDataInputStream;
import io.LittleEndianDataOutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joml.Vector4d;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaMaterial {
    private Vector4d diffuseColor = new Vector4d(1.0, 1.0, 1.0, 1.0);
    private Vector4d ambientColor = new Vector4d(1.0, 1.0, 1.0, 1.0);
    private Vector4d specularColor = new Vector4d(1.0, 1.0, 1.0, 1.0);
    private float shininess = 0.0f;

    private int id = -1;
    private String name = "no_name";
    private LinkedHashMap<TextureType, List<GaiaTexture>> textures = new LinkedHashMap<>();


    // experimental :: is repeat texture
    private boolean isRepeat = false;

    public void write(LittleEndianDataOutputStream stream) throws IOException {
        stream.writeInt(id);
        stream.writeText(name);
        stream.writeVector4(diffuseColor);
        stream.writeVector4(ambientColor);
        stream.writeVector4(specularColor);
        stream.writeFloat(shininess);
        stream.writeInt(textures.size());
        for (Map.Entry<TextureType, List<GaiaTexture>> entry : textures.entrySet()) {
            TextureType gaiaMaterialType = entry.getKey();
            List<GaiaTexture> gaiaTextures = entry.getValue();
            stream.writeByte(gaiaMaterialType.getValue());
            stream.writeInt(gaiaTextures.size());
            for (GaiaTexture gaiaTexture : gaiaTextures) {
                boolean isExist = gaiaTexture != null;
                stream.writeBoolean(isExist);
                if (isExist) {
                    gaiaTexture.write(stream);
                }
            }
        }
    }

    public void read(LittleEndianDataInputStream stream, Path parentPath) throws IOException {
        this.setId(stream.readInt());
        this.setName(stream.readText());
        this.setDiffuseColor(stream.readVector4());
        this.setAmbientColor(stream.readVector4());
        this.setSpecularColor(stream.readVector4());
        this.setShininess(stream.readFloat());
        int texturesSize = stream.readInt();
        for (int i = 0; i < texturesSize; i++) {
            List<GaiaTexture> gaiaTextures = new ArrayList<>();
            byte textureType = stream.readByte();
            int gaiaTexturesSize = stream.readInt();
            TextureType gaiaMaterialType = TextureType.fromValue(textureType);

            //int gaiaTexturesSize = BinaryUtils.readInt(stream);

            for (int j = 0; j < gaiaTexturesSize; j++) {
                boolean isExist = stream.readBoolean();
                if (isExist) {
                    GaiaTexture gaiaTexture = new GaiaTexture();
                    gaiaTexture.setParentPath(parentPath);
                    gaiaTexture.read(stream);
                    gaiaTextures.add(gaiaTexture);
                }
            }
            this.textures.put(gaiaMaterialType, gaiaTextures);
        }
    }

    public void deleteTextures()
    {
        for (Map.Entry<TextureType, List<GaiaTexture>> entry : textures.entrySet()) {
            List<GaiaTexture> gaiaTextures = entry.getValue();
            for (GaiaTexture gaiaTexture : gaiaTextures) {
                if (gaiaTexture != null) {
                    gaiaTexture.deleteObjects();
                }
            }
        }
    }

    public static boolean areEqualMaterials(GaiaMaterial materialA, GaiaMaterial materialB) throws IOException {
        // This function determines if two materials are equal.
        if (materialA == null && materialB == null) {
            return true;
        } else if (materialA == null || materialB == null) {
            return false;
        }

        if(materialA == materialB){
            return true;
        }

        LinkedHashMap<TextureType, List<GaiaTexture>> textureMapA = materialA.getTextures();

        Set<TextureType> keys = textureMapA.keySet();
        for(TextureType key : keys){
            List<GaiaTexture> listTexturesA = textureMapA.get(key);
            List<GaiaTexture> listTexturesB = materialB.getTextures().get(key);
            if(listTexturesA == null && listTexturesB == null){
                continue;
            }else if(listTexturesA == null || listTexturesB == null){
                return false;
            }
            if(listTexturesA.size() != listTexturesB.size()){
                return false;
            }
            for(int i = 0; i < listTexturesA.size(); i++){
                GaiaTexture textureA = listTexturesA.get(i);
                GaiaTexture textureB = listTexturesB.get(i);
                if(!GaiaTexture.areEqualTextures(textureA, textureB)){
                    return false;
                }
            }
        }
        return true;
    }

    public boolean compareTo(GaiaMaterial compare) {
        GaiaMaterial target = this;
        if (target.getId() == compare.getId()) {
            return true;
        }
        LinkedHashMap<TextureType, List<GaiaTexture>> targetTextureMap = target.getTextures();
        List<GaiaTexture> targetTextures = targetTextureMap.get(TextureType.DIFFUSE);
        LinkedHashMap<TextureType, List<GaiaTexture>> compareTextureMap = compare.getTextures();
        List<GaiaTexture> compareTextures = compareTextureMap.get(TextureType.DIFFUSE);
        GaiaTexture targetTexture = null;
        GaiaTexture compareTexture = null;
        if (targetTextures != null && targetTextures.size() > 0) {
            targetTexture = targetTextures.get(0);
        }
        if (compareTextures != null && compareTextures.size() > 0) {
            compareTexture = compareTextures.get(0);
        }

        if (targetTexture == null && compareTexture == null) {
            Vector4d targetDiffColor = target.getDiffuseColor();
            Vector4d compareDiffColor = compare.getDiffuseColor();
            return targetDiffColor.equals(compareDiffColor);
        } else if (targetTexture != null && compareTexture != null) {
            File diffuseTextureFile = new File(targetTexture.getParentPath() + File.separator + targetTexture.getPath());
            File searchDiffuseTextureFile = new File(compareTexture.getParentPath() + File.separator + compareTexture.getPath());
            if (diffuseTextureFile.equals(searchDiffuseTextureFile)) {
                return true;
            } else if (diffuseTextureFile.length() == searchDiffuseTextureFile.length()) {
                try {
                    FileUtils.contentEquals(diffuseTextureFile, searchDiffuseTextureFile);
                } catch (IOException e) {
                    log.error(e.getMessage());
                    return false;
                }
                return true;
            }
        }
        return false;
    }
}
