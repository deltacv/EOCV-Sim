/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package io.github.deltacv.vision.external.util;

import io.github.deltacv.vision.external.util.extension.CvExt;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CvUtil {

    public static void matToBufferedImage(Mat m, BufferedImage buffImg) {
        // Get the BufferedImage's backing array and copy the pixels directly into it
        byte[] data = ((DataBufferByte) buffImg.getRaster().getDataBuffer()).getData();
        m.get(0, 0, data);
    }

    private static BufferedImage convertTo3ByteBGRType(BufferedImage image) {
        BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR);
        convertedImage.getGraphics().drawImage(image, 0, 0, null);
        return convertedImage;
    }

    public static void bufferedImageToMat(BufferedImage buffImg, Mat m) {
        boolean flushLater = false;
        if(buffImg.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            buffImg = convertTo3ByteBGRType(buffImg);
            flushLater = true;
        }

        byte[] data = ((DataBufferByte) buffImg.getRaster().getDataBuffer()).getData();
        m.put(0, 0, data);

        if(flushLater) {
            buffImg.flush();
        }
    }

    public static BufferedImage matToBufferedImage(Mat m) {
        // Fastest code
        // output can be assigned either to a BufferedImage or to an Image
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (m.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }

        // Create an empty image in matching format
        BufferedImage buffImg = new BufferedImage(m.width(), m.height(), type);
        matToBufferedImage(m, buffImg);

        return buffImg;
    }


    public static boolean checkImageValid(String imagePath) {
        try {

            //test if image is valid
            Mat img = Imgcodecs.imread(imagePath);

            if (img != null && !img.empty()) { //image is valid
                img.release();
                return true;
            } else { //image is not valid
                return false;
            }

        } catch (Throwable ex) {
            return false;
        }
    }

    public static boolean checkVideoValid(String videoPath) {
        try {

            VideoCapture capture = new VideoCapture();

            Mat img = new Mat();

            capture.open(videoPath);
            capture.read(img);
            capture.release();

            if (!img.empty()) { //image is valid
                img.release();
                return true;
            } else { //image is not valid
                img.release();
                return false;
            }

        } catch (Exception ex) {
            return false;
        }
    }

    public static Size getImageSize(String imagePath) {
        try {

            //test if image is valid
            Mat img = Imgcodecs.imread(imagePath);

            if (img != null && !img.empty()) { //image is valid
                Size size = img.size();
                img.release();
                return size;
            } else { //image is not valid
                return new Size(0, 0);
            }

        } catch (Exception ex) {
            return new Size(0, 0);
        }
    }

    public static Size getVideoSize(String videoPath) {
        try {

            VideoCapture capture = new VideoCapture();

            Mat img = new Mat();

            capture.open(videoPath);
            capture.read(img);
            capture.release();

            Size size = img.size();
            img.release();

            return size;

        } catch (Exception ex) {
            return new Size();
        }
    }

    public static Mat readOnceFromVideo(String videoPath) {
        VideoCapture capture = new VideoCapture();

        Mat img = new Mat();

        try {
            capture.open(videoPath);
            capture.read(img);
        } finally {
            capture.release();
        }

        return img;
    }

    public static Size scaleToFit(Size currentSize, Size targetSize) {
        double targetAspectRatio = CvExt.aspectRatio(targetSize);
        double currentAspectRatio = CvExt.aspectRatio(currentSize);

        if(currentAspectRatio == targetAspectRatio) {
            return targetSize.clone();
        } else {

            double currentW = currentSize.width;
            double currentH = currentSize.height;

            double widthRatio = targetSize.width / currentW;
            double heightRatio = targetSize.height / currentH;
            double bestRatio = Math.min(widthRatio, heightRatio);

            return new Size(currentW * bestRatio, currentH * bestRatio);
        }
    }

}

