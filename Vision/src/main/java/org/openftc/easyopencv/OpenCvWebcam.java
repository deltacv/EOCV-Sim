/*
 * Copyright (c) 2020 OpenFTC Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.openftc.easyopencv;

public interface OpenCvWebcam extends OpenCvCamera {

    default void startStreaming(int width, int height, OpenCvCameraRotation cameraRotation, StreamFormat eocvStreamFormat) {
        startStreaming(width, height, cameraRotation);
    }

    enum StreamFormat
    {
        // The only format that was supported historically; it is uncompressed but
        // chroma subsampled and uses lots of bandwidth - this limits frame rate
        // at higher resolutions and also limits the ability to use two cameras
        // on the same bus to lower resolutions
        YUY2,

        // Compressed motion JPEG stream format; allows for higher resolutions at
        // full frame rate, and better ability to use two cameras on the same bus.
        // Requires extra CPU time to run decompression routine.
        MJPEG;
    }

}
