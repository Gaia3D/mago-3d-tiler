package com.gaia3d.basic.marchingcube;

import com.gaia3d.basic.geometry.voxel.VoxelCPGrid3D;
import com.gaia3d.basic.geometry.voxel.VoxelGrid3D;
import com.gaia3d.basic.legend.GaiaColor;
import com.gaia3d.basic.legend.LegendColors;
import com.gaia3d.basic.model.*;
import com.gaia3d.util.GeometryUtils;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.joml.Vector4d;

import java.util.List;

@Slf4j
public class MarchingCube {
    private static int[] EDGE_TABLE = {
            0x0, 0x109, 0x203, 0x30a, 0x406, 0x50f, 0x605, 0x70c,
            0x80c, 0x905, 0xa0f, 0xb06, 0xc0a, 0xd03, 0xe09, 0xf00,
            0x190, 0x99, 0x393, 0x29a, 0x596, 0x49f, 0x795, 0x69c,
            0x99c, 0x895, 0xb9f, 0xa96, 0xd9a, 0xc93, 0xf99, 0xe90,
            0x230, 0x339, 0x33, 0x13a, 0x636, 0x73f, 0x435, 0x53c,
            0xa3c, 0xb35, 0x83f, 0x936, 0xe3a, 0xf33, 0xc39, 0xd30,
            0x3a0, 0x2a9, 0x1a3, 0xaa, 0x7a6, 0x6af, 0x5a5, 0x4ac,
            0xbac, 0xaa5, 0x9af, 0x8a6, 0xfaa, 0xea3, 0xda9, 0xca0,
            0x460, 0x569, 0x663, 0x76a, 0x66, 0x16f, 0x265, 0x36c,
            0xc6c, 0xd65, 0xe6f, 0xf66, 0x86a, 0x963, 0xa69, 0xb60,
            0x5f0, 0x4f9, 0x7f3, 0x6fa, 0x1f6, 0xff, 0x3f5, 0x2fc,
            0xdfc, 0xcf5, 0xfff, 0xef6, 0x9fa, 0x8f3, 0xbf9, 0xaf0,
            0x650, 0x759, 0x453, 0x55a, 0x256, 0x35f, 0x55, 0x15c,
            0xe5c, 0xf55, 0xc5f, 0xd56, 0xa5a, 0xb53, 0x859, 0x950,
            0x7c0, 0x6c9, 0x5c3, 0x4ca, 0x3c6, 0x2cf, 0x1c5, 0xcc,
            0xfcc, 0xec5, 0xdcf, 0xcc6, 0xbca, 0xac3, 0x9c9, 0x8c0,
            0x8c0, 0x9c9, 0xac3, 0xbca, 0xcc6, 0xdcf, 0xec5, 0xfcc,
            0xcc, 0x1c5, 0x2cf, 0x3c6, 0x4ca, 0x5c3, 0x6c9, 0x7c0,
            0x950, 0x859, 0xb53, 0xa5a, 0xd56, 0xc5f, 0xf55, 0xe5c,
            0x15c, 0x55, 0x35f, 0x256, 0x55a, 0x453, 0x759, 0x650,
            0xaf0, 0xbf9, 0x8f3, 0x9fa, 0xef6, 0xfff, 0xcf5, 0xdfc,
            0x2fc, 0x3f5, 0xff, 0x1f6, 0x6fa, 0x7f3, 0x4f9, 0x5f0,
            0xb60, 0xa69, 0x963, 0x86a, 0xf66, 0xe6f, 0xd65, 0xc6c,
            0x36c, 0x265, 0x16f, 0x66, 0x76a, 0x663, 0x569, 0x460,
            0xca0, 0xda9, 0xea3, 0xfaa, 0x8a6, 0x9af, 0xaa5, 0xbac,
            0x4ac, 0x5a5, 0x6af, 0x7a6, 0xaa, 0x1a3, 0x2a9, 0x3a0,
            0xd30, 0xc39, 0xf33, 0xe3a, 0x936, 0x83f, 0xb35, 0xa3c,
            0x53c, 0x435, 0x73f, 0x636, 0x13a, 0x33, 0x339, 0x230,
            0xe90, 0xf99, 0xc93, 0xd9a, 0xa96, 0xb9f, 0x895, 0x99c,
            0x69c, 0x795, 0x49f, 0x596, 0x29a, 0x393, 0x99, 0x190,
            0xf00, 0xe09, 0xd03, 0xc0a, 0xb06, 0xa0f, 0x905, 0x80c,
            0x70c, 0x605, 0x50f, 0x406, 0x30a, 0x203, 0x109, 0x0
    };
    private static int[] TRIANGLE_TABLE = {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 1, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 8, 3, 9, 8, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 3, 1, 2, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            9, 2, 10, 0, 2, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            2, 8, 3, 2, 10, 8, 10, 9, 8, -1, -1, -1, -1, -1, -1, -1,
            3, 11, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 11, 2, 8, 11, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 9, 0, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 11, 2, 1, 9, 11, 9, 8, 11, -1, -1, -1, -1, -1, -1, -1,
            3, 10, 1, 11, 10, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 10, 1, 0, 8, 10, 8, 11, 10, -1, -1, -1, -1, -1, -1, -1,
            3, 9, 0, 3, 11, 9, 11, 10, 9, -1, -1, -1, -1, -1, -1, -1,
            9, 8, 10, 10, 8, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 7, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 3, 0, 7, 3, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 1, 9, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 1, 9, 4, 7, 1, 7, 3, 1, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 10, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            3, 4, 7, 3, 0, 4, 1, 2, 10, -1, -1, -1, -1, -1, -1, -1,
            9, 2, 10, 9, 0, 2, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1,
            2, 10, 9, 2, 9, 7, 2, 7, 3, 7, 9, 4, -1, -1, -1, -1,
            8, 4, 7, 3, 11, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            11, 4, 7, 11, 2, 4, 2, 0, 4, -1, -1, -1, -1, -1, -1, -1,
            9, 0, 1, 8, 4, 7, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1,
            4, 7, 11, 9, 4, 11, 9, 11, 2, 9, 2, 1, -1, -1, -1, -1,
            3, 10, 1, 3, 11, 10, 7, 8, 4, -1, -1, -1, -1, -1, -1, -1,
            1, 11, 10, 1, 4, 11, 1, 0, 4, 7, 11, 4, -1, -1, -1, -1,
            4, 7, 8, 9, 0, 11, 9, 11, 10, 11, 0, 3, -1, -1, -1, -1,
            4, 7, 11, 4, 11, 9, 9, 11, 10, -1, -1, -1, -1, -1, -1, -1,
            9, 5, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            9, 5, 4, 0, 8, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 5, 4, 1, 5, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            8, 5, 4, 8, 3, 5, 3, 1, 5, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 10, 9, 5, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            3, 0, 8, 1, 2, 10, 4, 9, 5, -1, -1, -1, -1, -1, -1, -1,
            5, 2, 10, 5, 4, 2, 4, 0, 2, -1, -1, -1, -1, -1, -1, -1,
            2, 10, 5, 3, 2, 5, 3, 5, 4, 3, 4, 8, -1, -1, -1, -1,
            9, 5, 4, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 11, 2, 0, 8, 11, 4, 9, 5, -1, -1, -1, -1, -1, -1, -1,
            0, 5, 4, 0, 1, 5, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1,
            2, 1, 5, 2, 5, 8, 2, 8, 11, 4, 8, 5, -1, -1, -1, -1,
            10, 3, 11, 10, 1, 3, 9, 5, 4, -1, -1, -1, -1, -1, -1, -1,
            4, 9, 5, 0, 8, 1, 8, 10, 1, 8, 11, 10, -1, -1, -1, -1,
            5, 4, 0, 5, 0, 11, 5, 11, 10, 11, 0, 3, -1, -1, -1, -1,
            5, 4, 8, 5, 8, 10, 10, 8, 11, -1, -1, -1, -1, -1, -1, -1,
            9, 7, 8, 5, 7, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            9, 3, 0, 9, 5, 3, 5, 7, 3, -1, -1, -1, -1, -1, -1, -1,
            0, 7, 8, 0, 1, 7, 1, 5, 7, -1, -1, -1, -1, -1, -1, -1,
            1, 5, 3, 3, 5, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            9, 7, 8, 9, 5, 7, 10, 1, 2, -1, -1, -1, -1, -1, -1, -1,
            10, 1, 2, 9, 5, 0, 5, 3, 0, 5, 7, 3, -1, -1, -1, -1,
            8, 0, 2, 8, 2, 5, 8, 5, 7, 10, 5, 2, -1, -1, -1, -1,
            2, 10, 5, 2, 5, 3, 3, 5, 7, -1, -1, -1, -1, -1, -1, -1,
            7, 9, 5, 7, 8, 9, 3, 11, 2, -1, -1, -1, -1, -1, -1, -1,
            9, 5, 7, 9, 7, 2, 9, 2, 0, 2, 7, 11, -1, -1, -1, -1,
            2, 3, 11, 0, 1, 8, 1, 7, 8, 1, 5, 7, -1, -1, -1, -1,
            11, 2, 1, 11, 1, 7, 7, 1, 5, -1, -1, -1, -1, -1, -1, -1,
            9, 5, 8, 8, 5, 7, 10, 1, 3, 10, 3, 11, -1, -1, -1, -1,
            5, 7, 0, 5, 0, 9, 7, 11, 0, 1, 0, 10, 11, 10, 0, -1,
            11, 10, 0, 11, 0, 3, 10, 5, 0, 8, 0, 7, 5, 7, 0, -1,
            11, 10, 5, 7, 11, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            10, 6, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 3, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            9, 0, 1, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 8, 3, 1, 9, 8, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1,
            1, 6, 5, 2, 6, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 6, 5, 1, 2, 6, 3, 0, 8, -1, -1, -1, -1, -1, -1, -1,
            9, 6, 5, 9, 0, 6, 0, 2, 6, -1, -1, -1, -1, -1, -1, -1,
            5, 9, 8, 5, 8, 2, 5, 2, 6, 3, 2, 8, -1, -1, -1, -1,
            2, 3, 11, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            11, 0, 8, 11, 2, 0, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1,
            0, 1, 9, 2, 3, 11, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1,
            5, 10, 6, 1, 9, 2, 9, 11, 2, 9, 8, 11, -1, -1, -1, -1,
            6, 3, 11, 6, 5, 3, 5, 1, 3, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 11, 0, 11, 5, 0, 5, 1, 5, 11, 6, -1, -1, -1, -1,
            3, 11, 6, 0, 3, 6, 0, 6, 5, 0, 5, 9, -1, -1, -1, -1,
            6, 5, 9, 6, 9, 11, 11, 9, 8, -1, -1, -1, -1, -1, -1, -1,
            5, 10, 6, 4, 7, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 3, 0, 4, 7, 3, 6, 5, 10, -1, -1, -1, -1, -1, -1, -1,
            1, 9, 0, 5, 10, 6, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1,
            10, 6, 5, 1, 9, 7, 1, 7, 3, 7, 9, 4, -1, -1, -1, -1,
            6, 1, 2, 6, 5, 1, 4, 7, 8, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 5, 5, 2, 6, 3, 0, 4, 3, 4, 7, -1, -1, -1, -1,
            8, 4, 7, 9, 0, 5, 0, 6, 5, 0, 2, 6, -1, -1, -1, -1,
            7, 3, 9, 7, 9, 4, 3, 2, 9, 5, 9, 6, 2, 6, 9, -1,
            3, 11, 2, 7, 8, 4, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1,
            5, 10, 6, 4, 7, 2, 4, 2, 0, 2, 7, 11, -1, -1, -1, -1,
            0, 1, 9, 4, 7, 8, 2, 3, 11, 5, 10, 6, -1, -1, -1, -1,
            9, 2, 1, 9, 11, 2, 9, 4, 11, 7, 11, 4, 5, 10, 6, -1,
            8, 4, 7, 3, 11, 5, 3, 5, 1, 5, 11, 6, -1, -1, -1, -1,
            5, 1, 11, 5, 11, 6, 1, 0, 11, 7, 11, 4, 0, 4, 11, -1,
            0, 5, 9, 0, 6, 5, 0, 3, 6, 11, 6, 3, 8, 4, 7, -1,
            6, 5, 9, 6, 9, 11, 4, 7, 9, 7, 11, 9, -1, -1, -1, -1,
            10, 4, 9, 6, 4, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 10, 6, 4, 9, 10, 0, 8, 3, -1, -1, -1, -1, -1, -1, -1,
            10, 0, 1, 10, 6, 0, 6, 4, 0, -1, -1, -1, -1, -1, -1, -1,
            8, 3, 1, 8, 1, 6, 8, 6, 4, 6, 1, 10, -1, -1, -1, -1,
            1, 4, 9, 1, 2, 4, 2, 6, 4, -1, -1, -1, -1, -1, -1, -1,
            3, 0, 8, 1, 2, 9, 2, 4, 9, 2, 6, 4, -1, -1, -1, -1,
            0, 2, 4, 4, 2, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            8, 3, 2, 8, 2, 4, 4, 2, 6, -1, -1, -1, -1, -1, -1, -1,
            10, 4, 9, 10, 6, 4, 11, 2, 3, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 2, 2, 8, 11, 4, 9, 10, 4, 10, 6, -1, -1, -1, -1,
            3, 11, 2, 0, 1, 6, 0, 6, 4, 6, 1, 10, -1, -1, -1, -1,
            6, 4, 1, 6, 1, 10, 4, 8, 1, 2, 1, 11, 8, 11, 1, -1,
            9, 6, 4, 9, 3, 6, 9, 1, 3, 11, 6, 3, -1, -1, -1, -1,
            8, 11, 1, 8, 1, 0, 11, 6, 1, 9, 1, 4, 6, 4, 1, -1,
            3, 11, 6, 3, 6, 0, 0, 6, 4, -1, -1, -1, -1, -1, -1, -1,
            6, 4, 8, 11, 6, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            7, 10, 6, 7, 8, 10, 8, 9, 10, -1, -1, -1, -1, -1, -1, -1,
            0, 7, 3, 0, 10, 7, 0, 9, 10, 6, 7, 10, -1, -1, -1, -1,
            10, 6, 7, 1, 10, 7, 1, 7, 8, 1, 8, 0, -1, -1, -1, -1,
            10, 6, 7, 10, 7, 1, 1, 7, 3, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 6, 1, 6, 8, 1, 8, 9, 8, 6, 7, -1, -1, -1, -1,
            2, 6, 9, 2, 9, 1, 6, 7, 9, 0, 9, 3, 7, 3, 9, -1,
            7, 8, 0, 7, 0, 6, 6, 0, 2, -1, -1, -1, -1, -1, -1, -1,
            7, 3, 2, 6, 7, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            2, 3, 11, 10, 6, 8, 10, 8, 9, 8, 6, 7, -1, -1, -1, -1,
            2, 0, 7, 2, 7, 11, 0, 9, 7, 6, 7, 10, 9, 10, 7, -1,
            1, 8, 0, 1, 7, 8, 1, 10, 7, 6, 7, 10, 2, 3, 11, -1,
            11, 2, 1, 11, 1, 7, 10, 6, 1, 6, 7, 1, -1, -1, -1, -1,
            8, 9, 6, 8, 6, 7, 9, 1, 6, 11, 6, 3, 1, 3, 6, -1,
            0, 9, 1, 11, 6, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            7, 8, 0, 7, 0, 6, 3, 11, 0, 11, 6, 0, -1, -1, -1, -1,
            7, 11, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            7, 6, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            3, 0, 8, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 1, 9, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            8, 1, 9, 8, 3, 1, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1,
            10, 1, 2, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 10, 3, 0, 8, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1,
            2, 9, 0, 2, 10, 9, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1,
            6, 11, 7, 2, 10, 3, 10, 8, 3, 10, 9, 8, -1, -1, -1, -1,
            7, 2, 3, 6, 2, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            7, 0, 8, 7, 6, 0, 6, 2, 0, -1, -1, -1, -1, -1, -1, -1,
            2, 7, 6, 2, 3, 7, 0, 1, 9, -1, -1, -1, -1, -1, -1, -1,
            1, 6, 2, 1, 8, 6, 1, 9, 8, 8, 7, 6, -1, -1, -1, -1,
            10, 7, 6, 10, 1, 7, 1, 3, 7, -1, -1, -1, -1, -1, -1, -1,
            10, 7, 6, 1, 7, 10, 1, 8, 7, 1, 0, 8, -1, -1, -1, -1,
            0, 3, 7, 0, 7, 10, 0, 10, 9, 6, 10, 7, -1, -1, -1, -1,
            7, 6, 10, 7, 10, 8, 8, 10, 9, -1, -1, -1, -1, -1, -1, -1,
            6, 8, 4, 11, 8, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            3, 6, 11, 3, 0, 6, 0, 4, 6, -1, -1, -1, -1, -1, -1, -1,
            8, 6, 11, 8, 4, 6, 9, 0, 1, -1, -1, -1, -1, -1, -1, -1,
            9, 4, 6, 9, 6, 3, 9, 3, 1, 11, 3, 6, -1, -1, -1, -1,
            6, 8, 4, 6, 11, 8, 2, 10, 1, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 10, 3, 0, 11, 0, 6, 11, 0, 4, 6, -1, -1, -1, -1,
            4, 11, 8, 4, 6, 11, 0, 2, 9, 2, 10, 9, -1, -1, -1, -1,
            10, 9, 3, 10, 3, 2, 9, 4, 3, 11, 3, 6, 4, 6, 3, -1,
            8, 2, 3, 8, 4, 2, 4, 6, 2, -1, -1, -1, -1, -1, -1, -1,
            0, 4, 2, 4, 6, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 9, 0, 2, 3, 4, 2, 4, 6, 4, 3, 8, -1, -1, -1, -1,
            1, 9, 4, 1, 4, 2, 2, 4, 6, -1, -1, -1, -1, -1, -1, -1,
            8, 1, 3, 8, 6, 1, 8, 4, 6, 6, 10, 1, -1, -1, -1, -1,
            10, 1, 0, 10, 0, 6, 6, 0, 4, -1, -1, -1, -1, -1, -1, -1,
            4, 6, 3, 4, 3, 8, 6, 10, 3, 0, 3, 9, 10, 9, 3, -1,
            10, 9, 4, 6, 10, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 9, 5, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 3, 4, 9, 5, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1,
            5, 0, 1, 5, 4, 0, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1,
            11, 7, 6, 8, 3, 4, 3, 5, 4, 3, 1, 5, -1, -1, -1, -1,
            9, 5, 4, 10, 1, 2, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1,
            6, 11, 7, 1, 2, 10, 0, 8, 3, 4, 9, 5, -1, -1, -1, -1,
            7, 6, 11, 5, 4, 10, 4, 2, 10, 4, 0, 2, -1, -1, -1, -1,
            3, 4, 8, 3, 5, 4, 3, 2, 5, 10, 5, 2, 11, 7, 6, -1,
            7, 2, 3, 7, 6, 2, 5, 4, 9, -1, -1, -1, -1, -1, -1, -1,
            9, 5, 4, 0, 8, 6, 0, 6, 2, 6, 8, 7, -1, -1, -1, -1,
            3, 6, 2, 3, 7, 6, 1, 5, 0, 5, 4, 0, -1, -1, -1, -1,
            6, 2, 8, 6, 8, 7, 2, 1, 8, 4, 8, 5, 1, 5, 8, -1,
            9, 5, 4, 10, 1, 6, 1, 7, 6, 1, 3, 7, -1, -1, -1, -1,
            1, 6, 10, 1, 7, 6, 1, 0, 7, 8, 7, 0, 9, 5, 4, -1,
            4, 0, 10, 4, 10, 5, 0, 3, 10, 6, 10, 7, 3, 7, 10, -1,
            7, 6, 10, 7, 10, 8, 5, 4, 10, 4, 8, 10, -1, -1, -1, -1,
            6, 9, 5, 6, 11, 9, 11, 8, 9, -1, -1, -1, -1, -1, -1, -1,
            3, 6, 11, 0, 6, 3, 0, 5, 6, 0, 9, 5, -1, -1, -1, -1,
            0, 11, 8, 0, 5, 11, 0, 1, 5, 5, 6, 11, -1, -1, -1, -1,
            6, 11, 3, 6, 3, 5, 5, 3, 1, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 10, 9, 5, 11, 9, 11, 8, 11, 5, 6, -1, -1, -1, -1,
            0, 11, 3, 0, 6, 11, 0, 9, 6, 5, 6, 9, 1, 2, 10, -1,
            11, 8, 5, 11, 5, 6, 8, 0, 5, 10, 5, 2, 0, 2, 5, -1,
            6, 11, 3, 6, 3, 5, 2, 10, 3, 10, 5, 3, -1, -1, -1, -1,
            5, 8, 9, 5, 2, 8, 5, 6, 2, 3, 8, 2, -1, -1, -1, -1,
            9, 5, 6, 9, 6, 0, 0, 6, 2, -1, -1, -1, -1, -1, -1, -1,
            1, 5, 8, 1, 8, 0, 5, 6, 8, 3, 8, 2, 6, 2, 8, -1,
            1, 5, 6, 2, 1, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 3, 6, 1, 6, 10, 3, 8, 6, 5, 6, 9, 8, 9, 6, -1,
            10, 1, 0, 10, 0, 6, 9, 5, 0, 5, 6, 0, -1, -1, -1, -1,
            0, 3, 8, 5, 6, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            10, 5, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            11, 5, 10, 7, 5, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            11, 5, 10, 11, 7, 5, 8, 3, 0, -1, -1, -1, -1, -1, -1, -1,
            5, 11, 7, 5, 10, 11, 1, 9, 0, -1, -1, -1, -1, -1, -1, -1,
            10, 7, 5, 10, 11, 7, 9, 8, 1, 8, 3, 1, -1, -1, -1, -1,
            11, 1, 2, 11, 7, 1, 7, 5, 1, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 3, 1, 2, 7, 1, 7, 5, 7, 2, 11, -1, -1, -1, -1,
            9, 7, 5, 9, 2, 7, 9, 0, 2, 2, 11, 7, -1, -1, -1, -1,
            7, 5, 2, 7, 2, 11, 5, 9, 2, 3, 2, 8, 9, 8, 2, -1,
            2, 5, 10, 2, 3, 5, 3, 7, 5, -1, -1, -1, -1, -1, -1, -1,
            8, 2, 0, 8, 5, 2, 8, 7, 5, 10, 2, 5, -1, -1, -1, -1,
            9, 0, 1, 5, 10, 3, 5, 3, 7, 3, 10, 2, -1, -1, -1, -1,
            9, 8, 2, 9, 2, 1, 8, 7, 2, 10, 2, 5, 7, 5, 2, -1,
            1, 3, 5, 3, 7, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 7, 0, 7, 1, 1, 7, 5, -1, -1, -1, -1, -1, -1, -1,
            9, 0, 3, 9, 3, 5, 5, 3, 7, -1, -1, -1, -1, -1, -1, -1,
            9, 8, 7, 5, 9, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            5, 8, 4, 5, 10, 8, 10, 11, 8, -1, -1, -1, -1, -1, -1, -1,
            5, 0, 4, 5, 11, 0, 5, 10, 11, 11, 3, 0, -1, -1, -1, -1,
            0, 1, 9, 8, 4, 10, 8, 10, 11, 10, 4, 5, -1, -1, -1, -1,
            10, 11, 4, 10, 4, 5, 11, 3, 4, 9, 4, 1, 3, 1, 4, -1,
            2, 5, 1, 2, 8, 5, 2, 11, 8, 4, 5, 8, -1, -1, -1, -1,
            0, 4, 11, 0, 11, 3, 4, 5, 11, 2, 11, 1, 5, 1, 11, -1,
            0, 2, 5, 0, 5, 9, 2, 11, 5, 4, 5, 8, 11, 8, 5, -1,
            9, 4, 5, 2, 11, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            2, 5, 10, 3, 5, 2, 3, 4, 5, 3, 8, 4, -1, -1, -1, -1,
            5, 10, 2, 5, 2, 4, 4, 2, 0, -1, -1, -1, -1, -1, -1, -1,
            3, 10, 2, 3, 5, 10, 3, 8, 5, 4, 5, 8, 0, 1, 9, -1,
            5, 10, 2, 5, 2, 4, 1, 9, 2, 9, 4, 2, -1, -1, -1, -1,
            8, 4, 5, 8, 5, 3, 3, 5, 1, -1, -1, -1, -1, -1, -1, -1,
            0, 4, 5, 1, 0, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            8, 4, 5, 8, 5, 3, 9, 0, 5, 0, 3, 5, -1, -1, -1, -1,
            9, 4, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 11, 7, 4, 9, 11, 9, 10, 11, -1, -1, -1, -1, -1, -1, -1,
            0, 8, 3, 4, 9, 7, 9, 11, 7, 9, 10, 11, -1, -1, -1, -1,
            1, 10, 11, 1, 11, 4, 1, 4, 0, 7, 4, 11, -1, -1, -1, -1,
            3, 1, 4, 3, 4, 8, 1, 10, 4, 7, 4, 11, 10, 11, 4, -1,
            4, 11, 7, 9, 11, 4, 9, 2, 11, 9, 1, 2, -1, -1, -1, -1,
            9, 7, 4, 9, 11, 7, 9, 1, 11, 2, 11, 1, 0, 8, 3, -1,
            11, 7, 4, 11, 4, 2, 2, 4, 0, -1, -1, -1, -1, -1, -1, -1,
            11, 7, 4, 11, 4, 2, 8, 3, 4, 3, 2, 4, -1, -1, -1, -1,
            2, 9, 10, 2, 7, 9, 2, 3, 7, 7, 4, 9, -1, -1, -1, -1,
            9, 10, 7, 9, 7, 4, 10, 2, 7, 8, 7, 0, 2, 0, 7, -1,
            3, 7, 10, 3, 10, 2, 7, 4, 10, 1, 10, 0, 4, 0, 10, -1,
            1, 10, 2, 8, 7, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 9, 1, 4, 1, 7, 7, 1, 3, -1, -1, -1, -1, -1, -1, -1,
            4, 9, 1, 4, 1, 7, 0, 8, 1, 8, 7, 1, -1, -1, -1, -1,
            4, 0, 3, 7, 4, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            4, 8, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            9, 10, 8, 10, 11, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            3, 0, 9, 3, 9, 11, 11, 9, 10, -1, -1, -1, -1, -1, -1, -1,
            0, 1, 10, 0, 10, 8, 8, 10, 11, -1, -1, -1, -1, -1, -1, -1,
            3, 1, 10, 11, 3, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 2, 11, 1, 11, 9, 9, 11, 8, -1, -1, -1, -1, -1, -1, -1,
            3, 0, 9, 3, 9, 11, 1, 2, 9, 2, 11, 9, -1, -1, -1, -1,
            0, 2, 11, 8, 0, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            3, 2, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            2, 3, 8, 2, 8, 10, 10, 8, 9, -1, -1, -1, -1, -1, -1, -1,
            9, 10, 2, 0, 9, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            2, 3, 8, 2, 8, 10, 0, 1, 8, 1, 10, 8, -1, -1, -1, -1,
            1, 10, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            1, 3, 8, 9, 1, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 9, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 3, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
    };

    public static Vector3d interpolate(Vector3d p1, Vector3d p2, float valp1, float valp2, float iso) {
        if (Math.abs(iso - valp1) < 0.00001) return p1;
        if (Math.abs(iso - valp2) < 0.00001) return p2;
        if (Math.abs(valp1 - valp2) < 0.00001) return p1;
        float mu = (iso - valp1) / (valp2 - valp1);
        return new Vector3d(
                p1.x + mu * (p2.x - p1.x),
                p1.y + mu * (p2.y - p1.y),
                p1.z + mu * (p2.z - p1.z)
        );
    }

    public static Vector3d interpolate(Vector3d p1, Vector3d p2, double valp1, double valp2, double iso) {
        if (Math.abs(iso - valp1) < 0.00001) return p1;
        if (Math.abs(iso - valp2) < 0.00001) return p2;
        if (Math.abs(valp1 - valp2) < 0.00001) return p1;
        double mu = (iso - valp1) / (valp2 - valp1);
        return new Vector3d(
                p1.x + mu * (p2.x - p1.x),
                p1.y + mu * (p2.y - p1.y),
                p1.z + mu * (p2.z - p1.z)
        );
    }

    public static GaiaScene makeGaiaScene(VoxelGrid3D voxelGrid3d, float isoValue) {
        GaiaScene gaiaScene = new GaiaScene();
        GaiaNode rootNode = new GaiaNode();
        gaiaScene.getNodes().add(rootNode);
        GaiaNode node = new GaiaNode();
        rootNode.getChildren().add(node);
        node.setName("VoxelGrid3D");
        GaiaMesh mesh = new GaiaMesh();
        node.getMeshes().add(mesh);
        GaiaPrimitive gaiaPrimitive = new GaiaPrimitive();
        mesh.getPrimitives().add(gaiaPrimitive);

        int gridsCountX = voxelGrid3d.getGridsCountX();
        int gridsCountY = voxelGrid3d.getGridsCountY();
        int gridsCountZ = voxelGrid3d.getGridsCountZ();

        List<GaiaVertex> gaiaVertices = gaiaPrimitive.getVertices();
        GaiaSurface gaiaSurface = new GaiaSurface();
        gaiaPrimitive.getSurfaces().add(gaiaSurface);

        log.info("start marching cube : {} {} {}", gridsCountX, gridsCountY, gridsCountZ);

        for (int x = 0; x < gridsCountX - 1; x++) {
            for (int y = 0; y < gridsCountY - 1; y++) {
                for (int z = 0; z < gridsCountZ - 1; z++) {
                    // Indices pointing to cube vertices
                    //                6  ___________________  7
                    //                  /|                 /|
                    //                 / |                / |
                    //                /  |               /  |
                    //           4   /___|______________/5  |
                    //              |    |              |   |                    z
                    //              |    |              |   |                    ^
                    //              |  2 |______________|___| 3                  |   y
                    //              |   /               |   /                    |  /
                    //              |  /                |  /                     | /
                    //              | /                 | /                      |/
                    //              |/__________________|/                       *-------> x
                    //             0                     1

                    float value0 = voxelGrid3d.getVoxelAlphaFloat(x, y, z);
                    float value1 = voxelGrid3d.getVoxelAlphaFloat(x + 1, y, z);
                    float value2 = voxelGrid3d.getVoxelAlphaFloat(x, y + 1, z);
                    float value3 = voxelGrid3d.getVoxelAlphaFloat(x + 1, y + 1, z);
                    float value4 = voxelGrid3d.getVoxelAlphaFloat(x, y, z + 1);
                    float value5 = voxelGrid3d.getVoxelAlphaFloat(x + 1, y, z + 1);
                    float value6 = voxelGrid3d.getVoxelAlphaFloat(x, y + 1, z + 1);
                    float value7 = voxelGrid3d.getVoxelAlphaFloat(x + 1, y + 1, z + 1);

                    int cubeIndex = 0;
                    if (value0 < isoValue) cubeIndex |= 1;
                    if (value1 < isoValue) cubeIndex |= 2;
                    if (value2 < isoValue) cubeIndex |= 8;
                    if (value3 < isoValue) cubeIndex |= 4;
                    if (value4 < isoValue) cubeIndex |= 16;
                    if (value5 < isoValue) cubeIndex |= 32;
                    if (value6 < isoValue) cubeIndex |= 128;
                    if (value7 < isoValue) cubeIndex |= 64;

                    if (cubeIndex == 0 || cubeIndex == 255) {
                        continue; // No triangles
                    }

                    // Get the edges of the cube that are intersected by the isosurface
                    int edges = EDGE_TABLE[cubeIndex];
                    if (edges == 0) {
                        continue; // No triangles
                    }

                    // Calculate the vertices of the triangles
                    float mu = 0.5f;
                    Vector3d[] vertices = new Vector3d[12];
                    if ((edges & 1) != 0) {
                        Vector3d p0 = voxelGrid3d.getVoxelPosition(x, y, z); // 0
                        Vector3d p1 = voxelGrid3d.getVoxelPosition(x + 1, y, z); // 1
                        vertices[0] = interpolate(p0, p1, value0, value1, isoValue);
                    }
                    if ((edges & 2) != 0) {
                        Vector3d p1 = voxelGrid3d.getVoxelPosition(x + 1, y, z); // 1
                        Vector3d p3 = voxelGrid3d.getVoxelPosition(x + 1, y + 1, z); // 3
                        vertices[1] = interpolate(p1, p3, value1, value3, isoValue);
                    }
                    if ((edges & 4) != 0) {
                        Vector3d p3 = voxelGrid3d.getVoxelPosition(x + 1, y + 1, z); // 3
                        Vector3d p2 = voxelGrid3d.getVoxelPosition(x, y + 1, z); // 2
                        vertices[2] = interpolate(p2, p3, value2, value3, isoValue);
                    }
                    if ((edges & 8) != 0) {
                        Vector3d p2 = voxelGrid3d.getVoxelPosition(x, y + 1, z); // 2
                        Vector3d p0 = voxelGrid3d.getVoxelPosition(x, y, z); // 0
                        vertices[3] = interpolate(p0, p2, value0, value2, isoValue);
                    }
                    if ((edges & 16) != 0) {
                        Vector3d p4 = voxelGrid3d.getVoxelPosition(x, y, z + 1); // 4
                        Vector3d p5 = voxelGrid3d.getVoxelPosition(x + 1, y, z + 1); // 5
                        vertices[4] = interpolate(p4, p5, value4, value5, isoValue);
                    }
                    if ((edges & 32) != 0) {
                        Vector3d p5 = voxelGrid3d.getVoxelPosition(x + 1, y, z + 1); // 5
                        Vector3d p7 = voxelGrid3d.getVoxelPosition(x + 1, y + 1, z + 1); // 7
                        vertices[5] = interpolate(p5, p7, value5, value7, isoValue);
                    }
                    if ((edges & 64) != 0) {
                        Vector3d p7 = voxelGrid3d.getVoxelPosition(x + 1, y + 1, z + 1); // 7
                        Vector3d p6 = voxelGrid3d.getVoxelPosition(x, y + 1, z + 1); // 6
                        vertices[6] = interpolate(p6, p7, value6, value7, isoValue);
                    }
                    if ((edges & 128) != 0) {
                        Vector3d p6 = voxelGrid3d.getVoxelPosition(x, y + 1, z + 1); // 6
                        Vector3d p4 = voxelGrid3d.getVoxelPosition(x, y, z + 1); // 4
                        vertices[7] = interpolate(p4, p6, value4, value6, isoValue);
                    }
                    if ((edges & 256) != 0) {
                        Vector3d p0 = voxelGrid3d.getVoxelPosition(x, y, z); // 0
                        Vector3d p4 = voxelGrid3d.getVoxelPosition(x, y, z + 1); // 4
                        vertices[8] = interpolate(p0, p4, value0, value4, isoValue);
                    }
                    if ((edges & 512) != 0) {
                        Vector3d p1 = voxelGrid3d.getVoxelPosition(x + 1, y, z); // 1
                        Vector3d p5 = voxelGrid3d.getVoxelPosition(x + 1, y, z + 1); // 5
                        vertices[9] = interpolate(p1, p5, value1, value5, isoValue);
                    }
                    if ((edges & 1024) != 0) {
                        Vector3d p3 = voxelGrid3d.getVoxelPosition(x + 1, y + 1, z); // 3
                        Vector3d p7 = voxelGrid3d.getVoxelPosition(x + 1, y + 1, z + 1); // 7
                        vertices[10] = interpolate(p3, p7, value3, value7, isoValue);
                    }
                    if ((edges & 2048) != 0) {
                        Vector3d p2 = voxelGrid3d.getVoxelPosition(x, y + 1, z); // 2
                        Vector3d p6 = voxelGrid3d.getVoxelPosition(x, y + 1, z + 1); // 6
                        vertices[11] = interpolate(p2, p6, value2, value6, isoValue);
                    }

                    int i = 0;
                    cubeIndex <<= 4;
                    while (TRIANGLE_TABLE[cubeIndex + i] != -1) {
                        int index0 = TRIANGLE_TABLE[cubeIndex + i];
                        int index1 = TRIANGLE_TABLE[cubeIndex + i + 1];
                        int index2 = TRIANGLE_TABLE[cubeIndex + i + 2];

                        Vector3d v0 = vertices[index0];
                        Vector3d v1 = vertices[index1];
                        Vector3d v2 = vertices[index2];

                        GaiaVertex gaiaVertex0 = new GaiaVertex();
                        gaiaVertex0.setPosition(v0);
                        GaiaVertex gaiaVertex1 = new GaiaVertex();
                        gaiaVertex1.setPosition(v1);
                        GaiaVertex gaiaVertex2 = new GaiaVertex();
                        gaiaVertex2.setPosition(v2);

                        gaiaVertices.add(gaiaVertex0);
                        gaiaVertices.add(gaiaVertex1);
                        gaiaVertices.add(gaiaVertex2);

                        GaiaFace gaiaFace = new GaiaFace();
                        int[] indices = new int[3];
                        indices[0] = gaiaVertices.size() - 3;
                        indices[1] = gaiaVertices.size() - 2;
                        indices[2] = gaiaVertices.size() - 1;
                        gaiaFace.setIndices(indices);

                        gaiaSurface.getFaces().add(gaiaFace);

                        i += 3;
                    }

                }
            }
        }

        return gaiaScene;
    }

    public static GaiaScene makeGaiaScene(VoxelCPGrid3D voxelGrid3d, double isoValue) {
        GaiaScene gaiaScene = null;
        List<GaiaVertex> gaiaVertices = null;
        GaiaSurface gaiaSurface = null;

        int gridsCountX = voxelGrid3d.getGridsCountX();
        int gridsCountY = voxelGrid3d.getGridsCountY();
        int gridsCountZ = voxelGrid3d.getGridsCountZ();

        log.info("start marching cube : {} {} {}", gridsCountX, gridsCountY, gridsCountZ);

        for (int x = 0; x < gridsCountX - 1; x++) {
            for (int y = 0; y < gridsCountY - 1; y++) {
                for (int z = 0; z < gridsCountZ - 1; z++) {
                    // Indices pointing to cube vertices
                    //                6  ___________________  7
                    //                  /|                 /|
                    //                 / |                / |
                    //                /  |               /  |
                    //           4   /___|______________/5  |
                    //              |    |              |   |                    z
                    //              |    |              |   |                    ^
                    //              |  2 |______________|___| 3                  |   y
                    //              |   /               |   /                    |  /
                    //              |  /                |  /                     | /
                    //              | /                 | /                      |/
                    //              |/__________________|/                       *-------> x
                    //             0                     1

                    double value0 = voxelGrid3d.getVoxelValue(x, y, z);
                    double value1 = voxelGrid3d.getVoxelValue(x + 1, y, z);
                    double value2 = voxelGrid3d.getVoxelValue(x, y + 1, z);
                    double value3 = voxelGrid3d.getVoxelValue(x + 1, y + 1, z);
                    double value4 = voxelGrid3d.getVoxelValue(x, y, z + 1);
                    double value5 = voxelGrid3d.getVoxelValue(x + 1, y, z + 1);
                    double value6 = voxelGrid3d.getVoxelValue(x, y + 1, z + 1);
                    double value7 = voxelGrid3d.getVoxelValue(x + 1, y + 1, z + 1);

                    int cubeIndex = 0;
                    if (value0 < isoValue) cubeIndex |= 1;
                    if (value1 < isoValue) cubeIndex |= 2;
                    if (value2 < isoValue) cubeIndex |= 8;
                    if (value3 < isoValue) cubeIndex |= 4;
                    if (value4 < isoValue) cubeIndex |= 16;
                    if (value5 < isoValue) cubeIndex |= 32;
                    if (value6 < isoValue) cubeIndex |= 128;
                    if (value7 < isoValue) cubeIndex |= 64;

                    if (cubeIndex == 0 || cubeIndex == 255) {
                        continue; // No triangles
                    }

                    // Get the edges of the cube that are intersected by the isosurface
                    int edges = EDGE_TABLE[cubeIndex];
                    if (edges == 0) {
                        continue; // No triangles
                    }

                    // Calculate the vertices of the triangles
                    float mu = 0.5f;
                    Vector3d[] vertices = new Vector3d[12];
                    if ((edges & 1) != 0) {
                        Vector3d p0 = voxelGrid3d.getVoxelPosition(x, y, z); // 0
                        Vector3d p1 = voxelGrid3d.getVoxelPosition(x + 1, y, z); // 1
                        vertices[0] = interpolate(p0, p1, value0, value1, isoValue);
                    }
                    if ((edges & 2) != 0) {
                        Vector3d p1 = voxelGrid3d.getVoxelPosition(x + 1, y, z); // 1
                        Vector3d p3 = voxelGrid3d.getVoxelPosition(x + 1, y + 1, z); // 3
                        vertices[1] = interpolate(p1, p3, value1, value3, isoValue);
                    }
                    if ((edges & 4) != 0) {
                        Vector3d p3 = voxelGrid3d.getVoxelPosition(x + 1, y + 1, z); // 3
                        Vector3d p2 = voxelGrid3d.getVoxelPosition(x, y + 1, z); // 2
                        vertices[2] = interpolate(p2, p3, value2, value3, isoValue);
                    }
                    if ((edges & 8) != 0) {
                        Vector3d p2 = voxelGrid3d.getVoxelPosition(x, y + 1, z); // 2
                        Vector3d p0 = voxelGrid3d.getVoxelPosition(x, y, z); // 0
                        vertices[3] = interpolate(p0, p2, value0, value2, isoValue);
                    }
                    if ((edges & 16) != 0) {
                        Vector3d p4 = voxelGrid3d.getVoxelPosition(x, y, z + 1); // 4
                        Vector3d p5 = voxelGrid3d.getVoxelPosition(x + 1, y, z + 1); // 5
                        vertices[4] = interpolate(p4, p5, value4, value5, isoValue);
                    }
                    if ((edges & 32) != 0) {
                        Vector3d p5 = voxelGrid3d.getVoxelPosition(x + 1, y, z + 1); // 5
                        Vector3d p7 = voxelGrid3d.getVoxelPosition(x + 1, y + 1, z + 1); // 7
                        vertices[5] = interpolate(p5, p7, value5, value7, isoValue);
                    }
                    if ((edges & 64) != 0) {
                        Vector3d p7 = voxelGrid3d.getVoxelPosition(x + 1, y + 1, z + 1); // 7
                        Vector3d p6 = voxelGrid3d.getVoxelPosition(x, y + 1, z + 1); // 6
                        vertices[6] = interpolate(p6, p7, value6, value7, isoValue);
                    }
                    if ((edges & 128) != 0) {
                        Vector3d p6 = voxelGrid3d.getVoxelPosition(x, y + 1, z + 1); // 6
                        Vector3d p4 = voxelGrid3d.getVoxelPosition(x, y, z + 1); // 4
                        vertices[7] = interpolate(p4, p6, value4, value6, isoValue);
                    }
                    if ((edges & 256) != 0) {
                        Vector3d p0 = voxelGrid3d.getVoxelPosition(x, y, z); // 0
                        Vector3d p4 = voxelGrid3d.getVoxelPosition(x, y, z + 1); // 4
                        vertices[8] = interpolate(p0, p4, value0, value4, isoValue);
                    }
                    if ((edges & 512) != 0) {
                        Vector3d p1 = voxelGrid3d.getVoxelPosition(x + 1, y, z); // 1
                        Vector3d p5 = voxelGrid3d.getVoxelPosition(x + 1, y, z + 1); // 5
                        vertices[9] = interpolate(p1, p5, value1, value5, isoValue);
                    }
                    if ((edges & 1024) != 0) {
                        Vector3d p3 = voxelGrid3d.getVoxelPosition(x + 1, y + 1, z); // 3
                        Vector3d p7 = voxelGrid3d.getVoxelPosition(x + 1, y + 1, z + 1); // 7
                        vertices[10] = interpolate(p3, p7, value3, value7, isoValue);
                    }
                    if ((edges & 2048) != 0) {
                        Vector3d p2 = voxelGrid3d.getVoxelPosition(x, y + 1, z); // 2
                        Vector3d p6 = voxelGrid3d.getVoxelPosition(x, y + 1, z + 1); // 6
                        vertices[11] = interpolate(p2, p6, value2, value6, isoValue);
                    }

                    int i = 0;
                    cubeIndex <<= 4;
                    while (TRIANGLE_TABLE[cubeIndex + i] != -1) {
                        if(gaiaScene == null){
                            gaiaScene = new GaiaScene();
                            GaiaNode rootNode = new GaiaNode();
                            gaiaScene.getNodes().add(rootNode);
                            GaiaNode node = new GaiaNode();
                            rootNode.getChildren().add(node);
                            node.setName("VoxelGrid3D");
                            GaiaMesh mesh = new GaiaMesh();
                            node.getMeshes().add(mesh);
                            GaiaPrimitive gaiaPrimitive = new GaiaPrimitive();
                            mesh.getPrimitives().add(gaiaPrimitive);
                            gaiaVertices = gaiaPrimitive.getVertices();
                            gaiaSurface = new GaiaSurface();
                            gaiaPrimitive.getSurfaces().add(gaiaSurface);
                        }
                        int index0 = TRIANGLE_TABLE[cubeIndex + i];
                        int index1 = TRIANGLE_TABLE[cubeIndex + i + 1];
                        int index2 = TRIANGLE_TABLE[cubeIndex + i + 2];

                        Vector3d v0 = vertices[index0];
                        Vector3d v1 = vertices[index1];
                        Vector3d v2 = vertices[index2];

                        GaiaVertex gaiaVertex0 = new GaiaVertex();
                        gaiaVertex0.setPosition(v0);
                        GaiaVertex gaiaVertex1 = new GaiaVertex();
                        gaiaVertex1.setPosition(v1);
                        GaiaVertex gaiaVertex2 = new GaiaVertex();
                        gaiaVertex2.setPosition(v2);

                        gaiaVertices.add(gaiaVertex0);
                        gaiaVertices.add(gaiaVertex1);
                        gaiaVertices.add(gaiaVertex2);

                        GaiaFace gaiaFace = new GaiaFace();
                        int[] indices = new int[3];
                        indices[0] = gaiaVertices.size() - 3;
                        indices[1] = gaiaVertices.size() - 2;
                        indices[2] = gaiaVertices.size() - 1;
                        gaiaFace.setIndices(indices);

                        gaiaSurface.getFaces().add(gaiaFace);

                        i += 3;
                    }
                }
            }
        }

        return gaiaScene;
    }

    public static GaiaScene makeGaiaSceneOnion(VoxelCPGrid3D voxelCPGrid3D, double[] isoValuesArray) {
        int isoValuesCount = isoValuesArray.length;
        GaiaScene gaiaSceneMaster = null;
        double totalMinValue = voxelCPGrid3D.getMinMaxValues()[0];
        double totalMaxValue = voxelCPGrid3D.getMinMaxValues()[1];

        for (int i = 0; i < isoValuesCount; i++) {
            double currIsoValue = isoValuesArray[i];
            if (totalMaxValue > currIsoValue) {

                // now, quantize the isoValue into rgba byte values.
                float quantizedIsoValue = (float) ((currIsoValue - totalMinValue) / (totalMaxValue - totalMinValue));
                byte[] encodedColor4 = new byte[4];
                GeometryUtils.encodeFloat(quantizedIsoValue, encodedColor4);

                GaiaScene gaiaScene = MarchingCube.makeGaiaScene(voxelCPGrid3D, currIsoValue);
                if (gaiaScene == null) {
                    continue;
                }

                gaiaScene.weldVertices(0.1, false, false, false, false);
                gaiaScene.calculateVertexNormals();

                List<GaiaPrimitive> gaiaPrimitives = gaiaScene.extractPrimitives(null);
                for (int j = 0; j < gaiaPrimitives.size(); j++) {
                    GaiaPrimitive gaiaPrimitive = gaiaPrimitives.get(j);
                    gaiaPrimitive.setMaterialIndex(0);
                    // set color to vertices.***
                    List<GaiaVertex> gaiaVertices = gaiaPrimitive.getVertices();
                    for (int k = 0; k < gaiaVertices.size(); k++) {
                        GaiaVertex gaiaVertex = gaiaVertices.get(k);
                        gaiaVertex.setColor(encodedColor4);
                    }
                }

                // set random color to material.***
                byte[] randomColor = new byte[4];
                float randomRed = (float) Math.random();
                randomColor[0] = (byte) (randomRed * 255.0f);
                float randomGreen = (float) Math.random();
                randomColor[1] = (byte) (randomGreen * 255.0f);
                float randomBlue = (float) Math.random();
                randomColor[2] = (byte) (randomBlue * 255.0f);
                float alpha = 0.5f;
                randomColor[3] = (byte) (alpha * 255.0f);
                List<GaiaMaterial> gaiaMaterials = gaiaScene.getMaterials();
                if (gaiaMaterials.size() == 0) {
                    // add a new material.
                    GaiaMaterial gaiaMaterial = new GaiaMaterial();
                    gaiaMaterial.setDiffuseColor(new Vector4d(randomRed, randomGreen, randomBlue, alpha));
                    gaiaMaterial.setBlend(true);
                    gaiaMaterials.add(gaiaMaterial);
                }
                for (int j = 0; j < gaiaMaterials.size(); j++) {
                    GaiaMaterial gaiaMaterial = gaiaMaterials.get(j);
                    gaiaMaterial.setDiffuseColor(new Vector4d(randomRed, randomGreen, randomBlue, alpha));
                    gaiaMaterial.setBlend(true);
                }

                if (gaiaSceneMaster == null) {
                    gaiaSceneMaster = gaiaScene;
                } else {
                    GaiaNode rootNodeMaster = gaiaSceneMaster.getNodes().get(0);
                    GaiaNode childNodeMaster = rootNodeMaster.getChildren().get(0);
                    GaiaMesh meshMaster = childNodeMaster.getMeshes().get(0);
                    GaiaPrimitive gaiaPrimitiveMaster = meshMaster.getPrimitives().get(0);

                    for (int j = 0; j < gaiaPrimitives.size(); j++) {
                        GaiaPrimitive gaiaPrimitive = gaiaPrimitives.get(j);
                        gaiaPrimitiveMaster.addPrimitive(gaiaPrimitive);
                    }
                }

                int hola = 0;
            }
        }

        return gaiaSceneMaster;
    }

    public static GaiaScene makeGaiaSceneOnion(VoxelCPGrid3D voxelCPGrid3D, double[] isoValuesArray, LegendColors legendColors) {
        int isoValuesCount = isoValuesArray.length;
        GaiaScene gaiaSceneMaster = null;
        double totalMinValue = voxelCPGrid3D.getMinMaxValues()[0];
        double totalMaxValue = voxelCPGrid3D.getMinMaxValues()[1];

        for (int i = 0; i < isoValuesCount; i++) {
            double currIsoValue = isoValuesArray[i];
            if (totalMaxValue > currIsoValue) {

                // now, quantize the isoValue into rgba byte values.
                //float quantizedIsoValue = (float) ((currIsoValue - totalMinValue) / (totalMaxValue - totalMinValue));
                //byte[] encodedColor4 = new byte[4];
                //GeometryUtils.encodeFloat(quantizedIsoValue, encodedColor4);

                GaiaScene gaiaScene = MarchingCube.makeGaiaScene(voxelCPGrid3D, currIsoValue);
                if (gaiaScene == null) {
                    continue;
                }

                gaiaScene.weldVertices(0.1, false, false, false, false);
                gaiaScene.calculateVertexNormals();

                // set color by legendColors.***
                GaiaColor gaiaColor = legendColors.getColorLinearInterpolation(currIsoValue);
                byte[] color4 = gaiaColor.getColorBytesArray();
                float redFloat = gaiaColor.getRed();
                float greenFloat = gaiaColor.getGreen();
                float blueFloat = gaiaColor.getBlue();
                float alpha = gaiaColor.getAlpha();

                List<GaiaPrimitive> gaiaPrimitives = gaiaScene.extractPrimitives(null);
                for (int j = 0; j < gaiaPrimitives.size(); j++) {
                    GaiaPrimitive gaiaPrimitive = gaiaPrimitives.get(j);
                    gaiaPrimitive.setMaterialIndex(0);
                    // set color to vertices.***
                    List<GaiaVertex> gaiaVertices = gaiaPrimitive.getVertices();
                    for (int k = 0; k < gaiaVertices.size(); k++) {
                        GaiaVertex gaiaVertex = gaiaVertices.get(k);
                        //gaiaVertex.setColor(encodedColor4);
                        gaiaVertex.setColor(color4);
                    }
                }



                List<GaiaMaterial> gaiaMaterials = gaiaScene.getMaterials();
                if (gaiaMaterials.size() == 0) {
                    // add a new material.
                    GaiaMaterial gaiaMaterial = new GaiaMaterial();
                    gaiaMaterial.setDiffuseColor(new Vector4d(redFloat, greenFloat, blueFloat, alpha));
                    gaiaMaterial.setBlend(true);
                    gaiaMaterials.add(gaiaMaterial);
                }
                for (int j = 0; j < gaiaMaterials.size(); j++) {
                    GaiaMaterial gaiaMaterial = gaiaMaterials.get(j);
                    gaiaMaterial.setDiffuseColor(new Vector4d(redFloat, greenFloat, blueFloat, alpha));
                    gaiaMaterial.setBlend(true);
                }

                if (gaiaSceneMaster == null) {
                    gaiaSceneMaster = gaiaScene;
                } else {
                    GaiaNode rootNodeMaster = gaiaSceneMaster.getNodes().get(0);
                    GaiaNode childNodeMaster = rootNodeMaster.getChildren().get(0);
                    GaiaMesh meshMaster = childNodeMaster.getMeshes().get(0);
                    GaiaPrimitive gaiaPrimitiveMaster = meshMaster.getPrimitives().get(0);

                    for (int j = 0; j < gaiaPrimitives.size(); j++) {
                        GaiaPrimitive gaiaPrimitive = gaiaPrimitives.get(j);
                        gaiaPrimitiveMaster.addPrimitive(gaiaPrimitive);
                    }
                }
            }
        }

        return gaiaSceneMaster;
    }
}