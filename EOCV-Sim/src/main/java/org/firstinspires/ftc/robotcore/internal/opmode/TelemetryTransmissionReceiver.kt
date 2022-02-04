package org.firstinspires.ftc.robotcore.internal.opmode

import org.firstinspires.ftc.robotcore.external.Telemetry

interface TelemetryTransmissionReceiver {
    fun onTelemetryTransmission(text: String, srcTelemetry: Telemetry)
}