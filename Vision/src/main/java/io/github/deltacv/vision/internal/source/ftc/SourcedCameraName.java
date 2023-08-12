package io.github.deltacv.vision.internal.source.ftc;

import io.github.deltacv.vision.external.source.VisionSource;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCharacteristics;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;

public abstract class SourcedCameraName implements WebcamName {

    public abstract VisionSource getSource();

    @Override
    public boolean isWebcam() {
        return false;
    }

    @Override
    public boolean isCameraDirection() {
        return false;
    }

    @Override
    public boolean isSwitchable() {
        return false;
    }

    @Override
    public boolean isUnknown() {
        return false;
    }

    @Override
    public CameraCharacteristics getCameraCharacteristics() {
        return null;
    }

}
