package com.gaia3d.process.postprocess.batch;

import org.junit.jupiter.api.Test;

import java.io.File;

class Batched3DModelTest {

    @Test
    void extract() {
        Batched3DModel batched3DModel = new Batched3DModel();
        batched3DModel.extract(new File("E:\\znkim\\Desktop\\R0.b3dm"), new File("E:\\znkim\\Desktop\\R0-extracted.glb"));
    }
}