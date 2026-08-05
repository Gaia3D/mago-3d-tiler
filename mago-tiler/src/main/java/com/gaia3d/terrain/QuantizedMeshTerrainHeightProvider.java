package com.gaia3d.terrain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;

@Slf4j
public class QuantizedMeshTerrainHeightProvider implements TerrainHeightProvider {
    private static final int TILE_CACHE_SIZE = 128;
    private static final double MAX_MERCATOR_LATITUDE = 85.0511287798066;

    private final Path layerDirectory;
    private final String tileTemplate;
    private final String version;
    private final int minimumZoom;
    private final int maximumZoom;
    private final Projection projection;
    private final Scheme scheme;
    private final Map<TileKey, QuantizedMeshTile> tileCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<TileKey, QuantizedMeshTile> eldest) {
            return size() > TILE_CACHE_SIZE;
        }
    };

    public QuantizedMeshTerrainHeightProvider(Path layerJson) throws IOException {
        this(layerJson.toFile());
    }

    public QuantizedMeshTerrainHeightProvider(File layerJson) throws IOException {
        if (!layerJson.isFile()) {
            throw new IOException("Quantized Mesh layer.json does not exist: " + layerJson);
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(layerJson);
        JsonNode tiles = root.path("tiles");
        if (!tiles.isArray() || tiles.isEmpty()) {
            throw new IOException("Quantized Mesh layer.json has no tile template: " + layerJson);
        }

        this.layerDirectory = layerJson.toPath().toAbsolutePath().normalize().getParent();
        this.tileTemplate = tiles.get(0).asText();
        this.version = root.path("version").asText("1.0.0");
        this.minimumZoom = root.path("minzoom").asInt(0);
        this.maximumZoom = determineMaximumZoom(root);
        validateZoom(minimumZoom);
        if (minimumZoom > maximumZoom) {
            throw new IOException("Quantized Mesh minzoom is greater than maxzoom");
        }
        this.projection = Projection.from(root.path("projection").asText("EPSG:4326"));
        this.scheme = Scheme.from(root.path("scheme").asText("tms"));
        log.info("Loaded Quantized Mesh layer: projection={}, scheme={}, zoom={}-{}",
                projection, scheme, minimumZoom, maximumZoom);
    }

    @Override
    public OptionalDouble sample(double longitude, double latitude) {
        if (!Double.isFinite(longitude) || !Double.isFinite(latitude)
                || longitude < -180.0 || longitude > 180.0
                || latitude < -90.0 || latitude > 90.0) {
            return OptionalDouble.empty();
        }

        for (int zoom = maximumZoom; zoom >= minimumZoom; zoom--) {
            TileLocation location = locate(longitude, latitude, zoom);
            TileKey key = new TileKey(zoom, location.x(), location.encodedY());
            QuantizedMeshTile tile = getTile(key);
            if (tile != null) {
                OptionalDouble height = tile.sample(location.localU(), location.localV());
                if (height.isPresent()) {
                    return height;
                }
            }
        }
        return OptionalDouble.empty();
    }

    private synchronized QuantizedMeshTile getTile(TileKey key) {
        QuantizedMeshTile cached = tileCache.get(key);
        if (cached != null) {
            return cached;
        }

        Path tilePath = resolveTilePath(key);
        if (!Files.isRegularFile(tilePath)) {
            return null;
        }
        try {
            QuantizedMeshTile tile = QuantizedMeshTile.decode(Files.readAllBytes(tilePath));
            tileCache.put(key, tile);
            return tile;
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Failed to decode Quantized Mesh tile: " + tilePath, e);
        }
    }

    private Path resolveTilePath(TileKey key) {
        String relative = tileTemplate
                .replace("{z}", Integer.toString(key.zoom()))
                .replace("{x}", Integer.toString(key.x()))
                .replace("{y}", Integer.toString(key.y()))
                .replace("{version}", version);
        int queryIndex = relative.indexOf('?');
        if (queryIndex >= 0) {
            relative = relative.substring(0, queryIndex);
        }
        relative = relative.replace('/', File.separatorChar);
        while (relative.startsWith(File.separator)) {
            relative = relative.substring(1);
        }
        Path resolved = layerDirectory.resolve(relative).normalize();
        if (!resolved.startsWith(layerDirectory)) {
            throw new IllegalArgumentException("Quantized Mesh tile path escapes layer directory: " + relative);
        }
        return resolved;
    }

    private TileLocation locate(double longitude, double latitude, int zoom) {
        double normalizedX = Math.min(Math.nextDown(1.0), Math.max(0.0, (longitude + 180.0) / 360.0));
        double normalizedSouthY;
        int xCount;
        int yCount;
        if (projection == Projection.EPSG_4326) {
            normalizedSouthY = Math.min(Math.nextDown(1.0), Math.max(0.0, (latitude + 90.0) / 180.0));
            xCount = 1 << (zoom + 1);
            yCount = 1 << zoom;
        } else {
            double clampedLatitude = Math.max(-MAX_MERCATOR_LATITUDE, Math.min(MAX_MERCATOR_LATITUDE, latitude));
            double latitudeRadians = Math.toRadians(clampedLatitude);
            double normalizedNorthY = (1.0 - Math.log(Math.tan(latitudeRadians) + 1.0 / Math.cos(latitudeRadians)) / Math.PI) * 0.5;
            normalizedSouthY = Math.min(Math.nextDown(1.0), Math.max(0.0, 1.0 - normalizedNorthY));
            xCount = 1 << zoom;
            yCount = 1 << zoom;
        }

        double scaledX = normalizedX * xCount;
        double scaledSouthY = normalizedSouthY * yCount;
        int x = Math.min(xCount - 1, (int) Math.floor(scaledX));
        int tmsY = Math.min(yCount - 1, (int) Math.floor(scaledSouthY));
        int encodedY = scheme == Scheme.TMS ? tmsY : yCount - 1 - tmsY;
        return new TileLocation(x, encodedY, scaledX - x, scaledSouthY - tmsY);
    }

    private static int determineMaximumZoom(JsonNode root) throws IOException {
        if (root.has("maxzoom")) {
            int maxZoom = root.path("maxzoom").asInt();
            validateZoom(maxZoom);
            return maxZoom;
        }
        JsonNode available = root.path("available");
        if (available.isArray() && !available.isEmpty()) {
            int maxZoom = available.size() - 1;
            validateZoom(maxZoom);
            return maxZoom;
        }
        throw new IOException("Quantized Mesh layer.json must define maxzoom or available levels");
    }

    private static void validateZoom(int zoom) throws IOException {
        if (zoom < 0 || zoom > 29) {
            throw new IOException("Unsupported Quantized Mesh zoom level: " + zoom);
        }
    }

    private enum Projection {
        EPSG_4326,
        EPSG_3857;

        static Projection from(String value) throws IOException {
            String normalized = value.toUpperCase(Locale.ROOT).replace("::", ":");
            return switch (normalized) {
                case "EPSG:4326", "EPSG4326" -> EPSG_4326;
                case "EPSG:3857", "EPSG3857" -> EPSG_3857;
                default -> throw new IOException("Unsupported Quantized Mesh projection: " + value);
            };
        }
    }

    private enum Scheme {
        TMS,
        SLIPPY_MAP;

        static Scheme from(String value) throws IOException {
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "tms" -> TMS;
                case "slippymap", "slippy_map", "slippy-map" -> SLIPPY_MAP;
                default -> throw new IOException("Unsupported Quantized Mesh scheme: " + value);
            };
        }
    }

    private record TileKey(int zoom, int x, int y) {
    }

    private record TileLocation(int x, int encodedY, double localU, double localV) {
    }
}
