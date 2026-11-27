package dev.cubo1ndev.categorychest;

import java.util.*;
import java.util.function.BiPredicate;

public final class Remap {
    private Remap() {}

    /** For each old index, its index in neu (same slot first, else the first free match), or -1 if gone. */
    public static <T> int[] remap(List<T> old, List<T> neu, BiPredicate<T, T> same) {
        int[] map = new int[old.size()];
        boolean[] taken = new boolean[neu.size()];
        Arrays.fill(map, -1);
        for (int i = 0; i < old.size(); i++)
            if (i < neu.size() && same.test(old.get(i), neu.get(i))) { map[i] = i; taken[i] = true; }
        for (int i = 0; i < old.size(); i++) {
            if (map[i] >= 0) continue;
            for (int j = 0; j < neu.size(); j++)
                if (!taken[j] && same.test(old.get(i), neu.get(j))) { map[i] = j; taken[j] = true; break; }
        }
        return map;
    }

    /** Each player's hidden indices moved through map; removed ones dropped, empty players dropped. */
    public static Map<UUID, List<Integer>> remapHidden(Map<UUID, List<Integer>> hidden, int[] map) {
        Map<UUID, List<Integer>> out = new HashMap<>();
        hidden.forEach((id, idx) -> {
            List<Integer> moved = new ArrayList<>();
            for (int i : idx) if (i >= 0 && i < map.length && map[i] >= 0) moved.add(map[i]);
            Collections.sort(moved);
            if (!moved.isEmpty()) out.put(id, moved);
        });
        return out;
    }

/** The categories beyond what the chest shows; taken once when it opens and kept after the chest's items. */
    public static <T> List<T> overflow(List<T> old, int slots) {
        return old.size() > slots ? List.copyOf(old.subList(slots, old.size())) : List.of();
    }

    private static void check(boolean ok) { if (!ok) throw new AssertionError(); }

    public static void main(String[] args) {
        BiPredicate<String, String> eq = String::equals;
        UUID u = new UUID(0, 1);
        check(Arrays.equals(remap(List.of("a", "b", "c"), List.of("c", "a", "b"), eq), new int[]{1, 2, 0}));
        check(Arrays.equals(remap(List.of("a", "b"), List.of("b"), eq), new int[]{-1, 0}));
        check(Arrays.equals(remap(List.of("a", "a"), List.of("x", "a"), eq), new int[]{-1, 1})); // duplicates: claimed once, same slot first
        check(Arrays.equals(remap(List.of("a", "b"), List.of("a", "b"), eq), new int[]{0, 1}));
        check(remapHidden(Map.of(u, List.of(0, 1)), new int[]{1, -1}).equals(Map.of(u, List.of(1))));
        check(remapHidden(Map.of(u, List.of(1)), new int[]{0, -1}).isEmpty());
        check(overflow(List.of("1", "2", "3"), 2).equals(List.of("3")));
        check(overflow(List.of("1"), 2).isEmpty());
        // two edits in a row keep every extra: the tail is taken once, from the list the chest opened on
        List<String> extra = overflow(List.of("1", "2", "3", "4"), 2), first = new ArrayList<>(List.of("2"));
        first.addAll(extra);
        List<String> second = new ArrayList<>(List.of("2", "9"));
        second.addAll(extra);
        check(first.equals(List.of("2", "3", "4")) && second.equals(List.of("2", "9", "3", "4")));
        System.out.println("remap checks passed");
    }
}
