package com.gaia3d.process.tileprocess.tile.tileset.implicit;

import com.gaia3d.process.tileprocess.TilesetBuildResult;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import com.gaia3d.process.tileprocess.tile.tileset.node.Content;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("default")
class ImplicitTilesetProcessorTest {

    @Test
    void preparesQuadtreeAndSkipsContentMarkerInNodeCode() throws ReflectiveOperationException {
        Node root = node("R", 100.0);
        Node contentNode = node("RC2", 10.0);
        ContentInfo contentInfo = new ContentInfo();
        contentInfo.setNodeCode("RC2");
        Content content = new Content();
        content.setContentInfo(contentInfo);
        contentNode.setContent(content);
        root.getChildren().add(contentNode);

        Tileset tileset = new Tileset();
        tileset.setRoot(root);

        TilesetBuildResult result = new ImplicitTilesetProcessor().prepareQuadtree(tileset, 2, "glb");

        assertEquals(1, result.contentInfos().size());
        assertNotNull(contentPathField(result.contentInfos().getFirst()));
        assertEquals("RC2/0/0/0", contentPathField(result.contentInfos().getFirst()));
        assertEquals("RC2/0/0/0", result.contentInfos().getFirst().getContentPath());
        assertEquals("data/RC2/{level}/{x}/{y}.glb", contentNode.getContent().getUri());
        assertEquals(SubdivisionScheme.QUADTREE, contentNode.getImplicitTiling().getSubdivisionScheme());
        assertNull(contentNode.getChildren());
        assertEquals(100.0, root.getGeometricError());
    }

    @Test
    void keepsContentLodChainAsImplicitChildren() throws ReflectiveOperationException {
        Node root = node("R", 100.0);
        Node contentNode = contentNode("RC0", 20.0);
        Node sameSpaceLodNode = contentNode("RC00", 10.0);
        Node spatialLogicalNode = node("RC001", 5.0);
        Node spatialContentNode = contentNode("RC001C0", 1.0);
        spatialLogicalNode.getChildren().add(spatialContentNode);
        sameSpaceLodNode.getChildren().add(spatialLogicalNode);
        contentNode.getChildren().add(sameSpaceLodNode);
        root.getChildren().add(contentNode);

        Tileset tileset = new Tileset();
        tileset.setRoot(root);

        TilesetBuildResult result = new ImplicitTilesetProcessor().prepareQuadtree(tileset, 2, "glb");

        assertEquals(3, result.contentInfos().size());
        assertEquals("RC0/0/0/0", contentPathField(result.contentInfos().get(0)));
        assertEquals("RC0/1/0/0", contentPathField(result.contentInfos().get(1)));
        assertEquals("RC0/3/2/0", contentPathField(result.contentInfos().get(2)));
    }

    private Node node(String nodeCode, double geometricError) {
        Node node = new Node();
        node.setNodeCode(nodeCode);
        node.setGeometricError(geometricError);
        node.setChildren(new ArrayList<>());
        return node;
    }

    private Node contentNode(String nodeCode, double geometricError) {
        Node node = node(nodeCode, geometricError);
        ContentInfo contentInfo = new ContentInfo();
        contentInfo.setNodeCode(nodeCode);
        Content content = new Content();
        content.setContentInfo(contentInfo);
        node.setContent(content);
        return node;
    }

    private String contentPathField(ContentInfo contentInfo) throws ReflectiveOperationException {
        Field field = ContentInfo.class.getDeclaredField("contentPath");
        field.setAccessible(true);
        return (String) field.get(contentInfo);
    }
}
