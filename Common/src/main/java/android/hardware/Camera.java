package android.hardware;

public class Camera {


    /**
     * Information about a camera
     */
    public static class CameraInfo {
        /**
         * The facing of the camera is opposite to that of the screen.
         */
        public static final int CAMERA_FACING_BACK = 0;
        /**
         * The facing of the camera is the same as that of the screen.
         */
        public static final int CAMERA_FACING_FRONT = 1;
        /**
         * The direction that the camera faces. It should be
         * CAMERA_FACING_BACK or CAMERA_FACING_FRONT.
         */
        public int facing;
        /**
         * <p>The orientation of the camera image. The value is the angle that the
         * camera image needs to be rotated clockwise so it shows correctly on
         * the display in its natural orientation. It should be 0, 90, 180, or 270.</p>
         *
         * <p>For example, suppose a device has a naturally tall screen. The
         * back-facing camera sensor is mounted in landscape. You are looking at
         * the screen. If the top side of the camera sensor is aligned with the
         * right edge of the screen in natural orientation, the value should be
         * 90. If the top side of a front-facing camera sensor is aligned with
         * the right of the screen, the value should be 270.</p>
         *
         * see #setDisplayOrientation(int)
         * see Parameters#setRotation(int)
         * see Parameters#setPreviewSize(int, int)
         * see Parameters#setPictureSize(int, int)
         * see Parameters#setJpegThumbnailSize(int, int)
         */
        public int orientation;
        /**
         * <p>Whether the shutter sound can be disabled.</p>
         *
         * <p>On some devices, the camera shutter sound cannot be turned off
         * through {link #enableShutterSound enableShutterSound}. This field
         * can be used to determine whether a call to disable the shutter sound
         * will succeed.</p>
         *
         * <p>If this field is set to true, then a call of
         * {@code enableShutterSound(false)} will be successful. If set to
         * false, then that call will fail, and the shutter sound will be played
         * when {link Camera#takePicture takePicture} is called.</p>
         */
        public boolean canDisableShutterSound;
    };

}
