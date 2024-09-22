/*
 * Copyright (c) 2024 FIRST
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.vision.opencv;

import com.qualcomm.robotcore.util.Range;
import org.opencv.core.Rect;

/**
 * An {@link ImageRegion} defines an area of an image buffer in terms of either a typical
 * image processing coordinate system wherein the origin is in the top left corner and
 * the domain of X and Y is dictated by the resolution of said image buffer; OR a "unity center"
 * coordinate system wherein the origin is at the middle of the image and the domain of
 * X and Y is {-1, 1} such that the region can be defined independent of the actual resolution
 * of the image buffer.
 */
public class ImageRegion
{
    final boolean imageCoords;
    final double left, top, right, bottom;

    /**
     * Internal constructor
     * @param imageCoords whether these coordinates are typical image processing coordinates
     * @param left left coordinate
     * @param top  top coordinate
     * @param right right coordiante
     * @param bottom bottom coordinate
     */
    private ImageRegion(boolean imageCoords, double left, double top, double right, double bottom )
    {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.imageCoords = imageCoords;
    }

    /**
     * Construct an {@link ImageRegion} using typical image processing coordinates
     *
     *  --------------------------------------------
     *  | (0,0)-------X                            |
     *  |  |                                       |
     *  |  |                                       |
     *  |  Y                                       |
     *  |                                          |
     *  |                           (width,height) |
     *  --------------------------------------------
     *
     * @param left left X coordinate {0, width}
     * @param top top Y coordinate {0, height}
     * @param right right X coordinate {0, width}
     * @param bottom bottom Y coordinate {0, height}
     * @return an {@link ImageRegion} object describing the region
     */
    public static ImageRegion asImageCoordinates(int left, int top, int right, int bottom )
    {
        return new ImageRegion(true, left, top, right, bottom);
    }

    /**
     * Construct an {@link ImageRegion} using "Unity Center" coordinates
     *
     *  --------------------------------------------
     *  | (-1,1)             Y               (1,1) |
     *  |                    |                     |
     *  |                    |                     |
     *  |                  (0,0) ----- X           |
     *  |                                          |
     *  | (-1,-1)                          (1, -1) |
     *  --------------------------------------------
     *
     * @param left left X coordinate {-1, 1}
     * @param top top Y coordinate {-1, 1}
     * @param right right X coordinate {-1, 1}
     * @param bottom bottom Y coordinate {-1, 1}
     * @return an {@link ImageRegion} object describing the region
     */
    public static ImageRegion asUnityCenterCoordinates(double left, double top, double right, double bottom)
    {
        return new ImageRegion(false, left, top, right, bottom);
    }

    /**
     * Construct an {@link ImageRegion} representing the entire frame
     * @return an {@link ImageRegion} representing the entire frame
     */
    public static ImageRegion entireFrame()
    {
        return ImageRegion.asUnityCenterCoordinates(-1, 1, 1, -1);
    }

    /**
     * Create an OpenCV Rect object which is representative of this {@link ImageRegion}
     * for a specific image buffer size
     *
     * @param imageWidth width of the image buffer
     * @param imageHeight height of the image buffer
     * @return OpenCV Rect
     */
    protected Rect asOpenCvRect(int imageWidth, int imageHeight)
    {
        Rect rect = new Rect();

        if (imageCoords)
        {
            rect.x = (int) left;
            rect.y = (int) top;
            rect.width = (int) (right - left);
            rect.height = (int) (bottom - top);
        }
        else // unity center
        {
            rect.x = (int) Range.scale(left, -1, 1, 0, imageWidth);
            rect.y = (int) ( imageHeight - Range.scale(top, -1, 1, 0, imageHeight));
            rect.width = (int) Range.scale(right - left, 0, 2, 0, imageWidth);
            rect.height = (int) Range.scale(top - bottom, 0, 2, 0, imageHeight);
        }

        // Adjust the window position to ensure it stays on the screen.  push it back into the screen area.
        // We could just crop it instead, but then it may completely miss the screen.
        rect.x = Math.max(rect.x, 0);
        rect.x = Math.min(rect.x, imageWidth - rect.width);
        rect.y = Math.max(rect.y, 0);
        rect.y = Math.min(rect.y, imageHeight - rect.height);

        return rect;
    }
}
