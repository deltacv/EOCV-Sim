package org.firstinspires.ftc.robotcore.internal.system;

import com.qualcomm.robotcore.util.RobotLog;

public class AppUtil {

    public static final String TAG= "AppUtil";

    protected AppUtil() { }

    private static AppUtil instance = null;

    public static AppUtil getInstance() {
        if(instance == null) {
            instance = new AppUtil();
        }

        return instance;
    }

    public RuntimeException unreachable()
    {
        return unreachable(TAG);
    }

    public RuntimeException unreachable(Throwable throwable)
    {
        return unreachable(TAG, throwable);
    }

    public RuntimeException unreachable(String tag)
    {
        return failFast(tag, "internal error: this code is unreachable");
    }

    public RuntimeException unreachable(String tag, Throwable throwable)
    {
        return failFast(tag, throwable, "internal error: this code is unreachable");
    }

    public RuntimeException failFast(String tag, String format, Object... args)
    {
        String message = String.format(format, args);
        return failFast(tag, message);
    }

    public RuntimeException failFast(String tag, String message)
    {
        RobotLog.ee(tag, message);
        exitApplication(-1);
        return new RuntimeException("keep compiler happy");
    }

    public RuntimeException failFast(String tag, Throwable throwable, String format, Object... args)
    {
        String message = String.format(format, args);
        return failFast(tag, throwable, message);
    }

    public RuntimeException failFast(String tag, Throwable throwable, String message)
    {
        RobotLog.ee(tag, throwable, message);
        exitApplication(-1);
        return new RuntimeException("keep compiler happy", throwable);
    }

    public void exitApplication(int code) {
        System.exit(code);
    }
}
