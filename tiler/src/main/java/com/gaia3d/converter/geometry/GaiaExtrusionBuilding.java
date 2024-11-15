package com.gaia3d.converter.geometry;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.io.*;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Setter
@Builder
public class GaiaExtrusionBuilding implements Serializable {
    private String id;
    private String name;
    private Classification classification;
    private double roofHeight;
    private double floorHeight;
    private GaiaBoundingBox boundingBox;
    private String originalFilePath;

    private List<Vector3d> positions;
    private Map<String, String> properties;

    /*private boolean isMinimized = false;
    private File minimizedFile;

    public void minimize(File minimizedFile) {
        if (!isMinimized) {
            try(ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(minimizedFile)))) {
                oos.writeObject(positions);
                positions = null;
            } catch (IOException e) {
                log.error("Failed to minimize GaiaExtrusionBuilding", e);
            }
            isMinimized = true;
            this.minimizedFile = minimizedFile;
        }
    }

    public void maximize() {
        if (isMinimized) {
            try(ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(minimizedFile)))) {
                positions = (List<Vector3d>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                log.error("Failed to maximize GaiaExtrusionBuilding", e);
            }

            isMinimized = false;
            minimizedFile = null;
        }
    }*/
}
