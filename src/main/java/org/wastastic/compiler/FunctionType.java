package org.wastastic.compiler;

final class FunctionType {
    private final Object parameterTypes;
    private final Object returnTypes;

    private String signatureString;

    FunctionType(Object parameterTypes, Object returnTypes) {
        this.parameterTypes = parameterTypes;
        this.returnTypes = returnTypes;
    }

    Object getParameterTypes() {
        return parameterTypes;
    }

    Object getReturnTypes() {
        return returnTypes;
    }

    int getParameterCount() {
        return ResultTypes.length(parameterTypes);
    }

    int getReturnCount() {
        return ResultTypes.length(returnTypes);
    }

    String getSignatureString() throws CompilationException {
        if (signatureString != null) {
            return signatureString;
        } else {
            return (signatureString = makeSignatureString());
        }
    }

    private String makeSignatureString() throws CompilationException {
        if (returnTypes instanceof ValueType[]) {
            throw new CompilationException("TODO implement multiple return values");
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
}
