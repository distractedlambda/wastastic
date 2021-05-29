package org.wastastic;

import static org.wastastic.Vectors.requireVectorOf;

public final class FunctionType implements ImportType {
    private final Object parameterTypes;
    private final Object returnTypes;

    public FunctionType(Object parameterTypes, Object returnTypes) {
        this.parameterTypes = requireVectorOf(parameterTypes, ValueType.class);
        this.returnTypes = requireVectorOf(returnTypes, ValueType.class);
    }

    public Object getParameterTypes() {
        return parameterTypes;
    }

    public Object getReturnTypes() {
        return returnTypes;
    }

    public String getSignatureString() {
        if (returnTypes instanceof ValueType[]) {
            throw new UnsupportedOperationException("TODO implement multiple return values");
        }

        if (parameterTypes == null) {
            if (returnTypes == null) {
                return "()V";
            } else {
                return "()" + ((ValueType) returnTypes).getDescriptor();
            }
        } else if (parameterTypes instanceof ValueType) {
            var parameterDescriptor = ((ValueType)parameterTypes).getDescriptor();
            if (returnTypes == null) {
                return '(' + parameterDescriptor + ")V";
            } else {
                return '(' + parameterDescriptor + ')' + ((ValueType) returnTypes).getDescriptor();
            }
        } else {
            var builder = new StringBuilder("(");

            for (var parameterType : (ValueType[]) parameterTypes) {
                builder.append(parameterType.getDescriptor());
            }

            builder.append(')');

            if (returnTypes == null) {
                builder.append('V');
            } else {
                builder.append(((ValueType) returnTypes).getDescriptor());
            }

            return builder.toString();
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof FunctionType other) {
            return this == other || (
                Vectors.equals(parameterTypes, other.parameterTypes, ValueType.class)
                    && Vectors.equals(returnTypes, other.returnTypes, ValueType.class)
            );
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 31 * Vectors.hashCode(parameterTypes, ValueType.class)
            + Vectors.hashCode(returnTypes, ValueType.class);
    }

    @Override
    public String toString() {
        return "FunctionType(parameterTypes = "
            + Vectors.toString(parameterTypes, ValueType.class)
            + "; returnTypes = "
            + Vectors.toString(returnTypes, ValueType.class)
            + ')';
    }
}
