/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package io.github.deltacv.common.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * A class that allows for recycling BufferedImages with different dimensions.
 */
public class DynamicBufferedImageRecycler {

    private final HashMap<Dimension, BufferedImageRecycler> recyclers = new HashMap<>();

    /**
     * Get a BufferedImage with the specified dimensions.
     * @param size The size of the BufferedImage
     * @param recyclerSize If a recycler with the specified dimensions doesn't exist, this is the size of the new recycler
     * @return A BufferedImage with the specified dimensions
     */
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

    /**
     * Return a BufferedImage to the recycler.
     * @param buffImg The BufferedImage to return
     */
    public synchronized void returnBufferedImage(BufferedImage buffImg) {
        Dimension dimension = new Dimension(buffImg.getWidth(), buffImg.getHeight());

        BufferedImageRecycler recycler = recyclers.get(dimension);

        if(recycler != null)
            recycler.returnBufferedImage((BufferedImageRecycler.RecyclableBufferedImage) buffImg);
    }

    /**
     * Flush all BufferedImages in all recyclers.
     * Should be called when the recycler is no longer needed to free up memory.
     */
    public synchronized void flushAll() {
        for(BufferedImageRecycler recycler : recyclers.values()) {
            recycler.flushAll();
        }
    }

}
