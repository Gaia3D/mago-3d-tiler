package util;

/**
 * GeometryUtils
 * @author znkim
 * @since 1.0.0
 */
public class GeometryUtils {
    public static boolean isIdentity(float[] matrix) {
        return matrix[0] == 1 && matrix[1] == 0 && matrix[2] == 0 && matrix[3] == 0 &&
                matrix[4] == 0 && matrix[5] == 1 && matrix[6] == 0 && matrix[7] == 0 &&
                matrix[8] == 0 && matrix[9] == 0 && matrix[10] == 1 && matrix[11] == 0 &&
                matrix[12] == 0 && matrix[13] == 0 && matrix[14] == 0 && matrix[15] == 1;
    }
}
