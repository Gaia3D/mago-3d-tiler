package com.gaia3d.basic.pointcloud;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

@Slf4j
@AllArgsConstructor
@Getter
@Setter
public class GaiaPoint {
    Vector3d position;
    Vector3d color;
}

