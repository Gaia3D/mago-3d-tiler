package com.gaia3d.basic.remesher;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import org.joml.Vector3d;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class FrontierClub {

    private final EnumSet<Side> sides;

    private FrontierClub(EnumSet<Side> sides) {
        this.sides = sides.clone();
    }

    /**
     * Classifies a frontier position according to the four XY sides
     * of the node bounding box.
     * <p>
     * NORTH = maxY
     * SOUTH = minY
     * EAST  = maxX
     * WEST  = minX
     */
    public static FrontierClub classify(Vector3d position, GaiaBoundingBox nodeBox, double tolerance) {
        Objects.requireNonNull(position, "position must not be null");

        Objects.requireNonNull(nodeBox, "nodeBox must not be null");

        if (!Double.isFinite(tolerance) || tolerance < 0.0) {
            throw new IllegalArgumentException("tolerance must be finite and >= 0");
        }

        EnumSet<Side> detectedSides = EnumSet.noneOf(Side.class);

        double distanceToNorth = Math.abs(position.y - nodeBox.getMaxY());

        double distanceToSouth = Math.abs(position.y - nodeBox.getMinY());

        double distanceToEast = Math.abs(position.x - nodeBox.getMaxX());

        double distanceToWest = Math.abs(position.x - nodeBox.getMinX());

        if (distanceToNorth <= tolerance) {
            detectedSides.add(Side.NORTH);
        }

        if (distanceToEast <= tolerance) {
            detectedSides.add(Side.EAST);
        }

        if (distanceToSouth <= tolerance) {
            detectedSides.add(Side.SOUTH);
        }

        if (distanceToWest <= tolerance) {
            detectedSides.add(Side.WEST);
        }

        return new FrontierClub(detectedSides);
    }

    /**
     * The frontier does not belong to any side of the node box.
     * It may be treated as a regular interior vertex.
     */
    public boolean isNone() {
        return sides.isEmpty();
    }

    /**
     * The frontier belongs to exactly one side.
     * It may use an outward clustering cell.
     */
    public boolean isSingle() {
        return sides.size() == 1;
    }

    /**
     * The frontier belongs to two or more sides.
     * Normally this is a corner and should not be remeshed.
     */
    public boolean isMultiple() {
        return sides.size() > 1;
    }

    public boolean contains(Side side) {
        return sides.contains(side);
    }

    /**
     * Returns the only side in this club.
     * @throws IllegalStateException if the club contains
     * zero or multiple sides.
     */
    public Side getSingleSide() {
        if (!isSingle()) {
            throw new IllegalStateException("FrontierClub does not contain exactly one side: " + sides);
        }

        return sides.iterator().next();
    }

    public Set<Side> getSides() {
        return Collections.unmodifiableSet(sides.clone());
    }

    @Override
    public String toString() {
        if (isNone()) {
            return "FrontierClub[NONE]";
        }

        return "FrontierClub" + sides;
    }

    public enum Side {
        NORTH(0, 1), EAST(1, 0), SOUTH(0, -1), WEST(-1, 0);

        private final int cellOffsetX;
        private final int cellOffsetY;

        Side(int cellOffsetX, int cellOffsetY) {
            this.cellOffsetX = cellOffsetX;
            this.cellOffsetY = cellOffsetY;
        }

        public int getCellOffsetX() {
            return cellOffsetX;
        }

        public int getCellOffsetY() {
            return cellOffsetY;
        }
    }
}
