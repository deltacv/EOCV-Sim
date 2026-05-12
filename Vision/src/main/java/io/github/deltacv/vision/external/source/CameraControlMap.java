/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package io.github.deltacv.vision.external.source;

import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.CameraControl;

public interface CameraControlMap {

    <T extends CameraControl> T get(Class<T> classType);

}
