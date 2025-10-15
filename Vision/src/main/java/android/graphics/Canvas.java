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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.skia.*;

public class Canvas {

    /**
     * Internal: theCanvas represents the underlying skiko Canvas
     * This field is not present in native android.graphics
     */
    public final org.jetbrains.skia.Canvas theCanvas;

    private Bitmap backingBitmap = null;

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

    public void drawPoint(float x, float y, Paint paint) {
        theCanvas.drawPoint(x, y, paint.thePaint);
    }

    public void drawRoundRect(float l, float t, float r, float b, float xRad, float yRad, Paint rectPaint) {
        theCanvas.drawRRect(RRect.makeLTRB(l, t, r, b, xRad, yRad), rectPaint.thePaint);
    }

    public void drawPath(Path path, Paint paint) {
        theCanvas.drawPath(path.thePath, paint.thePaint);
    }

    public void drawCircle(float x, float y, float radius, Paint paint) {
        theCanvas.drawCircle(x, y, radius, paint.thePaint);
    }

    public void drawOval(float left, float top, float right, float bottom, Paint paint) {
        theCanvas.drawOval(new org.jetbrains.skia.Rect(left, top, right, bottom), paint.thePaint);
    }

    public void drawArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle, boolean useCenter, Paint paint) {
        theCanvas.drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, paint.thePaint);
    }

    public void drawText(String text, int start, int end, float x, float y, Paint paint) {
        Font font = FontCache.makeFont(paint.getTypeface(), paint.getTextSize());
        theCanvas.drawString(text.substring(start, end), x, y, font, paint.thePaint);
    }

    public void drawText(String text, float x, float y, Paint paint) {
        Font font = FontCache.makeFont(paint.getTypeface(), paint.getTextSize());
        theCanvas.drawString(text, x, y, font, paint.thePaint);
    }

    public void drawPoints(float[] points, int offset, int count, Paint paint) {
        // not supported by the skija canvas so we have to do it manually
        for(int i = offset; i < offset + count; i += 2) {
            theCanvas.drawPoint(points[i], points[i + 1], paint.thePaint);
        }
    }

    public void drawPoints(float[] points, Paint paint) {
        theCanvas.drawPoints(points, paint.thePaint);
    }

    public void drawRGB(int r, int g, int b) {
        theCanvas.clear(Color.rgb(r, g, b));
    }

    public void drawLines(float[] points, Paint paint) {
        theCanvas.drawLines(points, paint.thePaint);
    }

    public void drawRect(Rect rect, Paint paint) {
        theCanvas.drawRect(rect.toSkijaRect(), paint.thePaint);
    }

    public void drawRect(float left, float top, float right, float bottom, Paint paint) {
        theCanvas.drawRect(new org.jetbrains.skia.Rect(left, top, right, bottom), paint.thePaint);
    }

    public void rotate(float degrees, float xCenter, float yCenter) {
        theCanvas.rotate(degrees, xCenter, yCenter);
    }

    public void rotate(float degrees) {
        theCanvas.rotate(degrees);
    }

    public int save() {
        return theCanvas.save();
    }

    public void restore() {
        theCanvas.restore();
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

    public void drawBitmap(Bitmap bitmap, float left, float top, Paint paint) {
        org.jetbrains.skia.Paint thePaint = null;

        if(paint != null) {
            thePaint = paint.thePaint;
        }

        theCanvas.drawImage(Image.Companion.makeFromBitmap(bitmap.theBitmap), left, top, thePaint);
    }

    public void skew(float sx, float sy) {
        theCanvas.skew(sx, sy);
    }

    public void translate(int dx, int dy) {
        theCanvas.translate(dx, dy);
    }

    public void scale(float sx, float sy) {
        theCanvas.scale(sx, sy);
    }

    public void restoreToCount(int saveCount) {
        theCanvas.restoreToCount(saveCount);
    }

    public boolean readPixels(@NotNull Bitmap lastFrame, int srcX, int srcY) {
        return theCanvas.readPixels(lastFrame.theBitmap, srcX, srcY);
    }

    public Canvas drawColor(int color) {
        theCanvas.clear(color);
        return this;
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