package com.gaia3d.process.postprocess.batch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * MaxRects strip packer (bottom-left rule). Several strip widths and placement orders are tried and the layout
 * with the shortest longest side is kept, because the batched atlas is capped and downscaled by its longest side.
 */
final class GaiaAtlasPacker {
    private static final int STRIP_WIDTH_STEPS = 21; // sqrt(area) * 1.00, 1.05, ... 2.00

    record Result(int[] xs, int[] ys, int width, int height) {
        int longestSide() {
            return Math.max(width, height);
        }
    }

    private GaiaAtlasPacker() {
    }

    static Result pack(int[] widths, int[] heights) {
        int count = widths.length;
        if (count == 0) {
            return new Result(new int[0], new int[0], 0, 0);
        }
        long totalArea = 0;
        int maxWidth = 0;
        for (int i = 0; i < count; i++) {
            totalArea += (long) widths[i] * heights[i];
            maxWidth = Math.max(maxWidth, widths[i]);
        }

        List<Comparator<Integer>> orderings = List.of(
                Comparator.<Integer>comparingInt(i -> Math.max(widths[i], heights[i])).thenComparingLong(i -> (long) widths[i] * heights[i]).reversed(),
                Comparator.<Integer>comparingInt(i -> heights[i]).thenComparingInt(i -> widths[i]).reversed(),
                Comparator.<Integer>comparingLong(i -> (long) widths[i] * heights[i]).reversed());

        double squareSide = Math.sqrt(totalArea);
        Result best = null;
        for (Comparator<Integer> ordering : orderings) {
            Integer[] order = IntStream.range(0, count).boxed().sorted(ordering).toArray(Integer[]::new);
            int previousStripWidth = -1;
            for (int step = 0; step < STRIP_WIDTH_STEPS; step++) {
                int stripWidth = Math.max(maxWidth, (int) Math.ceil(squareSide * (1.0 + 0.05 * step)));
                if (stripWidth == previousStripWidth) {
                    continue;
                }
                previousStripWidth = stripWidth;
                Result result = packStrip(widths, heights, order, stripWidth);
                if (best == null || result.longestSide() < best.longestSide()
                        || (result.longestSide() == best.longestSide() && (long) result.width * result.height < (long) best.width * best.height)) {
                    best = result;
                }
            }
        }
        return best;
    }

    private static Result packStrip(int[] widths, int[] heights, Integer[] order, int stripWidth) {
        int count = widths.length;
        int[] xs = new int[count];
        int[] ys = new int[count];
        int stripHeight = Arrays.stream(heights).sum();
        List<int[]> freeRects = new ArrayList<>(); // {x, y, w, h}
        freeRects.add(new int[]{0, 0, stripWidth, stripHeight});

        int usedWidth = 0;
        int usedHeight = 0;
        for (int index : order) {
            int w = widths[index];
            int h = heights[index];
            int[] target = null;
            for (int[] free : freeRects) {
                if (free[2] < w || free[3] < h) {
                    continue;
                }
                if (target == null || free[1] < target[1] || (free[1] == target[1] && free[0] < target[0])) {
                    target = free;
                }
            }
            if (target == null) {
                throw new IllegalStateException("No free space for atlas image " + w + "x" + h + " in strip width " + stripWidth);
            }
            int[] placed = {target[0], target[1], w, h};
            xs[index] = placed[0];
            ys[index] = placed[1];
            usedWidth = Math.max(usedWidth, placed[0] + w);
            usedHeight = Math.max(usedHeight, placed[1] + h);
            splitFreeRects(freeRects, placed);
        }
        return new Result(xs, ys, usedWidth, usedHeight);
    }

    private static void splitFreeRects(List<int[]> freeRects, int[] used) {
        List<int[]> created = new ArrayList<>();
        Iterator<int[]> iterator = freeRects.iterator();
        while (iterator.hasNext()) {
            int[] free = iterator.next();
            if (used[0] >= free[0] + free[2] || used[0] + used[2] <= free[0]
                    || used[1] >= free[1] + free[3] || used[1] + used[3] <= free[1]) {
                continue;
            }
            iterator.remove();
            if (used[0] > free[0]) {
                created.add(new int[]{free[0], free[1], used[0] - free[0], free[3]});
            }
            if (used[0] + used[2] < free[0] + free[2]) {
                created.add(new int[]{used[0] + used[2], free[1], free[0] + free[2] - used[0] - used[2], free[3]});
            }
            if (used[1] > free[1]) {
                created.add(new int[]{free[0], free[1], free[2], used[1] - free[1]});
            }
            if (used[1] + used[3] < free[1] + free[3]) {
                created.add(new int[]{free[0], used[1] + used[3], free[2], free[1] + free[3] - used[1] - used[3]});
            }
        }

        // keep only maximal rectangles: drop new ones covered by any other, then old ones covered by a new one
        List<int[]> kept = new ArrayList<>();
        for (int i = 0; i < created.size(); i++) {
            int[] rect = created.get(i);
            boolean covered = freeRects.stream().anyMatch(other -> contains(other, rect));
            for (int j = 0; j < created.size() && !covered; j++) {
                int[] other = created.get(j);
                covered = i != j && contains(other, rect) && (!contains(rect, other) || j < i);
            }
            if (!covered) {
                kept.add(rect);
            }
        }
        freeRects.removeIf(old -> kept.stream().anyMatch(rect -> contains(rect, old)));
        freeRects.addAll(kept);
    }

    private static boolean contains(int[] outer, int[] inner) {
        return inner[0] >= outer[0] && inner[1] >= outer[1]
                && inner[0] + inner[2] <= outer[0] + outer[2] && inner[1] + inner[3] <= outer[1] + outer[3];
    }
}
