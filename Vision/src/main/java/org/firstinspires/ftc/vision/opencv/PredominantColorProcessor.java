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

import org.firstinspires.ftc.vision.VisionProcessor;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@link PredominantColorProcessor} acts like a "Color Sensor",
 * allowing you to define a Region of Interest (ROI) of the camera
 * stream inside of which the dominant color is found. Additionally,
 * said color is matched to one of the {@link Swatch}s specified by
 * the user as a "best guess" at the general shade of the color
 */
public abstract class PredominantColorProcessor implements VisionProcessor
{
    /**
     * Class supporting construction of a {@link PredominantColorProcessor}
     */
    public static class Builder
    {
        ImageRegion roi;
        Swatch[] swatches;

        /**
         * Set the Region of Interest on which to perform color analysis
         * @param roi region of interest
         * @return Builder object, to allow for method chaining
         */
        public Builder setRoi(ImageRegion roi)
        {
            this.roi = roi;
            return this;
        }

        /**
         * Set the Swatches from which a "best guess" at the shade of the
         * predominant color will be made
         * @param swatches Swatches to choose from
         * @return Builder object, to allow for method chaining
         */
        public Builder setSwatches(Swatch... swatches)
        {
            this.swatches = swatches;
            return this;
        }

        /**
         * Construct a {@link PredominantColorProcessor} object using previously
         * set parameters
         * @return a {@link  PredominantColorProcessor} object which can be attached
         * to your {@link org.firstinspires.ftc.vision.VisionPortal}
         */
        public PredominantColorProcessor build()
        {
            if (roi == null)
            {
                throw new IllegalArgumentException("You must call setRoi()!");
            }

            if (swatches == null)
            {
                throw new IllegalArgumentException("You must call setSwatches()!");
            }

            return new PredominantColorProcessorImpl(roi, swatches);
        }
    }

    /**
     * Get the result of the most recent color analysis
     * @return result of the most recent color analysis
     */
    public abstract Result getAnalysis();

    /**
     * Class describing the result of color analysis on the ROI
     */
    public static class Result
    {
        /**
         * "Best guess" at the general shade of the dominant color in the ROI
         */
        public final Swatch closestSwatch;

        /**
         * Exact numerical value of the dominant color in the ROI
         */
        public final int rgb;

        public Result(Swatch closestSwatch, int rgb)
        {
            this.closestSwatch = closestSwatch;
            this.rgb = rgb;
        }
    }

    /**
     * Swatches from which you may choose from when invoking
     * {@link Builder#setSwatches(Swatch...)}
     */
    public enum Swatch
    {
        RED(0),
        ORANGE(30),
        YELLOW(46),
        GREEN(120),
        CYAN(180),
        BLUE(240),
        PURPLE(270),
        MAGENTA(300),
        BLACK(-1),
        WHITE(-2);

        final int hue;

        // hue range 0-360
        Swatch(int hue)
        {
            this.hue = hue;
        }

        private static Map map = new HashMap<>();

        static
        {
            for (Swatch swatch : Swatch.values())
            {
                map.put(swatch.hue, swatch);
            }
        }

        public static Swatch valueOf(int swatch)
        {
            return (Swatch) map.get(swatch);
        }
    }
}
