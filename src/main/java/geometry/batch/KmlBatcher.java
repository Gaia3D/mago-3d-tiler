package geometry.batch;

import geometry.exchangable.GaiaSet;
import org.apache.commons.cli.CommandLine;
import tiler.BatchInfo;

import java.io.IOException;

public class KmlBatcher extends GaiaBatcher {
    public KmlBatcher(BatchInfo tileInfo, CommandLine command) {
        super(tileInfo, command);
    }

    @Override
    public GaiaSet batch() throws IOException {
        calcTranslation();
        return null;
    }
}
