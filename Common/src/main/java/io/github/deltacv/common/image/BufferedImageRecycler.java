/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package io.github.deltacv.common.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * A class to recycle BufferedImages, to avoid creating and disposing them
 * constantly, which can be very slow.
 * This class is thread-safe.
 */
public class BufferedImageRecycler {

    private final RecyclableBufferedImage[] allBufferedImages;
    private final ArrayBlockingQueue<RecyclableBufferedImage> availableBufferedImages;

    /**
     * Creates a new BufferedImageRecycler with the specified number of BufferedImages
     * @param num The number of BufferedImages to create
     * @param allImgWidth The width of all BufferedImages
     * @param allImgHeight The height of all BufferedImages
     * @param allImgType The type of all BufferedImages
     */
    public BufferedImageRecycler(int num, int allImgWidth, int allImgHeight, int allImgType) {
        allBufferedImages = new RecyclableBufferedImage[num];
        availableBufferedImages = new ArrayBlockingQueue<>(num);

        for (int i = 0; i < allBufferedImages.length; i++) {
            allBufferedImages[i] = new RecyclableBufferedImage(i, allImgWidth, allImgHeight, allImgType);
            availableBufferedImages.add(allBufferedImages[i]);
        }
    }

    /**
     * Creates a new BufferedImageRecycler with the specified number of BufferedImages
     * @param num The number of BufferedImages to create
     * @param allImgSize The size of all BufferedImages
     * @param allImgType The type of all BufferedImages
     */
    public BufferedImageRecycler(int num, Dimension allImgSize, int allImgType) {
        this(num, (int)allImgSize.getWidth(), (int)allImgSize.getHeight(), allImgType);
    }

    /**
     * Creates a new BufferedImageRecycler with the specified number of BufferedImages
     * @param num The number of BufferedImages to create
     * @param allImgWidth The width of all BufferedImages
     * @param allImgHeight The height of all BufferedImages
     */
    public BufferedImageRecycler(int num, int allImgWidth, int allImgHeight) {
        this(num, allImgWidth, allImgHeight, BufferedImage.TYPE_3BYTE_BGR);
    }

    /**
     * Creates a new BufferedImageRecycler with the specified number of BufferedImages
     * And with a default type of BufferedImage.TYPE_3BYTE_BGR
     * @param num The number of BufferedImages to create
     * @param allImgSize The size of all BufferedImages
     */
    public BufferedImageRecycler(int num, Dimension allImgSize) {
        this(num, (int)allImgSize.getWidth(), (int)allImgSize.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
    }

    /**
     * Checks if at least one BufferedImage is currently being used
     * @return True if at least one BufferedImage is being used, false otherwise
     */
    public boolean isOnUse() { return allBufferedImages.length != availableBufferedImages.size(); }

    /**
     * Takes a BufferedImage from the recycler
     * @throws RuntimeException If all BufferedImages have been checked out
     * @return A BufferedImage from the recycler
     */
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

    /**
     * Returns a BufferedImage to the recycler
     * @param buffImg The BufferedImage to return
     */
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

    /**
     * Flushes all BufferedImages in the recycler
     * This method should be called when the recycler is no longer needed
     * to free up memory
     */
    public synchronized void flushAll() {
        for(BufferedImage img : allBufferedImages) {
            img.flush();
        }
    }

    /**
     * A BufferedImage that can be recycled by a BufferedImageRecycler
     */
    public static class RecyclableBufferedImage extends BufferedImage {
        private int idx = -1;
        private volatile boolean checkedOut = false;

        /**
         * Creates a new RecyclableBufferedImage
         * @param idx The index of the BufferedImage in the recycler
         * @param width The width of the BufferedImage
         * @param height The height of the BufferedImage
         * @param imageType The type of the BufferedImage
         */
        private RecyclableBufferedImage(int idx, int width, int height, int imageType) {
            super(width, height, imageType);
            this.idx = idx;
        }
    }

}

