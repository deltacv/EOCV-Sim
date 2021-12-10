/*
 * Copyright (c) 2021 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.serivesmejia.eocvsim.config;

import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel;
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanelConfig;
import com.github.serivesmejia.eocvsim.gui.theme.Theme;
import com.github.serivesmejia.eocvsim.gui.util.WebcamDriver;
import com.github.serivesmejia.eocvsim.pipeline.PipelineFps;
import com.github.serivesmejia.eocvsim.pipeline.PipelineTimeout;
import com.github.serivesmejia.eocvsim.pipeline.compiler.CompiledPipelineManager;
import org.opencv.core.Size;

import java.util.HashMap;

public class Config {
    public volatile Theme simTheme = Theme.Light;

    public volatile double zoom = 1;

    public volatile PipelineFps pipelineMaxFps = PipelineFps.MEDIUM;
    public volatile PipelineTimeout pipelineTimeout = PipelineTimeout.MEDIUM;

    public volatile boolean pauseOnImages = true;

    public volatile Size videoRecordingSize = new Size(640, 480);
    public volatile PipelineFps videoRecordingFps = PipelineFps.MEDIUM;

    public volatile WebcamDriver preferredWebcamDriver = WebcamDriver.OpenIMAJ;

    public volatile String workspacePath  = CompiledPipelineManager.Companion.getDEF_WORKSPACE_FOLDER().getAbsolutePath();

    public volatile TunableFieldPanelConfig.Config globalTunableFieldsConfig =
            new TunableFieldPanelConfig.Config(
                    new Size(0, 255),
                    TunableFieldPanelConfig.PickerColorSpace.RGB,
                    TunableFieldPanel.Mode.TEXTBOXES,
                    TunableFieldPanelConfig.ConfigSource.GLOBAL_DEFAULT
            );

    public volatile HashMap<String, TunableFieldPanelConfig.Config> specificTunableFieldConfig = new HashMap<>();

}