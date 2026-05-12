/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.firstinspires.ftc.robotcore.internal.opmode

import org.firstinspires.ftc.robotcore.external.Telemetry

interface TelemetryTransmissionReceiver {
    fun onTelemetryTransmission(text: String, srcTelemetry: Telemetry)
}
