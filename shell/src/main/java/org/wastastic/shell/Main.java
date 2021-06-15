package org.wastastic.shell;

import org.wastastic.Module;

import java.io.File;
import java.util.Map;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Throwable {
        var module = Module.read(new File("/home/lucas/Documents/lexical-wasm-test/target/wasm32-unknown-unknown/debug/lexical_wasm_test.wasm"));
        var instance = module.instantiationHandle().invoke(Map.of());
        System.out.println((int) module.exportedFunctionHandle("add").invoke(4, 5, instance));
    }
}
