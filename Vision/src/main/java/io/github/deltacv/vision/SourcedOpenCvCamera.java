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
        handedViewport.setRenderHook((canvas, onscreenWidth, onscreenHeight, scaleBmpPxToCanvasPx, scaleCanvasDensity, context) -> {
            OpenCvViewport.FrameContext frameContext = (OpenCvViewport.FrameContext) context;

            // We must make sure that we call onDrawFrame() for the same pipeline which set the
            // context object when requesting a draw hook. (i.e. we can't just call onDrawFrame()
            // for whatever pipeline happens to be currently attached; it might have an entirely
            // different notion of what to expect in the context object)
            if (frameContext.generatingPipeline != null)
            {
                frameContext.generatingPipeline.onDrawFrame(canvas, onscreenWidth, onscreenHeight, scaleBmpPxToCanvasPx, scaleCanvasDensity, frameContext.userContext);
            }
        });

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