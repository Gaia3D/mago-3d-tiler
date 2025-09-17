package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class GaiaOctree<E> {
    // children indices
    // down                         up
    // +---------+---------+        +---------+---------+
    // |         |         |        |         |         |
    // |    3    |    2    |        |    7    |    6    |
    // |         |         |        |         |         |
    // +---------+---------+        +---------+---------+
    // |         |         |        |         |         |
    // |    0    |    1    |        |    4    |    5    |
    // |         |         |        |         |         |
    // +---------+---------+        +---------+---------+
    // if null, this is the root octree
    private GaiaOctree<E> parent;
    // bounding box of this octree
    private final GaiaBoundingBox boundingBox = new GaiaBoundingBox();
    private GaiaOctreeIndex index = GaiaOctreeIndex.UNDEFINED;
    // depth in the octree
    private int depth = 0;
    private GaiaOctreeCoordinate coordinate = null;
    private List<GaiaOctree<E>> children;
    private final List<E> contents = new ArrayList<>();

    public GaiaOctree(GaiaOctree<E> parent, GaiaBoundingBox boundingBox) {
        this.parent = parent;
        this.setVolumeSize(boundingBox);
    }

    public void addContent(E content) {
        this.contents.add(content);
    }

    public void addContents(List<E> contents) {
        if (contents != null && !contents.isEmpty()) {
            this.contents.addAll(contents);
        }
    }

    public void removeContent(E content) {
        this.contents.remove(content);
    }

    public void clearContents() {
        this.contents.clear();
    }

    public int getContentsLength() {
        return this.contents.size();
    }

    public boolean isLeaf() {
        return (children == null || children.isEmpty());
    }

    public boolean isRoot() {
        return (parent == null);
    }

    protected GaiaOctree<E> createChild(GaiaBoundingBox boundingBox) {
        return new GaiaOctree<>(this, boundingBox);
    }

    public void createChildren() {
        if (children != null && (children.size() == 8)) {
            return;
        }

        children = new ArrayList<>(8);

        double minX = boundingBox.getMinX();
        double minY = boundingBox.getMinY();
        double minZ = boundingBox.getMinZ();
        double maxX = boundingBox.getMaxX();
        double maxY = boundingBox.getMaxY();
        double maxZ = boundingBox.getMaxZ();

        // now set children sizes
        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;

        GaiaBoundingBox childBBox = new GaiaBoundingBox();

        // idx 0. (minX, minY, minZ, midX, midY, midZ)
        childBBox.set(minX, minY, minZ, midX, midY, midZ);
        GaiaOctree<E> child = createChild(childBBox);
        child.index = GaiaOctreeIndex.LEFT_FRONT_BOTTOM;
        child.depth = this.getDepth() + 1;
        children.add(child);

        // idx 1. (midX, minY, minZ, maxX, midY, midZ)
        childBBox.set(midX, minY, minZ, maxX, midY, midZ);
        child = createChild(childBBox);
        child.index = GaiaOctreeIndex.RIGHT_FRONT_BOTTOM;
        child.depth = this.getDepth() + 1;
        children.add(child);

        // idx 2. (midX, midY, minZ, maxX, maxY, midZ)
        childBBox.set(midX, midY, minZ, maxX, maxY, midZ);
        child = createChild(childBBox);
        child.index = GaiaOctreeIndex.RIGHT_REAR_BOTTOM;
        child.depth = this.getDepth() + 1;
        children.add(child);

        // idx 3. (minX, midY, minZ, midX, maxY, midZ)
        childBBox.set(minX, midY, minZ, midX, maxY, midZ);
        child = createChild(childBBox);
        child.index = GaiaOctreeIndex.LEFT_REAR_BOTTOM;
        child.depth = this.getDepth() + 1;
        children.add(child);

        // idx 4.(minX, minY, midZ, midX, midY, maxZ)
        childBBox.set(minX, minY, midZ, midX, midY, maxZ);
        child = createChild(childBBox);
        child.index = GaiaOctreeIndex.LEFT_FRONT_TOP;
        child.depth = this.getDepth() + 1;
        children.add(child);

        // idx 5.(midX, minY, midZ, maxX, midY, maxZ)
        childBBox.set(midX, minY, midZ, maxX, midY, maxZ);
        child = createChild(childBBox);
        child.index = GaiaOctreeIndex.RIGHT_FRONT_TOP;
        child.depth = this.getDepth() + 1;
        children.add(child);

        // idx 6.(midX, midY, midZ, maxX, maxY, maxZ)
        childBBox.set(midX, midY, midZ, maxX, maxY, maxZ);
        child = createChild(childBBox);
        child.index = GaiaOctreeIndex.RIGHT_REAR_TOP;
        child.depth = this.getDepth() + 1;
        children.add(child);

        // idx 7.(minX, midY, midZ, midX, maxY, maxZ)
        childBBox.set(minX, midY, midZ, midX, maxY, maxZ);
        child = createChild(childBBox);
        child.index = GaiaOctreeIndex.LEFT_REAR_TOP;
        child.depth = this.getDepth() + 1;
        children.add(child);
    }

    public GaiaOctreeCoordinate getCoordinate() {
        if (this.coordinate != null) {
            return this.coordinate;
        }

        if (this.parent == null) {
            this.coordinate = new GaiaOctreeCoordinate();
            this.coordinate.setDepthAndCoord(0, 0, 0, 0);
        } else {
            GaiaOctreeCoordinate parentCoord = this.parent.getCoordinate();
            GaiaOctreeCoordinate coord = new GaiaOctreeCoordinate();
            // now set children coords
            int L = parentCoord.getDepth();
            int X = parentCoord.getX();
            int Y = parentCoord.getY();
            int Z = parentCoord.getZ();

            //        UNDEFINED(-1),
//                LEFT_FRONT_BOTTOM(0),
//                RIGHT_FRONT_BOTTOM(1),
//                RIGHT_REAR_BOTTOM(2),
//                LEFT_REAR_BOTTOM(3),
//                LEFT_FRONT_TOP(4),
//                RIGHT_FRONT_TOP(5),
//                RIGHT_REAR_TOP(6),
//                LEFT_REAR_TOP(7);

            if (this.index == GaiaOctreeIndex.LEFT_FRONT_BOTTOM) {
                coord.setDepthAndCoord(L + 1, X * 2, Y * 2, Z * 2);
            } else if (this.index == GaiaOctreeIndex.RIGHT_FRONT_BOTTOM) {
                coord.setDepthAndCoord(L + 1, X * 2 + 1, Y * 2, Z * 2);
            } else if (this.index == GaiaOctreeIndex.RIGHT_REAR_BOTTOM) {
                coord.setDepthAndCoord(L + 1, X * 2 + 1, Y * 2 + 1, Z * 2);
            } else if (this.index == GaiaOctreeIndex.LEFT_REAR_BOTTOM) {
                coord.setDepthAndCoord(L + 1, X * 2, Y * 2 + 1, Z * 2);
            } else if (this.index == GaiaOctreeIndex.LEFT_FRONT_TOP) {
                coord.setDepthAndCoord(L + 1, X * 2, Y * 2, Z * 2 + 1);
            } else if (this.index == GaiaOctreeIndex.RIGHT_FRONT_TOP) {
                coord.setDepthAndCoord(L + 1, X * 2 + 1, Y * 2, Z * 2 + 1);
            } else if (this.index == GaiaOctreeIndex.RIGHT_REAR_TOP) {
                coord.setDepthAndCoord(L + 1, X * 2 + 1, Y * 2 + 1, Z * 2 + 1);
            } else if (this.index == GaiaOctreeIndex.LEFT_REAR_TOP) {
                coord.setDepthAndCoord(L + 1, X * 2, Y * 2 + 1, Z * 2 + 1);
            }

            this.coordinate = coord;
        }
        return this.coordinate;
    }

    protected void setVolumeSize(GaiaBoundingBox boundingBox) {
        this.setVolumeSize(boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(),
                boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());
    }

    protected void setVolumeSize(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        boundingBox.setMinX(minX);
        boundingBox.setMinY(minY);
        boundingBox.setMinZ(minZ);
        boundingBox.setMaxX(maxX);
        boundingBox.setMaxY(maxY);
        boundingBox.setMaxZ(maxZ);
    }

    public List<GaiaOctree<E>> extractOctreesWithContents() {
        List<GaiaOctree<E>> octrees = new ArrayList<>();
        extractOctreesWithContents(octrees);
        return octrees;
    }

    protected void extractOctreesWithContents(List<GaiaOctree<E>> resultOctrees) {
        if (!contents.isEmpty()) {
            resultOctrees.add(this);
        }
        if (children != null) {
            for (GaiaOctree<E> child : children) {
                child.extractOctreesWithContents(resultOctrees);
            }
        }
    }

    public GaiaOctree<E> getRoot() {
        GaiaOctree<E> root = null;

        if (this.parent == null) {
            root = this;
        } else {
            root = this.parent.getRoot();
        }

        return root;
    }

    public boolean hasContents(boolean recursive) {
        if (!contents.isEmpty()) {
            return true;
        }

        if (recursive && children != null) {
            for (GaiaOctree<E> child : children) {
                if (child.hasContents(recursive)) {
                    return true;
                }
            }
        }

        return false;
    }

    public GaiaOctree<E> findOctreeByCoordinate(GaiaOctreeCoordinate coord) {
        if (coord.getDepth() < 0) {
            return null;
        }

        GaiaOctree<E> resultOctree = null;
        List<GaiaOctreeCoordinate> fullPath = coord.getFullPath(null);
        resultOctree = this.findOctreeByFullPath(fullPath);

        return resultOctree;
    }

    private GaiaOctree<E> findOctreeByFullPath(List<GaiaOctreeCoordinate> fullPath) {
        if (fullPath == null || fullPath.isEmpty()) {
            return null;
        }

        GaiaOctree<E> current = this.getRoot();
        int coordsCount = fullPath.size();
        for (int i = 0; i < coordsCount; i++) {
            GaiaOctreeCoordinate coord = fullPath.get(i);

            if (current == null) {
                return null;
            }

            if (current.getCoordinate().isEqual(coord)) {
                if (i < coordsCount - 1) {
                    GaiaOctreeCoordinate coordNext = fullPath.get(i + 1);

                    int idx = coordNext.getOctreeIndex().getIndex();
                    if (idx < 0 || idx >= 8) {
                        return null;
                    }

                    if (current.getChildren() != null) {
                        current = current.getChildren().get(idx);
                    } else {
                        return null;
                    }
                } else {
                    return current;
                }
            } else {
                return null;
            }
        }

        return current;
    }

}
