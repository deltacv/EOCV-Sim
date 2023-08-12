package org.openftc.easyopencv;

import io.github.deltacv.vision.external.SourcedOpenCvCamera;
import io.github.deltacv.vision.external.source.VisionSource;
import io.github.deltacv.vision.external.source.ThreadSourceHander;
import io.github.deltacv.vision.external.source.ViewportAndSourceHander;
import io.github.deltacv.vision.internal.source.ftc.SourcedCameraName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;

public class SourcedOpenCvCameraFactoryImpl extends OpenCvCameraFactory {


    private OpenCvViewport viewport() {
        if(ThreadSourceHander.threadHander() instanceof ViewportAndSourceHander) {
            return ((ViewportAndSourceHander) ThreadSourceHander.threadHander()).viewport();
        }

        return null;
    }

    private VisionSource source(String name) {
        if(ThreadSourceHander.threadHander() instanceof ViewportAndSourceHander) {
            return ThreadSourceHander.threadHander().hand(name);
        }

        return null;
    }

    @Override
    public OpenCvCamera createInternalCamera(OpenCvInternalCamera.CameraDirection direction) {
        return new SourcedOpenCvCamera(source("default"), viewport());
    }

    @Override
    public OpenCvCamera createInternalCamera(OpenCvInternalCamera.CameraDirection direction, int viewportContainerId) {
        return createInternalCamera(direction);
    }

    @Override
    public OpenCvCamera createInternalCamera2(OpenCvInternalCamera2.CameraDirection direction) {
        return new SourcedOpenCvCamera(source("default"), viewport());
    }

    @Override
    public OpenCvCamera createInternalCamera2(OpenCvInternalCamera2.CameraDirection direction, int viewportContainerId) {
        return createInternalCamera2(direction);
    }

    @Override
    public OpenCvWebcam createWebcam(WebcamName cameraName) {
        if(cameraName instanceof SourcedCameraName) {
            return new SourcedOpenCvCamera(((SourcedCameraName) cameraName).getSource(), viewport());
        } else {
            throw new IllegalArgumentException("cameraName is not compatible with SourcedOpenCvCamera");
        }
    }

    @Override
    public OpenCvWebcam createWebcam(WebcamName cameraName, int viewportContainerId) {
        return createWebcam(cameraName);
    }

}
