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
