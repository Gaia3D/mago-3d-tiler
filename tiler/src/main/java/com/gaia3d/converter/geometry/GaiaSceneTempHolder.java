package com.gaia3d.converter.geometry;

import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.structure.SceneStructure;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.List;
import java.util.UUID;

@Slf4j
@Getter
@Setter
@Builder
public class GaiaSceneTempHolder {
    private UUID uuid;
    private int index;
    private long size;

    private List<GaiaScene> tempScene = null;
    private boolean isMinimized = false;
    private File tempFile;

    public void minimize(File minimizedFile) {
        if (!isMinimized) {
            try(ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(minimizedFile)))) {

                List<GaiaSceneTemp> gaiaSceneTemps = tempScene.stream().map(GaiaSceneTemp::from).toList();
                //GaiaSceneTemp gaiaSceneTemp = GaiaSceneTemp.from(tempScene);
                oos.writeObject(gaiaSceneTemps);
                tempScene = null;
            } catch (IOException e) {
                log.error("Failed to minimize GaiaScene", e);
            }
            isMinimized = true;
            this.tempFile = minimizedFile;
        }
    }

    public void maximize() {
        if (isMinimized) {
            try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(tempFile)))) {
                List<GaiaSceneTemp> gaiaSceneTemps = (List<GaiaSceneTemp>) ois.readObject();
                List<GaiaScene> gaiaScenes = gaiaSceneTemps.stream().map(GaiaSceneTemp::to).toList();
                tempScene = gaiaScenes;
            } catch (IOException | ClassNotFoundException e) {
                log.error("Failed to maximize GaiaScene", e);
            }
            isMinimized = false;
            tempFile = null;
        }
    }
}
