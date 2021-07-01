package org.wastastic;

import static org.objectweb.asm.Type.getInternalName;
import static org.wastastic.Names.methodDescriptor;

final class InstructionImpls {
    private InstructionImpls() {}

    static final String INTERNAL_NAME = getInternalName(InstructionImpls.class);

    static final String I32_DIV_S_NAME = "i32DivS";
    static final String I32_DIV_S_DESCRIPTOR = methodDescriptor(int.class, int.class, int.class);

    @SuppressWarnings("unused")
    static int i32DivS(int lhs, int rhs) throws TrapException {
        if (rhs == 0 || (lhs == Integer.MIN_VALUE && rhs == -1)) {
            throw new TrapException();
        }

        return lhs / rhs;
    }

    static final String I64_DIV_S_NAME = "i64DivS";
    static final String I64_DIV_S_DESCRIPTOR = methodDescriptor(long.class, long.class, long.class);

    @SuppressWarnings("unused")
    static long i64DivS(long lhs, long rhs) throws TrapException {
        if (rhs == 0 || (lhs == Long.MIN_VALUE && rhs == -1)) {
            throw new TrapException();
        }

        return lhs / rhs;
    }

    static final String F32_TRUNC_NAME = "f32Trunc";
    static final String F32_TRUNC_DESCRIPTOR = methodDescriptor(float.class, float.class);

    @SuppressWarnings("unused")
    static float f32Trunc(float operand) {
        return operand < 0f ? (float) Math.ceil(operand) : (float) Math.floor(operand);
    }

    static final String F64_TRUNC_NAME = "f64Trunc";
    static final String F64_TRUNC_DESCRIPTOR = methodDescriptor(double.class, double.class);

    @SuppressWarnings("unused")
    static double f64Trunc(double operand) {
        return operand < 0.0 ? Math.ceil(operand) : Math.floor(operand);
    }

    static final String I32_TRUNC_F32_S_NAME = "i32TruncF32S";
    static final String I32_TRUNC_F32_S_DESCRIPTOR = methodDescriptor(int.class, float.class);

    @SuppressWarnings("unused")
    static int i32TruncF32S(float operand) throws TrapException {
        if (Float.isNaN(operand) || operand < -0x1p31f || operand >= 0x1p31f) {
            throw new TrapException();
        }

        return (int) operand;
    }

    static final String I32_TRUNC_F32_U_NAME = "i32TruncF32U";
    static final String I32_TRUNC_F32_U_DESCRIPTOR = methodDescriptor(int.class, float.class);

    @SuppressWarnings("unused")
    static int i32TruncF32U(float operand) throws TrapException {
        if (Float.isNaN(operand) || operand <= -1f || operand >= 0x1p32f) {
            throw new TrapException();
        }

        return (int) (long) operand;
    }

    static final String I32_TRUNC_F64_S_NAME = "i32TruncF64S";
    static final String I32_TRUNC_F64_S_DESCRIPTOR = methodDescriptor(int.class, double.class);

    @SuppressWarnings("unused")
    static int i32TruncF64S(double operand) throws TrapException {
        if (Double.isNaN(operand) || operand <= -0x1.00000002p31 || operand >= 0x1p31) {
            throw new TrapException();
        }

        return (int) operand;
    }

    static final String I32_TRUNC_F64_U_NAME = "i32TruncF64U";
    static final String I32_TRUNC_F64_U_DESCRIPTOR = methodDescriptor(int.class, double.class);

    @SuppressWarnings("unused")
    static int i32TruncF64U(double operand) throws TrapException {
        if (Double.isNaN(operand) || operand <= -1.0 || operand >= 0x1p32) {
            throw new TrapException();
        }

        return (int) (long) operand;
    }

    static final String I64_TRUNC_F32_S_NAME = "i64TruncF32S";
    static final String I64_TRUNC_F32_S_DESCRIPTOR = methodDescriptor(long.class, float.class);

    @SuppressWarnings("unused")
    static long i64TruncF32S(float operand) throws TrapException {
        if (Float.isNaN(operand) || operand < -0x1p63f || operand >= 0x1p63f) {
            throw new TrapException();
        }

        return (long) operand;
    }

    static final String I64_TRUNC_F32_U_NAME = "i64TruncF32U";
    static final String I64_TRUNC_F32_U_DESCRIPTOR = methodDescriptor(long.class, float.class);

    @SuppressWarnings("unused")
    static long i64TruncF32U(float operand) throws TrapException {
        if (Float.isNaN(operand) || operand <= -1f || operand >= 0x1p64f) {
            throw new TrapException();
        }

        if (operand >= 0x1p63f) {
            return ((long) Math.scalb(operand, -1)) << 1;
        } else {
            return (long) operand;
        }
    }

    static final String I64_TRUNC_F64_S_NAME = "i64TruncF64S";
    static final String I64_TRUNC_F64_S_DESCRIPTOR = methodDescriptor(long.class, double.class);

    @SuppressWarnings("unused")
    static long i64TruncF64S(double operand) throws TrapException {
        if (Double.isNaN(operand) || operand < -0x1p63 || operand >= 0x1p63) {
            throw new TrapException();
        }

        return (long) operand;
    }

    static final String I64_TRUNC_F64_U_NAME = "i64TruncF64U";
    static final String I64_TRUNC_F64_U_DESCRIPTOR = methodDescriptor(long.class, double.class);

    @SuppressWarnings("unused")
    static long i64TruncF64U(double operand) throws TrapException {
        if (Double.isNaN(operand) || operand <= -1.0 || operand >= 0x1p64) {
            throw new TrapException();
        }

        if (operand >= 0x1p63) {
            return ((long) Math.scalb(operand, -1)) << 1;
        } else {
            return (long) operand;
        }
    }

    static final String F32_CONVERT_I64_U_NAME = "f32ConvertI64U";
    static final String F32_CONVERT_I64_U_DESCRIPTOR = methodDescriptor(float.class, long.class);

    @SuppressWarnings("unused")
    static float f32ConvertI64U(long operand) {
        if (operand < 0) {
            return Math.scalb((float) (operand >>> 1), 1);
        }
        else {
            return (float) operand;
        }
    }

    static final String F64_CONVERT_I64_U_NAME = "f64ConvertI64U";
    static final String F64_CONVERT_I64_U_DESCRIPTOR = methodDescriptor(double.class, long.class);

    @SuppressWarnings("unused")
    static double f64ConvertI64U(long operand) {
        if (operand < 0) {
            return Math.scalb((double) (operand >>> 1), 1);
        }
        else {
            return (double) operand;
        }
    }

    static final String I32_TRUNC_SAT_F32_U_NAME = "i32TruncSatF32U";
    static final String I32_TRUNC_SAT_F32_U_DESCRIPTOR = methodDescriptor(int.class, float.class);

    @SuppressWarnings("unused")
    static int i32TruncSatF32U(float operand) {
        return (int) Math.min(Math.max(0, (long) operand), Integer.toUnsignedLong(-1));
    }

    static final String I32_TRUNC_SAT_F64_U_NAME = "i32TruncSatF64U";
    static final String I32_TRUNC_SAT_F64_U_DESCRIPTOR = methodDescriptor(int.class, double.class);

    @SuppressWarnings("unused")
    static int i32TruncSatF64U(double operand) {
        return (int) Math.min(Math.max(0, (long) operand), Integer.toUnsignedLong(-1));
    }

    static final String I64_TRUNC_SAT_F32_U_NAME = "i64TruncSatF32U";
    static final String I64_TRUNC_SAT_F32_U_DESCRIPTOR = methodDescriptor(long.class, float.class);

    @SuppressWarnings("unused")
    static long i64TruncSatF32U(float operand) {
        if (operand >= 0x1p63f) {
            return ((long) Math.scalb(operand, -1) << 1);
        }
        else {
            return (long) operand;
        }
    }

    static final String I64_TRUNC_SAT_F64_U_NAME = "i64TruncSatF64U";
    static final String I64_TRUNC_SAT_F64_U_DESCRIPTOR = methodDescriptor(long.class, double.class);

    @SuppressWarnings("unused")
    static long i64TruncSatF64U(double operand) {
        if (operand >= 0x1p63) {
            return ((long) Math.scalb(operand, -1) << 1);
        }
        else {
            return (long) operand;
        }
    }
}
