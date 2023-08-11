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

import org.jetbrains.skia.Font;

import java.util.HashMap;

class FontCache {

    private static HashMap<Typeface, HashMap<Integer, Font>> cache = new HashMap<>();

    public static Font makeFont(Typeface theTypeface, float textSize) {
        if(!cache.containsKey(theTypeface)) {
            cache.put(theTypeface, new HashMap<>());
        }

        HashMap<Integer, Font> sizeCache = cache.get(theTypeface);

        if(!sizeCache.containsKey((int) (textSize * 1000))) {
            sizeCache.put((int) (textSize * 1000), new Font(theTypeface.theTypeface, textSize));
        }

        return sizeCache.get((int) (textSize * 1000));
    }

}
