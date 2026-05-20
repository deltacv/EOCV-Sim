/*
Copyright (c) 2016 Robert Atkinson

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Robert Atkinson nor the names of his contributors may be used to
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

