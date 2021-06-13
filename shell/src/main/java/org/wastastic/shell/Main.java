package org.wastastic.shell;

import org.wastastic.Module;
import org.wastastic.TranslationException;

import java.io.File;
import java.io.IOException;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws TranslationException, IOException {
        var module = Module.read(new File("/home/lucas/Documents/wastastic/zig-tests/test.wasm"));
    }
}
