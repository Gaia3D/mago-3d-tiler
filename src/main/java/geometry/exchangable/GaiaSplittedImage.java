package geometry.exchangable;

import geometry.basic.GaiaRectangle;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GaiaSplittedImage {
    int materialId;
    GaiaRectangle originalRectangle; // original rectangle size
    GaiaRectangle splittedRectangle; // batched rectangle size
}
