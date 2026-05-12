/*
 * Copyright (c) 2020 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.openftc.easyopencv;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class PipelineRecordingParameters
{
    public final String path;
    public final Encoder encoder;
    public final OutputFormat outputFormat;
    public final int bitrate;
    public final int frameRate;

    public enum Encoder
    {
        H264(),
        H263(),
        VP8(),
        MPEG_4_SP();
    }

    public enum OutputFormat
    {
        MPEG_4(),
        THREE_GPP(),
        WEBM();
    }

    public enum BitrateUnits
    {
        bps(1),
        Kbps(1000),
        Mbps(1000000);

        final int scalar;

        BitrateUnits(int scalar)
        {
            this.scalar = scalar;
        }
    }

    public PipelineRecordingParameters(OutputFormat outputFormat, Encoder encoder, int frameRate, int bitrate, String path)
    {
        this.outputFormat = outputFormat;
        this.encoder = encoder;
        this.frameRate = frameRate;
        this.bitrate = bitrate;
        this.path = path;
    }

    public static class Builder
    {
        private String path = "/sdcard/EasyOpenCV/pipeline_recording_"+new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss", Locale.getDefault()).format(new Date())+".mp4";
        private Encoder encoder = Encoder.H264;
        private OutputFormat outputFormat = OutputFormat.MPEG_4;
        private int bitrate = 4000000;
        private int frameRate = 30;

        public Builder setPath(String path)
        {
            this.path = path;
            return this;
        }

        public Builder setEncoder(Encoder encoder)
        {
            this.encoder = encoder;
            return this;
        }

        public Builder setOutputFormat(OutputFormat outputFormat)
        {
            this.outputFormat = outputFormat;
            return this;
        }

        public Builder setBitrate(int bitrate, BitrateUnits units)
        {
            this.bitrate = bitrate*units.scalar;
            return this;
        }

        public Builder setFrameRate(int frameRate)
        {
            this.frameRate = frameRate;
            return this;
        }

        public PipelineRecordingParameters build()
        {
            return new PipelineRecordingParameters(outputFormat, encoder, frameRate, bitrate, path);
        }
    }
}
