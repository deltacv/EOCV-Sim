package com.qualcomm.robotcore.hardware;

import io.github.deltacv.vision.source.SourceHander;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.openftc.easyopencv.OpenCvViewport;

public class HardwareMap {

    public HardwareMap(SourceHander sourceHander, OpenCvViewport viewport) {
    }

    public static boolean hasSuperclass(Class<?> clazz, Class<?> superClass) {
        try {
            clazz.asSubclass(superClass);
            return true;
        } catch (ClassCastException ex) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> classType, String deviceName) {
        if(hasSuperclass(classType, CameraName.class)) {
           //  return (T) new QueueOpenCvCamera(cameraFramesQueue, viewportOutputQueue);
        }

        return null;
    }

}