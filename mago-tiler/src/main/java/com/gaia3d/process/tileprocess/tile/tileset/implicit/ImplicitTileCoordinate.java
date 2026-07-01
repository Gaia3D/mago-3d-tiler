package com.gaia3d.process.tileprocess.tile.tileset.implicit;

import java.util.Objects;

public class ImplicitTileCoordinate {
    private final int level;
    private final int x;
    private final int y;
    private final int z;

    public ImplicitTileCoordinate(int level, int x, int y, int z) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static ImplicitTileCoordinate root() {
        return new ImplicitTileCoordinate(0, 0, 0, 0);
    }

    public ImplicitTileCoordinate child(char childCode) {
        int childIndex = octreeChildIndex(childCode);
        return child(childIndex, SubdivisionScheme.OCTREE);
    }

    public ImplicitTileCoordinate child(int childIndex, SubdivisionScheme subdivisionScheme) {
        if (subdivisionScheme == SubdivisionScheme.QUADTREE) {
            int childX = switch (childIndex) {
                case 0, 3 -> 0;
                case 1, 2 -> 1;
                default -> throw new IllegalArgumentException("Invalid quadtree child index: " + childIndex);
            };
            int childY = switch (childIndex) {
                case 0, 1 -> 0;
                case 2, 3 -> 1;
                default -> throw new IllegalArgumentException("Invalid quadtree child index: " + childIndex);
            };
            return new ImplicitTileCoordinate(
                    level + 1,
                    (x << 1) | childX,
                    (y << 1) | childY,
                    0);
        }
        return new ImplicitTileCoordinate(
                level + 1,
                (x << 1) | (childIndex & 1),
                (y << 1) | ((childIndex >> 1) & 1),
                (z << 1) | ((childIndex >> 2) & 1));
    }

    public ImplicitTileCoordinate ancestor(int ancestorLevel) {
        if (ancestorLevel < 0 || ancestorLevel > level) {
            throw new IllegalArgumentException("Invalid ancestor level: " + ancestorLevel);
        }
        int shift = level - ancestorLevel;
        return new ImplicitTileCoordinate(ancestorLevel, x >> shift, y >> shift, z >> shift);
    }

    public String toContentPath(String rootCode) {
        return rootCode + "/" + level + "/" + x + "/" + y + "/" + z;
    }

    public String toContentPath(String rootCode, SubdivisionScheme subdivisionScheme) {
        if (subdivisionScheme == SubdivisionScheme.QUADTREE) {
            return toQuadtreeContentPath(rootCode);
        }
        return toContentPath(rootCode);
    }

    public String toQuadtreeContentPath(String rootCode) {
        return rootCode + "/" + level + "/" + x + "/" + y;
    }

    public static int octreeChildIndex(char childCode) {
        return switch (childCode) {
            case 'A' -> 0;
            case 'B' -> 1;
            case 'C' -> 2;
            case 'D' -> 3;
            case 'E' -> 4;
            case 'F' -> 5;
            case 'G' -> 6;
            case 'H' -> 7;
            default -> throw new IllegalArgumentException("Unsupported octree child code: " + childCode);
        };
    }

    public int localMortonIndex(int localLevel, ImplicitTileCoordinate subtreeRoot, SubdivisionScheme subdivisionScheme) {
        int shift = level - subtreeRoot.level - localLevel;
        int localX = shift == 0 ? x : x >> shift;
        int localY = shift == 0 ? y : y >> shift;
        int localZ = shift == 0 ? z : z >> shift;
        int mask = (1 << localLevel) - 1;
        localX &= mask;
        localY &= mask;
        localZ &= mask;

        int index = 0;
        for (int bit = 0; bit < localLevel; bit++) {
            if (subdivisionScheme == SubdivisionScheme.QUADTREE) {
                index |= ((localX >> bit) & 1) << (bit * 2);
                index |= ((localY >> bit) & 1) << (bit * 2 + 1);
            } else {
                index |= ((localX >> bit) & 1) << (bit * 3);
                index |= ((localY >> bit) & 1) << (bit * 3 + 1);
                index |= ((localZ >> bit) & 1) << (bit * 3 + 2);
            }
        }
        return index;
    }

    public int getLevel() {
        return level;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ImplicitTileCoordinate that)) {
            return false;
        }
        return level == that.level && x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, x, y, z);
    }
}
