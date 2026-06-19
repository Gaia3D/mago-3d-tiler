package com.gaia3d.basic.magogl;

import java.nio.ByteBuffer;
import java.util.Objects;

public final class MagoBuffer {

    private final ByteBuffer data;

    public MagoBuffer(ByteBuffer data) {
        Objects.requireNonNull(data, "data must not be null");

        this.data = data.slice()
                .order(data.order())
                .asReadOnlyBuffer()
                .order(data.order());
    }

    public ByteBuffer getData() {
        ByteBuffer view = data.asReadOnlyBuffer()
                .order(data.order());

        view.position(0);
        return view;
    }

    public int getSizeBytes() {
        return data.limit();
    }
}