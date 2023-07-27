package command;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

@Builder
@Getter
@Setter
public class KmlInfo {
    private String name;
    private Vector3d position;
    //private double longitude;
    //private double latitude;
    //private double altitude;
    private String altitudeMode;
    private double heading;
    private double tilt;
    private double roll;
    private String href;
    private double scaleX;
    private double scaleY;
    private double scaleZ;
}
