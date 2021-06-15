package org.wastastic;

import java.util.List;

final class Lists {
    private Lists() {}

    static <E> E single(List<E> list) {
        if (list.size() != 1) {
            throw new IllegalArgumentException();
        }

        return list.get(0);
    }

    static <E> E first(List<E> list) {
        return list.get(0);
    }

    static <E> E last(List<E> list) {
        return list.get(list.size() - 1);
    }

    static void removeLast(List<?> list, int count) {
        list.subList(list.size() - count, list.size()).clear();;
    }

    static <E> E removeLast(List<E> list) {
        return list.remove(list.size() - 1);
    }

    static <E> void replaceLast(List<E> list, E newValue) {
        list.set(list.size() - 1, newValue);
    }
}
