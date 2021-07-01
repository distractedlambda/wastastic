package org.wastastic.shell;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.wastastic.Memory;
import org.wastastic.Module;
import org.wastastic.ModuleInstance;

import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Throwable {
        try (var scope = ResourceScope.newConfinedScope()) {
            var path = Path.of("/home/lucas/Documents/lexical-wasm-test/target/wasm32-unknown-unknown/debug/lexical_wasm_test.wasm");
            var module = Module.compile(MemorySegment.mapFile(path, 0, Files.size(path), FileChannel.MapMode.READ_ONLY, scope));
            module.precompileFunctions();

            var instance = (ModuleInstance) module.instantiationHandle().invoke(Map.of());

            var memory = (Memory) module.exportedMemoryHandle("memory").get(instance);

            var allocFn = module.exportedFunctionHandle("allocate_memory");
            var freeFn = module.exportedFunctionHandle("free_memory");
            var parseF32Fn = module.exportedFunctionHandle("parse_f32");

            var input = "3.141592";
            var inputBytes = input.getBytes(StandardCharsets.UTF_8);
            var charsPtr = (int) allocFn.invokeExact(-1, instance);
            var dstPtr = (int) allocFn.invokeExact(4, instance);

            memory.setBytes(charsPtr, inputBytes);
            var successful = (int) parseF32Fn.invokeExact(charsPtr, inputBytes.length, -1, instance);

            System.out.println(memory.getFloat(dstPtr));

            freeFn.invokeExact(charsPtr, inputBytes.length, instance);
            freeFn.invokeExact(dstPtr, 4, instance);
        }
    }
}
