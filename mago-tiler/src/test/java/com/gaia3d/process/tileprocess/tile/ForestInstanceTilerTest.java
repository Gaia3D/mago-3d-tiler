package com.gaia3d.process.tileprocess.tile;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import org.joml.Vector3d;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("manual")
class ForestInstanceTilerTest {

    @Test
    void refineAddCountsAreStableAcrossRuns() {
        Set<String> signatures = new LinkedHashSet<>();

        for (int run = 0; run < 5; run++) {
            configureOptions();

            ForestInstanceTiler tiler = new ForestInstanceTiler();
            Tileset tileset = tiler.run(createTileInfos());

            List<ContentInfo> contentInfos = tileset.findAllContentInfo();
            assertTrue(contentInfos.size() > 1);

            for (ContentInfo contentInfo : contentInfos) {
                long distinctCount = contentInfo.getTileInfos().stream()
                        .map(TileInfo::getName)
                        .distinct()
                        .count();
                assertEquals(distinctCount, contentInfo.getTileInfos().size(), contentInfo.getNodeCode());
            }

            signatures.add(contentInfos.stream()
                    .map(contentInfo -> contentInfo.getNodeCode() + ":" + contentInfo.getTileInfos().size())
                    .sorted()
                    .collect(Collectors.joining(",")));
        }

        assertEquals(1, signatures.size(), String.join(System.lineSeparator(), signatures));
    }

    @Test
    void refineAddKeepsAllInstancesWhenMaxLodIsFive() {
        configureOptions();
        GlobalOptions.getInstance().setMaxLod(5);
        GlobalOptions.getInstance().setMaxNodeDepth(12);

        List<TileInfo> tileInfos = createTileInfos();
        ForestInstanceTiler tiler = new ForestInstanceTiler();
        Tileset tileset = tiler.run(tileInfos);

        List<ContentInfo> contentInfos = tileset.findAllContentInfo();
        Set<String> allInstanceNames = contentInfos.stream()
                .flatMap(contentInfo -> contentInfo.getTileInfos().stream())
                .map(TileInfo::getName)
                .collect(Collectors.toSet());

        assertEquals(tileInfos.size(), allInstanceNames.size());
    }

    @Test
    void finalLodZeroContainsEveryInputInstance() {
        configureOptions();
        GlobalOptions.getInstance().setMaxLod(5);

        List<TileInfo> tileInfos = createTileInfosWithAlternatingAltitude();
        ForestInstanceTiler tiler = new ForestInstanceTiler();
        Tileset tileset = tiler.run(tileInfos);

        Set<String> inputInstanceNames = tileInfos.stream()
                .map(TileInfo::getName)
                .collect(Collectors.toSet());
        Set<String> finalLodInstanceNames = tileset.findAllContentInfo().stream()
                .filter(contentInfo -> contentInfo.getLod().getLevel() == 0)
                .flatMap(contentInfo -> contentInfo.getTileInfos().stream())
                .map(TileInfo::getName)
                .collect(Collectors.toSet());

        assertEquals(inputInstanceNames, finalLodInstanceNames);
    }

    private void configureOptions() {
        GlobalOptions.recreateInstance();
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setTilesVersion("1.0");
        globalOptions.setRefineAdd(true);
        globalOptions.setMinLod(0);
        globalOptions.setMaxLod(3);
        globalOptions.setMinGeometricError(1);
        globalOptions.setMaxGeometricError(1024);
        globalOptions.setMaxInstance(10_000);
        globalOptions.setMaxNodeDepth(8);
        globalOptions.setClassicTransformMatrix(false);
    }

    private List<TileInfo> createTileInfos() {
        List<TileInfo> tileInfos = new ArrayList<>();
        int serial = 0;
        for (int y = 0; y < 24; y++) {
            for (int x = 0; x < 24; x++) {
                double longitude = 127.0 + (x * 0.00001);
                double latitude = 37.0 + (y * 0.00001);
                String name = "tree-" + serial;
                TileTransformInfo transformInfo = TileTransformInfo.builder()
                        .name(name)
                        .position(new Vector3d(longitude, latitude, 10.0))
                        .scaleX(1.0)
                        .scaleY(1.0)
                        .scaleZ(1.0)
                        .build();

                tileInfos.add(TileInfo.builder()
                        .serial(serial)
                        .name(name)
                        .tileTransformInfo(transformInfo)
                        .boundingBox(new GaiaBoundingBox(-0.5, -0.5, 0.0, 0.5, 0.5, 1.0))
                        .build());
                serial++;
            }
        }
        return tileInfos;
    }

    private List<TileInfo> createTileInfosWithAlternatingAltitude() {
        List<TileInfo> tileInfos = createTileInfos();
        for (TileInfo tileInfo : tileInfos) {
            double altitude = tileInfo.getSerial() % 2 == 0 ? 0.0 : 300.0;
            tileInfo.getTileTransformInfo().setPosition(new Vector3d(
                    tileInfo.getTileTransformInfo().getPosition().x,
                    tileInfo.getTileTransformInfo().getPosition().y,
                    altitude));
        }
        return tileInfos;
    }
}
