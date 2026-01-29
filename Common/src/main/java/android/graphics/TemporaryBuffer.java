package android.graphics;

import com.android.internal.util.ArrayUtils;

/**
 * @hide
 */
public class TemporaryBuffer {
    public static char[] obtain(int len) {
        char[] buf;
        synchronized (TemporaryBuffer.class) {
            buf = sTemp;
            sTemp = null;
        }
        if (buf == null || buf.length < len) {
            buf = new char[ArrayUtils.idealCharArraySize(len)];
        }
        return buf;
    }
    public static void recycle(char[] temp) {
        if (temp.length > 1000) return;
        synchronized (TemporaryBuffer.class) {
            sTemp = temp;
        }
    }
    private static char[] sTemp = null;
}