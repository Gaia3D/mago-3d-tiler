package tiler;

import geometry.types.FormatType;
import lombok.Builder;
import lombok.Getter;
import org.locationtech.proj4j.CoordinateReferenceSystem;

import java.nio.file.Path;

@Getter
@Builder
public class TilerInfo {
    private final Path inputPath;
    private final Path outputPath;
    private final FormatType inputFormatType;
    private final CoordinateReferenceSystem source;
}
