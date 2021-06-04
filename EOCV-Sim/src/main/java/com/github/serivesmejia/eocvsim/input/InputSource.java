/*
 * Copyright (c) 2021 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.input;

import com.github.serivesmejia.eocvsim.EOCVSim;
import org.opencv.core.Mat;

import javax.swing.filechooser.FileFilter;

public abstract class InputSource implements Comparable<InputSource> {

    public transient boolean isDefault = false;
    public transient EOCVSim eocvSim = null;

    protected transient String name = "";
    protected transient boolean isPaused = false;
    private transient boolean beforeIsPaused = false;

    protected long createdOn = -1L;

    public abstract boolean init();
    public abstract void reset();
    public abstract void close();

    public abstract void onPause();
    public abstract void onResume();

    public Mat update() {
        return null;
    }

    public final InputSource cloneSource() {
        InputSource source = internalCloneSource();
        source.createdOn = createdOn;
        return source;
    }

    protected abstract InputSource internalCloneSource();

    public final void setPaused(boolean paused) {

        isPaused = paused;

        if (beforeIsPaused != isPaused) {
            if (isPaused) {
                onPause();
            } else {
                onResume();
            }
        }

        beforeIsPaused = paused;

    }

    public final String getName() {
        return name;
    }

    public abstract FileFilter getFileFilters();

    @Override
    public final int compareTo(InputSource source) {
        return createdOn > source.createdOn ? 1 : -1;
    }

}