package com.gaia3d.process.tileprocess.tile.tileset.implicit;

import com.gaia3d.process.tileprocess.tile.tileset.subtree.Subtree;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("default")
class ImplicitSubtreeBuilderTest {

    @Test
    void buildsRootAndChildSubtreeAvailability() {
        ImplicitTileCoordinate root = ImplicitTileCoordinate.root();
        ImplicitTileCoordinate a = root.child('A');
        ImplicitTileCoordinate ah = a.child('H');

        ImplicitSubtreeBuilder builder = new ImplicitSubtreeBuilder("RR1", 2);
        builder.addContent(root);
        builder.addContent(a);
        builder.addContent(ah);

        List<ImplicitSubtreeArtifact> artifacts = builder.build();

        assertEquals(2, artifacts.size());

        ImplicitSubtreeArtifact rootArtifact = artifacts.stream()
                .filter(artifact -> artifact.subtreeUri().equals("subtrees/RR1/0/0/0/0.json"))
                .findFirst()
                .orElseThrow();
        Subtree rootSubtree = rootArtifact.subtree();
        assertEquals(2, rootSubtree.getTileAvailability().getAvailableCount());
        assertEquals(2, rootSubtree.getContentAvailability().getFirst().getAvailableCount());
        assertEquals(1, rootSubtree.getChildSubtreeAvailability().getAvailableCount());
        assertFalse(rootArtifact.availabilityBuffer().length == 0);

        ImplicitSubtreeArtifact childArtifact = artifacts.stream()
                .filter(artifact -> artifact.subtreeUri().equals("subtrees/RR1/2/1/1/1.json"))
                .findFirst()
                .orElseThrow();
        assertEquals(1, childArtifact.subtree().getTileAvailability().getAvailableCount());
        assertEquals(1, childArtifact.subtree().getContentAvailability().getFirst().getAvailableCount());
    }

    @Test
    void subtreeJsonKeepsZeroBufferViewFields() throws JsonProcessingException {
        ImplicitSubtreeBuilder builder = new ImplicitSubtreeBuilder("RR1", 2);
        builder.addContent(ImplicitTileCoordinate.root());

        ObjectMapper mapper = new ObjectMapper();
        mapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

        String json = mapper.writeValueAsString(builder.build().getFirst().subtree());

        assertTrue(json.contains("\"buffer\":0"));
        assertTrue(json.contains("\"byteOffset\":0"));
        assertTrue(json.contains("\"bitstream\":0"));
    }

    @Test
    void buildsQuadtreeSubtreeUris() {
        ImplicitSubtreeBuilder builder = new ImplicitSubtreeBuilder("R", 2, SubdivisionScheme.QUADTREE);
        builder.addContent(ImplicitTileCoordinate.root().child(1, SubdivisionScheme.QUADTREE));

        List<ImplicitSubtreeArtifact> artifacts = builder.build();

        assertEquals("subtrees/R/0/0/0.json", artifacts.getFirst().subtreeUri());
        assertEquals("subtrees/R/0/0/0.bin", artifacts.getFirst().bufferUri());
    }

    @Test
    void quadtreeChildCoordinatesMatchSpatialQuadrants() {
        ImplicitTileCoordinate root = ImplicitTileCoordinate.root();

        assertEquals("R/1/0/0", root.child(0, SubdivisionScheme.QUADTREE).toQuadtreeContentPath("R"));
        assertEquals("R/1/1/0", root.child(1, SubdivisionScheme.QUADTREE).toQuadtreeContentPath("R"));
        assertEquals("R/1/1/1", root.child(2, SubdivisionScheme.QUADTREE).toQuadtreeContentPath("R"));
        assertEquals("R/1/0/1", root.child(3, SubdivisionScheme.QUADTREE).toQuadtreeContentPath("R"));
    }
}
