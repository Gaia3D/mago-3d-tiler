package com.gaia3d.basic.pointcloud;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UniqueRandomNumbers {
    public static List<Integer> temp = null;

    public static List<Integer> generateUniqueRandom(int count, int max) {
        if (count > max) {
            throw new IllegalArgumentException("범위 내에서 중복 없는 값을 찾을 수 없습니다.");
        }
        Set<Integer> set = new LinkedHashSet<>();
        Random random = new Random();

        while (set.size() < count) {
            int num = random.nextInt(max);
            set.add(num);
        }
        return new ArrayList<>(set);
    }

    public static List<Integer> generateUniqueRandomCache(int count) {
        if (temp == null) {
            temp = IntStream.range(0, 100000000).boxed().collect(Collectors.toList());
            Collections.shuffle(temp);
        }

        int tempSize = temp.size();
        List<Integer> result = new ArrayList<>();
        Random random = new Random();
        while (result.size() < count) {
            int num = random.nextInt(tempSize);
            int value = temp.get(num);
            if (value > count) {
                continue;
            }
            result.add(value);
        }
        return result;
    }

    public static List<Integer> generateUniqueRandomCacheOld(int count) {
        List<Integer> result = IntStream.range(0, count).boxed().collect(Collectors.toList());
        Collections.shuffle(result);
        return result;
    }
}