package org.firstinspires.ftc.robotcore.robocol;

/**
 * Placeholder for telemetry message constants
 */
public class TelemetryMessage {

    static final int cbTimestamp = 8;
    static final int cbSorted   = 1;
    static final int cbRobotState = 1;
    static final int cbTagLen   = 1;
    static final int cbCountLen = 1;
    static final int cbKeyLen   = 2;
    static final int cbValueLen = 2;
    static final int cbFloat    = 4;

    public final static int cbTagMax   = (1 << (cbTagLen*8))   - 1;
    public final static int cCountMax  = (1 << (cbCountLen*8)) - 1;
    public final static int cbKeyMax   = (1 << (cbKeyLen*8))   - 1;
    public final static int cbValueMax = (1 << (cbValueLen*8)) - 1;

}
