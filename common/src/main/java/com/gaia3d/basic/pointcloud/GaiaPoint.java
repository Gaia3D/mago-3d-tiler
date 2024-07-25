package com.gaia3d.basic.pointcloud;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.io.Serializable;

@Slf4j
@AllArgsConstructor
@Getter
@Setter
public class GaiaPoint implements Serializable {
    private Vector3d position;
    private Vector3d color;
}

