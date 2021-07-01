package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.Stream;

import static org.objectweb.asm.Type.getInternalName;
import static org.wastastic.Names.methodDescriptor;

public final class TrapException extends Exception {
    TrapException(@Nullable String message, @Nullable Throwable cause, int stackTraceSkipCount) {
        super(message, cause);
        setStackTrace(recoverStackTrace(stackTraceSkipCount));
    }

    TrapException(@Nullable String message) {
        this(message, null, 3);
    }

    TrapException(@Nullable Throwable cause) {
        this(null, cause, 3);
    }

    TrapException() {
        this(null, null, 3);
    }

    static final String INTERNAL_NAME = getInternalName(TrapException.class);

    static final String UNREACHABLE_NAME = "unreachable";
    static final String UNREACHABLE_DESCRIPTOR = methodDescriptor(TrapException.class);

    @SuppressWarnings("unused")
    static @NotNull TrapException unreachable() {
        return new TrapException("unreachable instruction executed");
    }

    @Override public synchronized Throwable fillInStackTrace() {
        return this;
    }

    private static final StackWalker STACK_WALKER = StackWalker.getInstance(Set.of(StackWalker.Option.SHOW_HIDDEN_FRAMES, StackWalker.Option.RETAIN_CLASS_REFERENCE));

    private static @NotNull StackTraceElement @NotNull[] recoverStackTrace(int skipCount) {
        return STACK_WALKER.walk(frames -> {
            if (skipCount != 0) {
                frames = frames.skip(skipCount);
            }

            return frames.flatMap(frame -> {
                var clazz = frame.getDeclaringClass();

                StackTraceElement element;
                if (clazz.isHidden()) {
                    if (!GeneratedFunction.class.isAssignableFrom(clazz)) {
                        return Stream.empty();
                    }

                    var moduleName = clazz.getAnnotation(GeneratedFunction.ModuleName.class);
                    var functionName = clazz.getAnnotation(GeneratedFunction.FunctionName.class);

                    String declaringClass;
                    if (moduleName != null) {
                        declaringClass = "<WASM module '" + moduleName.value() + "'>";
                    }
                    else {
                        declaringClass = "<WASM module>";
                    }

                    String methodName;
                    if (functionName != null) {
                        methodName = functionName.value();
                    }
                    else {
                        methodName = "<unknown>";
                    }

                    element = new StackTraceElement(declaringClass, methodName, null, -1);
                }
                else {
                    element = frame.toStackTraceElement();
                }

                return Stream.of(element);
            }).toArray(StackTraceElement[]::new);
        });
    }
}
