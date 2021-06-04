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
import java.util.concurrent.ArrayBlockingQueue;

public class BufferedImageRecycler {

    private final RecyclableBufferedImage[] allBufferedImages;
    private final ArrayBlockingQueue<RecyclableBufferedImage> availableBufferedImages;

    public BufferedImageRecycler(int num, int allImgWidth, int allImgHeight, int allImgType) {
        allBufferedImages = new RecyclableBufferedImage[num];
        availableBufferedImages = new ArrayBlockingQueue<>(num);

        for (int i = 0; i < allBufferedImages.length; i++) {
            allBufferedImages[i] = new RecyclableBufferedImage(i, allImgWidth, allImgHeight, allImgType);
            availableBufferedImages.add(allBufferedImages[i]);
        }
    }

    public BufferedImageRecycler(int num, Dimension allImgSize, int allImgType) {
        this(num, (int)allImgSize.getWidth(), (int)allImgSize.getHeight(), allImgType);
    }

    public BufferedImageRecycler(int num, int allImgWidth, int allImgHeight) {
        this(num, allImgWidth, allImgHeight, BufferedImage.TYPE_3BYTE_BGR);
    }

    public BufferedImageRecycler(int num, Dimension allImgSize) {
        this(num, (int)allImgSize.getWidth(), (int)allImgSize.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
    }

    public boolean isOnUse() { return allBufferedImages.length != availableBufferedImages.size(); }

    public synchronized RecyclableBufferedImage takeBufferedImage() {

        if (availableBufferedImages.size() == 0) {
            throw new RuntimeException("All buffered images have been checked out!");
        }

        RecyclableBufferedImage buffImg = null;
        try {
            buffImg = availableBufferedImages.take();
            buffImg.checkedOut = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return buffImg;

    }

    public synchronized void returnBufferedImage(RecyclableBufferedImage buffImg) {
        if (buffImg != allBufferedImages[buffImg.idx]) {
            throw new IllegalArgumentException("This BufferedImage does not belong to this recycler!");
        }

        if (buffImg.checkedOut) {
            buffImg.checkedOut = false;
            buffImg.flush();
            availableBufferedImages.add(buffImg);
        } else {
            throw new IllegalArgumentException("This BufferedImage has already been returned!");
        }
    }

    public synchronized void flushAll() {
        for(BufferedImage img : allBufferedImages) {
            img.flush();
        }
    }

    public static class RecyclableBufferedImage extends BufferedImage {
        private int idx = -1;
        private volatile boolean checkedOut = false;

        private RecyclableBufferedImage(int idx, int width, int height, int imageType) {
            super(width, height, imageType);
            this.idx = idx;
        }
    }

}
