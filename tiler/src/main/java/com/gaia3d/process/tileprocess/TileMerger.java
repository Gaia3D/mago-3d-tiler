package com.gaia3d.process.tileprocess;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import com.gaia3d.process.tileprocess.tile.tileset.asset.Asset;
import com.gaia3d.process.tileprocess.tile.tileset.node.BoundingVolume;
import com.gaia3d.process.tileprocess.tile.tileset.node.Content;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class TileMerger {

    private static GlobalOptions globalOptions = GlobalOptions.getInstance();

    public void merge() {
        log.info("Starting tileset merging.");

        String tilesetName = "tileset.json";
        File inputPath = new File(globalOptions.getInputPath());
        File outputPath = new File(globalOptions.getOutputPath());
        File tilesetPath = new File(outputPath, tilesetName);

        // find all tileset.json files
        List<File> tilesetJsons = findAllTilesetJsons(inputPath);

        log.info("Found {} tileset.json files.", tilesetJsons.size());

        // parse all tileset.json files
        Map<File, Tileset> tilesets = parseTilesetJsons(tilesetJsons);

        // calculate bounding box and geospatial information and merge tilesets
        Tileset tileset = mergeTilesets(tilesets);

        // write merged tileset.json
        writeTilesetJson(tilesetPath, tileset);
        log.info("End tileset combining.");
    }

    private void writeTilesetJson(File tilesetPath, Tileset tileset) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

        try {
            objectMapper.writeValue(tilesetPath, tileset);
            log.info("Tileset.json is written to {}", tilesetPath);
        } catch (IOException e) {
            log.error("Failed to write tileset.json.", e);
            throw new RuntimeException(e);
        }
    }

    private Map<File, Tileset> parseTilesetJsons(List<File> tilesetJsons) {
        //List<Tileset> tilesets = new ArrayList<>();
        Map<File, Tileset> tilesetMap = new HashMap<>();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

        for (File tilesetJson : tilesetJsons) {
            try {
                Tileset tileset = objectMapper.readValue(tilesetJson, Tileset.class);
                tilesetMap.put(tilesetJson, tileset);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return tilesetMap;
    }

    private Tileset mergeTilesets(Map<File, Tileset> tilesetMap) {
        File inputPath = new File(globalOptions.getInputPath());

        double geometricError = 0.0;
        Tileset mergedTileset = new Tileset();

        // region calculate bounding box
        double[] globalBoundingBox = new double[6];
        globalBoundingBox[0] = Double.MAX_VALUE;
        globalBoundingBox[1] = Double.MAX_VALUE;
        globalBoundingBox[2] = -Double.MAX_VALUE;
        globalBoundingBox[3] = -Double.MAX_VALUE;
        globalBoundingBox[4] = Double.MAX_VALUE;
        globalBoundingBox[5] = -Double.MAX_VALUE;

        List<Node> children = new ArrayList<>();
        Node root = new Node();
        root.setRefine(Node.RefineType.ADD);

        List<File> tilesetFiles = new ArrayList<>(tilesetMap.keySet());
        for (File tilesetFile : tilesetFiles) {
            Tileset tileset = tilesetMap.get(tilesetFile);
            Node tilesetRoot = tileset.getRoot();
            double tilesetGeometricError = tileset.getGeometricError();
            geometricError = Math.max(geometricError, tilesetGeometricError);

            Node newChildNode = new Node();
            newChildNode.setRefine(Node.RefineType.REPLACE);
            newChildNode.setGeometricError(tilesetGeometricError);
            if (tilesetRoot.getTransform() != null)
                newChildNode.setTransform(tilesetRoot.getTransform());
            if (tilesetRoot.getBoundingVolume() != null)
                newChildNode.setBoundingVolume(tilesetRoot.getBoundingVolume());

            BoundingVolume boundingVolume = tilesetRoot.getBoundingVolume();
            //BoundingVolume.BoundingVolumeType boundingVolumeType = boundingVolume.getType();

            if (boundingVolume.getBox() != null) {
                // calculate bounding box
            } else if (boundingVolume.getSphere() != null) {
                // calculate bounding sphere
            } else if (boundingVolume.getRegion() != null) {
                // calculate bounding region
                double[] boundingBox = boundingVolume.getRegion();

                globalBoundingBox[0] = Math.min(globalBoundingBox[0], boundingBox[0]); // minX
                globalBoundingBox[1] = Math.min(globalBoundingBox[1], boundingBox[1]); // minY
                globalBoundingBox[2] = Math.max(globalBoundingBox[2], boundingBox[2]); // maxX
                globalBoundingBox[3] = Math.max(globalBoundingBox[3], boundingBox[3]); // maxY
                globalBoundingBox[4] = Math.min(globalBoundingBox[4], boundingBox[4]); // minZ
                globalBoundingBox[5] = Math.max(globalBoundingBox[5], boundingBox[5]); // maxZ
            }

            String uri = getRelativePath(inputPath, tilesetFile);

            Content content = new Content();
            content.setUri(uri);
            newChildNode.setContent(content);

            children.add(newChildNode);
            root.setChildren(children);
        }

        geometricError = Math.min(geometricError, globalOptions.getMaxGeometricError());

        Asset asset = new Asset();
        asset.setVersion("1.1");
        mergedTileset.setAsset(asset);
        mergedTileset.setGeometricError(geometricError);
        mergedTileset.setRoot(root);

        BoundingVolume globalBoundingVolume = new BoundingVolume(BoundingVolume.BoundingVolumeType.REGION);
        globalBoundingVolume.setRegion(globalBoundingBox);
        root.setGeometricError(geometricError);
        root.setBoundingVolume(globalBoundingVolume);


        // merge tilesets

        return mergedTileset;
    }


    private List<File> findAllTilesetJsons(File inputPath) {
        List<File> files = (List<File>) FileUtils.listFiles(inputPath, new String[]{"json"}, true);
        files.removeIf(file -> !file.getName().equals("tileset.json"));
        return files;
    }

    /* getRelativePath */
    private String getRelativePath(File parent, File child) {
        return parent.toURI().relativize(child.toURI()).getPath();
    }
}
