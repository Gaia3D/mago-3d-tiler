package com.gaia3d.process.tileprocess.tile.tileset.implicit;

import com.gaia3d.process.tileprocess.tile.tileset.subtree.Subtree;

public record ImplicitSubtreeArtifact(String subtreeUri, String bufferUri, Subtree subtree, byte[] availabilityBuffer) {
}
