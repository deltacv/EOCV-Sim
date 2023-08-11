package io.github.deltacv.vision;

import android.graphics.Canvas;
import io.github.deltacv.vision.source.Source;
import io.github.deltacv.vision.source.Sourced;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.openftc.easyopencv.*;

public class SourcedOpenCvCamera extends OpenCvCameraBase implements Sourced {

    private final Source source;
    OpenCvViewport handedViewport = null;

    boolean streaming = false;

    public SourcedOpenCvCamera(Source source, OpenCvViewport handedViewport) {
        this.source = source;
        this.handedViewport = handedViewport;
    }

    @Override
    public int openCameraDevice() {
        return source.init();
    }

    @Override
    public void openCameraDeviceAsync(AsyncCameraOpenListener cameraOpenListener) {
        new Thread(() -> {
            int code = openCameraDevice();

            if(code == 0) {
                cameraOpenListener.onOpened();
            } else {
                cameraOpenListener.onError(code);
            }
        }).start();
    }

    @Override
    public void closeCameraDevice() {
        synchronized (source) {
            source.close();
        }
    }

    @Override
    public void closeCameraDeviceAsync(AsyncCameraCloseListener cameraCloseListener) {
        new Thread(() -> {
            closeCameraDevice();
            cameraCloseListener.onClose();
        }).start();
    }

    @Override
    public void startStreaming(int width, int height) {
        synchronized (source) {
            source.start(new Size(width, height));
            source.attach(this);
        }

        streaming = true;
    }

    @Override
    public void startStreaming(int width, int height, OpenCvCameraRotation rotation) {
        startStreaming(width, height);
    }

    @Override
    public void stopStreaming() {
        source.stop();
    }

    @Override
    protected OpenCvViewport setupViewport() {
        handedViewport.setRenderHook(PipelineRenderHook.INSTANCE);

        return handedViewport;
    }

    @Override
    protected OpenCvCameraRotation getDefaultRotation() {
        return OpenCvCameraRotation.UPRIGHT;
    }

    @Override
    protected int mapRotationEnumToOpenCvRotateCode(OpenCvCameraRotation rotation) {
        return 0;
    }

    @Override
    protected boolean cameraOrientationIsTiedToDeviceOrientation() {
        return false;
    }

    @Override
    protected boolean isStreaming() {
        return false;
    }

    @Override
    public void onNewFrame(Mat frame, long timestamp) {
        handleFrameUserCrashable(frame, timestamp);
    }
}