package geometry.batch;

import geometry.exchangable.GaiaSet;

import java.io.IOException;

public interface Batcher {
    GaiaSet batch() throws IOException;
}
