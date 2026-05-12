/*
 * Copyright (c) 2019 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.openftc.easyopencv;

import android.hardware.Camera;

public interface OpenCvInternalCamera2 extends OpenCvCamera
{
    enum CameraDirection
    {
        FRONT(Camera.CameraInfo.CAMERA_FACING_FRONT),
        BACK(Camera.CameraInfo.CAMERA_FACING_BACK);

        public int id;

        CameraDirection(int id)
        {
            this.id = id;
        }
    }
}

