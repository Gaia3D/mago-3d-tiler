package com.gaia3d.basic.magogl;

import java.nio.ByteBuffer;
import java.util.Objects;

public final class MagoBuffer {

    private ByteBuffer data;

    public MagoBuffer(ByteBuffer data) {
        this.data = Objects.requireNonNull(
                data,
                "data must not be null"
        );
    }

    public ByteBuffer getData() {
        if (data == null) {
            throw new IllegalStateException(
                    "MagoBuffer has already been deleted."
            );
        }

        return data;
    }

    public int getSizeBytes() {
        return data == null
                ? 0
                : data.capacity();
    }

    public boolean isDeleted() {
        return data == null;
    }

    public void delete() {
        data = null;
    }
}