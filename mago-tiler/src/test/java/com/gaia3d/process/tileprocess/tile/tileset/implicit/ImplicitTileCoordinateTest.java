package com.gaia3d.process.tileprocess.tile.tileset.implicit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("default")
class ImplicitTileCoordinateTest {

    @Test
    void mapsPointCloudChildCodesToOctreeCoordinates() {
        ImplicitTileCoordinate root = ImplicitTileCoordinate.root();

        ImplicitTileCoordinate b = root.child('B');
        assertEquals(1, b.getLevel());
        assertEquals(1, b.getX());
        assertEquals(0, b.getY());
        assertEquals(0, b.getZ());

        ImplicitTileCoordinate c = root.child('C');
        assertEquals(0, c.getX());
        assertEquals(1, c.getY());
        assertEquals(0, c.getZ());

        ImplicitTileCoordinate h = root.child('H');
        assertEquals(1, h.getX());
        assertEquals(1, h.getY());
        assertEquals(1, h.getZ());
        assertEquals(7, h.localMortonIndex(1, root, SubdivisionScheme.OCTREE));
    }

    @Test
    void formatsContentPath() {
        ImplicitTileCoordinate coordinate = ImplicitTileCoordinate.root().child('B').child('H');

        assertEquals("RR1/2/3/1/1", coordinate.toContentPath("RR1"));
    }

    @Test
    void formatsQuadtreeContentPath() {
        ImplicitTileCoordinate coordinate = ImplicitTileCoordinate.root()
                .child(1, SubdivisionScheme.QUADTREE)
                .child(2, SubdivisionScheme.QUADTREE);

        assertEquals("R/2/3/1", coordinate.toContentPath("R", SubdivisionScheme.QUADTREE));
        assertEquals(7, coordinate.localMortonIndex(2, ImplicitTileCoordinate.root(), SubdivisionScheme.QUADTREE));
    }
}
