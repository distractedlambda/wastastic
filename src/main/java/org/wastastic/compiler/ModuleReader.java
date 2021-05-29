package org.wastastic.compiler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Byte.toUnsignedLong;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Float.intBitsToFloat;

public interface ModuleReader {
    byte nextByte() throws IOException;

    default void skip(int unsignedCount) throws IOException {
        while (unsignedCount != 0) {
            nextByte();
            unsignedCount--;
        }
    }

    default int nextSigned32() throws IOException {
        byte b;
        var total = 0;

        total |= (b = nextByte()) & 0x7f;
        if (b > 0) {
            if (b > 63) {
                total |= 0xffffff80;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 7;
        if (b > 0) {
            if (b > 63) {
                total |= 0xffffc000;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 14;
        if (b > 0) {
            if (b > 63) {
                total |= 0xffe00000;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 21;
        if (b > 0) {
            if (b > 63) {
                total |= 0xf0000000;
            }
            return total;
        }

        total |= nextByte() << 28;
        return total;
    }

    default int nextUnsigned32() throws IOException {
        byte b;
        var total = 0;

        total |= (b = nextByte()) & 0x7f;
        if (b > 0) {
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 7;
        if (b > 0) {
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 14;
        if (b > 0) {
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 21;
        if (b > 0) {
            return total;
        }

        total |= nextByte() << 28;
        return total;
    }

    default long nextSigned33() throws IOException {
        byte b;
        var total = 0;

        total |= (b = nextByte()) & 0x7f;
        if (b > 0) {
            if (b > 63) {
                total |= 0xffffffffffffff80L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 7;
        if (b > 0) {
            if (b > 63) {
                total |= 0xffffffffffffc000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 14;
        if (b > 0) {
            if (b > 63) {
                total |= 0xffffffffffe00000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 21;
        if (b > 0) {
            if (b > 63) {
                total |= 0xfffffffff0000000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 28;
        if (b > 63) {
            total |= 0xfffffff800000000L;
        }
        return total;
    }

    default long nextSigned64() throws IOException {
        byte b;
        var total = 0L;

        total |= (b = nextByte()) & 0x7f;
        if (b > 0) {
            if (b > 63) {
                total |= 0xffffffffffffff80L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 7;
        if (b > 0) {
            if (b > 63) {
                total |= 0xffffffffffffc000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 14;
        if (b > 0) {
            if (b > 63) {
                total |= 0xffffffffffe00000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 21;
        if (b > 0) {
            if (b > 63) {
                total |= 0xfffffffff0000000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 28;
        if (b > 0) {
            if (b > 63) {
                total |= 0xfffffff800000000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 35;
        if (b > 0) {
            if (b > 63) {
                total |= 0xfffffc0000000000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 42;
        if (b > 0) {
            if (b > 63) {
                total |= 0xfffe000000000000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 49;
        if (b > 0) {
            if (b > 63) {
                total |= 0xff00000000000000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 56;
        if (b > 0) {
            if (b > 63) {
                total |= 0x8000000000000000L;
            }
            return total;
        }

        total |= (long) nextByte() << 63;
        return total;
    }

    default long nextUnsigned64() throws IOException {
        byte b;
        var total = 0L;

        total |= (b = nextByte()) & 0x7f;
        if (b > 0) {
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 7;
        if (b > 0) {
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 14;
        if (b > 0) {
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 21;
        if (b > 0) {
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 28;
        if (b > 0) {
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 35;
        if (b > 0) {
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 42;
        if (b > 0) {
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 49;
        if (b > 0) {
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 56;
        if (b > 0) {
            return total;
        }

        total |= (long) nextByte() << 63;
        return total;
    }

    default float nextFloat32() throws IOException {
        var b0 = toUnsignedInt(nextByte());
        var b1 = toUnsignedInt(nextByte());
        var b2 = toUnsignedInt(nextByte());
        var b3 = toUnsignedInt(nextByte());
        var bits = (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
        return intBitsToFloat(bits);
    }

    default double nextFloat64() throws IOException {
        var b0 = toUnsignedLong(nextByte());
        var b1 = toUnsignedLong(nextByte());
        var b2 = toUnsignedLong(nextByte());
        var b3 = toUnsignedLong(nextByte());
        var b4 = toUnsignedLong(nextByte());
        var b5 = toUnsignedLong(nextByte());
        var b6 = toUnsignedLong(nextByte());
        var b7 = toUnsignedLong(nextByte());
        var bits = (b7 << 56) | (b6 << 48) | (b5 << 40) | (b4 << 32) | (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
        return longBitsToDouble(bits);
    }

    default String nextUtf8(int length) throws IOException {
        var bytes = new byte[length];

        for (var i = 0; i != length; i++) {
            bytes[i] = nextByte();
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }
}
