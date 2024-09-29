package com.qualcomm.robotcore.hardware;

import io.github.deltacv.vision.external.source.ThreadSourceHander;
import io.github.deltacv.vision.internal.source.ftc.SourcedCameraNameImpl;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;

public class HardwareMap {

    private static boolean hasSuperclass(Class<?> clazz, Class<?> superClass) {
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
            return (T) new SourcedCameraNameImpl(ThreadSourceHander.hand(deviceName));
        }

        throw new IllegalArgumentException("Unknown device type " + classType.getName());
    }

}