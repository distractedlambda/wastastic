package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.wastastic.WasmOpcodes.OP_END;
import static org.wastastic.WasmOpcodes.OP_GLOBAL_GET;
import static org.wastastic.WasmOpcodes.OP_I32_CONST;
import static org.wastastic.WasmOpcodes.OP_I64_CONST;
import static org.wastastic.WasmOpcodes.OP_REF_FUNC;
import static org.wastastic.WasmOpcodes.OP_REF_NULL;
import static org.wastastic.WasmOpcodes.TYPE_EXTERNREF;
import static org.wastastic.WasmOpcodes.TYPE_F32;
import static org.wastastic.WasmOpcodes.TYPE_F64;
import static org.wastastic.WasmOpcodes.TYPE_FUNCREF;
import static org.wastastic.WasmOpcodes.TYPE_I32;
import static org.wastastic.WasmOpcodes.TYPE_I64;

final class WasmReader {
    private final @NotNull MemorySegment input;
    private long offset = 0;

    WasmReader(@NotNull MemorySegment input) {
        this.input = requireNonNull(input);
    }

    byte nextByte() {
        return (byte) Memory.VH_BYTE.get(input, offset++);
    }

    byte peekByte() {
        return (byte) Memory.VH_BYTE.get(input, offset);
    }

    int nextUnsigned32() {
        byte b;
        var total = 0;

        total |= (b = nextByte()) & 0x7f;
        if (b >= 0) {
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 7;
        if (b >= 0) {
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 14;
        if (b >= 0) {
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 21;
        if (b >= 0) {
            return total;
        }

        total |= nextByte() << 28;
        return total;
    }

    int nextSigned32() {
        byte b;
        var total = 0;

        total |= (b = nextByte()) & 0x7f;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffffff80;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 7;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffffc000;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 14;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffe00000;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 21;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xf0000000;
            }
            return total;
        }

        total |= nextByte() << 28;
        return total;
    }

    long nextSigned33() {
        byte b;
        var total = 0;

        total |= (b = nextByte()) & 0x7f;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffffffffffffff80L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 7;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffffffffffffc000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 14;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffffffffffe00000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 21;
        if (b >= 0) {
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

    long nextSigned64() {
        byte b;
        var total = 0L;

        total |= (b = nextByte()) & 0x7f;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffffffffffffff80L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 7;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffffffffffffc000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 14;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffffffffffe00000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 21;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xfffffffff0000000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 28;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xfffffff800000000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 35;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xfffffc0000000000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 42;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xfffe000000000000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 49;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xff00000000000000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 56;
        if (b >= 0) {
            if (b > 63) {
                total |= 0x8000000000000000L;
            }
            return total;
        }

        total |= (long) nextByte() << 63;
        return total;
    }

    float nextFloat32() {
        var value = (float) Memory.VH_FLOAT.get(input, offset);
        offset += 4;
        return value;
    }

    double nextFloat64() {
        var value = (double) Memory.VH_DOUBLE.get(input, offset);
        offset += 8;
        return value;
    }

    @NotNull String nextUtf8(int length) {
        var bytes = new byte[length];

        MemorySegment.ofArray(bytes).copyFrom(input.asSlice(offset, length));

        offset += length;
        return new String(bytes);
    }

    @NotNull ValueType nextValueType() throws TranslationException {
        var code = nextByte();
        return switch (code) {
            case TYPE_EXTERNREF -> ValueType.EXTERNREF;
            case TYPE_FUNCREF -> ValueType.FUNCREF;
            case TYPE_F64 -> ValueType.F64;
            case TYPE_F32 -> ValueType.F32;
            case TYPE_I64 -> ValueType.I64;
            case TYPE_I32 -> ValueType.I32;
            default -> throw new TranslationException("Invalid value type: " + Integer.toHexString(Byte.toUnsignedInt(code)));
        };
    }

    @NotNull String nextName() {
        return nextUtf8(nextUnsigned32());
    }

    @NotNull ValueType nextReferenceType() throws TranslationException {
        return switch (nextByte()) {
            case TYPE_EXTERNREF -> ValueType.EXTERNREF;
            case TYPE_FUNCREF -> ValueType.FUNCREF;
            default -> throw new TranslationException("Invalid reference type");
        };
    }

    @NotNull List<ValueType> nextResultType() throws TranslationException {
        var unsignedSize = nextUnsigned32();
        switch (unsignedSize) {
            case 0:
                return List.of();
            case 1:
                return List.of(nextValueType());
            default: {
                var array = new ValueType[unsignedSize];

                for (var i = 0; i != array.length; i++) {
                    array[i] = nextValueType();
                }

                return Arrays.asList(array);
            }
        }
    }

    @NotNull TableType nextTableType() throws TranslationException {
        return new TableType(nextReferenceType(), nextLimits());
    }

    @NotNull Limits nextLimits() throws TranslationException {
        return switch (nextByte()) {
            case 0x00 -> new Limits(nextUnsigned32());
            case 0x01 -> new Limits(nextUnsigned32(), nextUnsigned32());
            default -> throw new TranslationException("Invalid limits encoding");
        };
    }

    @NotNull MemoryType nextMemoryType() throws TranslationException {
        return new MemoryType(nextLimits());
    }

    int nextI32ConstantExpression() throws TranslationException {
        var value = switch (nextByte()) {
            case OP_GLOBAL_GET -> throw new TranslationException("TODO implement global.get constants");
            case OP_I32_CONST -> nextSigned32();
            default -> throw new TranslationException("Invalid i32 constant expression");
        };

        if (nextByte() != OP_END) {
            throw new TranslationException("Invalid i32 constant expression");
        }

        return value;
    }

    @NotNull Constant nextFunctionRefConstantExpression() throws TranslationException {
        var value = switch (nextByte()) {
            case OP_GLOBAL_GET -> throw new TranslationException("TODO implement global.get constants");
            case OP_REF_NULL -> NullConstant.INSTANCE;
            case OP_REF_FUNC -> new FunctionRefConstant(nextUnsigned32());
            default -> throw new TranslationException("Invalid funcref constant expression");
        };

        if (nextByte() != OP_END) {
            throw new TranslationException("Invalid funcref constant expression");
        }

        return value;
    }

    @NotNull Constant nextConstantExpression() throws TranslationException {
        var value = switch (nextByte()) {
            case OP_GLOBAL_GET -> throw new TranslationException("TODO implement global.get constants");
            case OP_I32_CONST -> new I32Constant(nextSigned32());
            case OP_I64_CONST -> new I64Constant(nextSigned64());
            case OP_REF_NULL -> NullConstant.INSTANCE;
            case OP_REF_FUNC -> new FunctionRefConstant(nextUnsigned32());
            default -> throw new TranslationException("Invalid constant expression");
        };

        if (nextByte() != OP_END) {
            throw new TranslationException("Invalid constant expression");
        }

        return value;
    }

    void nextElementKind() throws TranslationException {
        // FIXME this should be getting used somewhere
        if (nextByte() != 0) {
            throw new TranslationException("Unsupported elemkind");
        }
    }

    @NotNull Constant nextExternRefConstantExpression() throws TranslationException {
        var value = switch (nextByte()) {
            case OP_GLOBAL_GET -> throw new TranslationException("TODO implement global.get constants");
            case OP_REF_NULL -> NullConstant.INSTANCE;
            default -> throw new TranslationException("Invalid externref constant expression");
        };

        if (nextByte() != OP_END) {
            throw new TranslationException("Invalid externref constant expression");
        }

        return value;
    }

    boolean hasRemaining() {
        return offset != input.byteSize();
    }

    @NotNull MemorySegment nextSlice(long size) {
        var slice = input.asSlice(offset, size);
        offset += size;
        return slice;
    }
}
