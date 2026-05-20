/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 */

package android.graphics;

import org.jetbrains.skia.Font;

import java.util.HashMap;

/**
 * A cache for fonts to avoid creating the same font multiple times.
 */
class FontCache {

    private static HashMap<Typeface, HashMap<Integer, Font>> cache = new HashMap<>();

    static Font makeFont(Typeface typeface, float textSize) {
        if(!cache.containsKey(typeface)) {
            cache.put(typeface, new HashMap<>());
        }

        HashMap<Integer, Font> sizeCache = cache.get(typeface);

        if(!sizeCache.containsKey((int) (textSize * 1000))) {
            sizeCache.put((int) (textSize * 1000), new Font(typeface.theTypeface, textSize));
        }

        return sizeCache.get((int) (textSize * 1000));
    }

}

