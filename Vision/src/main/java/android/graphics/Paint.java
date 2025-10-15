/*
 * Some Original work Copyright (C) 2006 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

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
import org.jetbrains.skia.FontMetrics;
import org.jetbrains.skia.PaintStrokeCap;
import org.jetbrains.skia.PaintStrokeJoin;

public class Paint {
    /**
     * The Style specifies if the primitive being drawn is filled, stroked, or
     * both (in the same color). The default is FILL.
     */
    public enum Style {
        /**
         * Geometry and text drawn with this style will be filled, ignoring all
         * stroke-related settings in the paint.
         */
        FILL            (0),
        /**
         * Geometry and text drawn with this style will be stroked, respecting
         * the stroke-related fields on the paint.
         */
        STROKE          (1),
        /**
         * Geometry and text drawn with this style will be both filled and
         * stroked at the same time, respecting the stroke-related fields on
         * the paint. This mode can give unexpected results if the geometry
         * is oriented counter-clockwise. This restriction does not apply to
         * either FILL or STROKE.
         */
        FILL_AND_STROKE (2);
        Style(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }
    /**
     * The Cap specifies the treatment for the beginning and ending of
     * stroked lines and paths. The default is BUTT.
     */
    public enum Cap {
        /**
         * The stroke ends with the path, and does not project beyond it.
         */
        BUTT    (0),
        /**
         * The stroke projects out as a semicircle, with the center at the
         * end of the path.
         */
        ROUND   (1),
        /**
         * The stroke projects out as a square, with the center at the end
         * of the path.
         */
        SQUARE  (2);
        private Cap(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }
    /**
     * The Join specifies the treatment where lines and curve segments
     * join on a stroked path. The default is MITER.
     */
    public enum Join {
        /**
         * The outer edges of a join meet at a sharp angle
         */
        MITER   (0),
        /**
         * The outer edges of a join meet in a circular arc.
         */
        ROUND   (1),
        /**
         * The outer edges of a join meet with a straight line
         */
        BEVEL   (2);
        private Join(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }
    /**
     * Align specifies how drawText aligns its text relative to the
     * [x,y] coordinates. The default is LEFT.
     */
    public enum Align {
        /**
         * The text is drawn to the right of the x,y origin
         */
        LEFT    (0),
        /**
         * The text is drawn centered horizontally on the x,y origin
         */
        CENTER  (1),
        /**
         * The text is drawn to the left of the x,y origin
         */
        RIGHT   (2);
        private Align(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }


    /**
     * Class that describes the various metrics for a font at a given text size.
     * Remember, Y values increase going down, so those values will be positive,
     * and values that measure distances going up will be negative. This class
     * is returned by getFontMetrics().
     */
    public static class FontMetrics {
        /**
         * The maximum distance above the baseline for the tallest glyph in
         * the font at a given text size.
         */
        public float   top;
        /**
         * The recommended distance above the baseline for singled spaced text.
         */
        public float   ascent;
        /**
         * The recommended distance below the baseline for singled spaced text.
         */
        public float   descent;
        /**
         * The maximum distance below the baseline for the lowest glyph in
         * the font at a given text size.
         */
        public float   bottom;
        /**
         * The recommended additional space to add between lines of text.
         */
        public float   leading;
    }

    /**
     * Internal: thePaint represents the underlying skiko paint
     * This field is not present in native android.graphics
     */
    public org.jetbrains.skia.Paint thePaint;

    private Typeface typeface;
    private float textSize;

    public Paint() {
        thePaint = new org.jetbrains.skia.Paint();
    }

    public Paint(Paint paint) {
        thePaint = paint.thePaint.makeClone();

        typeface = paint.typeface;
        textSize = paint.textSize;
    }

    public Paint setColor(int color) {
        thePaint.setColor(color);
        return this;
    }

    public void setAntiAlias(boolean value) {
        thePaint.setAntiAlias(value);
    }

    public Paint setStyle(Style style) {
        // Map Style to Skiko Mode enum
        org.jetbrains.skia.PaintMode mode = null;

        switch(style) {
            case FILL:
                mode = org.jetbrains.skia.PaintMode.FILL;
                break;
            case STROKE:
                mode = org.jetbrains.skia.PaintMode.STROKE;
                break;
            case FILL_AND_STROKE:
                mode = org.jetbrains.skia.PaintMode.STROKE_AND_FILL;
                break;
        }

        thePaint.setMode(mode);

        return this;
    }

    public Paint setTypeface(Typeface typeface) {
        this.typeface = typeface;
        return this;
    }

    public Paint setTextSize(float v) {
        textSize = v;
        return this;
    }

    public void setARGB(int a, int r, int g, int b) {
        thePaint.setColor(Color.argb(a, r, g, b));
    }

    public void setAlpha(int alpha) {
        thePaint.setAlpha(alpha);
    }

    public void setStrokeJoin(Join join) {
        PaintStrokeJoin strokeJoin = null;

        // conversion
        switch(join) {
            case MITER:
                strokeJoin = PaintStrokeJoin.MITER;
                break;
            case ROUND:
                strokeJoin = PaintStrokeJoin.ROUND;
                break;
            case BEVEL:
                strokeJoin = PaintStrokeJoin.BEVEL;
                break;
        }

        thePaint.setStrokeJoin(strokeJoin);
    }

    public void setStrokeCap(Cap cap) {
        PaintStrokeCap strokeCap = null;

        // conversion
        switch(cap) {
            case BUTT:
                strokeCap = PaintStrokeCap.BUTT;
                break;
            case ROUND:
                strokeCap = PaintStrokeCap.ROUND;
                break;
            case SQUARE:
                strokeCap = PaintStrokeCap.SQUARE;
                break;
        }

        thePaint.setStrokeCap(strokeCap);
    }

    public void setStrokeWidth(float width) {
        thePaint.setStrokeWidth(width);
    }

    public void setStrokeMiter(float miter) {
        thePaint.setStrokeMiter(miter);
    }

    public void set(Paint src) {
        thePaint = src.thePaint.makeClone();
        typeface = src.typeface;
        textSize = src.textSize;
    }

    public void reset() {
        thePaint = new org.jetbrains.skia.Paint();
        typeface = null;
        textSize = 0;
    }

    public boolean hasGlyph(String text) {
        return typeface.theTypeface.getStringGlyphs(text).length != 0;
    }

    // write getters here
    public int getColor() {
        return thePaint.getColor();
    }

    public boolean isAntiAlias() {
        return thePaint.isAntiAlias();
    }

    public Style getStyle() {
        switch(thePaint.getMode()) {
            case FILL:
                return Style.FILL;
            case STROKE:
                return Style.STROKE;
            default:
                return Style.FILL_AND_STROKE;
        }
    }

    public float getStrokeWidth() {
        return thePaint.getStrokeWidth();
    }

    public Cap getStrokeCap() {
        switch(thePaint.getStrokeCap()) {
            case ROUND:
                return Cap.ROUND;
            case SQUARE:
                return Cap.SQUARE;
            default:
                return Cap.BUTT;
        }
    }

    public Join getStrokeJoin() {
        switch(thePaint.getStrokeJoin()) {
            case ROUND:
                return Join.ROUND;
            case BEVEL:
                return Join.BEVEL;
            default:
                return Join.MITER;
        }
    }

    public float getStrokeMiter() {
        return thePaint.getStrokeMiter();
    }

    public Typeface getTypeface() {
        if(typeface == null) {
            return Typeface.DEFAULT;
        }

        return typeface;
    }

    private Font getFont() {
        return FontCache.makeFont(getTypeface(), getTextSize());
    }

    public FontMetrics getFontMetrics() {
        FontMetrics metrics = new FontMetrics();
        org.jetbrains.skia.FontMetrics fontMetrics = getFont().getMetrics();

        metrics.top = fontMetrics.getTop();
        metrics.ascent = fontMetrics.getAscent();
        metrics.descent = fontMetrics.getDescent();
        metrics.bottom = fontMetrics.getBottom();
        metrics.leading = fontMetrics.getLeading();

        return metrics;
    }

    public float getTextSize() {
        return textSize;
    }

}
