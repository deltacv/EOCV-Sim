/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 */

package android.graphics;

import org.jetbrains.skia.Data;
import org.jetbrains.skia.FontMgr;

import java.io.IOException;

public class Typeface {

    public static Typeface DEFAULT = new Typeface(FontMgr.Companion.getDefault().makeFromData(loadDataFromResource("/fonts/Roboto-Regular.ttf"), 0));
    public static Typeface DEFAULT_BOLD = new Typeface(FontMgr.Companion.getDefault().makeFromData(loadDataFromResource("/fonts/Roboto-Bold.ttf"), 0));
    public static Typeface DEFAULT_ITALIC = new Typeface(FontMgr.Companion.getDefault().makeFromData(loadDataFromResource("/fonts/Roboto-Italic.ttf"), 0));

    /**
     * Internal: theTypeface represents the underlying skiko Typeface
     * This field is not present in native android.graphics
     */
    public org.jetbrains.skia.Typeface theTypeface;

    public Typeface(long ptr) {
        theTypeface = new org.jetbrains.skia.Typeface(ptr);
    }

    private Typeface(org.jetbrains.skia.Typeface typeface) {
        theTypeface = typeface;
    }

    public android.graphics.Rect getBounds() {
        return new Rect(
                (int) theTypeface.getBounds().getLeft(),
                (int) theTypeface.getBounds().getTop(),
                (int) theTypeface.getBounds().getRight(),
                (int) theTypeface.getBounds().getBottom()
        );
    }

    private static Data loadDataFromResource(String resource) {
        try {
            byte[] bytes = Typeface.class.getResourceAsStream(resource).readAllBytes();

            return Data.Companion.makeFromBytes(
                    bytes,
                    0, bytes.length
            );
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load from resource: " + resource, e);
        }
    }

}

