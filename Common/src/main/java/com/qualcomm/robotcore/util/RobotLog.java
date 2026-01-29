package com.qualcomm.robotcore.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RobotLog {

    private RobotLog() { }

    public static void ee(String tag, Throwable throwable, String message) {
        LoggerFactory.getLogger(tag).error(message, throwable);
    }

    public static void ee(String tag, String message) {
        LoggerFactory.getLogger(tag).error(message);
    }
}
