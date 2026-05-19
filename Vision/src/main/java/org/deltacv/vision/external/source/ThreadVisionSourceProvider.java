/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.vision.external.source;

public final class ThreadVisionSourceProvider {

    private ThreadVisionSourceProvider() {} // No instantiation

    private static final ThreadLocal<VisionSourceProvider> provider = new ThreadLocal<>();

    public static void register(VisionSourceProvider provider) {
        ThreadVisionSourceProvider.provider.set(provider);
    }

    public static VisionSourceProvider getCurrentProvider() {
        return provider.get();
    }

    public static VisionSource get(String name) {
        return getCurrentProvider().get(name);
    }

}

