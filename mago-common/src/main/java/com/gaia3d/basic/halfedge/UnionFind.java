package com.gaia3d.basic.halfedge;

import java.util.HashMap;
import java.util.Map;

public class UnionFind<T> {
    private final Map<T, T> parent = new HashMap<>();

    public void makeSet(T x) {
        parent.put(x, x);
    }

    public T find(T x) {
        T p = parent.get(x);
        if (p != x) {
            p = find(p);
            parent.put(x, p); // path compression
        }
        return p;
    }

    public void union(T a, T b) {
        T rootA = find(a);
        T rootB = find(b);

        if (rootA != rootB) {
            parent.put(rootB, rootA);
        }
    }
}
