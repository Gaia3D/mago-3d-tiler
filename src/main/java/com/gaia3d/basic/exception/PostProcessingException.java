package com.gaia3d.basic.exception;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PostProcessingException extends RuntimeException {
    public PostProcessingException(String message) {
        super(message);
    }

    public PostProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
