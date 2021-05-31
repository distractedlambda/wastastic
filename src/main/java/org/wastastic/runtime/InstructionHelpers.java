package org.wastastic.runtime;

import org.wastastic.TrapException;

import static java.lang.Integer.compareUnsigned;
import static java.lang.Integer.divideUnsigned;
import static java.lang.Integer.remainderUnsigned;
import static java.lang.Integer.toUnsignedLong;
import static java.lang.Long.compareUnsigned;
import static java.lang.Long.divideUnsigned;
import static java.lang.Long.remainderUnsigned;

@SuppressWarnings("unused")
public final class InstructionHelpers {
    private InstructionHelpers() {}

    public static RuntimeException trap() throws TrapException {
        throw new TrapException();
    }

    public static int effectiveAddress(int dynamicAddress, int offset) throws TrapException {
        var longSum = toUnsignedLong(dynamicAddress) + toUnsignedLong(offset);

        if (longSum > 0xFFFFFFFF) {
            throw new TrapException();
        }

        return (int) longSum;
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

    public static int divU(int lhs, int rhs) throws TrapException {
        if (rhs == 0) {
            throw new TrapException();
        }

        return divideUnsigned(lhs, rhs);
    }

    public static int divS(int lhs, int rhs) throws TrapException {
        if (rhs == 0 || (lhs == Integer.MIN_VALUE && rhs == -1)) {
            throw new TrapException();
        }

        return lhs / rhs;
    }

    public static long divU(long lhs, long rhs) throws TrapException {
        if (rhs == 0) {
            throw new TrapException();
        }

        return divideUnsigned(lhs, rhs);
    }

    public static long divS(long lhs, long rhs) throws TrapException {
        if (rhs == 0 || (lhs == Long.MIN_VALUE && rhs == -1)) {
            throw new TrapException();
        }

        return lhs / rhs;
    }

    public static int remU(int lhs, int rhs) throws TrapException {
        if (rhs == 0) {
            throw new TrapException();
        }

        return remainderUnsigned(lhs, rhs);
    }

    public static long remU(long lhs, long rhs) throws TrapException {
        if (rhs == 0) {
            throw new TrapException();
        }

        return remainderUnsigned(lhs, rhs);
    }

    public static int remS(int lhs, int rhs) throws TrapException {
        if (rhs == 0) {
            throw new TrapException();
        }

        return lhs % rhs;
    }

    public static long remS(long lhs, long rhs) throws TrapException {
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
        return compareUnsigned(lhs, rhs) < 0;
    }

    public static boolean ltu(long lhs, long rhs) {
        return compareUnsigned(lhs, rhs) < 0;
    }

    public static boolean lts(int lhs, int rhs) {
        return lhs < rhs;
    }

    public static boolean lts(long lhs, long rhs) {
        return lhs < rhs;
    }

    public static boolean gtu(int lhs, int rhs) {
        return compareUnsigned(lhs, rhs) > 0;
    }

    public static boolean gtu(long lhs, long rhs) {
        return compareUnsigned(lhs, rhs) > 0;
    }

    public static boolean gts(int lhs, int rhs) {
        return lhs > rhs;
    }

    public static boolean gts(long lhs, long rhs) {
        return lhs > rhs;
    }

    public static boolean leu(int lhs, int rhs) {
        return compareUnsigned(lhs, rhs) <= 0;
    }

    public static boolean leu(long lhs, long rhs) {
        return compareUnsigned(lhs, rhs) <= 0;
    }

    public static boolean les(int lhs, int rhs) {
        return lhs <= rhs;
    }

    public static boolean les(long lhs, long rhs) {
        return lhs <= rhs;
    }

    public static boolean geu(int lhs, int rhs) {
        return compareUnsigned(lhs, rhs) >= 0;
    }

    public static boolean geu(long lhs, long rhs) {
        return compareUnsigned(lhs, rhs) >= 0;
    }

    public static boolean ges(int lhs, int rhs) {
        return lhs >= rhs;
    }

    public static boolean ges(long lhs, long rhs) {
        return lhs >= rhs;
    }
}
