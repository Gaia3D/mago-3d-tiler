package com.gaia3d.basic.exception;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TileProcessingException extends RuntimeException {
    public TileProcessingException(String message) {
        super(message);
    }

    public TileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
