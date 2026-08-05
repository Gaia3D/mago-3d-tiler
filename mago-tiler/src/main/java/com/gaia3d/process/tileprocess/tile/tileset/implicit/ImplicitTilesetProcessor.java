package com.gaia3d.process.tileprocess.tile.tileset.implicit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.exception.TileProcessingException;
import com.gaia3d.process.tileprocess.TilesetBuildResult;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import com.gaia3d.process.tileprocess.tile.tileset.node.BoundingVolume;
import com.gaia3d.process.tileprocess.tile.tileset.node.Content;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImplicitTilesetProcessor {

    public TilesetBuildResult prepareQuadtree(Tileset tileset, int subtreeLevels, String contentExtension) {
        Node root = tileset.getRoot();
        if (root == null || root.getChildren() == null || root.getChildren().isEmpty()) {
            throw new TileProcessingException("Implicit tiling requires an explicit root with children.");
        }

        root.refineDepth();
        root.refineParentNode();

        List<ContentInfo> contentInfos = new ArrayList<>();
        List<ImplicitSubtreeArtifact> artifacts = new ArrayList<>();
        for (Node implicitRoot : root.getChildren()) {
            artifacts.addAll(prepareRoot(implicitRoot, subtreeLevels, contentExtension, contentInfos));
        }
        return new TilesetBuildResult(tileset, contentInfos, artifacts);
    }

    public void writeSubtrees(File outputPath, ObjectMapper objectMapper, List<ImplicitSubtreeArtifact> artifacts) {
        for (ImplicitSubtreeArtifact artifact : artifacts) {
            try {
                File subtreeFile = new File(outputPath, artifact.subtreeUri());
                File bufferFile = new File(outputPath, artifact.bufferUri());
                Files.createDirectories(subtreeFile.toPath().getParent());
                Files.createDirectories(bufferFile.toPath().getParent());
                objectMapper.writeValue(subtreeFile, artifact.subtree());
                Files.write(bufferFile.toPath(), artifact.availabilityBuffer());
            } catch (IOException e) {
                throw new TileProcessingException("Failed to write implicit subtree: " + artifact.subtreeUri(), e);
            }
        }
    }

    private List<ImplicitSubtreeArtifact> prepareRoot(Node root, int subtreeLevels, String contentExtension, List<ContentInfo> contentInfos) {
        String rootCode = normalizeRootCode(root.getNodeCode());
        ImplicitSubtreeBuilder subtreeBuilder = new ImplicitSubtreeBuilder(rootCode, subtreeLevels, SubdivisionScheme.QUADTREE);
        int maxLevel = collect(root, rootCode, ImplicitTileCoordinate.root(), subtreeBuilder, contentInfos, new HashSet<>());
        fitRootBoundingVolumeToQuadtree(root);
        adjustRootGeometricError(root);

        Content contentTemplate = new Content();
        contentTemplate.setUri("data/" + rootCode + "/{level}/{x}/{y}." + contentExtension);
        root.setContent(contentTemplate);
        root.setChildren(null);

        ImplicitTiling implicitTiling = new ImplicitTiling();
        implicitTiling.setSubdivisionScheme(SubdivisionScheme.QUADTREE);
        implicitTiling.setAvailableLevels(maxLevel + 1);
        implicitTiling.setSubtreeLevels(subtreeLevels);
        implicitTiling.setSubtrees(new TemplateUri("subtrees/" + rootCode + "/{level}/{x}/{y}.json"));
        root.setImplicitTiling(implicitTiling);

        return subtreeBuilder.build();
    }

    private int collect(Node node, String rootCode, ImplicitTileCoordinate coordinate, ImplicitSubtreeBuilder subtreeBuilder, List<ContentInfo> contentInfos, Set<String> contentPaths) {
        int maxLevel = coordinate.level();
        Content content = node.getContent();
        boolean hasContent = content != null && content.getContentInfo() != null;
        if (hasContent) {
            ContentInfo contentInfo = content.getContentInfo();
            String contentPath = quadtreeContentPath(rootCode, coordinate);
            if (!contentPaths.add(contentPath)) {
                throw new TileProcessingException("Duplicate implicit content path: " + contentPath);
            }
            contentInfo.setContentPath(contentPath);
            contentInfos.add(contentInfo);
            subtreeBuilder.addContent(coordinate);
        }

        if (node.getChildren() == null) {
            return maxLevel;
        }

        for (Node child : node.getChildren()) {
            int childIndex = getQuadtreeChildIndex(node, child);
            ImplicitTileCoordinate childCoordinate = coordinate.child(childIndex, SubdivisionScheme.QUADTREE);
            maxLevel = Math.max(maxLevel, collect(child, rootCode, childCoordinate, subtreeBuilder, contentInfos, contentPaths));
        }
        return maxLevel;
    }

    private int getQuadtreeChildIndex(Node parent, Node child) {
        String parentCode = parent.getNodeCode();
        String childCode = child.getNodeCode();
        if (childCode == null || parentCode == null || !childCode.startsWith(parentCode) || childCode.length() <= parentCode.length()) {
            throw new TileProcessingException("Cannot derive implicit child index from node: " + childCode);
        }
        String suffix = childCode.substring(parentCode.length());
        for (int i = 0; i < suffix.length(); i++) {
            char ch = suffix.charAt(i);
            if (ch >= '0' && ch <= '3') {
                return ch - '0';
            }
        }
        throw new TileProcessingException("Implicit quadtree only supports child index 0-3. Node: " + childCode);
    }

    private String quadtreeContentPath(String rootCode, ImplicitTileCoordinate coordinate) {
        return rootCode + "/" + coordinate.level() + "/" + coordinate.x() + "/" + coordinate.y();
    }

    private String normalizeRootCode(String rootCode) {
        if (rootCode == null) {
            throw new TileProcessingException("Implicit root node code is null.");
        }
        String normalized = rootCode.replace('\\', '/');
        int separatorIndex = normalized.indexOf('/');
        if (separatorIndex >= 0) {
            return normalized.substring(0, separatorIndex);
        }
        return normalized;
    }

    private void adjustRootGeometricError(Node root) {
        List<Node> children = root.getChildren();
        if (children == null || children.isEmpty()) {
            return;
        }
        double originalRootGeometricError = root.getGeometricError();
        double maxChildGeometricError = children.stream()
                .mapToDouble(Node::getGeometricError)
                .max()
                .orElse(0.0d);
        if (maxChildGeometricError > 0.0d) {
            root.setGeometricError(Math.max(originalRootGeometricError, maxChildGeometricError * 2.0d));
        }
    }

    private void fitRootBoundingVolumeToQuadtree(Node root) {
        BoundingVolume boundingVolume = root.getBoundingVolume();
        if (boundingVolume != null) {
            root.setBoundingVolume(boundingVolume.createSqureBoundingVolume());
        }
    }
}
