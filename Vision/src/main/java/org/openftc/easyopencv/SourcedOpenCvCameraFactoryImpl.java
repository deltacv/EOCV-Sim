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
        return new SourcedOpenCvCamera(source("default"), viewport(), false);
    }

    @Override
    public OpenCvCamera createInternalCamera(OpenCvInternalCamera.CameraDirection direction, int viewportContainerId) {
        if(viewportContainerId <= 0) {
            return createInternalCamera(direction);
        }

        return new SourcedOpenCvCamera(source("default"), viewport(), true);
    }

    @Override
    public OpenCvCamera createInternalCamera2(OpenCvInternalCamera2.CameraDirection direction) {
        return new SourcedOpenCvCamera(source("default"), viewport(), false);
    }

    @Override
    public OpenCvCamera createInternalCamera2(OpenCvInternalCamera2.CameraDirection direction, int viewportContainerId) {
        if(viewportContainerId <= 0) {
            return createInternalCamera2(direction);
        }

        return new SourcedOpenCvCamera(source("default"), viewport(), true);
    }

    @Override
    public OpenCvWebcam createWebcam(WebcamName cameraName) {
        if(cameraName instanceof SourcedCameraName) {
            return new SourcedOpenCvCamera(((SourcedCameraName) cameraName).getSource(), viewport(), false);
        } else {
            throw new IllegalArgumentException("cameraName is not compatible with SourcedOpenCvCamera");
        }
    }

    @Override
    public OpenCvWebcam createWebcam(WebcamName cameraName, int viewportContainerId) {
        if(viewportContainerId <= 0) {
            return createWebcam(cameraName);
        }

        if(cameraName instanceof SourcedCameraName) {
            return new SourcedOpenCvCamera(((SourcedCameraName) cameraName).getSource(), viewport(), true);
        } else {
            throw new IllegalArgumentException("cameraName is not compatible with SourcedOpenCvCamera");
        }
    }

}
