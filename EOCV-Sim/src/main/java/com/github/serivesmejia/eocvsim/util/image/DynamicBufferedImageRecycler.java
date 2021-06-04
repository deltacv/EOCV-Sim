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

package com.github.serivesmejia.eocvsim.util.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DynamicBufferedImageRecycler {

    private final HashMap<Dimension, BufferedImageRecycler> recyclers = new HashMap<>();

    public synchronized BufferedImage giveBufferedImage(Dimension size, int recyclerSize) {

        //look for existing buff image recycler with desired dimensions
        for(Map.Entry<Dimension, BufferedImageRecycler> entry : recyclers.entrySet()) {
            Dimension dimension = entry.getKey();
            BufferedImageRecycler recycler = entry.getValue();

            if(dimension.equals(size)) {
                BufferedImage buffImg = recycler.takeBufferedImage();
                buffImg.flush();
                return buffImg;
            } else if(!recycler.isOnUse()) {
                recycler.flushAll();
                recyclers.remove(dimension);
            }
        }

        //create new one if didn't found an existing recycler
        BufferedImageRecycler recycler = new BufferedImageRecycler(recyclerSize, size);
        recyclers.put(size, recycler);

        BufferedImage buffImg = recycler.takeBufferedImage();

        return buffImg;
    }

    public synchronized void returnBufferedImage(BufferedImage buffImg) {
        Dimension dimension = new Dimension(buffImg.getWidth(), buffImg.getHeight());

        BufferedImageRecycler recycler = recyclers.get(dimension);

        if(recycler != null)
            recycler.returnBufferedImage((BufferedImageRecycler.RecyclableBufferedImage) buffImg);
    }
    
    public synchronized void flushAll() {
        for(BufferedImageRecycler recycler : recyclers.values()) {
            recycler.flushAll();
        }
    }

}