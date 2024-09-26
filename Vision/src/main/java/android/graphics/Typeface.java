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

import org.jetbrains.skia.FontMgr;
import org.jetbrains.skia.FontStyle;

public class Typeface {

    public static Typeface DEFAULT = new Typeface(FontMgr.Companion.getDefault().matchFamilyStyle(null, FontStyle.Companion.getNORMAL()));
    public static Typeface DEFAULT_BOLD = new Typeface(FontMgr.Companion.getDefault().matchFamilyStyle(null, FontStyle.Companion.getBOLD()));
    public static Typeface DEFAULT_ITALIC = new Typeface(FontMgr.Companion.getDefault().matchFamilyStyle(null, FontStyle.Companion.getITALIC()));

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

}
