/* Copyright (c) 2014, 2015 Qualcomm Technologies Inc

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Qualcomm Technologies Inc nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.qualcomm.robotcore.util;

public class SerialNumber {


    protected final String serialNumberString;

    //------------------------------------------------------------------------------------------------
    // Construction
    //------------------------------------------------------------------------------------------------

    /**
     * Constructs a serial number using the supplied initialization string. If the initialization
     * string is a legacy form of fake serial number, a unique fake serial number is created.
     *
     * @param serialNumberString the initialization string for the serial number.
     */
    protected SerialNumber(String serialNumberString) {
        this.serialNumberString = serialNumberString;
    }


    /**
     * Returns the string contents of the serial number. Result is not intended to be
     * displayed to humans.
     * @see #toString()
     */
    public String getString() {
        return serialNumberString;
    }


    /**
     * Returns the {@link SerialNumber} of the device associated with this one that would appear
     * in a {link ScannedDevices}.
     */
    public SerialNumber getScannableDeviceSerialNumber() {
        return this;
    }

    //------------------------------------------------------------------------------------------------
    // Comparison
    //------------------------------------------------------------------------------------------------

    public boolean matches(Object pattern) {
        return this.equals(pattern);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) return false;
        if (object == this) return true;

        if (object instanceof SerialNumber) {
            return serialNumberString.equals(((SerialNumber) object).serialNumberString);
        }

        if (object instanceof String) {
            return this.equals((String)object);
        }

        return false;
    }

    // separate method to avoid annoying Android Studio inspection warnings when comparing SerialNumber against String
    public boolean equals(String string) {
        return serialNumberString.equals(string);
    }

    @Override
    public int hashCode() {
        return serialNumberString.hashCode() ^ 0xabcd9873;
    }

}
