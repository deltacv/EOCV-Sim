/*
 * Copyright (c) 2023 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package android.graphics;

import org.jetbrains.skia.Data;
import org.jetbrains.skia.FontMgr;
import org.jetbrains.skia.FontStyle;

import java.io.FileNotFoundException;
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

    public Rect getBounds() {
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
