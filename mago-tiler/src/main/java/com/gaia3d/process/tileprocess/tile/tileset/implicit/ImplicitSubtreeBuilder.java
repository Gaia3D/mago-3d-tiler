package com.gaia3d.process.tileprocess.tile.tileset.implicit;

import com.gaia3d.process.tileprocess.tile.tileset.subtree.Availability;
import com.gaia3d.process.tileprocess.tile.tileset.subtree.Subtree;
import com.gaia3d.process.tileprocess.tile.tileset.subtree.SubtreeBuffer;
import com.gaia3d.process.tileprocess.tile.tileset.subtree.SubtreeBufferView;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImplicitSubtreeBuilder {
    private final String rootCode;
    private final int subtreeLevels;
    private final SubdivisionScheme subdivisionScheme;
    private final Set<ImplicitTileCoordinate> availableTiles = new HashSet<>();
    private final Set<ImplicitTileCoordinate> availableContents = new HashSet<>();

    public ImplicitSubtreeBuilder(String rootCode, int subtreeLevels) {
        this(rootCode, subtreeLevels, SubdivisionScheme.OCTREE);
    }

    public ImplicitSubtreeBuilder(String rootCode, int subtreeLevels, SubdivisionScheme subdivisionScheme) {
        if (subtreeLevels < 1) {
            throw new IllegalArgumentException("subtreeLevels must be greater than zero.");
        }
        this.rootCode = rootCode;
        this.subtreeLevels = subtreeLevels;
        this.subdivisionScheme = subdivisionScheme;
    }

    public void addContent(ImplicitTileCoordinate coordinate) {
        availableContents.add(coordinate);
        for (int level = 0; level <= coordinate.getLevel(); level++) {
            availableTiles.add(coordinate.ancestor(level));
        }
    }

    public List<ImplicitSubtreeArtifact> build() {
        Set<ImplicitTileCoordinate> subtreeRoots = findSubtreeRoots();
        List<ImplicitSubtreeArtifact> artifacts = new ArrayList<>();
        for (ImplicitTileCoordinate subtreeRoot : subtreeRoots.stream().sorted(Comparator.comparingInt(ImplicitTileCoordinate::getLevel)
                .thenComparingInt(ImplicitTileCoordinate::getX)
                .thenComparingInt(ImplicitTileCoordinate::getY)
                .thenComparingInt(ImplicitTileCoordinate::getZ)).toList()) {
            artifacts.add(buildSubtree(subtreeRoot, subtreeRoots));
        }
        return artifacts;
    }

    private Set<ImplicitTileCoordinate> findSubtreeRoots() {
        Set<ImplicitTileCoordinate> subtreeRoots = new HashSet<>();
        subtreeRoots.add(ImplicitTileCoordinate.root());
        for (ImplicitTileCoordinate coordinate : availableTiles) {
            int rootLevel = (coordinate.getLevel() / subtreeLevels) * subtreeLevels;
            subtreeRoots.add(coordinate.ancestor(rootLevel));
        }
        return subtreeRoots;
    }

    private ImplicitSubtreeArtifact buildSubtree(ImplicitTileCoordinate subtreeRoot, Set<ImplicitTileCoordinate> subtreeRoots) {
        Bitset tileBits = new Bitset(tileAvailabilityLength());
        Bitset contentBits = new Bitset(tileAvailabilityLength());
        Bitset childSubtreeBits = new Bitset(childSubtreeAvailabilityLength());

        for (ImplicitTileCoordinate coordinate : availableTiles) {
            if (!isInsideSubtree(subtreeRoot, coordinate)) {
                continue;
            }
            int bitIndex = availabilityBitIndex(subtreeRoot, coordinate);
            tileBits.set(bitIndex);
        }

        for (ImplicitTileCoordinate coordinate : availableContents) {
            if (!isInsideSubtree(subtreeRoot, coordinate)) {
                continue;
            }
            int bitIndex = availabilityBitIndex(subtreeRoot, coordinate);
            contentBits.set(bitIndex);
        }

        int childSubtreeLevel = subtreeRoot.getLevel() + subtreeLevels;
        for (ImplicitTileCoordinate childSubtreeRoot : subtreeRoots) {
            if (childSubtreeRoot.getLevel() != childSubtreeLevel) {
                continue;
            }
            if (!isChildSubtreeOf(subtreeRoot, childSubtreeRoot)) {
                continue;
            }
            int bitIndex = childSubtreeRoot.localMortonIndex(subtreeLevels, subtreeRoot, subdivisionScheme);
            childSubtreeBits.set(bitIndex);
        }

        byte[] tileAvailabilityBytes = tileBits.toByteArray();
        byte[] contentAvailabilityBytes = contentBits.toByteArray();
        byte[] childSubtreeAvailabilityBytes = childSubtreeBits.toByteArray();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        List<SubtreeBufferView> bufferViews = new ArrayList<>();
        appendAligned(buffer, bufferViews, tileAvailabilityBytes);
        appendAligned(buffer, bufferViews, contentAvailabilityBytes);
        appendAligned(buffer, bufferViews, childSubtreeAvailabilityBytes);

        String subtreeUri = subtreeUri(subtreeRoot);
        String bufferUri = bufferUri(subtreeRoot);

        Subtree subtree = new Subtree();
        subtree.setBuffers(List.of(new SubtreeBuffer(bufferUri.substring(bufferUri.lastIndexOf('/') + 1), buffer.size())));
        subtree.setBufferViews(bufferViews);
        subtree.setTileAvailability(Availability.bitstream(0, tileBits.count()));
        subtree.setContentAvailability(List.of(Availability.bitstream(1, contentBits.count())));
        subtree.setChildSubtreeAvailability(Availability.bitstream(2, childSubtreeBits.count()));

        return new ImplicitSubtreeArtifact(subtreeUri, bufferUri, subtree, buffer.toByteArray());
    }

    private boolean isInsideSubtree(ImplicitTileCoordinate subtreeRoot, ImplicitTileCoordinate coordinate) {
        int localLevel = coordinate.getLevel() - subtreeRoot.getLevel();
        if (localLevel < 0 || localLevel >= subtreeLevels) {
            return false;
        }
        return coordinate.ancestor(subtreeRoot.getLevel()).equals(subtreeRoot);
    }

    private boolean isChildSubtreeOf(ImplicitTileCoordinate subtreeRoot, ImplicitTileCoordinate childSubtreeRoot) {
        return childSubtreeRoot.getLevel() > subtreeRoot.getLevel()
                && childSubtreeRoot.ancestor(subtreeRoot.getLevel()).equals(subtreeRoot);
    }

    private int availabilityBitIndex(ImplicitTileCoordinate subtreeRoot, ImplicitTileCoordinate coordinate) {
        int localLevel = coordinate.getLevel() - subtreeRoot.getLevel();
        return levelOffset(localLevel) + coordinate.localMortonIndex(localLevel, subtreeRoot, subdivisionScheme);
    }

    private int tileAvailabilityLength() {
        return levelOffset(subtreeLevels);
    }

    private int childSubtreeAvailabilityLength() {
        return powBranch(subtreeLevels);
    }

    private int levelOffset(int localLevel) {
        int offset = 0;
        int levelSize = 1;
        for (int i = 0; i < localLevel; i++) {
            offset += levelSize;
            levelSize *= branchFactor();
        }
        return offset;
    }

    private int powBranch(int exponent) {
        int result = 1;
        for (int i = 0; i < exponent; i++) {
            result *= branchFactor();
        }
        return result;
    }

    private int branchFactor() {
        return subdivisionScheme == SubdivisionScheme.QUADTREE ? 4 : 8;
    }

    private void appendAligned(ByteArrayOutputStream buffer, List<SubtreeBufferView> bufferViews, byte[] bytes) {
        int byteOffset = buffer.size();
        buffer.writeBytes(bytes);
        bufferViews.add(new SubtreeBufferView(0, byteOffset, bytes.length));
        int padding = alignmentPadding(buffer.size());
        if (padding > 0) {
            buffer.writeBytes(new byte[padding]);
        }
    }

    private int alignmentPadding(int byteLength) {
        int remainder = byteLength % 8;
        return remainder == 0 ? 0 : 8 - remainder;
    }

    private String subtreeUri(ImplicitTileCoordinate coordinate) {
        if (subdivisionScheme == SubdivisionScheme.QUADTREE) {
            return "subtrees/" + rootCode + "/" + coordinate.getLevel() + "/" + coordinate.getX() + "/" + coordinate.getY() + ".json";
        }
        return "subtrees/" + rootCode + "/" + coordinate.getLevel() + "/" + coordinate.getX() + "/" + coordinate.getY() + "/" + coordinate.getZ() + ".json";
    }

    private String bufferUri(ImplicitTileCoordinate coordinate) {
        if (subdivisionScheme == SubdivisionScheme.QUADTREE) {
            return "subtrees/" + rootCode + "/" + coordinate.getLevel() + "/" + coordinate.getX() + "/" + coordinate.getY() + ".bin";
        }
        return "subtrees/" + rootCode + "/" + coordinate.getLevel() + "/" + coordinate.getX() + "/" + coordinate.getY() + "/" + coordinate.getZ() + ".bin";
    }

    private static class Bitset {
        private final byte[] bytes;
        private int count;

        private Bitset(int bitLength) {
            this.bytes = new byte[(bitLength + 7) / 8];
        }

        private void set(int bitIndex) {
            int byteIndex = bitIndex / 8;
            int bitOffset = bitIndex % 8;
            int mask = 1 << bitOffset;
            if ((bytes[byteIndex] & mask) == 0) {
                bytes[byteIndex] = (byte) (bytes[byteIndex] | mask);
                count++;
            }
        }

        private int count() {
            return count;
        }

        private byte[] toByteArray() {
            return bytes;
        }
    }
}
