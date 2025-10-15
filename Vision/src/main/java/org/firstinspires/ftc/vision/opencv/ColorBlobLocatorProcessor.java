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

import android.graphics.Color;

import androidx.annotation.ColorInt;

import com.qualcomm.robotcore.util.SortOrder;

import org.firstinspires.ftc.vision.VisionProcessor;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The {@link ColorBlobLocatorProcessor} finds "blobs" of a user-specified color
 * in the image. You can restrict the search area to a specified Region
 * of Interest (ROI).
 */
public abstract class ColorBlobLocatorProcessor implements VisionProcessor
{
    /**
     * Class supporting construction of a {@link ColorBlobLocatorProcessor}
     */
    public static class Builder
    {
        private ColorRange colorRange;
        private ContourMode contourMode;
        private ImageRegion imageRegion = ImageRegion.entireFrame();
        private MorphOperationType morphOperationType = MorphOperationType.OPENING;
        private int erodeSize = -1;
        private int dilateSize = -1;
        private boolean drawContours = false;
        private int blurSize = -1;
        private int boundingBoxColor = Color.rgb(255, 120, 31);
        private int circleFitColor = 0;
        private int roiColor = Color.rgb(255, 255, 255);
        private int contourColor = Color.rgb(3, 227, 252);

        /**
         * Sets whether to draw the contour outline for the detected
         * blobs on the camera preview. This can be helpful for debugging
         * thresholding.
         * @param drawContours whether to draw contours on the camera preview
         * @return Builder object, to allow for method chaining
         */
        public Builder setDrawContours(boolean drawContours)
        {
            this.drawContours = drawContours;
            return this;
        }

        /**
         * Set the color used to draw the "best fit" bounding boxes for blobs
         * @param color Android color int or 0 to disable
         * @return Builder object, to allow for method chaining
         */
        public Builder setBoxFitColor(@ColorInt int color)
        {
            this.boundingBoxColor = color;
            return this;
        }

        /**
         * Set the color used to draw the enclosing circle around blobs
         * @param color Android color int or 0 to disable
         * @return Builder object, to allow for method chaining
         */
        public Builder setCircleFitColor(@ColorInt int color)
        {
            this.circleFitColor = color;
            return this;
        }

        /**
         * Set the color used to draw the ROI on the camera preview
         * @param color Android color int
         * @return Builder object, to allow for method chaining
         */
        public Builder setRoiColor(@ColorInt int color)
        {
            this.roiColor = color;
            return this;
        }

        /**
         * Set the color used to draw blob contours on the camera preview
         * @param color Android color int
         * @return Builder object, to allow for method chaining
         */
        public Builder setContourColor(@ColorInt int color)
        {
            this.contourColor = color;
            return this;
        }

        /**
         * Set the color range used to find blobs
         * @param colorRange the color range used to find blobs
         * @return Builder object, to allow for method chaining
         */
        public Builder setTargetColorRange(ColorRange colorRange)
        {
            this.colorRange = colorRange;
            return this;
        }

        /**
         * Set the contour mode which will be used when generating
         * the results provided by {@link #getBlobs()}
         * @param contourMode contour mode which will be used when generating
         *                    the results provided by {@link #getBlobs()}
         * @return Builder object, to allow for method chaining
         */
        public Builder setContourMode(ContourMode contourMode)
        {
            this.contourMode = contourMode;
            return this;
        }

        /**
         * Set the Region of Interest on which to perform blob detection
         * @param roi region of interest
         * @return Builder object, to allow for method chaining
         */
        public Builder setRoi(ImageRegion roi)
        {
            this.imageRegion = roi;
            return this;
        }

        /**
         * Set the size of the blur kernel. Blurring can improve
         * color thresholding results by smoothing color variation.
         * @param blurSize size of the blur kernel
         *                 0 to disable
         * @return Builder object, to allow for method chaining
         */
        public Builder setBlurSize(int blurSize)
        {
            this.blurSize = blurSize;
            return this;
        }

        /**
         * Set the type of morph operation to perform. Only relevant
         * if using both erosion and dilation.
         * @param morphOperationType type of morph operation to perform
         * @return Builder object, to allow for method chaining
         * @see #setErodeSize(int)
         * @see #setDilateSize(int)
         */
        public Builder setMorphOperationType(MorphOperationType morphOperationType)
        {
            this.morphOperationType = morphOperationType;
            return this;
        }

        /**
         * Set the size of the Erosion operation performed after applying
         * the color threshold. Erosion eats away at the mask, reducing
         * noise by eliminating super small areas, but also reduces the
         * contour areas of everything a little bit.
         * @param erodeSize size of the Erosion operation
         *                  0 to disable
         * @return Builder object, to allow for method chaining
         */
        public Builder setErodeSize(int erodeSize)
        {
            this.erodeSize = erodeSize;
            return this;
        }

        /**
         * Set the size of the Dilation operation performed after applying
         * the Erosion operation. Dilation expands mask areas, making up
         * for shrinkage caused during erosion, and can also clean up results
         * by closing small interior gaps in the mask.
         * @param dilateSize the size of the Dilation operation performed
         *                   0 to disable
         * @return Builder object, to allow for method chaining
         */
        public Builder setDilateSize(int dilateSize)
        {
            this.dilateSize = dilateSize;
            return this;
        }

        /**
         * Construct a {@link ColorBlobLocatorProcessor} object using previously
         * set parameters
         * @return a {@link  ColorBlobLocatorProcessor} object which can be attached
         * to your {@link org.firstinspires.ftc.vision.VisionPortal}
         */
        public ColorBlobLocatorProcessor build()
        {
            if (imageRegion == null)
            {
                throw new IllegalArgumentException("You must set a region of interest!");
            }

            if (colorRange == null)
            {
                throw new IllegalArgumentException("You must set a color range!");
            }

            if (contourMode == null)
            {
                throw new IllegalArgumentException("You must set a contour mode!");
            }

            return new ColorBlobLocatorProcessorImpl(colorRange, imageRegion, contourMode, morphOperationType, erodeSize, dilateSize, drawContours, blurSize, boundingBoxColor, circleFitColor, roiColor, contourColor);
        }
    }

    /**
     * Determines what you get in {@link #getBlobs()}
     */
    public enum ContourMode
    {
        /**
         * Only return blobs from external contours
         */
        EXTERNAL_ONLY,

        /**
         * Return blobs which may be from nested contours
         */
        ALL_FLATTENED_HIERARCHY
    }

    /**
     * Determines which compound morphological operation to perform on blobs
     */
    public enum MorphOperationType
    {
        /**
         * Performs erosion followed by dilation
         */
        OPENING,
        /**
         * Performs dilation followed by erosion
         */
        CLOSING
    }

    /**
     * The criteria used for filtering and sorting.
     */
    public enum BlobCriteria
    {
        BY_CONTOUR_AREA,
        BY_DENSITY,
        BY_ASPECT_RATIO,
        BY_ARC_LENGTH,
        BY_CIRCULARITY,
    }

    /**
     * Class describing how to filter blobs.
     */
    public static class BlobFilter {
        public final BlobCriteria criteria;
        public final double minValue;
        public final double maxValue;

        public BlobFilter(BlobCriteria criteria, double minValue,  double maxValue)
        {
            this.criteria = criteria;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }
    }

    /**
     * Class describing how to sort blobs.
     */
    public static class BlobSort
    {
        public final BlobCriteria criteria;
        public final SortOrder sortOrder;

        public BlobSort(BlobCriteria criteria, SortOrder sortOrder)
        {
            this.criteria = criteria;
            this.sortOrder = sortOrder;
        }
    }

    /**
     * Class describing a Blob of color found inside the image
     */
    public static abstract class Blob
    {
        /**
         * Get the OpenCV contour for this blob
         * @return OpenCV contour
         */
        public abstract MatOfPoint getContour();

        /**
         * Get the contour points for this blob
         * @return contour points for this blob
         */
        public abstract Point[] getContourPoints();

        /**
         * Get this contour as a MatOfPoint2f
         * @return a MatOfPoint2f of this contour
         */
        public abstract MatOfPoint2f getContourAsFloat();

        /**
         * Get the area enclosed by this blob's contour
         * @return area enclosed by this blob's contour
         */
        public abstract int getContourArea();

        /**
         * Get the density of this blob, i.e. ratio of
         * contour area to convex hull area
         * @return density of this blob
         */
        public abstract double getDensity();

        /**
         * Get the aspect ratio of this blob, i.e. the ratio
         * of longer side of the bounding box to the shorter side
         * @return aspect ratio of this blob
         */
        public abstract double getAspectRatio();

        /**
         * Get a "best fit" bounding box for this blob
         * @return "best fit" bounding box for this blob
         */
        public abstract RotatedRect getBoxFit();

        /**
         * Get the arc length of this blob
         * @return the arc length of this blob
         */
        public abstract double getArcLength();

        /**
         * Get the circularity of this blob
         * @return the circularity of this blob
         */
        public abstract double getCircularity();

        /**
         * Get the center Point and radius of the circle enclosing this blob
         * @return the center Point and radius of the circle enclosing this blob
         */
        public abstract Circle getCircle();
    }

    /**
     * Add a filter.
     */
    public abstract void addFilter(BlobFilter filter);

    /**
     * Remove a filter.
     */
    public abstract void removeFilter(BlobFilter filter);

    /**
     * Remove all filters.
     */
    public abstract void removeAllFilters();

    /**
     * Sets the sort.
     */
    public abstract void setSort(BlobSort sort);

    /**
     * Get the results of the most recent blob analysis
     * @return results of the most recent blob analysis
     */
    public abstract List<Blob> getBlobs();

    /**
     * Utility class for post-processing results from {@link #getBlobs()}
     */
    public static class Util
    {
        /**
         * Remove from a List of Blobs those which fail to meet a given criteria
         * @param criteria criteria by which to filter by
         * @param minValue minimum value
         * @param maxValue maximum value
         * @param blobs List of Blobs to operate on
         */
        public static void filterByCriteria(BlobCriteria criteria, double minValue, double maxValue, List<Blob> blobs)
        {
            ArrayList<Blob> toRemove = new ArrayList<>();

            for (Blob b : blobs)
            {
                double value = 0;
                switch (criteria)
                {
                    case BY_CONTOUR_AREA:
                        value = b.getContourArea();
                        break;
                    case BY_DENSITY:
                        value = b.getDensity();
                        break;
                    case BY_ASPECT_RATIO:
                        value = b.getAspectRatio();
                        break;
                    case BY_ARC_LENGTH:
                        value = b.getArcLength();
                        break;
                    case BY_CIRCULARITY:
                        value = b.getCircularity();
                        break;
                }

                if (value > maxValue || value < minValue)
                {
                    toRemove.add(b);
                }
            }

            blobs.removeAll(toRemove);
        }

        public static void sortByCriteria(BlobCriteria criteria, SortOrder sortOrder, List<Blob> blobs)
        {
            blobs.sort((c1, c2) -> {
                int tmp = 0;
                switch (criteria)
                {
                    case BY_CONTOUR_AREA:
                        tmp = (int)Math.signum(c2.getContourArea() - c1.getContourArea());
                        break;
                    case BY_DENSITY:
                        tmp = (int)Math.signum(c2.getDensity() - c1.getDensity());
                        break;
                    case BY_ASPECT_RATIO:
                        tmp = (int)Math.signum(c2.getAspectRatio() - c1.getAspectRatio());
                        break;
                    case BY_ARC_LENGTH:
                        tmp = (int)Math.signum(c2.getArcLength() - c1.getArcLength());
                        break;
                    case BY_CIRCULARITY:
                        tmp = (int)Math.signum(c2.getCircularity() - c1.getCircularity());
                        break;
                }

                if (sortOrder == SortOrder.ASCENDING)
                {
                    tmp = -tmp;
                }

                return tmp;
            });
        }

        /**
         * Remove from a List of Blobs those which fail to meet an area criteria
         * @param minArea minimum area
         * @param maxArea maximum area
         * @param blobs List of Blobs to operate on
         * @deprecated use {@link #filterByCriteria} instead
         */
        @Deprecated
        public static void filterByArea(double minArea, double maxArea, List<Blob> blobs)
        {
            ArrayList<Blob> toRemove = new ArrayList<>();

            for(Blob b : blobs)
            {
                if (b.getContourArea() > maxArea || b.getContourArea() < minArea)
                {
                    toRemove.add(b);
                }
            }

            blobs.removeAll(toRemove);
        }

        /**
         * Sort a list of Blobs based on area
         * @param sortOrder sort order
         * @param blobs List of Blobs to operate on
         * @deprecated use {@link #sortByCriteria} instead
         */
        @Deprecated
        public static void sortByArea(SortOrder sortOrder, List<Blob> blobs)
        {
            blobs.sort(new Comparator<Blob>()
            {
                public int compare(Blob c1, Blob c2)
                {
                    int tmp = (int)Math.signum(c2.getContourArea() - c1.getContourArea());

                    if (sortOrder == SortOrder.ASCENDING)
                    {
                        tmp = -tmp;
                    }

                    return tmp;
                }
            });
        }

        /**
         * Remove from a List of Blobs those which fail to meet a density criteria
         * @param minDensity minimum density
         * @param maxDensity maximum desnity
         * @param blobs List of Blobs to operate on
         * @deprecated use {@link #filterByCriteria} instead
         */
        @Deprecated
        public static void filterByDensity(double minDensity, double maxDensity, List<Blob> blobs)
        {
            ArrayList<Blob> toRemove = new ArrayList<>();

            for(Blob b : blobs)
            {
                if (b.getDensity() > maxDensity || b.getDensity() < minDensity)
                {
                    toRemove.add(b);
                }
            }

            blobs.removeAll(toRemove);
        }

        /**
         * Sort a list of Blobs based on density
         * @param sortOrder sort order
         * @param blobs List of Blobs to operate on
         * @deprecated use {@link #sortByCriteria} instead
         */
        @Deprecated
        public static void sortByDensity(SortOrder sortOrder, List<Blob> blobs)
        {
            blobs.sort(new Comparator<Blob>()
            {
                public int compare(Blob c1, Blob c2)
                {
                    int tmp = (int)Math.signum(c2.getDensity() - c1.getDensity());

                    if (sortOrder == SortOrder.ASCENDING)
                    {
                        tmp = -tmp;
                    }

                    return tmp;
                }
            });
        }

        /**
         * Remove from a List of Blobs those which fail to meet an aspect ratio criteria
         * @param minAspectRatio minimum aspect ratio
         * @param maxAspectRatio maximum aspect ratio
         * @param blobs List of Blobs to operate on
         * @deprecated use {@link #filterByCriteria} instead
         */
        @Deprecated
        public static void filterByAspectRatio(double minAspectRatio, double maxAspectRatio, List<Blob> blobs)
        {
            ArrayList<Blob> toRemove = new ArrayList<>();

            for(Blob b : blobs)
            {
                if (b.getAspectRatio() > maxAspectRatio || b.getAspectRatio() < minAspectRatio)
                {
                    toRemove.add(b);
                }
            }

            blobs.removeAll(toRemove);
        }

        /**
         * Sort a list of Blobs based on aspect ratio
         * @param sortOrder sort order
         * @param blobs List of Blobs to operate on
         * @deprecated use {@link #sortByCriteria} instead
         */
        @Deprecated
        public static void sortByAspectRatio(SortOrder sortOrder, List<Blob> blobs)
        {
            blobs.sort(new Comparator<Blob>()
            {
                public int compare(Blob c1, Blob c2)
                {
                    int tmp = (int)Math.signum(c2.getAspectRatio() - c1.getAspectRatio());

                    if (sortOrder == SortOrder.ASCENDING)
                    {
                        tmp = -tmp;
                    }

                    return tmp;
                }
            });
        }
    }
}
