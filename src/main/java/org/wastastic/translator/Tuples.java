package org.wastastic.translator;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V16;

final class Tuples {
    private Tuples() {}

    static @NotNull Class<?> getTupleClass(@NotNull List<ValueType> types) {
        return TUPLE_CLASSES.computeIfAbsent(types, key -> {
            var suffixChars = new char[key.size()];
            for (var i = 0; i < suffixChars.length; i++) {
                suffixChars[i] = key.get(i).tupleSuffixChar();
            }

            var internalName = "org/wastastic/Tuple-" + new String(suffixChars);

            var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            writer.visit(V16, ACC_FINAL, internalName, null, "java/lang/Object", null);

            var baseDescriptorBuilder = new StringBuilder("(");

            for (var i = 0; i < key.size(); i++) {
                var type = key.get(i);
                var fieldDescriptor = type.erasedDescriptor();
                writer.visitField(ACC_FINAL, "" + i, fieldDescriptor, null, null);
                baseDescriptorBuilder.append(fieldDescriptor);
            }

            baseDescriptorBuilder.append(')');
            var baseDescriptor = baseDescriptorBuilder.toString();

            var constructorWriter = writer.visitMethod(ACC_PRIVATE, "<init>", baseDescriptor + 'V', null, null);
            constructorWriter.visitCode();

            var localIndex = 0;
            for (var i = 0; i < key.size(); i++) {
                var type = key.get(i);
                constructorWriter.visitInsn(DUP);
                constructorWriter.visitVarInsn(type.localLoadOpcode(), localIndex);
                constructorWriter.visitFieldInsn(PUTFIELD, internalName, "" + i, type.erasedDescriptor());
                localIndex += type.width();
            }

            constructorWriter.visitInsn(RETURN);
            constructorWriter.visitMaxs(0, 0);
            constructorWriter.visitEnd();

            var factoryWriter = writer.visitMethod(ACC_STATIC, "create", baseDescriptor + 'L' + internalName + ';', null, null);
            factoryWriter.visitCode();
            factoryWriter.visitTypeInsn(NEW, internalName);

            var nextArgumentIndex = 0;
            for (var parameterType : key) {
                factoryWriter.visitVarInsn(parameterType.localLoadOpcode(), nextArgumentIndex);
                nextArgumentIndex += parameterType.width();
            }

            factoryWriter.visitMethodInsn(INVOKESPECIAL, internalName, "<init>", baseDescriptor + 'V', false);
            factoryWriter.visitInsn(ARETURN);
            factoryWriter.visitMaxs(0, 0);
            factoryWriter.visitEnd();

            writer.visitEnd();

            try {
                return MethodHandles.lookup().defineClass(writer.toByteArray());
            } catch (IllegalAccessException exception) {
                throw new UnsupportedOperationException("Unable to define tuple class", exception);
            }
        });
    }

    private static final ConcurrentHashMap<List<ValueType>, Class<?>> TUPLE_CLASSES = new ConcurrentHashMap<>();
}
