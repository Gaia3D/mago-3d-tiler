package com.gaia3d.basic.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class GaiaPointCloudHeader {
    private UUID uuid;
    private int index;
    private long size;
    private short blockSize = (8 * 3) + 3; // POSITION(DOUBLE * 3), RGB(byte * 3)
    private GaiaBoundingBox srsBoundingBox;
    private GaiaBoundingBox crsBoundingBox;
    private GaiaPointCloudTemp[][] tempGrid;

    public GaiaPointCloudTemp findTemp(Vector3d position) {
        int gridXLength = tempGrid.length;
        int gridYLength = tempGrid[0].length;

        Vector3d volume = srsBoundingBox.getVolume();
        int gridX = (int) Math.floor((position.x - srsBoundingBox.getMinX()) / volume.x * gridXLength);
        int gridY = (int) Math.floor((position.y - srsBoundingBox.getMinY()) / volume.y * gridYLength);
        return tempGrid[gridX][gridY];
    }

    public static GaiaPointCloudHeader combineHeaders(List<GaiaPointCloudHeader> headers){
        UUID uuid = UUID.randomUUID();
        int index = -1;
        long size = 0;
        GaiaBoundingBox srsBoundingBox = new GaiaBoundingBox();
        GaiaBoundingBox crsBoundingBox = new GaiaBoundingBox();

        for (GaiaPointCloudHeader header : headers) {
            index = header.getIndex();
            size += header.getSize();
            srsBoundingBox.addBoundingBox(header.getSrsBoundingBox());
            crsBoundingBox.addBoundingBox(header.getCrsBoundingBox());
        }

        return GaiaPointCloudHeader.builder()
            .uuid(uuid)
            .index(index)
            .size(size)
            .srsBoundingBox(srsBoundingBox)
            .crsBoundingBox(crsBoundingBox)
            .build();
    }
}