package org.wastastic;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class Lists {
    private Lists() {}

    @Contract(pure = true) static <E> E first(@NotNull List<E> list) {
        return list.get(0);
    }

    static <E> E last(@NotNull List<E> list) {
        return list.get(list.size() - 1);
    }

    static void removeLast(@NotNull List<?> list, int count) {
        list.subList(list.size() - count, list.size()).clear();;
    }

    static <E> E removeLast(@NotNull List<E> list) {
        return list.remove(list.size() - 1);
    }
}
