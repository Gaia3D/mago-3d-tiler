package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArrayUtils {
    public static short[] convertShortArrayToArrayList(List<Short> list) {
        short[] array = new short[list.size()];
        int num = 0;
        for (Short s : list) {
            array[num++] = (s != null ? s : 0);
        }
        return array;
    }

    public static int[] convertIntArrayToArrayList(List<Integer> list) {
        int[] array = new int[list.size()];
        int num = 0;
        for (Integer i : list) {
            array[num++] = (i != null ? i : 0);;
        }
        return array;
    }

    public static float[] convertFloatArrayToArrayList(List<Float> list) {
        float[] array = new float[list.size()];
        int num = 0;
        for (Float f : list) {
            array[num++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    public static List<Short> convertArrayListToShortArray(short[] array) {
        List<Short> shorts = new ArrayList<>();
        for (short s : array) {
            shorts.add(s);
        }
        return shorts;
    }

    public static List<Integer> convertIntArrayListToShortArray(short[] array) {
        List<Integer> integers = new ArrayList<>();
        for (short s : array) {
            integers.add((int) s);
        }
        return integers;
    }

    public static List<Integer> convertArrayListToIntArray(int[] array) {
        List<Integer> integers = new ArrayList<>();
        for (int i : array) {
            integers.add(i);
        }
        return integers;
    }

    public static List<Float> convertArrayListToFloatArray(float[] array) {
        List<Float> floats = new ArrayList<>();
        for (float f : array) {
            floats.add(f);
        }
        return floats;
    }

    /*public static short[] convertColorsVBO(float[] list) {
        short[] array = new short[list.length];
        int num = 0;
        for (float f : list) {
            array[num++] = (short) (f * 255);
        }
        return array;
    }*/
}
