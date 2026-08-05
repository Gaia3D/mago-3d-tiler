package com.gaia3d.process.preprocess.ingestion;

import java.io.File;

public class BasicFileValidator {

    public boolean validate(File file) {
        return file.exists() && file.isFile() && file.canRead();
    }
}
