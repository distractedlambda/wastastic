package org.wastastic;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public final class Vectors {
    private Vectors() {}

    private static IllegalArgumentException makeNotAVectorException(Object vector, Class<?> elementType) {
        return new IllegalArgumentException(vector + " is not a vector of " + elementType);
    }

    public static Object requireVectorOf(Object vector, Class<?> elementType) {
        if (vector != null &&
            !elementType.isInstance(vector) &&
            !elementType.arrayType().isInstance(vector)
        ) {
            throw makeNotAVectorException(vector, elementType);
        }

        return vector;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> asList(Object vector, Class<? extends T> elementType) {
        if (vector == null) {
            return emptyList();
        } else if (elementType.isInstance(vector)) {
            return singletonList((T) vector);
        } else if (elementType.arrayType().isInstance(vector)) {
            return Arrays.asList((T[]) vector);
        } else {
            throw makeNotAVectorException(vector, elementType);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Stream<T> toStream(Object vector, Class<? extends T> elementType) {
        if (vector == null) {
            return Stream.empty();
        } else if (elementType.isInstance(vector)) {
            return Stream.of((T) vector);
        } else if (elementType.arrayType().isInstance(vector)) {
            return Arrays.stream((T[]) vector);
        } else {
            throw makeNotAVectorException(vector, elementType);
        }
    }

    public static boolean equals(Object lhs, Object rhs, Class<?> elementType) {
        if (lhs == rhs) {
            return true;
        } else if (lhs == null || rhs == null) {
            return false;
        } else if (elementType.isInstance(lhs)) {
            return elementType.isInstance(rhs) && lhs.equals(rhs);
        } else if (elementType.arrayType().isInstance(lhs)) {
            return elementType.arrayType().isInstance(rhs) && Arrays.equals((Object[]) lhs, (Object[]) rhs);
        } else {
            throw makeNotAVectorException(lhs, elementType);
        }
    }

    public static int hashCode(Object vector, Class<?> elementType) {
        if (vector == null) {
            return 0;
        } else if (elementType.isInstance(vector)) {
            return vector.hashCode();
        } else if (elementType.arrayType().isInstance(vector)) {
            return Arrays.hashCode((Object[]) vector);
        } else {
            throw makeNotAVectorException(vector, elementType);
        }
    }

    public static String toString(Object vector, Class<?> elementType) {
        if (vector == null) {
            return "[]";
        } else if (elementType.isInstance(vector)) {
            return '[' + vector.toString() + ']';
        } else if (elementType.arrayType().isInstance(vector)) {
            return Arrays.toString((Object[]) vector);
        } else {
            throw makeNotAVectorException(vector, elementType);
        }
    }
}
