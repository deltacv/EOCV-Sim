package dalvik.system;

import java.lang.reflect.Array;

public class VMRuntime {

    // singleton class

    private static VMRuntime runtime = new VMRuntime();

    private VMRuntime() {
    }

    public static VMRuntime getRuntime() {
        return runtime;
    }

    public Object newUnpaddedArray(Class<?> componentType, int length) {
        return Array.newInstance(componentType, length); // we do a little bit of trolling -SEM
    }

}
