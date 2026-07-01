package com.gaia3d.process.tileprocess.tile.tileset;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.process.tileprocess.tile.tileset.node.BoundingVolume;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("default")
class TilesetSerializationTest {

    @Test
    void keepsZeroGeometricErrorWhenSkippingDefaultValues() throws JsonProcessingException {
        Node root = new Node();
        root.setBoundingVolume(new BoundingVolume(BoundingVolume.BoundingVolumeType.REGION));
        root.setGeometricError(0.0d);

        Tileset tileset = new Tileset();
        tileset.setGeometricError(0.0d);
        tileset.setRoot(root);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

        String json = mapper.writeValueAsString(tileset);
        JsonNode rootNode = mapper.readTree(json);

        assertTrue(rootNode.has("geometricError"));
        assertTrue(rootNode.get("geometricError").isDouble());
        assertEquals(0.0d, rootNode.get("geometricError").asDouble());
        assertTrue(rootNode.get("root").has("geometricError"));
        assertTrue(rootNode.get("root").get("geometricError").isDouble());
        assertEquals(0.0d, rootNode.get("root").get("geometricError").asDouble());
    }
}
