package org.wastastic.runtime;

import org.wastastic.TrapException;

public final class InstructionHelpers {
    private InstructionHelpers() {}

    public static RuntimeException trap() throws TrapException {
        throw new TrapException();
    }

    public static int select(int condition, int ifTrue, int ifFalse) {
        if (condition != 0) {
            return ifTrue;
        } else {
            return ifFalse;
        }
    }

    public static float select(int condition, float ifTrue, float ifFalse) {
        if (condition != 0) {
            return ifTrue;
        } else {
            return ifFalse;
        }
    }

    public static long select(int condition, long ifTrue, long ifFalse) {
        if (condition != 0) {
            return ifTrue;
        } else {
            return ifFalse;
        }
    }

    public static double select(int condition, double ifTrue, double ifFalse) {
        if (condition != 0) {
            return ifTrue;
        } else {
            return ifFalse;
        }
    }

    public static Object select(int condition, Object ifTrue, Object ifFalse) {
        if (condition != 0) {
            return ifTrue;
        } else {
            return ifFalse;
        }
    }

    public static boolean refIsNull(Object ref) {
        return ref == null;
    }

    public static int divU(int lhs, int rhs) {
        if (rhs == 0) {
            throw new TrapException();
        }

        return Integer.divideUnsigned(lhs, rhs);
    }

    public static int divS(int lhs, int rhs) {
        if (rhs == 0 || (lhs == Integer.MIN_VALUE && rhs == -1)) {
            throw new TrapException();
        }

        return lhs / rhs;
    }

    public static long divU(long lhs, long rhs) {
        if (rhs == 0) {
            throw new TrapException();
        }

        return Long.divideUnsigned(lhs, rhs);
    }

    public static long divS(long lhs, long rhs) {
        if (rhs == 0 || (lhs == Long.MIN_VALUE && rhs == -1)) {
            throw new TrapException();
        }

        return lhs / rhs;
    }

    public static int remU(int lhs, int rhs) {
        if (rhs == 0) {
            throw new TrapException();
        }

        return Integer.remainderUnsigned(lhs, rhs);
    }

    public static long remU(long lhs, long rhs) {
        if (rhs == 0) {
            throw new TrapException();
        }

        return Long.remainderUnsigned(lhs, rhs);
    }

    public static int remS(int lhs, int rhs) {
        if (rhs == 0) {
            throw new TrapException();
        }

        return lhs % rhs;
    }

    public static long remS(long lhs, long rhs) {
        if (rhs == 0) {
            throw new TrapException();
        }

        return lhs % rhs;
    }

    public static boolean eqz(int operand) {
        return operand == 0;
    }

    public static boolean eqz(long operand) {
        return operand == 0;
    }

    public static boolean eq(int lhs, int rhs) {
        return lhs == rhs;
    }

    public static boolean eq(long lhs, long rhs) {
        return lhs == rhs;
    }

    public static boolean ne(int lhs, int rhs) {
        return lhs != rhs;
    }

    public static boolean ne(long lhs, long rhs) {
        return lhs != rhs;
    }

    public static boolean ltu(int lhs, int rhs) {
        return Integer.compareUnsigned(lhs, rhs) < 0;
    }

    public static boolean ltu(long lhs, long rhs) {
        return Long.compareUnsigned(lhs, rhs) < 0;
    }

    public static boolean lts(int lhs, int rhs) {
        return lhs < rhs;
    }

    public static boolean lts(long lhs, long rhs) {
        return lhs < rhs;
    }

    public static boolean gtu(int lhs, int rhs) {
        return Integer.compareUnsigned(lhs, rhs) > 0;
    }

    public static boolean gtu(long lhs, long rhs) {
        return Long.compareUnsigned(lhs, rhs) > 0;
    }

    public static boolean gts(int lhs, int rhs) {
        return lhs > rhs;
    }

    public static boolean gts(long lhs, long rhs) {
        return lhs > rhs;
    }

    public static boolean leu(int lhs, int rhs) {
        return Integer.compareUnsigned(lhs, rhs) <= 0;
    }

    public static boolean leu(long lhs, long rhs) {
        return Long.compareUnsigned(lhs, rhs) <= 0;
    }

    public static boolean les(int lhs, int rhs) {
        return lhs <= rhs;
    }

    public static boolean les(long lhs, long rhs) {
        return lhs <= rhs;
    }

    public static boolean geu(int lhs, int rhs) {
        return Integer.compareUnsigned(lhs, rhs) >= 0;
    }

    public static boolean geu(long lhs, long rhs) {
        return Long.compareUnsigned(lhs, rhs) >= 0;
    }

    public static boolean ges(int lhs, int rhs) {
        return lhs >= rhs;
    }

    public static boolean ges(long lhs, long rhs) {
        return lhs >= rhs;
    }

    public static long rotl(long lhs, long rhs) {
        return Long.rotateLeft(lhs, (int) rhs);
    }

    public static long rotr(long lhs, long rhs) {
        return Long.rotateRight(lhs, (int) rhs);
    }

    public static long clz(long operand) {
        return Long.numberOfLeadingZeros(operand);
    }

    public static long ctz(long operand) {
        return Long.numberOfTrailingZeros(operand);
    }

    public static long popcnt(long operand) {
        return Long.bitCount(operand);
    }

    public static float fsqrt(float operand) {
        return (float) Math.sqrt(operand);
    }

    public static float fceil(float operand) {
        return (float) Math.ceil(operand);
    }

    public static float ffloor(float operand) {
        return (float) Math.floor(operand);
    }

    public static float ftrunc(float operand) {
        return operand < 0f ? (float) Math.ceil(operand) : (float) Math.floor(operand);
    }

    public static double ftrunc(double operand) {
        return operand < 0.0 ? Math.ceil(operand) : Math.floor(operand);
    }

    public static float fnearest(float operand) {
        return (float) Math.rint(operand);
    }

    public static boolean feq(float lhs, float rhs) {
        return lhs == rhs;
    }

    public static boolean feq(double lhs, double rhs) {
        return lhs == rhs;
    }

    public static boolean fne(float lhs, float rhs) {
        return lhs != rhs;
    }

    public static boolean fne(double lhs, double rhs) {
        return lhs != rhs;
    }

    public static boolean flt(float lhs, float rhs) {
        return lhs < rhs;
    }

    public static boolean flt(double lhs, double rhs) {
        return lhs < rhs;
    }

    public static boolean fgt(float lhs, float rhs) {
        return lhs > rhs;
    }

    public static boolean fgt(double lhs, double rhs) {
        return lhs > rhs;
    }

    public static boolean fle(float lhs, float rhs) {
        return lhs <= rhs;
    }

    public static boolean fle(double lhs, double rhs) {
        return lhs <= rhs;
    }

    public static boolean fge(float lhs, float rhs) {
        return lhs >= rhs;
    }

    public static boolean fge(double lhs, double rhs) {
        return lhs >= rhs;
    }

    public static int i32TruncU(float operand) throws TrapException {
        if (Float.isNaN(operand) || operand <= -1f || operand >= 0x1p32f) {
            throw new TrapException();
        }

        return (int) (long) operand;
    }

    public static int i32TruncU(double operand) throws TrapException {
        if (Double.isNaN(operand) || operand <= -1.0 || operand >= 0x1p32) {
            throw new TrapException();
        }

        return (int) (long) operand;
    }

    public static int i32TruncS(float operand) throws TrapException {
        if (Float.isNaN(operand) || operand < -0x1p31f || operand >= 0x1p31f) {
            throw new TrapException();
        }

        return (int) operand;
    }

    public static int i32TruncS(double operand) throws TrapException {
        if (Double.isNaN(operand) || operand <= -0x1.00000002p31 || operand >= 0x1p31) {
            throw new TrapException();
        }

        return (int) operand;
    }

    public static long i64TruncU(float operand) throws TrapException {
        if (Float.isNaN(operand) || operand <= -1f || operand >= 0x1p64f) {
            throw new TrapException();
        }

        if (operand >= 0x1p63f) {
            return ((long) Math.scalb(operand, -1)) << 1;
        } else {
            return (long) operand;
        }
    }

    public static long i64TruncU(double operand) throws TrapException {
        if (Double.isNaN(operand) || operand <= -1.0 || operand >= 0x1p64) {
            throw new TrapException();
        }

        if (operand >= 0x1p63) {
            return ((long) Math.scalb(operand, -1)) << 1;
        } else {
            return (long) operand;
        }
    }

    public static long i64TruncS(float operand) throws TrapException {
        if (Float.isNaN(operand) || operand < -0x1p63f || operand >= 0x1p63f) {
            throw new TrapException();
        }

        return (long) operand;
    }

    public static long i64TruncS(double operand) throws TrapException {
        if (Double.isNaN(operand) || operand < -0x1p63 || operand >= 0x1p63) {
            throw new TrapException();
        }

        return (long) operand;
    }

    public static float f32ConvertU(int operand) {
        return (float) Integer.toUnsignedLong(operand);
    }

    public static float f32ConvertU(long operand) {
        if (Long.compareUnsigned(operand, Long.MAX_VALUE) > 0) {
            return Math.scalb((float) (operand >>> 1), 1);
        }
        else {
            return (float) operand;
        }
    }

    public static double f64ConvertU(int operand) {
        return (double) Integer.toUnsignedLong(operand);
    }

    public static double f64ConvertU(long operand) {
        if (Long.compareUnsigned(operand, Long.MAX_VALUE) > 0) {
            return Math.scalb((double) (operand >>> 1), 1);
        }
        else {
            return (double) operand;
        }
    }
}
