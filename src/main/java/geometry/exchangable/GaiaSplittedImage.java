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
    GaiaRectangle originalRectangle;
    GaiaRectangle splittedRectangle;
}
