/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.config;

import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel;
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanelConfig;
import com.github.serivesmejia.eocvsim.gui.theme.Theme;
import com.github.serivesmejia.eocvsim.pipeline.PipelineFps;
import com.github.serivesmejia.eocvsim.pipeline.PipelineTimeout;
import com.github.serivesmejia.eocvsim.pipeline.compiled.CompiledPipelineManager;
import org.opencv.core.Size;

import java.util.HashMap;

public class Config {
    public volatile Theme simTheme = Theme.Dark;

    public volatile PipelineFps pipelineMaxFps = PipelineFps.MEDIUM;
    public volatile PipelineTimeout pipelineTimeout = PipelineTimeout.MEDIUM;

    public volatile boolean pauseOnImages = true;

    public volatile double webcamOpenTimeoutSec = 5.0;
    public volatile double webcamNewFrameTimeoutSec = 3.0;

    public volatile Size videoRecordingSize = new Size(640, 480);
    public volatile PipelineFps videoRecordingFps = PipelineFps.MEDIUM;

    public volatile String workspacePath = CompiledPipelineManager.Companion.getDEF_WORKSPACE_FOLDER().getAbsolutePath();

    public volatile TunableFieldPanelConfig.Config globalTunableFieldsConfig =
            new TunableFieldPanelConfig.Config(
                    new Size(0, 255),
                    TunableFieldPanelConfig.PickerColorSpace.RGB,
                    TunableFieldPanel.Mode.TEXTBOXES,
                    TunableFieldPanelConfig.ConfigSource.GLOBAL_DEFAULT
            );

    public volatile HashMap<String, TunableFieldPanelConfig.Config> specificTunableFieldConfig = new HashMap<>();

    public volatile boolean autoAcceptSuperAccessOnTrusted = true;

    public volatile HashMap<String, Boolean> flags = new HashMap<>();

    public boolean hasFlag(String flagName) {
        return flags.get(flagName) != null;
    }

    public boolean getFlag(String flagName) {
        return flags.get(flagName) != null && flags.get(flagName);
    }

}
