/*
Copyright (c) 2022 REV Robotics

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of REV Robotics nor the names of its contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.firstinspires.ftc.robotcore.external.navigation;

import java.util.Locale;

/**
 * A simplified view of the orientation of an object in 3D space.
 * <p>
 * Yaw is side-to-side lateral rotation, where the object remains flat, but turns left and right.
 * Sometimes yaw is also referred to as "heading".
 * <p>
 * Pitch is front-to-back rotation, where the front of the object moves upwards while the rear of the
 * object moves downwards, or vice versa.
 * <p>
 * Roll is side-to-side tilt, where the left side of the object moves upwards while the right side of
 * the object moves downwards, or vice versa.
 * <p>
 * All angles are in the range of -180 degrees to 180 degrees.
 * <p>
 * The angles are applied intrinsically, in the order of yaw, then pitch, then roll. "Intrinsically"
 * means that the axes move along with the object as you perform the rotations. As an example using
 * a robot, if the yaw is 30 degrees, the pitch is 40 degrees, and the roll is 10 degrees, that means
 * that you would reach the described orientation by first rotating the object 30 degrees counter-clockwise
 * from the starting point, with all wheels continuing to touch the ground (rotation around the Z
 * axis, as defined in the Robot Coordinate System). Then, you make your robot point 40 degrees upward
 * (rotate it 40 degrees around the X axis, as defined in the Robot Coordinate System). Because the X
 * axis moved with the robot, the pitch is not affected by the yaw value. Then from that position, the
 * robot is tilted 10 degrees to the right, around the newly positioned Y axis, to produce the actual
 * position of the robot.
 */
public class YawPitchRollAngles {
    private final AngleUnit angleUnit;
    private final double yaw;
    private final double pitch;
    private final double roll;
    private final long acquisitionTime;

    /**
     * See the top-level class Javadoc for the format that these angles need to be in.
     */
    public YawPitchRollAngles(AngleUnit angleUnit, double yaw, double pitch, double roll, long acquisitionTime) {
        this.angleUnit = angleUnit;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        this.acquisitionTime = acquisitionTime;
    }

    /**
     * @return The side-to-side lateral rotation of the object,
     *         normalized to the range of [-180,+180) degrees.
     */
    public double getYaw() {
        return yaw;
    }

    /**
     * @param angleUnit The unit that will be used for the result.
     * @return The side-to-side lateral rotation of the object,
     *         normalized to the range of [-180,+180) degrees.
     */
    public double getYaw(AngleUnit angleUnit) {
        return angleUnit.fromUnit(this.angleUnit, yaw);
    }

    /**
     * @return The front-to-back rotation of the object, normalized to
     *         the range of [-180,+180) degrees
     */
    public double getPitch() {
        return pitch;
    }

    /**
     * @param angleUnit The unit that will be used for the result.
     * @return The front-to-back rotation of the object, normalized to
     *         the range of [-180,+180) degrees
     */
    public double getPitch(AngleUnit angleUnit) {
        return angleUnit.fromUnit(this.angleUnit, pitch);
    }

    /**
     * @return The side-to-side tilt of the object, normalized to
     *         the range of [-180,+180) degrees
     */
    public double getRoll() {
        return roll;
    }

    /**
     * @param angleUnit The unit that will be used for the result.
     * @return The side-to-side tilt of the object, normalized to
     *         the range of [-180,+180) degrees
     */
    public double getRoll(AngleUnit angleUnit) {
        return angleUnit.fromUnit(this.angleUnit, roll);
    }

    /**
     * @return The time on the System.nanoTime() clock at which the data was acquired. If no
     * timestamp is associated with this particular set of data, this value is zero.
     */
    public long getAcquisitionTime() {
        return acquisitionTime;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "{yaw=%.3f, pitch=%.3f, roll=%.3f}",
                angleUnit.toDegrees(yaw), angleUnit.toDegrees(pitch), angleUnit.toDegrees(roll));
    }
}
