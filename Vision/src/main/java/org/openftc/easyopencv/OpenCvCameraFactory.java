package org.openftc.easyopencv;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;

public abstract class OpenCvCameraFactory {

    private static OpenCvCameraFactory instance = new SourcedOpenCvCameraFactoryImpl();

    public static OpenCvCameraFactory getInstance() {
        return instance;
    }

    /*
     * Internal
     */
    public abstract OpenCvCamera createInternalCamera(OpenCvInternalCamera.CameraDirection direction);
    public abstract OpenCvCamera createInternalCamera(OpenCvInternalCamera.CameraDirection direction, int viewportContainerId);

    /*
     * Internal2
     */
    public abstract OpenCvCamera createInternalCamera2(OpenCvInternalCamera2.CameraDirection direction);
    public abstract OpenCvCamera createInternalCamera2(OpenCvInternalCamera2.CameraDirection direction, int viewportContainerId);

    /*
     * Webcam
     */
    public abstract OpenCvWebcam createWebcam(WebcamName cameraName);
    public abstract OpenCvWebcam createWebcam(WebcamName cameraName, int viewportContainerId);

    public enum ViewportSplitMethod
    {
        VERTICALLY,
        HORIZONTALLY
    }

}
