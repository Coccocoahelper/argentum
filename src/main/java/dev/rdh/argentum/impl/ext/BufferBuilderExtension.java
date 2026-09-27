package dev.rdh.argentum.impl.ext;

import java.nio.IntBuffer;

public interface BufferBuilderExtension {
    default void argentum$appendTranslated(int[] vertices, float x, float y) {
        throw new UnsupportedOperationException();
    }

    default void argentum$appendVertices(int[] vertices, int length) {
        throw new UnsupportedOperationException();
    }

    default IntBuffer argentum$rawIntBuffer() {
        throw new UnsupportedOperationException();
    }
}
