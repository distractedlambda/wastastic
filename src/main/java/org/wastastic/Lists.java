package org.wastastic;

import java.util.List;

final class Lists {
    private Lists() {}

    static <E> E removeLast(List<E> list) {
        return list.remove(list.size() - 1);
    }

    static <E> void replaceLast(List<E> list, E newValue) {
        list.set(list.size() - 1, newValue);
    }
}
