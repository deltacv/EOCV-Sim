/*
 * Copyright (c) 2023 FIRST
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

package org.firstinspires.ftc.vision.apriltag;

import android.graphics.Canvas;
import android.util.Log;

import com.qualcomm.robotcore.util.MovingStatistics;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.matrices.GeneralMatrixF;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.imgproc.Imgproc;
import org.openftc.apriltag.AprilTagDetectorJNI;
import org.openftc.apriltag.ApriltagDetectionJNI;

import java.util.ArrayList;

public class AprilTagProcessorImpl extends AprilTagProcessor
{
    public static final String TAG = "AprilTagProcessorImpl";

    private long nativeApriltagPtr;
    private Mat grey = new Mat();
    private ArrayList<AprilTagDetection> detections = new ArrayList<>();

    private ArrayList<AprilTagDetection> detectionsUpdate = new ArrayList<>();
    private final Object detectionsUpdateSync = new Object();
    private boolean drawAxes;
    private boolean drawCube;
    private boolean drawOutline;
    private boolean drawTagID;

    private Mat cameraMatrix;

    private double fx;
    private double fy;
    private double cx;
    private double cy;

    private final AprilTagLibrary tagLibrary;

    private float decimation;
    private boolean needToSetDecimation;
    private final Object decimationSync = new Object();

    private AprilTagCanvasAnnotator canvasAnnotator;

    private final DistanceUnit outputUnitsLength;
    private final AngleUnit outputUnitsAngle;

    private volatile PoseSolver poseSolver = PoseSolver.OPENCV_ITERATIVE;

    public AprilTagProcessorImpl(double fx, double fy, double cx, double cy, DistanceUnit outputUnitsLength, AngleUnit outputUnitsAngle, AprilTagLibrary tagLibrary, boolean drawAxes, boolean drawCube, boolean drawOutline, boolean drawTagID, TagFamily tagFamily, int threads)
    {
        this.fx = fx;
        this.fy = fy;
        this.cx = cx;
        this.cy = cy;

        this.tagLibrary = tagLibrary;
        this.outputUnitsLength = outputUnitsLength;
        this.outputUnitsAngle = outputUnitsAngle;
        this.drawAxes = drawAxes;
        this.drawCube = drawCube;
        this.drawOutline = drawOutline;
        this.drawTagID = drawTagID;

        // Allocate a native context object. See the corresponding deletion in the finalizer
        nativeApriltagPtr = AprilTagDetectorJNI.createApriltagDetector(tagFamily.ATLibTF.string, 3, threads);
    }

    @Override
    protected void finalize()
    {
        // Might be null if createApriltagDetector() threw an exception
        if(nativeApriltagPtr != 0)
        {
            // Delete the native context we created in the constructor
            AprilTagDetectorJNI.releaseApriltagDetector(nativeApriltagPtr);
            nativeApriltagPtr = 0;
        }
        else
        {
            System.out.println("AprilTagDetectionPipeline.finalize(): nativeApriltagPtr was NULL");
        }
    }

    @Override
    public void init(int width, int height, CameraCalibration calibration)
    {
        // If the user didn't give us a calibration, but we have one built in,
        // then go ahead and use it!!
        if (calibration != null && fx == 0 && fy == 0 && cx == 0 && cy == 0
            && !(calibration.focalLengthX == 0 && calibration.focalLengthY == 0 && calibration.principalPointX == 0 && calibration.principalPointY == 0)) // needed because we may get an all zero calibration to indicate none, instead of null
        {
            fx = calibration.focalLengthX;
            fy = calibration.focalLengthY;
            cx = calibration.principalPointX;
            cy = calibration.principalPointY;

            Log.d(TAG, String.format("User did not provide a camera calibration; but we DO have a built in calibration we can use.\n [%dx%d] (may be scaled) %s\nfx=%7.3f fy=%7.3f cx=%7.3f cy=%7.3f",
                    calibration.getSize().getWidth(), calibration.getSize().getHeight(), calibration.getIdentity().toString(), fx, fy, cx, cy));
        }
        else if (fx == 0 && fy == 0 && cx == 0 && cy == 0)
        {
            // set it to *something* so we don't crash the native code

            String warning = "User did not provide a camera calibration, nor was a built-in calibration found for this camera; 6DOF pose data will likely be inaccurate.";
            Log.d(TAG, warning);
            RobotLog.addGlobalWarningMessage(warning);

            fx = 578.272;
            fy = 578.272;
            cx = width/2;
            cy = height/2;
        }
        else
        {
            Log.d(TAG, String.format("User provided their own camera calibration fx=%7.3f fy=%7.3f cx=%7.3f cy=%7.3f",
                    fx, fy, cx, cy));
        }

        constructMatrix();

        canvasAnnotator = new AprilTagCanvasAnnotator(cameraMatrix);
    }

    @Override
    public Object processFrame(Mat input, long captureTimeNanos)
    {
        // Convert to greyscale
        Imgproc.cvtColor(input, grey, Imgproc.COLOR_RGBA2GRAY);

        synchronized (decimationSync)
        {
            if(needToSetDecimation)
            {
                AprilTagDetectorJNI.setApriltagDetectorDecimation(nativeApriltagPtr, decimation);
                needToSetDecimation = false;
            }
        }

        // Run AprilTag
        detections = runAprilTagDetectorForMultipleTagSizes(captureTimeNanos);

        synchronized (detectionsUpdateSync)
        {
            detectionsUpdate = detections;
        }

        // TODO do we need to deep copy this so the user can't mess with it before use in onDrawFrame()?
        return detections;
    }

    private MovingStatistics solveTime = new MovingStatistics(50);

    // We cannot use runAprilTagDetectorSimple because we cannot assume tags are all the same size
    ArrayList<AprilTagDetection> runAprilTagDetectorForMultipleTagSizes(long captureTimeNanos)
    {
        long ptrDetectionArray = AprilTagDetectorJNI.runApriltagDetector(nativeApriltagPtr, grey.dataAddr(), grey.width(), grey.height());
        if (ptrDetectionArray != 0)
        {
            long[] detectionPointers = ApriltagDetectionJNI.getDetectionPointers(ptrDetectionArray);
            ArrayList<AprilTagDetection> detections = new ArrayList<>(detectionPointers.length);

            for (long ptrDetection : detectionPointers)
            {
                AprilTagDetection detection = new AprilTagDetection();
                detection.frameAcquisitionNanoTime = captureTimeNanos;

                detection.id = ApriltagDetectionJNI.getId(ptrDetection);

                AprilTagMetadata metadata = tagLibrary.lookupTag(detection.id);
                detection.metadata = metadata;

                detection.hamming = ApriltagDetectionJNI.getHamming(ptrDetection);
                detection.decisionMargin = ApriltagDetectionJNI.getDecisionMargin(ptrDetection);
                double[] center = ApriltagDetectionJNI.getCenterpoint(ptrDetection);
                detection.center = new Point(center[0], center[1]);
                double[][] corners = ApriltagDetectionJNI.getCorners(ptrDetection);

                detection.corners = new Point[4];
                for (int p = 0; p < 4; p++)
                {
                    detection.corners[p] = new Point(corners[p][0], corners[p][1]);
                }

                if (metadata != null)
                {
                    PoseSolver solver = poseSolver; // snapshot, can change

                    detection.rawPose = new AprilTagPoseRaw();

                    long startSolveTime = System.currentTimeMillis();

                    if (solver == PoseSolver.APRILTAG_BUILTIN)
                    {
                        // Translation
                        double[] pose = ApriltagDetectionJNI.getPoseEstimate(
                                ptrDetection,
                                outputUnitsLength.fromUnit(metadata.distanceUnit, metadata.tagsize),
                                fx, fy, cx, cy);

                        detection.rawPose.x = pose[0];
                        detection.rawPose.y = pose[1];
                        detection.rawPose.z = pose[2];

                        // Rotation
                        float[] rotMtxVals = new float[3 * 3];
                        for (int i = 0; i < 9; i++)
                        {
                            rotMtxVals[i] = (float) pose[3 + i];
                        }
                        detection.rawPose.R = new GeneralMatrixF(3, 3, rotMtxVals);
                    }
                    else
                    {
                        Pose opencvPose = poseFromTrapezoid(
                                detection.corners,
                                cameraMatrix,
                                outputUnitsLength.fromUnit(metadata.distanceUnit, metadata.tagsize),
                                solver.code);

                        detection.rawPose.x = opencvPose.tvec.get(0,0)[0];
                        detection.rawPose.y = opencvPose.tvec.get(1,0)[0];
                        detection.rawPose.z = opencvPose.tvec.get(2,0)[0];

                        Mat R = new Mat(3, 3, CvType.CV_32F);
                        Calib3d.Rodrigues(opencvPose.rvec, R);

                        float[] tmp2 = new float[9];
                        R.get(0,0, tmp2);
                        detection.rawPose.R = new GeneralMatrixF(3,3, tmp2);
                    }

                    long endSolveTime = System.currentTimeMillis();
                    solveTime.add(endSolveTime-startSolveTime);
                }
                else
                {
                    // We don't know anything about the tag size so we can't solve the pose
                    detection.rawPose = null;
                }

                if (detection.rawPose != null)
                {
                    detection.ftcPose = new AprilTagPoseFtc();

                    detection.ftcPose.x =  detection.rawPose.x;
                    detection.ftcPose.y =  detection.rawPose.z;
                    detection.ftcPose.z = -detection.rawPose.y;

                    Orientation rot = Orientation.getOrientation(detection.rawPose.R, AxesReference.INTRINSIC, AxesOrder.YXZ, outputUnitsAngle);
                    detection.ftcPose.yaw = -rot.firstAngle;
                    detection.ftcPose.roll = rot.thirdAngle;
                    detection.ftcPose.pitch = rot.secondAngle;

                    detection.ftcPose.range = Math.hypot(detection.ftcPose.x, detection.ftcPose.y);
                    detection.ftcPose.bearing = outputUnitsAngle.fromUnit(AngleUnit.RADIANS, Math.atan2(-detection.ftcPose.x, detection.ftcPose.y));
                    detection.ftcPose.elevation = outputUnitsAngle.fromUnit(AngleUnit.RADIANS, Math.atan2(detection.ftcPose.z, detection.ftcPose.y));
                }

                detections.add(detection);
            }

            ApriltagDetectionJNI.freeDetectionList(ptrDetectionArray);
            return detections;
        }

        return new ArrayList<>();
    }

    private final Object drawSync = new Object();

    @Override
    public void onDrawFrame(Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext)
    {
        // Only one draw operation at a time thank you very much.
        // (we could be called from two different threads - viewport or camera stream)
        synchronized (drawSync)
        {
            if ((drawAxes || drawCube || drawOutline || drawTagID) && userContext != null)
            {
                canvasAnnotator.noteDrawParams(scaleBmpPxToCanvasPx, scaleCanvasDensity);

                ArrayList<AprilTagDetection> dets = (ArrayList<AprilTagDetection>) userContext;

                // For fun, draw 6DOF markers on the image.
                for(AprilTagDetection detection : dets)
                {
                    if (drawTagID)
                    {
                        canvasAnnotator.drawTagID(detection, canvas);
                    }

                    // Could be null if we couldn't solve the pose earlier due to not knowing tag size
                    if (detection.rawPose != null)
                    {
                        AprilTagMetadata metadata = tagLibrary.lookupTag(detection.id);
                        double tagSize = outputUnitsLength.fromUnit(metadata.distanceUnit, metadata.tagsize);

                        if (drawOutline)
                        {
                            canvasAnnotator.drawOutlineMarker(detection, canvas, tagSize);
                        }
                        if (drawAxes)
                        {
                            canvasAnnotator.drawAxisMarker(detection, canvas, tagSize);
                        }
                        if (drawCube)
                        {
                            canvasAnnotator.draw3dCubeMarker(detection, canvas, tagSize);
                        }
                    }
                }
            }
        }
    }

    public void setDecimation(float decimation)
    {
        synchronized (decimationSync)
        {
            this.decimation = decimation;
            needToSetDecimation = true;
        }
    }

    @Override
    public void setPoseSolver(PoseSolver poseSolver)
    {
        this.poseSolver = poseSolver;
    }

    @Override
    public int getPerTagAvgPoseSolveTime()
    {
        return (int) Math.round(solveTime.getMean());
    }

    public ArrayList<AprilTagDetection> getDetections()
    {
        return detections;
    }

    public ArrayList<AprilTagDetection> getFreshDetections()
    {
        synchronized (detectionsUpdateSync)
        {
            ArrayList<AprilTagDetection> ret = detectionsUpdate;
            detectionsUpdate = null;
            return ret;
        }
    }

    void constructMatrix()
    {
        //     Construct the camera matrix.
        //
        //      --         --
        //     | fx   0   cx |
        //     | 0    fy  cy |
        //     | 0    0   1  |
        //      --         --
        //

        cameraMatrix = new Mat(3,3, CvType.CV_32FC1);

        cameraMatrix.put(0,0, fx);
        cameraMatrix.put(0,1,0);
        cameraMatrix.put(0,2, cx);

        cameraMatrix.put(1,0,0);
        cameraMatrix.put(1,1,fy);
        cameraMatrix.put(1,2,cy);

        cameraMatrix.put(2, 0, 0);
        cameraMatrix.put(2,1,0);
        cameraMatrix.put(2,2,1);
    }

    /**
     * Converts an AprilTag pose to an OpenCV pose
     * @param aprilTagPose pose to convert
     * @return OpenCV output pose
     */
    static Pose aprilTagPoseToOpenCvPose(AprilTagPoseRaw aprilTagPose)
    {
        Pose pose = new Pose();
        pose.tvec.put(0,0, aprilTagPose.x);
        pose.tvec.put(1,0, aprilTagPose.y);
        pose.tvec.put(2,0, aprilTagPose.z);

        Mat R = new Mat(3, 3, CvType.CV_32F);

        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                R.put(i,j, aprilTagPose.R.get(i,j));
            }
        }

        Calib3d.Rodrigues(R, pose.rvec);

        return pose;
    }

    /**
     * Extracts 6DOF pose from a trapezoid, using a camera intrinsics matrix and the
     * original size of the tag.
     *
     * @param points the points which form the trapezoid
     * @param cameraMatrix the camera intrinsics matrix
     * @param tagsize the original length of the tag
     * @return the 6DOF pose of the camera relative to the tag
     */
    static Pose poseFromTrapezoid(Point[] points, Mat cameraMatrix, double tagsize, int solveMethod)
    {
        // The actual 2d points of the tag detected in the image
        MatOfPoint2f points2d = new MatOfPoint2f(points);

        // The 3d points of the tag in an 'ideal projection'
        Point3[] arrayPoints3d = new Point3[4];
        arrayPoints3d[0] = new Point3(-tagsize/2, tagsize/2, 0);
        arrayPoints3d[1] = new Point3(tagsize/2, tagsize/2, 0);
        arrayPoints3d[2] = new Point3(tagsize/2, -tagsize/2, 0);
        arrayPoints3d[3] = new Point3(-tagsize/2, -tagsize/2, 0);
        MatOfPoint3f points3d = new MatOfPoint3f(arrayPoints3d);

        // Using this information, actually solve for pose
        Pose pose = new Pose();
        Calib3d.solvePnP(points3d, points2d, cameraMatrix, new MatOfDouble(), pose.rvec, pose.tvec, false, solveMethod);

        return pose;
    }

    /*
     * A simple container to hold both rotation and translation
     * vectors, which together form a 6DOF pose.
     */
    static class Pose
    {
        Mat rvec;
        Mat tvec;

        public Pose()
        {
            rvec = new Mat(3, 1, CvType.CV_32F);
            tvec = new Mat(3, 1, CvType.CV_32F);
        }

        public Pose(Mat rvec, Mat tvec)
        {
            this.rvec = rvec;
            this.tvec = tvec;
        }
    }
}