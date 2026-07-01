package com.gaia3d.process.tileprocess.tile.tileset.implicit;

import com.gaia3d.process.tileprocess.tile.tileset.subtree.Subtree;

public class ImplicitSubtreeArtifact {
    private final String subtreeUri;
    private final String bufferUri;
    private final Subtree subtree;
    private final byte[] availabilityBuffer;

    public ImplicitSubtreeArtifact(String subtreeUri, String bufferUri, Subtree subtree, byte[] availabilityBuffer) {
        this.subtreeUri = subtreeUri;
        this.bufferUri = bufferUri;
        this.subtree = subtree;
        this.availabilityBuffer = availabilityBuffer;
    }

    public String getSubtreeUri() {
        return subtreeUri;
    }

    public String getBufferUri() {
        return bufferUri;
    }

    public Subtree getSubtree() {
        return subtree;
    }

    public byte[] getAvailabilityBuffer() {
        return availabilityBuffer;
    }
}
