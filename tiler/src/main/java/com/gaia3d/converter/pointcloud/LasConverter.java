package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.octree.GaiaOctreeVertices;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.basic.model.GaiaVertex;
import com.gaia3d.basic.pointcloud.GaiaPointCloudHeader;
import com.gaia3d.basic.pointcloud.GaiaPointCloudTemp;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.util.GlobeUtils;
import com.github.mreutegg.laszip4j.CloseablePointIterable;
import com.github.mreutegg.laszip4j.LASHeader;
import com.github.mreutegg.laszip4j.LASPoint;
import com.github.mreutegg.laszip4j.LASReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class LasConverter {
    public List<GaiaPointCloud> load(String path) {
        return convert(new File(path));
    }

    public List<GaiaPointCloud> load(File file) {
        return convert(file);
    }

    public List<GaiaPointCloud> load(Path path) {
        return convert(path.toFile());
    }

    public void loadToTemp(GaiaPointCloudHeader pointCloudHeader, File file) {
        LASReader reader = new LASReader(file);
        LASHeader header = reader.getHeader();
        double xScaleFactor = header.getXScaleFactor();
        double xOffset = header.getXOffset();
        double yScaleFactor = header.getYScaleFactor();
        double yOffset = header.getYOffset();
        double zScaleFactor = header.getZScaleFactor();
        double zOffset = header.getZOffset();
        CloseablePointIterable pointIterable = reader.getCloseablePoints();

        GlobalOptions options = GlobalOptions.getInstance();
        int pointSkip = options.getPointSkip();


        long pointRecords = header.getNumberOfPointRecords();
        long legacyPointRecords = header.getLegacyNumberOfPointRecords();
        long totalPointRecords = pointRecords + legacyPointRecords;

        AtomicInteger pointIndex = new AtomicInteger(0);
        for (LASPoint point : pointIterable) {
            int index = pointIndex.getAndIncrement();
            if (index % pointSkip != 0) {
                continue;
            }

            double x = point.getX() * xScaleFactor + xOffset;
            double y = point.getY() * yScaleFactor + yOffset;
            double z = point.getZ() * zScaleFactor + zOffset;
            byte[] rgb = getColorByRGB(point);
            //byte[] rgb = getColorByByteRGB(point); // only for test
            Vector3d position = new Vector3d(x, y, z);

            GaiaVertex vertex = new GaiaVertex();
            vertex.setPosition(position);
            vertex.setColor(rgb);
            vertex.setBatchId(0);

            GaiaPointCloudTemp tempFile = pointCloudHeader.findTemp(position);
            if (tempFile == null) {
                log.error("Failed to find temp file.");
            } else {
                tempFile.writePosition(position, rgb);
            }
        }
    }

    public GaiaPointCloudHeader readHeader(File file) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        LASReader reader = new LASReader(file);
        LASHeader header = reader.getHeader();
        /*byte major = header.getVersionMajor();
        byte minor = header.getVersionMinor();
        byte recordFormatValue = header.getPointDataRecordFormat();
        long recordLength = header.getPointDataRecordLength();*/
        //LasRecordFormat recordFormat = LasRecordFormat.fromFormatNumber(recordFormatValue);

        double getMinX = header.getMinX();
        double getMinY = header.getMinY();
        double getMinZ = header.getMinZ();
        double getMaxX = header.getMaxX();
        double getMaxY = header.getMaxY();
        double getMaxZ = header.getMaxZ();
        Vector3d min = new Vector3d(getMinX, getMinY, getMinZ);
        Vector3d max = new Vector3d(getMaxX, getMaxY, getMaxZ);

        GaiaBoundingBox srsBoundingBox = new GaiaBoundingBox();
        srsBoundingBox.addPoint(min);
        srsBoundingBox.addPoint(max);

        CoordinateReferenceSystem crs = globalOptions.getCrs();
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(crs, GlobeUtils.wgs84);
        //ProjCoordinate srsMinCoordinate = new ProjCoordinate(getMinX, getMinY, getMinZ);
        //ProjCoordinate srsMaxCoordinate = new ProjCoordinate(getMaxX, getMaxY, getMaxZ);
        //ProjCoordinate crsMinCoordinate = transformer.transform(srsMinCoordinate, new ProjCoordinate());
        //ProjCoordinate crsMaxCoordinate = transformer.transform(srsMaxCoordinate, new ProjCoordinate());
        //Vector3d minCrs = new Vector3d(crsMinCoordinate.x, crsMinCoordinate.y, srsMinCoordinate.z);
        //Vector3d maxCrs = new Vector3d(crsMaxCoordinate.x, crsMaxCoordinate.y, srsMaxCoordinate.z);

        //GaiaBoundingBox crsBoundingBox = new GaiaBoundingBox();
        //crsBoundingBox.addPoint(minCrs);
        //crsBoundingBox.addPoint(maxCrs);

        long pointRecords = header.getNumberOfPointRecords();
        long legacyPointRecords = header.getLegacyNumberOfPointRecords();
        long totalPointRecords = pointRecords + legacyPointRecords;

        return GaiaPointCloudHeader.builder()
                .index(-1)
                .uuid(UUID.randomUUID())
                .size(totalPointRecords)
                .srsBoundingBox(srsBoundingBox)
                //.crsBoundingBox(crsBoundingBox)
                .build();
    }

    private List<GaiaPointCloud> convert(File file) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        List<GaiaPointCloud> pointClouds = new ArrayList<>();
        GaiaPointCloud pointCloud = new GaiaPointCloud();
        CoordinateReferenceSystem source = globalOptions.getCrs();
        GaiaBoundingBox boundingBox = pointCloud.getGaiaBoundingBox();
        UUID uuid = UUID.randomUUID();
        File newTempFile = new File(file.getParent(), uuid.toString());
        //GaiaPointCloudTemp newTemp = new GaiaPointCloudTemp(newTempFile);

        try {
            GaiaPointCloudTemp readTemp = new GaiaPointCloudTemp(file);
            readTemp.readHeader();

            double[] quantizationOffset = readTemp.getQuantizedVolumeOffset();
            double[] quantizationScale = readTemp.getQuantizedVolumeScale();
            double[] originalMinPosition = new double[]{quantizationOffset[0], quantizationOffset[1], quantizationOffset[2]};
            double[] originalMaxPosition = new double[]{quantizationOffset[0] + quantizationScale[0], quantizationOffset[1] + quantizationScale[1], quantizationOffset[2] + quantizationScale[2]};
            Vector3d minPosition = new Vector3d(originalMinPosition[0], originalMinPosition[1], originalMinPosition[2]);
            Vector3d maxPosition = new Vector3d(originalMaxPosition[0], originalMaxPosition[1], originalMaxPosition[2]);

            boundingBox.addPoint(minPosition);
            boundingBox.addPoint(maxPosition);

            readTemp.getInputStream().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        GaiaPointCloudTemp readTemp = new GaiaPointCloudTemp(file);
        pointCloud.setMinimized(true);
        pointCloud.setVertices(null);
        pointCloud.setGaiaBoundingBox(boundingBox);
        pointCloud.setPointCloudTemp(readTemp);


        /*DataInputStream inputStream = null;
        DataOutputStream outputStream = null;
        try {
            BasicCoordinateTransform transformer = new BasicCoordinateTransform(source, GlobeUtils.wgs84);

            GaiaPointCloudTemp readTemp = new GaiaPointCloudTemp(file);
            boolean isSuccess = readTemp.readHeader();
            if (isSuccess) {
                *//*inputStream = readTemp.getInputStream();
                double[] quantizationOffset = readTemp.getQuantizedVolumeOffset();
                double[] quantizationScale = readTemp.getQuantizedVolumeScale();
                double[] originalMinPosition = new double[]{quantizationOffset[0], quantizationOffset[1], quantizationOffset[2]};
                double[] originalMaxPosition = new double[]{quantizationOffset[0] + quantizationScale[0], quantizationOffset[1] + quantizationScale[1], quantizationOffset[2] + quantizationScale[2]};

                //ProjCoordinate transformedMinCoordinate = transformer.transform(new ProjCoordinate(originalMinPosition[0], originalMinPosition[1], originalMinPosition[2]), new ProjCoordinate());
                //Vector3d minPosition = new Vector3d(transformedMinCoordinate.x, transformedMinCoordinate.y, originalMinPosition[2]);
                //ProjCoordinate transformedMaxCoordinate = transformer.transform(new ProjCoordinate(originalMaxPosition[0], originalMaxPosition[1], originalMaxPosition[2]), new ProjCoordinate());
                //Vector3d maxPosition = new Vector3d(transformedMaxCoordinate.x, transformedMaxCoordinate.y, originalMaxPosition[2]);
                boundingBox.addPoint(minPosition);
                boundingBox.addPoint(maxPosition);

                newTemp.getQuantizedVolumeOffset()[0] = minPosition.x;
                newTemp.getQuantizedVolumeOffset()[1] = minPosition.y;
                newTemp.getQuantizedVolumeOffset()[2] = minPosition.z;
                newTemp.getQuantizedVolumeScale()[0] = maxPosition.x - minPosition.x;
                newTemp.getQuantizedVolumeScale()[1] = maxPosition.y - minPosition.y;
                newTemp.getQuantizedVolumeScale()[2] = maxPosition.z - minPosition.z;

                newTemp.writeHeader();
                outputStream = newTemp.getOutputStream();

                log.info("original min position : {}", minPosition);
                log.info("original max position : {}", maxPosition);
                log.info("transformed min position : {}", minPosition);
                log.info("transformed max position : {}", maxPosition);

                int chunkSize = 65536 * 8 * 16;
                boolean isEndFile = false;
                while (!isEndFile) {
                    List<GaiaVertex> vertices = readTemp.readTempChunk(chunkSize);
                    if (vertices.isEmpty()) {
                        isEndFile = true;
                        break;
                    }
                    vertices.forEach((vertex) -> {
                        Vector3d position = vertex.getPosition();
                        *//**//*ProjCoordinate coordinate = new ProjCoordinate(position.x, position.y, position.z);
                        ProjCoordinate transformedCoordinate = transformer.transform(coordinate, new ProjCoordinate());
                        Vector3d newPosition = new Vector3d(transformedCoordinate.x, transformedCoordinate.y, position.z);
                        if (newPosition.x < minPosition.x || newPosition.x > maxPosition.x || newPosition.y < minPosition.y || newPosition.y > maxPosition.y) {
                            log.error("Out of bound : {}", newPosition);
                        } else {
                            boundingBox.addPoint(newPosition);
                        }*//**//*
                        vertex.setPosition(position);
                    });
                    newTemp.writePositionsFast(vertices);
                }*//*

                //inputStream.close();
                //outputStream.close();
            }
        } catch (IOException e) {
            log.error("Failed to read temp file.", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("Failed to close input stream.", e);
                }
            }
        }*/

        pointClouds.add(pointCloud);
        return pointClouds;
    }


    /*private List<GaiaPointCloud> convertOld(File file) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        List<GaiaPointCloud> pointClouds = new ArrayList<>();
        GaiaPointCloud pointCloud = new GaiaPointCloud();
        List<GaiaVertex> vertices = pointCloud.getVertices();

        LASReader reader = new LASReader(file);
        LASHeader header = reader.getHeader();

        byte major = header.getVersionMajor();
        byte minor = header.getVersionMinor();
        byte recordFormatValue = header.getPointDataRecordFormat();
        long recordLength = header.getPointDataRecordLength();

        LasRecordFormat recordFormat = LasRecordFormat.fromFormatNumber(recordFormatValue);

        boolean hasRgbColor;
        if (recordFormat != null) {
            hasRgbColor = recordFormat.hasColor;
        } else {
            hasRgbColor = false;
        }
        GaiaBoundingBox boundingBox = pointCloud.getGaiaBoundingBox();

        double xScaleFactor = header.getXScaleFactor();
        double xOffset = header.getXOffset();
        double yScaleFactor = header.getYScaleFactor();
        double yOffset = header.getYOffset();
        double zScaleFactor = header.getZScaleFactor();
        double zOffset = header.getZOffset();

        double getMinX = header.getMinX();
        double getMinY = header.getMinY();
        double getMinZ = header.getMinZ();
        double getMaxX = header.getMaxX();
        double getMaxY = header.getMaxY();
        double getMaxZ = header.getMaxZ();

        CoordinateReferenceSystem crs = globalOptions.getCrs();

        BasicCoordinateTransform transformer = new BasicCoordinateTransform(crs, GlobeUtils.wgs84);
        ProjCoordinate srsMinCoordinate = new ProjCoordinate(getMinX, getMinY, getMinZ);
        ProjCoordinate srsMaxCoordinate = new ProjCoordinate(getMaxX, getMaxY, getMaxZ);
        ProjCoordinate crsMinCoordinate = transformer.transform(srsMinCoordinate, new ProjCoordinate());
        ProjCoordinate crsMaxCoordinate = transformer.transform(srsMaxCoordinate, new ProjCoordinate());
        crsMinCoordinate.z = getMinZ;
        crsMaxCoordinate.z = getMaxZ;

        ProjCoordinate srsBoundingBox = new ProjCoordinate();
        srsBoundingBox.x = srsMaxCoordinate.x - srsMinCoordinate.x;
        srsBoundingBox.y = srsMaxCoordinate.y - srsMinCoordinate.y;
        srsBoundingBox.z = srsMaxCoordinate.z - srsMinCoordinate.z;

        int pointSkip = globalOptions.getPointSkip();
        CloseablePointIterable pointIterable = reader.getCloseablePoints();
        long pointRecords = header.getNumberOfPointRecords();
        long legacyPointRecords = header.getLegacyNumberOfPointRecords();

        long totalPointRecords = pointRecords + legacyPointRecords;
        long totalPointRecords1percent = totalPointRecords / 100;

        log.info("[Pre] Loading a pointcloud file. : {}", file.getAbsolutePath());
        log.debug("----------------------------------------");
        log.debug(" - LAS Version : {}.{}", major, minor);
        log.debug(" - LAS Point Data Record Format : {}", recordFormat);
        log.debug(" - LAS Point Data Record Length : {}", recordLength);
        log.debug(" - LAS Point Data Record has RGB Color : {}", hasRgbColor);
        log.debug(" - LAS Total Point Records : {}", pointRecords);
        log.debug(" - LAS Total Legacy Point Records : {}", legacyPointRecords);
        log.debug(" - LAS Total Record size : {}", totalPointRecords * recordLength);
        log.debug(" - LAS Min Local Coordinate : {}", srsMinCoordinate);
        log.debug(" - LAS Max Local Coordinate : {}", srsMaxCoordinate);
        log.debug(" - LAS Min World Coordinate : {}", crsMinCoordinate);
        log.debug(" - LAS Max World Coordinate : {}", crsMaxCoordinate);
        log.debug(" - LAS Bounding Box : {}", srsBoundingBox);
        log.debug("----------------------------------------");

        long pointIndex = 0;
        Vector3d recentPosition = null;
        for (LASPoint point : pointIterable) {
            if (totalPointRecords1percent > 0 && pointIndex % totalPointRecords1percent == 0 && pointIndex != 0) {
                log.debug(" - Las Records Loading progress. ({}/100)%", pointIndex / totalPointRecords1percent);
            }

            if (pointIndex % pointSkip == 0) {
                double x = point.getX() * xScaleFactor + xOffset;
                double y = point.getY() * yScaleFactor + yOffset;
                double z = point.getZ() * zScaleFactor + zOffset;

                ProjCoordinate coordinate = new ProjCoordinate(x, y, z);
                ProjCoordinate transformedCoordinate = new ProjCoordinate();
                transformer.transform(coordinate, transformedCoordinate);
                Vector3d position = new Vector3d(transformedCoordinate.x, transformedCoordinate.y, z);
                recentPosition = position;
                coordinate = null;
                transformedCoordinate = null;

                byte[] rgb;
                if (hasRgbColor) {
                    rgb = getColorByRGB(point);
                    //rgb = getColorByByteRGB(point); // only for test
                } else {
                    rgb = getColorIntensity(point);
                }

                GaiaVertex vertex = new GaiaVertex();
                vertex.setPosition(position);
                vertex.setColor(rgb);
                vertex.setBatchId(0);
                vertices.add(vertex);
                boundingBox.addPoint(position);
            }
            pointIndex++;
        }
        pointIterable.close();

        List<GaiaVertex> reducedPoints = vertices;
        //List<GaiaVertex> reducedPoints = reducePointsA(vertices);
        //List<GaiaVertex> reducedPoints = reducePointsB(vertices);
        //log.info("original vertices count : {}", vertices.size());
        //log.info("reduced vertices count : {}", reducedPoints.size());

        System.gc();
        Collections.shuffle(reducedPoints);
        pointClouds.add(pointCloud);
        pointCloud.setVertices(reducedPoints);

        log.debug("----------------------------------------");
        return pointClouds;
    }*/

    /**
     * Reduce points by using HashMap
     * @param vertices List<GaiaVertex>
     * @return List<GaiaVertex>
     */
    private List<GaiaVertex> reducePointsA(List<GaiaVertex> vertices) {
        String format = "%.5f";
        Map<String, GaiaVertex> hashMap = new HashMap<>();
        vertices.forEach((vertex) -> {
            Vector3d position = vertex.getPosition();
            double x = position.x;
            double y = position.y;
            double z = position.z;

            String xStr = String.format(format, x);
            String yStr = String.format(format, y);
            String zStr = String.format(format, z);
            String key = xStr + "-" + yStr + "-" + zStr;
            if (hashMap.containsKey(key)) {
                //log.error("Duplicated key : {}", key);
            } else {
                hashMap.put(key, vertex);
            }
        });
        List<GaiaVertex> newVertices = new ArrayList<>(hashMap.values());
        return newVertices;
    }

    /**
     * Reduce points by using GaiaOctreeVertices
     * @param vertices List<GaiaVertex>
     * @return List<GaiaVertex>
     */
    /*private List<GaiaVertex> reducePointsB(List<GaiaVertex> vertices) {
        GaiaOctreeVertices octreeVertices = new GaiaOctreeVertices(null);
        octreeVertices.getVertices().addAll(vertices);
        octreeVertices.calculateSize();
        octreeVertices.setAsCube();
        //octreeVertices.setMinBoxSize(0.1); // 1m.***
        //octreeVertices.makeTreeByMinBoxSize(1.0);
        octreeVertices.setMaxDepth(20);
        octreeVertices.makeTreeByMinBoxSize(0.1);
        octreeVertices.reduceVertices(10);
        List<GaiaVertex> newVertices = octreeVertices.getAllVertices(null);
        return newVertices;
    }*/


    private void transformPositions(List<GaiaVertex> vertices) {
        vertices.forEach((vertex) -> {
            Vector3d positions = vertex.getPosition();
            ProjCoordinate coordinate = new ProjCoordinate(positions.x, positions.y, positions.z);
            ProjCoordinate transformedCoordinate = GlobeUtils.transform(GlobalOptions.getInstance().getCrs(), coordinate);
            positions.x = transformedCoordinate.x;
            positions.y = transformedCoordinate.y;
            positions.z = transformedCoordinate.z;
        });
    }

    /**
     * Get color by RGB
     * @param point LASPoint
     * @return byte[3]
     */
    private byte[] getColorByRGB(LASPoint point) {
        double red = (double) point.getRed() / 65535;
        double green = (double) point.getGreen() / 65535;
        double blue = (double) point.getBlue() / 65535;

        byte[] rgb = new byte[3];
        rgb[0] = (byte) (red * 255);
        rgb[1] = (byte) (green * 255);
        rgb[2] = (byte) (blue * 255);
        return rgb;
    }

    /**
     * Get color by RGB
     * @param point LASPoint
     * @return byte[3]
     */
    private byte[] getColorByByteRGB(LASPoint point) {
        byte[] rgb = new byte[3];
        rgb[0] = (byte) point.getRed();
        rgb[1] = (byte) point.getGreen();
        rgb[2] = (byte) point.getBlue();

        return rgb;
    }

    /**
     * Get color by intensity (Gray scale)
     * @param point LASPoint
     * @return byte[3]
     */
    private byte[] getColorIntensity(LASPoint point) {
        char intensity = point.getIntensity();
        double intensityDouble = (double) intensity / 65535;

        byte color = (byte) (intensityDouble * 255);
        byte[] rgb = new byte[3];
        rgb[0] = color;
        rgb[1] = color;
        rgb[2] = color;
        return rgb;
    }
}
