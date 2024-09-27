/*
 * Copyright (c) 2014, 2015 Qualcomm Technologies Inc
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Qualcomm Technologies Inc nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.qualcomm.robotcore.hardware;

/**
 * Monitor a hardware gamepad. In the case of EOCV-Sim, this is a stub class that does nothing.
 * <p>
 * The buttons, analog sticks, and triggers are represented a public
 * member variables that can be read from or written to directly.
 * <p>
 * Analog sticks are represented as floats that range from -1.0 to +1.0. They will be 0.0 while at
 * rest. The horizontal axis is labeled x, and the vertical axis is labeled y.
 * <p>
 * Triggers are represented as floats that range from 0.0 to 1.0. They will be at 0.0 while at
 * rest.
 * <p>
 * Buttons are boolean values. They will be true if the button is pressed, otherwise they will be
 * false.
 * <p>
 * The codes KEYCODE_BUTTON_SELECT and KEYCODE_BACK are both be handled as a "back" button event.
 * Older Android devices (Kit Kat) map a Logitech F310 "back" button press to a KEYCODE_BUTTON_SELECT event.
 * Newer Android devices (Marshmallow or greater) map this "back" button press to a KEYCODE_BACK event.
 * Also, the REV Robotics Gamepad (REV-31-1159) has a "select" button instead of a "back" button on the gamepad.
 * <p>
 * The dpad is represented as 4 buttons, dpad_up, dpad_down, dpad_left, and dpad_right
 */
@SuppressWarnings("unused")
public class Gamepad {

    /**
     * A gamepad with an ID equal to ID_UNASSOCIATED has not been associated with any device.
     */
    public static final int ID_UNASSOCIATED = -1;

    /**
     * A gamepad with a phantom id a synthetic one made up by the system
     */
    public static final int ID_SYNTHETIC = -2;

    public enum Type {
        // Do NOT change the order/names of existing entries,
        // you will break backwards compatibility!!
        UNKNOWN(LegacyType.UNKNOWN),
        LOGITECH_F310(LegacyType.LOGITECH_F310),
        XBOX_360(LegacyType.XBOX_360),
        SONY_PS4(LegacyType.SONY_PS4), // This indicates a PS4-compatible controller that is being used through our compatibility mode
        SONY_PS4_SUPPORTED_BY_KERNEL(LegacyType.SONY_PS4); // This indicates a PS4-compatible controller that is being used through the DualShock 4 Linux kernel driver.

        private final LegacyType correspondingLegacyType;
        Type(LegacyType correspondingLegacyType) {
            this.correspondingLegacyType = correspondingLegacyType;
        }
    }

    // LegacyType is necessary because robocol gamepad version 3 was written in a way that was not
    // forwards-compatible, so we have to keep sending V3-compatible values.
    public enum LegacyType {
        // Do NOT change the order or names of existing entries, or add new entries.
        // You will break backwards compatibility!!
        UNKNOWN,
        LOGITECH_F310,
        XBOX_360,
        SONY_PS4
    }

    @SuppressWarnings("UnusedAssignment")
    public volatile Type type = Type.UNKNOWN; // IntelliJ thinks this is redundant, but it is NOT. Must be a bug in the analyzer?

    /**
     * left analog stick horizontal axis
     */
    public volatile float left_stick_x = 0f;

    /**
     * left analog stick vertical axis
     */
    public volatile float left_stick_y = 0f;

    /**
     * right analog stick horizontal axis
     */
    public volatile float right_stick_x = 0f;

    /**
     * right analog stick vertical axis
     */
    public volatile float right_stick_y = 0f;

    /**
     * dpad up
     */
    public volatile boolean dpad_up = false;

    /**
     * dpad down
     */
    public volatile boolean dpad_down = false;

    /**
     * dpad left
     */
    public volatile boolean dpad_left = false;

    /**
     * dpad right
     */
    public volatile boolean dpad_right = false;

    /**
     * button a
     */
    public volatile boolean a = false;

    /**
     * button b
     */
    public volatile boolean b = false;

    /**
     * button x
     */
    public volatile boolean x = false;

    /**
     * button y
     */
    public volatile boolean y = false;

    /**
     * button guide - often the large button in the middle of the controller. The OS may
     * capture this button before it is sent to the app; in which case you'll never
     * receive it.
     */
    public volatile boolean guide = false;

    /**
     * button start
     */
    public volatile boolean start = false;

    /**
     * button back
     */
    public volatile boolean back = false;

    /**
     * button left bumper
     */
    public volatile boolean left_bumper = false;

    /**
     * button right bumper
     */
    public volatile boolean right_bumper = false;

    /**
     * left stick button
     */
    public volatile boolean left_stick_button = false;

    /**
     * right stick button
     */
    public volatile boolean right_stick_button = false;

    /**
     * left trigger
     */
    public volatile float left_trigger = 0f;

    /**
     * right trigger
     */
    public volatile float right_trigger = 0f;

    /**
     * PS4 Support - Circle
     */
    public volatile boolean circle = false;

    /**
     * PS4 Support - cross
     */
    public volatile boolean cross = false;

    /**
     * PS4 Support - triangle
     */
    public volatile boolean triangle = false;

    /**
     * PS4 Support - square
     */
    public volatile boolean square = false;

    /**
     * PS4 Support - share
     */
    public volatile boolean share = false;

    /**
     * PS4 Support - options
     */
    public volatile boolean options = false;

    /**
     * PS4 Support - touchpad
     */
    public volatile boolean touchpad = false;
    public volatile boolean touchpad_finger_1;
    public volatile boolean touchpad_finger_2;
    public volatile float touchpad_finger_1_x;
    public volatile float touchpad_finger_1_y;
    public volatile float touchpad_finger_2_x;
    public volatile float touchpad_finger_2_y;

    /**
     * PS4 Support - PS Button
     */
    public volatile boolean ps = false;

    /**
     * ID assigned to this gamepad by the OS. This value can change each time the device is plugged in.
     */
    public volatile int id = ID_UNASSOCIATED;  // public only for historical reasons

    public void setGamepadId(int id) {
        this.id = id;
    }
    public int getGamepadId() {
        return this.id;
    }

    /**
     * Relative timestamp of the last time an event was detected
     */
    public volatile long timestamp = 0;

    public Gamepad() {
        this.type = type();
    }

    /**
     * Reset this gamepad into its initial state
     */
    public void reset() {
        left_stick_x = 0f;
        left_stick_y = 0f;
        right_stick_x = 0f;
        right_stick_y = 0f;
        dpad_up = false;
        dpad_down = false;
        dpad_left = false;
        dpad_right = false;
        a = false;
        b = false;
        x = false;
        y = false;
        guide = false;
        start = false;
        back = false;
        left_bumper = false;
        right_bumper = false;
        left_stick_button = false;
        right_stick_button = false;
        left_trigger = 0f;
        right_trigger = 0f;
        circle = false;
        cross = false;
        triangle = false;
        square = false;
        share = false;
        options = false;
        touchpad = false;
        touchpad_finger_1 = false;
        touchpad_finger_2 = false;
        touchpad_finger_1_x = 0f;
        touchpad_finger_1_y = 0f;
        touchpad_finger_2_x = 0f;
        touchpad_finger_2_y = 0f;
        ps = false;
        timestamp = 0;
    }

    /**
     * Are all analog sticks and triggers in their rest position?
     * @return true if all analog sticks and triggers are at rest; otherwise false
     */
    public boolean atRest() {
        return (
                left_stick_x == 0f && left_stick_y == 0f &&
                        right_stick_x == 0f && right_stick_y == 0f &&
                        left_trigger == 0f && right_trigger == 0f);
    }

    /**
     * Get the type of gamepad as a {@link Type}. This method defaults to "UNKNOWN".
     * @return gamepad type
     */
    public Type type() {
        return type;
    }

    /**
     * Get the type of gamepad as a {@link LegacyType}. This method defaults to "UNKNOWN".
     * @return gamepad type
     */
    private LegacyType legacyType() {
        return type.correspondingLegacyType;
    }


    /**
     * Display a summary of this gamepad, including the state of all buttons, analog sticks, and triggers
     * @return a summary
     */
    @Override
    public String toString() {

        switch (type) {
            case SONY_PS4:
            case SONY_PS4_SUPPORTED_BY_KERNEL:
                return ps4ToString();

            case UNKNOWN:
            case LOGITECH_F310:
            case XBOX_360:
            default:
                return genericToString();
        }
    }


    protected String ps4ToString() {
        String buttons = new String();
        if (dpad_up) buttons += "dpad_up ";
        if (dpad_down) buttons += "dpad_down ";
        if (dpad_left) buttons += "dpad_left ";
        if (dpad_right) buttons += "dpad_right ";
        if (cross) buttons += "cross ";
        if (circle) buttons += "circle ";
        if (square) buttons += "square ";
        if (triangle) buttons += "triangle ";
        if (ps) buttons += "ps ";
        if (share) buttons += "share ";
        if (options) buttons += "options ";
        if (touchpad) buttons += "touchpad ";
        if (left_bumper) buttons += "left_bumper ";
        if (right_bumper) buttons += "right_bumper ";
        if (left_stick_button) buttons += "left stick button ";
        if (right_stick_button) buttons += "right stick button ";

        return String.format("ID: %2d user: %2d lx: % 1.2f ly: % 1.2f rx: % 1.2f ry: % 1.2f lt: %1.2f rt: %1.2f %s",
                id, 0, left_stick_x, left_stick_y,
                right_stick_x, right_stick_y, left_trigger, right_trigger, buttons);
    }

    protected String genericToString() {
        String buttons = new String();
        if (dpad_up) buttons += "dpad_up ";
        if (dpad_down) buttons += "dpad_down ";
        if (dpad_left) buttons += "dpad_left ";
        if (dpad_right) buttons += "dpad_right ";
        if (a) buttons += "a ";
        if (b) buttons += "b ";
        if (x) buttons += "x ";
        if (y) buttons += "y ";
        if (guide) buttons += "guide ";
        if (start) buttons += "start ";
        if (back) buttons += "back ";
        if (left_bumper) buttons += "left_bumper ";
        if (right_bumper) buttons += "right_bumper ";
        if (left_stick_button) buttons += "left stick button ";
        if (right_stick_button) buttons += "right stick button ";

        return String.format("ID: %2d user: %2d lx: % 1.2f ly: % 1.2f rx: % 1.2f ry: % 1.2f lt: %1.2f rt: %1.2f %s",
                id, 0, left_stick_x, left_stick_y,
                right_stick_x, right_stick_y, left_trigger, right_trigger, buttons);
    }

    /**
     * Alias buttons so that XBOX &amp; PS4 native button labels can be used in use code.
     * Should allow a team to program with whatever controllers they prefer, but
     * be able to swap controllers easily without changing code.
     */
    protected void updateButtonAliases(){
        // There is no assignment for touchpad because there is no equivalent on XBOX controllers.
        circle = b;
        cross = a;
        triangle = y;
        square = x;
        share = back;
        options = start;
        ps = guide;
    }
}
