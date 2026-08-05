package com.gaia3d.process.tileprocess.tile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.exception.TileProcessingException;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.types.LevelOfDetail;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.tileprocess.Tiler;
import com.gaia3d.process.tileprocess.TilesetBuildResult;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import com.gaia3d.process.tileprocess.tile.tileset.TilesetV2;
import com.gaia3d.process.tileprocess.tile.tileset.asset.AssetV2;
import com.gaia3d.process.tileprocess.tile.tileset.implicit.*;
import com.gaia3d.process.tileprocess.tile.tileset.node.BoundingVolume;
import com.gaia3d.process.tileprocess.tile.tileset.node.Content;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import com.gaia3d.util.DecimalUtils;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AbstractImplicitModelTiler extends DefaultTiler implements Tiler {
    protected final GlobalOptions globalOptions = GlobalOptions.getInstance();
    private List<ImplicitSubtreeArtifact> implicitSubtreeArtifacts = List.of();

    @Override
    public Tileset run(List<TileInfo> tileInfos) {
        return runWithResult(tileInfos).tileset();
    }

    @Override
    public TilesetBuildResult runWithResult(List<TileInfo> tileInfos) {
        validateTileInfos(tileInfos, getClass().getSimpleName());
        if (!"1.1".equals(globalOptions.getTilesVersion())) {
            throw new TileProcessingException("Implicit tiling requires 3D Tiles 1.1.");
        }

        GaiaBoundingBox rootBoundingBox = calcCartographicBoundingBox(tileInfos);
        GaiaBoundingBox squareBoundingBox = squareXY(rootBoundingBox);
        beforeBuildImplicitTiles(tileInfos, squareBoundingBox);
        Matrix4d transformMatrix = getTransformMatrixFromCartographic(squareBoundingBox);
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }

        double rootGeometricError = DecimalUtils.cutFast(rootGeometricError(tileInfos, squareBoundingBox));
        double tilesetGeometricError = DecimalUtils.cutFast(tilesetGeometricError(tileInfos, squareBoundingBox));
        Node root = createRoot();
        root.setBoundingVolume(new BoundingVolume(squareBoundingBox, BoundingVolume.BoundingVolumeType.REGION));
        root.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        root.setGeometricError(rootGeometricError);
        root.setRefine(refineType());
        root.setChildren(null);

        Content contentTemplate = new Content();
        contentTemplate.setUri("data/R/{level}/{x}/{y}.glb");
        root.setContent(contentTemplate);

        ImplicitTiling implicitTiling = new ImplicitTiling();
        implicitTiling.setSubdivisionScheme(SubdivisionScheme.QUADTREE);
        implicitTiling.setSubtreeLevels(globalOptions.getImplicitSubtreeLevels());
        implicitTiling.setSubtrees(new TemplateUri("subtrees/R/{level}/{x}/{y}.json"));
        root.setImplicitTiling(implicitTiling);

        ImplicitSubtreeBuilder subtreeBuilder = new ImplicitSubtreeBuilder("R", globalOptions.getImplicitSubtreeLevels(), SubdivisionScheme.QUADTREE);
        List<ContentInfo> contentInfos = new ArrayList<>();
        int maxLevel = buildImplicitTiles(tileInfos, squareBoundingBox, ImplicitTileCoordinate.root(), subtreeBuilder, contentInfos);
        implicitTiling.setAvailableLevels(maxLevel + 1);
        implicitSubtreeArtifacts = subtreeBuilder.build();

        TilesetV2 tileset = new TilesetV2();
        tileset.setAsset(new AssetV2());
        tileset.setGeometricError(tilesetGeometricError);
        tileset.setRoot(root);
        return new TilesetBuildResult(tileset, contentInfos, implicitSubtreeArtifacts);
    }

    @Override
    public void writeTileset(Tileset tileset) {
        Node rootNode = tileset.getRoot();
        if (rootNode == null) {
            throw new TileProcessingException("Tileset root node is null");
        }
        if (rootNode.getBoundingVolume() == null) {
            throw new TileProcessingException("Tileset root node bounding volume is null");
        }
        if (rootNode.getImplicitTiling() == null) {
            throw new TileProcessingException("Implicit tileset root implicitTiling is null");
        }

        Path outputPath = new File(globalOptions.getOutputPath()).toPath();
        File tilesetFile = outputPath.resolve("tileset.json").toFile();
        ObjectMapper objectMapper = new ObjectMapper();
        if (!globalOptions.isDebug()) {
            objectMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
        }
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        try {
            java.nio.file.Files.createDirectories(outputPath);
        } catch (IOException e) {
            throw new TileProcessingException("Failed to create output directory: " + outputPath, e);
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tilesetFile))) {
            String result = objectMapper.writeValueAsString(tileset);
            writer.write(result);
            globalOptions.setTilesetSize(result.length());
        } catch (IOException e) {
            throw new TileProcessingException(e.getMessage(), e);
        }
        writeSubtrees(outputPath.toFile(), objectMapper);
    }

    protected abstract boolean shouldSplit(List<TileInfo> tileInfos, GaiaBoundingBox cellBoundingBox, int depth);

    protected abstract long countForLog(List<TileInfo> tileInfos);

    protected Node.RefineType refineType() {
        return Node.RefineType.ADD;
    }

    protected double rootGeometricError(List<TileInfo> tileInfos, GaiaBoundingBox rootBoundingBox) {
        return calcGeometricError(tileInfos);
    }

    protected double tilesetGeometricError(List<TileInfo> tileInfos, GaiaBoundingBox rootBoundingBox) {
        return rootGeometricError(tileInfos, rootBoundingBox);
    }

    protected List<TileInfo> contentTileInfos(List<TileInfo> tileInfos, GaiaBoundingBox cellBoundingBox, SplitResult splitResult, boolean canSplit, int depth) {
        return canSplit ? splitResult.retainedTileInfos() : tileInfos;
    }

    protected void beforeBuildImplicitTiles(List<TileInfo> tileInfos, GaiaBoundingBox rootBoundingBox) {
    }

    protected boolean hasContent(List<TileInfo> tileInfos, GaiaBoundingBox cellBoundingBox, int depth) {
        return true;
    }

    protected List<TileInfo> childTileInfos(List<TileInfo> tileInfos, List<TileInfo> contentTileInfos, SplitResult splitResult, int childIndex, boolean canSplit, int depth) {
        return splitResult.children().get(childIndex);
    }

    private int buildImplicitTiles(List<TileInfo> tileInfos, GaiaBoundingBox cellBoundingBox, ImplicitTileCoordinate coordinate,
                                   ImplicitSubtreeBuilder subtreeBuilder, List<ContentInfo> contentInfos) {
        int maxDepth = globalOptions.getMaxNodeDepth();
        SplitResult splitResult = split(tileInfos, cellBoundingBox);
        boolean canSplit = depth(coordinate) < maxDepth
                && shouldSplit(tileInfos, cellBoundingBox, depth(coordinate))
                && splitResult.hasChildren()
                && (splitResult.hasProgress(tileInfos.size()) || allowDegenerateSplit(tileInfos, cellBoundingBox, depth(coordinate)));

        List<TileInfo> contentTileInfos = contentTileInfos(tileInfos, cellBoundingBox, splitResult, canSplit, depth(coordinate));
        if (!contentTileInfos.isEmpty() && hasContent(contentTileInfos, cellBoundingBox, depth(coordinate))) {
            addContentInfo(contentTileInfos, cellBoundingBox, coordinate, contentInfos);
            subtreeBuilder.addContent(coordinate);
            log.info("[Tile][ImplicitContent][{}][LOD{}][OBJECT{}]", contentPath(coordinate), lodForContent(contentTileInfos, depth(coordinate)).getLevel(), countForLog(contentTileInfos));
        }

        if (!canSplit) {
            return coordinate.level();
        }

        int maxLevel = coordinate.level();
        for (int i = 0; i < splitResult.children().size(); i++) {
            List<TileInfo> childTileInfos = childTileInfos(tileInfos, contentTileInfos, splitResult, i, canSplit, depth(coordinate));
            if (childTileInfos.isEmpty()) {
                continue;
            }
            ImplicitTileCoordinate childCoordinate = coordinate.child(i, SubdivisionScheme.QUADTREE);
            maxLevel = Math.max(maxLevel, buildImplicitTiles(childTileInfos, childBoundingBox(cellBoundingBox, i), childCoordinate, subtreeBuilder, contentInfos));
        }
        return maxLevel;
    }

    private void addContentInfo(List<TileInfo> tileInfos, GaiaBoundingBox cellBoundingBox, ImplicitTileCoordinate coordinate, List<ContentInfo> contentInfos) {
        GaiaBoundingBox contentBoundingBox = calcCartographicBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransformMatrixFromCartographic(contentBoundingBox);
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }

        ContentInfo contentInfo = new ContentInfo();
        contentInfo.setName(contentPath(coordinate));
        contentInfo.setNodeCode(contentPath(coordinate));
        contentInfo.setContentPath(contentPath(coordinate));
        contentInfo.setLod(lodForContent(tileInfos, depth(coordinate)));
        contentInfo.setBoundingBox(contentBoundingBox.isValid() ? contentBoundingBox : cellBoundingBox);
        contentInfo.setTileInfos(tileInfos);
        contentInfo.setRemainTileInfos(List.of());
        contentInfo.setTransformMatrix(transformMatrix);
        contentInfo.setIsolateTextureLod(isolateTextureLod());
        contentInfos.add(contentInfo);
    }

    private SplitResult split(List<TileInfo> tileInfos, GaiaBoundingBox cellBoundingBox) {
        List<List<TileInfo>> children = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        List<TileInfo> retained = new ArrayList<>();
        for (TileInfo tileInfo : tileInfos) {
            GaiaBoundingBox tileBoundingBox = toCartographicBoundingBox(tileInfo);
            int childIndex = childIndexForCenter(cellBoundingBox, tileBoundingBox.getCenter().x, tileBoundingBox.getCenter().y);
            children.get(childIndex).add(tileInfo);
        }
        return new SplitResult(retained, children);
    }

    private int childIndexForCenter(GaiaBoundingBox cellBoundingBox, double centerX, double centerY) {
        double midX = (cellBoundingBox.getMinX() + cellBoundingBox.getMaxX()) / 2.0d;
        double midY = (cellBoundingBox.getMinY() + cellBoundingBox.getMaxY()) / 2.0d;
        if (centerX >= midX) {
            return centerY >= midY ? 2 : 1;
        }
        return centerY >= midY ? 3 : 0;
    }

    private GaiaBoundingBox toCartographicBoundingBox(TileInfo tileInfo) {
        return tileInfo.getBoundingBox().convertLocalToLonlatBoundingBox(tileInfo.getTileTransformInfo().getPosition());
    }

    private GaiaBoundingBox squareXY(GaiaBoundingBox boundingBox) {
        GaiaBoundingBox square = boundingBox.clone();
        double lengthX = square.getMaxX() - square.getMinX();
        double lengthY = square.getMaxY() - square.getMinY();
        if (lengthX > lengthY) {
            square.setMaxY(square.getMaxY() + (lengthX - lengthY));
        } else {
            square.setMaxX(square.getMaxX() + (lengthY - lengthX));
        }
        return square;
    }

    private GaiaBoundingBox childBoundingBox(GaiaBoundingBox parent, int childIndex) {
        double midX = (parent.getMinX() + parent.getMaxX()) / 2.0d;
        double midY = (parent.getMinY() + parent.getMaxY()) / 2.0d;
        return switch (childIndex) {
            case 0 -> new GaiaBoundingBox(parent.getMinX(), parent.getMinY(), parent.getMinZ(), midX, midY, parent.getMaxZ(), true);
            case 1 -> new GaiaBoundingBox(midX, parent.getMinY(), parent.getMinZ(), parent.getMaxX(), midY, parent.getMaxZ(), true);
            case 2 -> new GaiaBoundingBox(midX, midY, parent.getMinZ(), parent.getMaxX(), parent.getMaxY(), parent.getMaxZ(), true);
            case 3 -> new GaiaBoundingBox(parent.getMinX(), midY, parent.getMinZ(), midX, parent.getMaxY(), parent.getMaxZ(), true);
            default -> throw new TileProcessingException("Invalid quadtree child index: " + childIndex);
        };
    }

    protected LevelOfDetail lodForDepth(int depth) {
        int minLod = globalOptions.getMinLod();
        int maxLod = globalOptions.getMaxLod();
        return LevelOfDetail.getByLevel(Math.max(minLod, maxLod - depth));
    }

    protected LevelOfDetail lodForContent(List<TileInfo> tileInfos, int depth) {
        return lodForDepth(depth);
    }

    protected boolean allowDegenerateSplit(List<TileInfo> tileInfos, GaiaBoundingBox cellBoundingBox, int depth) {
        return false;
    }

    protected boolean isolateTextureLod() {
        return false;
    }

    private int depth(ImplicitTileCoordinate coordinate) {
        return coordinate.level();
    }

    private String contentPath(ImplicitTileCoordinate coordinate) {
        return "R/" + coordinate.level() + "/" + coordinate.x() + "/" + coordinate.y();
    }

    private void writeSubtrees(File outputPath, ObjectMapper objectMapper) {
        for (ImplicitSubtreeArtifact artifact : implicitSubtreeArtifacts) {
            try {
                File subtreeFile = new File(outputPath, artifact.subtreeUri());
                File bufferFile = new File(outputPath, artifact.bufferUri());
                java.nio.file.Files.createDirectories(subtreeFile.toPath().getParent());
                java.nio.file.Files.createDirectories(bufferFile.toPath().getParent());
                objectMapper.writeValue(subtreeFile, artifact.subtree());
                java.nio.file.Files.write(bufferFile.toPath(), artifact.availabilityBuffer());
            } catch (IOException e) {
                throw new TileProcessingException("Failed to write implicit subtree: " + artifact.subtreeUri(), e);
            }
        }
    }

    protected record SplitResult(List<TileInfo> retainedTileInfos, List<List<TileInfo>> children) {
        private boolean hasChildren() {
            return children.stream().anyMatch(child -> !child.isEmpty());
        }

        private boolean hasProgress(int parentSize) {
            return children.stream().anyMatch(child -> !child.isEmpty() && child.size() < parentSize);
        }
    }
}
