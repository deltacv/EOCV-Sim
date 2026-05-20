/*
 * Copyright (c) 2021 OpenFTC Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.openftc.apriltag;

import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
import org.firstinspires.ftc.robotcore.external.matrices.GeneralMatrixF;
import org.opencv.core.Point;
import org.wpilib.vision.apriltag.AprilTagPoseEstimate;
import org.wpilib.vision.apriltag.AprilTagPoseEstimator;

import java.util.ArrayList;

public class ApriltagDetectionJNI
{
    /**
     * Get the tag ID of a detection
     * @param ptr pointer to a detection, obtained from {@link #getDetectionPointers(long)}
     * @return the tag ID
     */
    public static int getId(long ptr)
    {
        return getSingleDetection(ptr).getId();
    }

    /**
     * Get the hamming of a detection
     * @param ptr pointer to a detection, obtained from {@link #getDetectionPointers(long)}
     * @return the hamming value
     */
    public static int getHamming(long ptr)
    {
        return getSingleDetection(ptr).getHamming();
    }

    /**
     * Get the decision margin of a detection
     * @param ptr pointer to a detection, obtained from {@link #getDetectionPointers(long)}
     * @return the decision margin of the detection
     */
    public static float getDecisionMargin(long ptr)
    {
        return getSingleDetection(ptr).getDecisionMargin();
    }

    /**
     * Get the centerpoint of a detection
     * @param ptr pointer to a detection, obtained from {@link #getDetectionPointers(long)}
     * @return the centerpoint of the detection
     */
    public static double[] getCenterpoint(long ptr)
    {
        org.wpilib.vision.apriltag.AprilTagDetection det = getSingleDetection(ptr);
        return new double[]{ det.getCenterX(), det.getCenterY() };
    }

    /**
     * Get the corners of a detection
     * @param ptr pointer to a detection, obtained from {@link #getDetectionPointers(long)}
     * @return the corners of the detection
     */
    public static double[][] getCorners(long ptr)
    {
        org.wpilib.vision.apriltag.AprilTagDetection det = getSingleDetection(ptr);

        // WPILib gives corners as a flat double[] of length 8: [x0,y0, x1,y1, x2,y2, x3,y3]
        double[] flat = det.getCorners();

        double[][] corners = new double[4][2];
        for (int i = 0; i < 4; i++)
        {
            corners[i][0] = flat[i * 2];
            corners[i][1] = flat[i * 2 + 1];
        }
        return corners;
    }

    /**
     * Get the pose estimate for a detection
     * @param ptr pointer to a detection, obtained from {@link #getDetectionPointers(long)}
     * @param tagSize size of the tag in meters
     * @param fx lens intrinsics fx
     * @param fy lens intrinsics fy
     * @param cx lens intrinsics cx
     * @param cy lens intrinsics cy
     * @return pose estimate for the detection. 0-2 are translation XYZ, 3-5 are yaw, pitch, roll
     */
    public static double[] getPoseEstimate(long ptr, double tagSize, double fx, double fy, double cx, double cy)
    {
        org.wpilib.vision.apriltag.AprilTagDetection det = getSingleDetection(ptr);

        AprilTagPoseEstimator estimator = new AprilTagPoseEstimator(
                new AprilTagPoseEstimator.Config(tagSize, fx, fy, cx, cy));

        AprilTagPoseEstimate result = estimator.estimateOrthogonalIteration(det, 50);

        Transform3d pose = (result.error1 <= result.error2) ? result.pose1 : result.pose2;

        double tx = pose.getTranslation().getX();
        double ty = pose.getTranslation().getY();
        double tz = pose.getTranslation().getZ();

        Rotation3d rot = pose.getRotation();
        double[] rotMatrix = quaternionToRotationMatrix(
                rot.getQuaternion().getW(),
                rot.getQuaternion().getX(),
                rot.getQuaternion().getY(),
                rot.getQuaternion().getZ()
        );

        double[] out = new double[12];
        out[0] = tx;
        out[1] = ty;
        out[2] = tz;
        System.arraycopy(rotMatrix, 0, out, 3, 9);
        return out;
    }

    /**
     * Get a pointer for each of the detections inside a list returned by {@link AprilTagDetectorJNI#runApriltagDetector(long, long, int, int)}
     * @param ptrZarray native pointer from {@link AprilTagDetectorJNI#runApriltagDetector(long, long, int, int)}
     * @return an array of native pointers to detections inside the list
     */
    public static long[] getDetectionPointers(long ptrZarray)
    {
        org.wpilib.vision.apriltag.AprilTagDetection[] detections = AprilTagDetectionCache.getDetections(ptrZarray);
        if (detections == null) return new long[0];

        long[] ptrs = new long[detections.length];
        for (int i = 0; i < detections.length; i++)
        {
            ptrs[i] = AprilTagDetectionCache.cacheDetections(new org.wpilib.vision.apriltag.AprilTagDetection[]{ detections[i] });
        }
        return ptrs;
    }

    /**
     * Frees a list of detections obtained from {@link AprilTagDetectorJNI#runApriltagDetector(long, long, int, int)}
     * AND the detections themselves. (Thus, after calling this, any pointers you may have previously obtained
     * from {@link #getDetectionPointers(long)} are INVALID)
     * @param ptrDetections native pointer to a list of detections
     */
    public static void freeDetectionList(long ptrDetections)
    {
        AprilTagDetectionCache.freeDetections(ptrDetections);
    }

    /**
     * Creates a nice decoupled java representation of the detections in the native detection list
     * @param ptrDetections native pointer from {@link AprilTagDetectorJNI#runApriltagDetector(long, long, int, int)}
     * @param tagSize size of the tag in meters
     * @param fx lens intrinsics fx
     * @param fy lens intrinsics fy
     * @param cx lens intrinsics cx
     * @param cy lens intrinsics cy
     * @return a nice decoupled java representation of the detections in the native detection list
     */
    public static ArrayList<AprilTagDetection> getDetections(long ptrDetections, double tagSize, double fx, double fy, double cx, double cy)
    {
        long[] detectionPointers = getDetectionPointers(ptrDetections);
        ArrayList<AprilTagDetection> detections = new ArrayList<>(detectionPointers.length);

        for (long ptrDetection : detectionPointers)
        {
            AprilTagDetection detection = new AprilTagDetection();
            detection.id = getId(ptrDetection);
            detection.hamming = getHamming(ptrDetection);
            detection.decisionMargin = getDecisionMargin(ptrDetection);
            double[] center = getCenterpoint(ptrDetection);
            detection.center = new Point(center[0], center[1]);
            double[][] corners = getCorners(ptrDetection);

            detection.corners = new Point[4];
            for (int p = 0; p < 4; p++)
            {
                detection.corners[p] = new Point(corners[p][0], corners[p][1]);
            }

            detection.pose = new AprilTagPose();
            double[] pose = getPoseEstimate(ptrDetection, tagSize, fx, fy, cx, cy);
            detection.pose.x = pose[0];
            detection.pose.y = pose[1];
            detection.pose.z = pose[2];

            float[] rotMtxVals = new float[3*3];

            for (int i = 0; i < 9; i++)
            {
                rotMtxVals[i] = (float) pose[3 + i];
            }

            detection.pose.R = new GeneralMatrixF(3, 3, rotMtxVals);

            // Free the individual detection's cache entry now that we're done with it
            AprilTagDetectionCache.freeDetections(ptrDetection);

            detections.add(detection);
        }

        return detections;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Retrieves the single WPILib detection stored under a per-detection fake pointer.
     */
    private static org.wpilib.vision.apriltag.AprilTagDetection getSingleDetection(long ptr)
    {
        org.wpilib.vision.apriltag.AprilTagDetection[] arr = AprilTagDetectionCache.getDetections(ptr);
        if (arr == null || arr.length == 0)
            throw new IllegalStateException("No detection found for pointer: " + ptr);
        return arr[0];
    }

    /**
     * Converts a unit quaternion (w, x, y, z) to a row-major 3x3 rotation matrix.
     */
    private static double[] quaternionToRotationMatrix(double w, double x, double y, double z)
    {
        double[] m = new double[9];

        m[0] = 1 - 2*(y*y + z*z);
        m[1] =     2*(x*y - z*w);
        m[2] =     2*(x*z + y*w);

        m[3] =     2*(x*y + z*w);
        m[4] = 1 - 2*(x*x + z*z);
        m[5] =     2*(y*z - x*w);

        m[6] =     2*(x*z - y*w);
        m[7] =     2*(y*z + x*w);
        m[8] = 1 - 2*(x*x + y*y);

        return m;
    }
}