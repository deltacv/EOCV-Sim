package android.graphics;

import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.FontStyle;

public class Typeface {

    public static Typeface DEFAULT = new Typeface(FontMgr.getDefault().matchFamilyStyle(null, FontStyle.NORMAL));
    public static Typeface DEFAULT_BOLD = new Typeface(FontMgr.getDefault().matchFamilyStyle(null, FontStyle.BOLD));
    public static Typeface DEFAULT_ITALIC = new Typeface(FontMgr.getDefault().matchFamilyStyle(null, FontStyle.ITALIC));

    public io.github.humbleui.skija.Typeface theTypeface;

    public Typeface(long ptr) {
        theTypeface = new io.github.humbleui.skija.Typeface(ptr);
    }

    private Typeface(io.github.humbleui.skija.Typeface typeface) {
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
