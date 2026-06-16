package com.gaia3d.process.tileprocess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.tileprocess.tile.tileset.TilesetV2;
import com.gaia3d.process.tileprocess.tile.tileset.node.Content;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Hierarchical TileDivider
 * <p>
 * 목적:
 * 큰 3D Tileset을 여러 external tileset 계층으로 분리하여
 * 이후 Implicit Tiling을 적용할 수 있는 구조로 만드는 전처리기
 */
@Slf4j
public class TileDivider {

    private static GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();
    private long subtreeIdCounter = 0;

    /**
     * Entry point
     */
    public void divide() {

        try {

            File inputPath = new File(globalOptions.getInputPath());
            File tilesetFile = inputPath.isDirectory() ? new File(inputPath, "tileset.json") : inputPath;

            File outputDir = new File(globalOptions.getOutputPath());
            outputDir.mkdirs();

            TilesetV2 original = mapper.readValue(tilesetFile, TilesetV2.class);
            original.getRoot().refineDepth();
            original.getRoot().refineParentNode();

            int maxDepth = original.getRoot().getMaxDepth();
            int chunkDepth = Math.max(4, maxDepth / 3); // 예시로 전체 깊이의 절반을 chunkDepth로 설정 (조정 가능)

            int totalTiles = original.findAllContentInfo().size();
            log.info("MaxDepth={}, chunkDepth={}", maxDepth, chunkDepth);
            log.info("Total tiles in original tileset: {}", totalTiles);

            // root 생성
            TilesetV2 rootTileset = buildTileset(original.getRoot(), chunkDepth, outputDir);

            // 루트 tileset 저장
            mapper.writeValue(new File(outputDir, "tileset.json"), rootTileset);

            log.info("TileDivider finished");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 현재 노드를 root로 하는 tileset 생성
     */
    private TilesetV2 buildTileset(Node rootNode, int chunkDepth, File outputDir) throws Exception {
        Node newRoot = deepCopyNode(rootNode);
        newRoot.refineParentNode();
        newRoot.refineDepth();

        int baseDepth = rootNode.getDepth();

        splitChildren(newRoot, baseDepth, chunkDepth, outputDir);

        TilesetV2 tileset = new TilesetV2();
        tileset.setRoot(newRoot);
        tileset.setGeometricError(rootNode.getGeometricError());

        return tileset;
    }

    private void splitChildren(Node node, int baseDepth, int chunkDepth, File outputDir) throws Exception {

        if (node.getChildren() == null || node.getChildren().isEmpty())
            return;

        List<Node> children = new ArrayList<>(node.getChildren());

        for (Node child : children) {

            // ★ 이제 올바른 기준
            int relativeDepth = child.getDepth() - baseDepth;

            if (relativeDepth >= chunkDepth) {

                String subtreeFileName = nextSubtreeId();

                log.info("Create subtree {} at depth {}", subtreeFileName, child.getDepth());

                // subtree 생성
                TilesetV2 subTileset = buildTileset(child, chunkDepth, outputDir);
                mapper.writeValue(new File(outputDir, subtreeFileName), subTileset);

                // external tileset 연결
                child.setChildren(null);

                Content external = new Content();
                external.setUri(subtreeFileName);
                child.setContent(external);

            } else {
                // 계속 탐색 (baseDepth는 그대로 유지!)
                splitChildren(child, baseDepth, chunkDepth, outputDir);
            }
        }
    }

    /**
     * Node deep copy
     * (Jackson round-trip이 가장 안전)
     */
    private Node deepCopyNode(Node node) throws Exception {
        byte[] bytes = mapper.writeValueAsBytes(node);
        return mapper.readValue(bytes, Node.class);
    }

    private synchronized String nextSubtreeId() {
        return "subtree_" + (++subtreeIdCounter) + ".json";
    }
}
