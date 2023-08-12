package io.github.deltacv.vision.external.source;

import java.util.HashMap;

public class ThreadSourceHander {

    private static HashMap<Thread, VisionSourceHander> handlers = new HashMap<>();

    private ThreadSourceHander() {} // No instantiation

    public static void register(Thread thread, VisionSourceHander handler) {
        handlers.put(thread, handler);
    }

    public static void register(VisionSourceHander hander) {
        register(Thread.currentThread(), hander);
    }

    public static VisionSourceHander threadHander() {
        return handlers.get(Thread.currentThread());
    }

    public static VisionSource hand(String name) {
        return threadHander().hand(name);
    }

}
