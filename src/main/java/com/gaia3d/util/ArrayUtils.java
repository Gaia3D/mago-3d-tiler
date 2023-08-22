package com.gaia3d.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ArrayUtils
 * @author znkim
 * @since 1.0.0
 */
public class ArrayUtils {
    public static byte[] convertByteArrayToList(List<Byte> list) {
        byte[] array = new byte[list.size()];
        int num = 0;
        for (Byte s : list) {
            array[num++] = (s != null ? s : 0);
        }
        return array;
    }
    public static short[] convertShortArrayToList(List<Short> list) {
        short[] array = new short[list.size()];
        int num = 0;
        for (Short s : list) {
            array[num++] = (s != null ? s : 0);
        }
        return array;
    }
    public static int[] convertIntArrayToList(List<Integer> list) {
        int[] array = new int[list.size()];
        int num = 0;
        for (Integer i : list) {
            array[num++] = (i != null ? i : 0);
        }
        return array;
    }
    public static float[] convertFloatArrayToList(List<Float> list) {
        float[] array = new float[list.size()];
        int num = 0;
        for (Float f : list) {
            array[num++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    public static List<Byte> convertByteListToShortArray(byte[] array) {
        List<Byte> list = new ArrayList<>();
        for (byte s : array) {
            list.add(s);
        }
        return list;
    }
    public static List<Short> convertListToShortArray(short[] array) {
        List<Short> list = new ArrayList<>();
        for (short s : array) {
            list.add(s);
        }
        return list;
    }
    public static List<Integer> convertIntListToShortArray(short[] array) {
        return convertListToShortArray(array)
                .stream()
                .map(Short::intValue)
                .collect(Collectors.toList());
    }
    public static List<Integer> convertListToIntArray(int[] array) {
        return Arrays.stream(array)
                .boxed()
                .collect(Collectors.toList());
    }
    public static List<Float> convertListToFloatArray(float[] array) {
        List<Float> list = new ArrayList<>();
        for (float f : array) {
            list.add(f);
        }
        return list;
    }
}
