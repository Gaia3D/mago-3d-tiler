package com.gaia3d.basic.temp;

import com.gaia3d.basic.model.GaiaScene;
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
public class GaiaSceneTempGroup {
    private UUID uuid;
    private int index;
    private long size;

    @Builder.Default
    private List<GaiaScene> tempScene = null;
    @Builder.Default
    private boolean isMinimized = false;
    private File tempFile;

    public void minimize(File minimizedFile) {
        if (!isMinimized) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(minimizedFile)))) {
                List<GaiaSceneTemp> gaiaSceneTemps = tempScene.stream().map(GaiaSceneTemp::from).toList();
                oos.writeObject(gaiaSceneTemps);
                tempScene = null;
            } catch (IOException e) {
                log.error("[ERROR] Failed to minimize GaiaScene", e);
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
                log.error("[ERROR] Failed to maximize GaiaScene", e);
            }
            isMinimized = false;
            tempFile = null;
        }
    }
}
