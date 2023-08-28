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

import org.jetbrains.skia.PathDirection;
import org.jetbrains.skia.RRect;
import org.jetbrains.skia.Rect;

public class Path {

    /*
     * Copyright (C) 2006 The Android Open Source Project
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */

    /**
     * Specifies how closed shapes (e.g. rects, ovals) are oriented when they
     * are added to a path.
     */
    public enum Direction {
        /** clockwise */
        CW  (0),    // must match enum in SkPath.h
        /** counter-clockwise */
        CCW (1);    // must match enum in SkPath.h
        Direction(int ni) {
            nativeInt = ni;
        }
        final int nativeInt;
    }

    public org.jetbrains.skia.Path thePath;

    public Path() {
        thePath = new org.jetbrains.skia.Path();
    }

    public Path(Path src) {
        this();
        thePath = src.thePath;
    }

    public void set(Path src) {
        thePath = src.thePath;
    }

    public void addCircle(float x, float y, float radius, Direction dir) {
        // map to skia direction
        PathDirection skDir = null;
        switch (dir) {
            case CW:
                skDir = PathDirection.CLOCKWISE;
                break;
            case CCW:
                skDir = PathDirection.COUNTER_CLOCKWISE;
                break;
        }

        thePath.addCircle(x, y, radius, skDir);
    }

    public void addRect(float left, float top, float right, float bottom, Direction dir) {
        // map to skia direction
        PathDirection skDir = null;
        switch (dir) {
            case CW:
                skDir = PathDirection.CLOCKWISE;
                break;
            case CCW:
                skDir = PathDirection.COUNTER_CLOCKWISE;
                break;
        }

        thePath.addRect(new Rect(left, top, right, bottom), skDir, 0);
    }

    public void addRoundRect(float left, float top, float right, float bottom, float rx, float ry, Direction dir) {
        // map to skia direction
        PathDirection skDir = null;
        switch (dir) {
            case CW:
                skDir = PathDirection.CLOCKWISE;
                break;
            case CCW:
                skDir = PathDirection.COUNTER_CLOCKWISE;
                break;
        }

        thePath.addRRect(RRect.makeLTRB(left, top, right, bottom, rx, ry), skDir, 0);
    }

    public void addRoundRect(float left, float top, float right, float bottom, float[] radii, Direction dir) {
        // map to skia direction
        PathDirection skDir = null;
        switch (dir) {
            case CW:
                skDir = PathDirection.CLOCKWISE;
                break;
            case CCW:
                skDir = PathDirection.COUNTER_CLOCKWISE;
                break;
        }

        thePath.addRRect(new RRect(left, top, right, bottom, radii), skDir, 0);
    }

    public void addRoundRect(Rect rect, float[] radii, Direction dir) {
        // map to skia direction
        PathDirection skDir = null;
        switch (dir) {
            case CW:
                skDir = PathDirection.CLOCKWISE;
                break;
            case CCW:
                skDir = PathDirection.COUNTER_CLOCKWISE;
                break;
        }

        thePath.addRRect(new RRect(rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom(), radii), skDir, 0);
    }

    public void addRoundRect(Rect rect, float rx, float ry, Direction dir) {
        // map to skia direction
        PathDirection skDir = null;
        switch (dir) {
            case CW:
                skDir = PathDirection.CLOCKWISE;
                break;
            case CCW:
                skDir = PathDirection.COUNTER_CLOCKWISE;
                break;
        }

        thePath.addRRect(RRect.makeLTRB(rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom(), rx, ry), skDir, 0);
    }

    public void addOval(float left, float top, float right, float bottom, Direction dir) {
        // map to skia direction
        PathDirection skDir = null;
        switch (dir) {
            case CW:
                skDir = PathDirection.CLOCKWISE;
                break;
            case CCW:
                skDir = PathDirection.COUNTER_CLOCKWISE;
                break;
        }

        thePath.addOval(new Rect(left, top, right, bottom), skDir, 0);
    }

    public void addOval(Rect oval, Direction dir) {
        // map to skia direction
        PathDirection skDir = null;
        switch (dir) {
            case CW:
                skDir = PathDirection.CLOCKWISE;
                break;
            case CCW:
                skDir = PathDirection.COUNTER_CLOCKWISE;
                break;
        }

        thePath.addOval(oval, skDir, 0);
    }

    public void addArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
        thePath.addArc(new Rect(left, top, right, bottom), startAngle, sweepAngle);
    }

    public void addArc(Rect oval, float startAngle, float sweepAngle) {
        thePath.addArc(oval, startAngle, sweepAngle);
    }

    public void moveTo(float x, float y) {
        thePath.moveTo(x, y);
    }

    public void lineTo(float x, float y) {
        thePath.lineTo(x, y);
    }

    public void quadTo(float x1, float y1, float x2, float y2) {
        thePath.quadTo(x1, y1, x2, y2);
    }

    public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        thePath.cubicTo(x1, y1, x2, y2, x3, y3);
    }

    public void close() {
        thePath.close();
    }

    public void reset() {
        thePath.reset();
    }

    public void addPath(Path path) {
        thePath.addPath(path.thePath, false);
    }

}
