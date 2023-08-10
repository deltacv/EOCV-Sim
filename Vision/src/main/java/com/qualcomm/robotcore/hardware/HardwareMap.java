package com.qualcomm.robotcore.external.hardware;

import com.qualcomm.robotcore.hardware.camera.CameraName;
import io.github.deltacv.vision.util.FrameQueue;
import org.openftc.easyopencv.QueueOpenCvCamera;

public class HardwareMap {

    private FrameQueue cameraFramesQueue;
    private FrameQueue viewportOutputQueue;


    public HardwareMap(FrameQueue cameraFramesQueue, FrameQueue viewportOutputQueue) {
        this.cameraFramesQueue = cameraFramesQueue;
        this.viewportOutputQueue = viewportOutputQueue;
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
            return (T) new QueueOpenCvCamera(cameraFramesQueue, viewportOutputQueue);
        }

        return null;
    }

}