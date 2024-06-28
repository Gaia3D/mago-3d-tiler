package com.gaia3d.basic.exception;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PreprocessingException extends RuntimeException {
    public PreprocessingException(String message) {
        super(message);
    }

    public PreprocessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
