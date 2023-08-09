package android.graphics;

import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Typeface;

import java.util.HashMap;

class FontCache {

    private static HashMap<Typeface, HashMap<Integer, Font>> cache = new HashMap<>();

    public static Font makeFont(Typeface theTypeface, float textSize) {
        if(!cache.containsKey(theTypeface)) {
            cache.put(theTypeface, new HashMap<>());
        }

        HashMap<Integer, Font> sizeCache = cache.get(theTypeface);

        if(!sizeCache.containsKey((int) (textSize * 1000))) {
            sizeCache.put((int) (textSize * 1000), new Font(theTypeface, textSize));
        }

        return sizeCache.get((int) (textSize * 1000));
    }

}
