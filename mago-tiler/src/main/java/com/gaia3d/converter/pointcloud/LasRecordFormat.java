package com.gaia3d.converter.pointcloud;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum LasRecordFormat {
    /*
     * Referenced by LAS 1.4 specification
     */

    /**
     *
     */
    FORMAT_0((byte) 0, false),
    /**
     * Point Data Record Format 0 contains the core 20 bytes that are shared by Point Data Record
     * Formats 0 to 5.
     */
    FORMAT_1((byte) 1, false),
    /**
     * Point Data Record Format 1 is the same as Point Data Record Format 0 with the addition of GPS
     * Time.
     */
    FORMAT_2((byte) 2, true),
    /**
     * Point Data Record Format 3 is the same as Point Data Record Format 2 with the addition of GPS
     * Time.
     */
    FORMAT_3((byte) 3, true),
    /**
     * Point Data Record Format 4 adds Wave Packets to Point Data Record Format 1.
     */
    FORMAT_4((byte) 4, false),
    /**
     * Point Data Record Format 5 adds Wave Packets to Point Data Record Format 3.
     */
    FORMAT_5((byte) 5, true),
    /**
     * Point Data Record Format 6 contains the core 30 bytes that are shared by Point Data Record
     * Formats 6 to 10. The difference to the core 20 bytes of Point Data Record Formats 0 to 5 is that
     * there are more bits for return numbers in order to support up to 15 returns, there are more bits for
     * point classifications to support up to 256 classes, there is a higher precision scan angle (16 bits
     * instead of 8), and the GPS time is mandatory.
     */
    FORMAT_6((byte) 6, false),
    /**
     * Point Data Record Format 7 is the same as Point Data Record Format 6 with the addition of three
     * RGB color channels. These fields are used when “colorizing” a LIDAR point using ancillary data,
     * typically from a camera.
     */
    FORMAT_7((byte) 7, true),
    /**
     * Point Data Record Format 8 is the same as Point Data Record Format 7 with the addition of a NIR (near infrared) channel.
     */
    FORMAT_8((byte) 8, true), //
    /**
     * Point Data Record Format 9 adds Wave Packets to Point Data Record Format 6
     */
    FORMAT_9((byte) 9, false),
    /**
     * Point Data Record Format 10 adds Wave Packets to Point Data Record Format 7.
     */
    FORMAT_10((byte) 10, true);

    final byte formatNumber;
    final boolean hasColor;

    public static LasRecordFormat fromFormatNumber(byte formatNumber) {
        for (LasRecordFormat format : LasRecordFormat.values()) {
            if (format.formatNumber == formatNumber) {
                return format;
            }
        }
        return null;
    }
}
