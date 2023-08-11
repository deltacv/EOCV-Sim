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

import org.jetbrains.skia.*;

public class Canvas {

    public final org.jetbrains.skia.Canvas theCanvas;

    private Bitmap backingBitmap = null;

    final Object surfaceLock = new Object();
    private int providedWidth;
    private int providedHeight;

    public Canvas(Bitmap bitmap) {
        theCanvas = new org.jetbrains.skia.Canvas(bitmap.theBitmap, new SurfaceProps());
        backingBitmap = bitmap;
    }

    public Canvas(Surface surface) {
        theCanvas = surface.getCanvas();

        providedWidth = surface.getWidth();
        providedHeight = surface.getHeight();
    }

    public Canvas(org.jetbrains.skia.Canvas skiaCanvas, int width, int height) {
        theCanvas = skiaCanvas;

        providedWidth = width;
        providedHeight = height;
    }

    public Canvas drawLine(float x, float y, float x1, float y1, Paint paint) {
        theCanvas.drawLine(x, y, x1, y1, paint.thePaint);
        return this;
    }


    public Canvas drawRoundRect(float l, float t, float r, float b, float xRad, float yRad, Paint rectPaint) {
        theCanvas.drawRRect(RRect.makeLTRB(l, t, r, b, xRad, yRad), rectPaint.thePaint);

        return this;
    }


    public Canvas drawText(String text, float x, float y, Paint paint) {
        Font font = FontCache.makeFont(paint.getTypeface(), paint.getTextSize());
        theCanvas.drawString(text, x, y, font, paint.thePaint);

        return this;
    }

    public Canvas rotate(float degrees, float xCenter, float yCenter) {
        theCanvas.rotate(degrees);
        return this;
    }

    public Canvas rotate(float degrees) {
        theCanvas.rotate(degrees);
        return this;
    }

    public int save() {
        return theCanvas.save();
    }

    public Canvas restore() {
        theCanvas.restore();
        return this;
    }

    public Canvas drawLines(float[] points, Paint paint) {
        theCanvas.drawLines(points, paint.thePaint);
        return this;
    }

    public void drawRect(Rect rect, Paint paint) {
        theCanvas.drawRect(rect.toSkijaRect(), paint.thePaint);
    }

    public void drawBitmap(Bitmap bitmap, Rect src, Rect rect, Paint paint) {
        int left, top, right, bottom;
        if (src == null) {
            left = top = 0;
            right = bitmap.getWidth();
            bottom = bitmap.getHeight();
        } else {
            left = src.left;
            top = src.top;
            right = src.right;
            bottom = src.bottom;
        }

        org.jetbrains.skia.Paint thePaint = null;

        if(paint != null) {
            thePaint = paint.thePaint;
        }

        theCanvas.drawImageRect(
                Image.Companion.makeFromBitmap(bitmap.theBitmap),
                new org.jetbrains.skia.Rect(left, top, right, bottom),
                rect.toSkijaRect(), thePaint
        );
    }

    public void translate(int dx, int dy) {
        theCanvas.translate(dx, dy);
    }

    public void restoreToCount(int saveCount) {
        theCanvas.restoreToCount(saveCount);
    }

    public void drawColor(int color) {
        theCanvas.clear(color);
    }

    public int getWidth() {
        if(backingBitmap != null) {
            return backingBitmap.getWidth();
        }

        return providedWidth;
    }

    public int getHeight() {
        if(backingBitmap != null) {
            return backingBitmap.getHeight();
        }

        return providedHeight;
    }
}