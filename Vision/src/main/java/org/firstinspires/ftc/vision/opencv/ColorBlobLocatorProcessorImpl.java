package org.firstinspires.ftc.vision.opencv;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import androidx.annotation.ColorInt;

import com.qualcomm.robotcore.util.SortOrder;

import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.firstinspires.ftc.vision.VisionProcessor;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

class ColorBlobLocatorProcessorImpl extends ColorBlobLocatorProcessor implements VisionProcessor
{
    private ColorRange colorRange;
    private ImageRegion roiImg;
    private Rect roi;
    private int frameWidth;
    private int frameHeight;
    private Mat roiMat;
    private Mat roiMat_userColorSpace;
    private final int contourCode;

    private Mat mask = new Mat();

    private final Paint boundingRectPaint;
    private final Paint circleFitPaint;
    private final Paint roiPaint;
    private final Paint contourPaint;
    private final boolean drawContours;
    private final @ColorInt int boundingBoxColor;
    private final @ColorInt int roiColor;
    private final @ColorInt int contourColor;

    private final MorphOperationType morphOperationType;
    private final Mat erodeElement;
    private final Mat dilateElement;
    private final Size blurElement;

    private final Object lockFilters = new Object();
    private final List<BlobFilter> filters = new ArrayList<>();
    private volatile BlobSort sort;

    private volatile ArrayList<Blob> userBlobs = new ArrayList<>();

    ColorBlobLocatorProcessorImpl(ColorRange colorRange, ImageRegion roiImg, ContourMode contourMode,
                                  MorphOperationType morphOperationType, int erodeSize, int dilateSize,
                                  boolean drawContours, int blurSize, @ColorInt int boundingBoxColor,
                                  @ColorInt int circleFitColor, @ColorInt int roiColor, @ColorInt int contourColor)
    {
        this.colorRange = colorRange;
        this.roiImg = roiImg;
        this.drawContours = drawContours;
        this.boundingBoxColor = boundingBoxColor;
        this.roiColor = roiColor;
        this.contourColor = contourColor;
        this.morphOperationType = morphOperationType;

        if (blurSize > 0)
        {
            // enforce Odd blurSize
            blurElement = new Size(blurSize | 0x01, blurSize | 0x01);
        }
        else
        {
            blurElement = null;
        }

        if (contourMode == ContourMode.EXTERNAL_ONLY)
        {
            contourCode = Imgproc.RETR_EXTERNAL;
        }
        else
        {
            contourCode = Imgproc.RETR_LIST;
        }

        if (erodeSize > 0)
        {
            erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(erodeSize, erodeSize));
        }
        else
        {
            erodeElement = null;
        }

        if (dilateSize > 0)
        {
            dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(dilateSize, dilateSize));
        }
        else
        {
            dilateElement = null;
        }

        if (boundingBoxColor != 0)
        {
            boundingRectPaint = new Paint();
            boundingRectPaint.setAntiAlias(true);
            boundingRectPaint.setStrokeCap(Paint.Cap.BUTT);
            boundingRectPaint.setColor(boundingBoxColor);
        }
        else
        {
            boundingRectPaint = null;
        }

        if (circleFitColor != 0)
        {
            circleFitPaint = new Paint();
            circleFitPaint.setAntiAlias(true);
            circleFitPaint.setStrokeCap(Paint.Cap.BUTT);
            circleFitPaint.setStyle(Paint.Style.STROKE);
            circleFitPaint.setColor(circleFitColor);
        }
        else
        {
            circleFitPaint = null;
        }

        roiPaint = new Paint();
        roiPaint.setAntiAlias(true);
        roiPaint.setStrokeCap(Paint.Cap.BUTT);
        roiPaint.setColor(roiColor);

        contourPaint = new Paint();
        contourPaint.setStyle(Paint.Style.STROKE);
        contourPaint.setColor(contourColor);
    }

    @Override
    public void init(int width, int height, CameraCalibration calibration)
    {
        frameWidth = width;
        frameHeight = height;

        roi = roiImg.asOpenCvRect(width, height);
    }

    @Override
    public Object processFrame(Mat frame, long captureTimeNanos)
    {
        if (roiMat == null)
        {
            roiMat = frame.submat(roi);
            roiMat_userColorSpace = roiMat.clone();
        }

        if (colorRange.colorSpace == ColorSpace.YCrCb)
        {
            Imgproc.cvtColor(roiMat, roiMat_userColorSpace, Imgproc.COLOR_RGB2YCrCb);
        }
        else if (colorRange.colorSpace == ColorSpace.HSV)
        {
            Imgproc.cvtColor(roiMat, roiMat_userColorSpace, Imgproc.COLOR_RGB2HSV);
        }
        else if (colorRange.colorSpace == ColorSpace.RGB)
        {
            Imgproc.cvtColor(roiMat, roiMat_userColorSpace, Imgproc.COLOR_RGBA2RGB);
        }

        if (blurElement != null)
        {
            Imgproc.GaussianBlur(roiMat_userColorSpace, roiMat_userColorSpace, blurElement, 0);
        }

        Core.inRange(roiMat_userColorSpace, colorRange.min, colorRange.max, mask);

        switch (morphOperationType)
        {
            case OPENING:
                if (erodeElement != null)
                {
                    Imgproc.erode(mask, mask, erodeElement);
                }

                if (dilateElement != null)
                {
                    Imgproc.dilate(mask, mask, dilateElement);
                }
                break;

            case CLOSING:
                if (dilateElement != null)
                {
                    Imgproc.dilate(mask, mask, dilateElement);
                }

                if (erodeElement != null)
                {
                    Imgproc.erode(mask, mask, erodeElement);
                }
                break;
        }

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy, contourCode, Imgproc.CHAIN_APPROX_SIMPLE);
        hierarchy.release();

        ArrayList<Blob> blobs = new ArrayList<>();
        for (MatOfPoint contour : contours)
        {
            Core.add(contour, new Scalar(roi.x, roi.y), contour);
            blobs.add(new BlobImpl(contour));
        }

        // Apply filters.
        synchronized (lockFilters)
        {
            for (BlobFilter filter : filters)
            {
                Util.filterByCriteria(filter.criteria, filter.minValue, filter.maxValue, blobs);
            }
        }

        // Apply sorting.
        BlobSort sort = this.sort; // Put the field into a local variable for thread safety.
        if (sort != null)
        {
            Util.sortByCriteria(sort.criteria, sort.sortOrder, blobs);
        }
        else
        {
            // Apply a default sort by area
            Util.sortByCriteria(BlobCriteria.BY_CONTOUR_AREA, SortOrder.DESCENDING, blobs);
        }

        // Deep copy this to prevent concurrent modification exception
        userBlobs = new ArrayList<>(blobs);

        return blobs;
    }

    @Override
    public void onDrawFrame(Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext)
    {
        ArrayList<Blob> blobs = (ArrayList<Blob>) userContext;

        contourPaint.setStrokeWidth(scaleCanvasDensity * 4);
        if (boundingRectPaint != null)
        {
            boundingRectPaint.setStrokeWidth(scaleCanvasDensity * 10);
        }
        if (circleFitPaint != null)
        {
            circleFitPaint.setStrokeWidth(scaleCanvasDensity * 10);
        }
        roiPaint.setStrokeWidth(scaleCanvasDensity * 10);

        android.graphics.Rect gfxRect = makeGraphicsRect(roi, scaleBmpPxToCanvasPx);

        for (Blob blob : blobs)
        {
            if (drawContours)
            {
                Path path = new Path();

                Point[] contourPts = blob.getContourPoints();

                path.moveTo((float) (contourPts[0].x) * scaleBmpPxToCanvasPx, (float)(contourPts[0].y) * scaleBmpPxToCanvasPx);
                for (int i = 1; i < contourPts.length; i++)
                {
                    path.lineTo((float) (contourPts[i].x) * scaleBmpPxToCanvasPx, (float) (contourPts[i].y) * scaleBmpPxToCanvasPx);
                }
                path.close();

                canvas.drawPath(path, contourPaint);
            }

            /*
             * Draws a rotated rect by drawing each of the 4 lines individually
             */
            if (boundingRectPaint != null)
            {
                Point[] rotRectPts = new Point[4];
                blob.getBoxFit().points(rotRectPts);

                for (int i = 0; i < 4; ++i)
                {
                    canvas.drawLine(
                            (float) (rotRectPts[i].x) * scaleBmpPxToCanvasPx, (float) (rotRectPts[i].y) * scaleBmpPxToCanvasPx,
                            (float) (rotRectPts[(i + 1) % 4].x) * scaleBmpPxToCanvasPx, (float) (rotRectPts[(i + 1) % 4].y) * scaleBmpPxToCanvasPx,
                            boundingRectPaint
                    );
                }
            }

            if (circleFitPaint != null)
            {
                Circle circle = blob.getCircle();
                canvas.drawCircle(
                        circle.getX() * scaleBmpPxToCanvasPx, circle.getY() * scaleBmpPxToCanvasPx,
                        circle.getRadius() * scaleBmpPxToCanvasPx, circleFitPaint
                );
            }
        }

        canvas.drawLine(gfxRect.left, gfxRect.top, gfxRect.right, gfxRect.top, roiPaint);
        canvas.drawLine(gfxRect.right, gfxRect.top, gfxRect.right, gfxRect.bottom, roiPaint);
        canvas.drawLine(gfxRect.right, gfxRect.bottom, gfxRect.left, gfxRect.bottom, roiPaint);
        canvas.drawLine(gfxRect.left, gfxRect.bottom, gfxRect.left, gfxRect.top, roiPaint);
    }

    private android.graphics.Rect makeGraphicsRect(Rect rect, float scaleBmpPxToCanvasPx)
    {
        int left = Math.round(rect.x * scaleBmpPxToCanvasPx);
        int top = Math.round(rect.y * scaleBmpPxToCanvasPx);
        int right = left + Math.round(rect.width * scaleBmpPxToCanvasPx);
        int bottom = top + Math.round(rect.height * scaleBmpPxToCanvasPx);

        return new android.graphics.Rect(left, top, right, bottom);
    }

    @Override
    public void addFilter(BlobFilter filter)
    {
        synchronized (lockFilters)
        {
            filters.add(filter);
        }
    }

    @Override
    public void removeFilter(BlobFilter filter)
    {
        synchronized (lockFilters)
        {
            filters.remove(filter);
        }
    }

    @Override
    public void removeAllFilters()
    {
        synchronized (lockFilters)
        {
            filters.clear();
        }
    }

    @Override
    public void setSort(BlobSort sort)
    {
        this.sort = sort;
    }

    @Override
    public List<Blob> getBlobs()
    {
        return userBlobs;
    }

    class BlobImpl extends Blob
    {
        private MatOfPoint contour;
        private Point[] contourPts;
        private MatOfPoint2f contourAsFloat;
        private int area = -1;
        private double density = -1;
        private double aspectRatio = -1;
        private RotatedRect rect;
        private double arcLength = -1;
        private double circularity = -1;
        private Circle circle;

        BlobImpl(MatOfPoint contour)
        {
            this.contour = contour;
        }

        @Override
        public MatOfPoint getContour()
        {
            return contour;
        }

        @Override
        public Point[] getContourPoints()
        {
            if (contourPts == null)
            {
                contourPts = contour.toArray();
            }

            return contourPts;
        }

        @Override
        public MatOfPoint2f getContourAsFloat() {
            if (contourAsFloat == null) {
                contourAsFloat = new MatOfPoint2f(getContourPoints());
            }

            return contourAsFloat;
        }

        @Override
        public int getContourArea()
        {
            if (area < 0)
            {
                area = Math.max(1, (int) Imgproc.contourArea(contour));  //  Fix zero area issue
            }

            return area;
        }

        @Override
        public double getDensity()
        {
            Point[] contourPts = getContourPoints();

            if (density < 0)
            {
                // Compute the convex hull of the contour
                MatOfInt hullMatOfInt = new MatOfInt();
                Imgproc.convexHull(contour, hullMatOfInt);

                // The convex hull calculation tells us the INDEX of the points which
                // which were passed in eariler which form the convex hull. That's all
                // well and good, but now we need filter out that original list to find
                // the actual POINTS which form the convex hull
                Point[] hullPoints = new Point[hullMatOfInt.rows()];
                List<Integer> hullContourIdxList = hullMatOfInt.toList();

                for (int i = 0; i < hullContourIdxList.size(); i++)
                {
                    hullPoints[i] = contourPts[hullContourIdxList.get(i)];
                }

                double hullArea = Math.max(1.0,Imgproc.contourArea(new MatOfPoint(hullPoints)));  //  Fix zero area issue

                density = getContourArea() / hullArea;
            }
            return density;
        }

        @Override
        public double getAspectRatio()
        {
            if (aspectRatio < 0)
            {
                RotatedRect r = getBoxFit();

                double longSize  = Math.max(1, Math.max(r.size.width, r.size.height));
                double shortSize = Math.max(1, Math.min(r.size.width, r.size.height));

                aspectRatio = longSize / shortSize;
            }

            return aspectRatio;
        }

        @Override
        public RotatedRect getBoxFit()
        {
            if (rect == null)
            {
                rect = Imgproc.minAreaRect(getContourAsFloat());
            }
            return rect;
        }

        @Override
        public double getArcLength()
        {
            if (arcLength < 0)
            {
                arcLength = Imgproc.arcLength(getContourAsFloat(), true);
            }

            return arcLength;
        }

        @Override
        public double getCircularity()
        {
            if (circularity < 0)
            {
                circularity = 4 * Math.PI * (getContourArea() / Math.pow(getArcLength(), 2));
            }

            return circularity;
        }

        @Override
        public Circle getCircle()
        {
            if (circle == null)
            {
                Point center = new Point();
                float[] radius = new float[1];
                Imgproc.minEnclosingCircle(getContourAsFloat(), center, radius);
                circle = new Circle(center, radius[0]);
            }

            return circle;
        }
    }
}
