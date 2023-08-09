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

import io.github.humbleui.skija.Font;
import io.github.humbleui.types.RRect;
import org.firstinspires.ftc.vision.apriltag.AprilTagCanvasAnnotator;

public class Canvas {

    public final io.github.humbleui.skija.Canvas theCanvas;

    public Canvas(Bitmap bitmap) {
        theCanvas = new io.github.humbleui.skija.Canvas(bitmap.theBitmap);
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
        Font font = FontCache.makeFont(paint.getTypeface().theTypeface, paint.getTextSize());
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
}
