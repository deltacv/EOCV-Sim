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

package com.github.serivesmejia.eocvsim.gui.dialog;

import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.config.Config;
import com.github.serivesmejia.eocvsim.gui.component.input.EnumComboBox;
import com.github.serivesmejia.eocvsim.gui.component.input.SizeFields;
import com.github.serivesmejia.eocvsim.gui.theme.Theme;
import com.github.serivesmejia.eocvsim.pipeline.PipelineFps;
import com.github.serivesmejia.eocvsim.pipeline.PipelineTimeout;

import javax.swing.*;
import java.awt.*;

public class Configuration {

    private final EOCVSim eocvSim;
    public JPanel contents = new JPanel(new GridLayout(6, 1, 1, 8));
    public JComboBox<String> themeComboBox = new JComboBox<>();

    public JButton acceptButton = new JButton("Accept");

    public JCheckBox pauseOnImageCheckBox = new JCheckBox();

    public EnumComboBox<PipelineTimeout> pipelineTimeoutComboBox = null;
    public EnumComboBox<PipelineFps> pipelineFpsComboBox = null;

    public SizeFields videoRecordingSize = null;

    JDialog configuration;

    public Configuration(JFrame parent, EOCVSim eocvSim) {
        configuration = new JDialog(parent);
        this.eocvSim = eocvSim;

        eocvSim.visualizer.childDialogs.add(configuration);

        initConfiguration();
    }

    private void initConfiguration() {

        Config config = this.eocvSim.configManager.getConfig();
        configuration.setModal(true);
        configuration.setTitle("Settings");
        configuration.setSize(350, 260);

        JPanel themePanel = new JPanel(new FlowLayout());
        JLabel themeLabel = new JLabel("Theme: ");

        themeLabel.setHorizontalAlignment(0);

        for (Theme theme : Theme.values()) {
            this.themeComboBox.addItem(theme.toString().replace("_", " "));
        }

        themeComboBox.setSelectedIndex(eocvSim.getConfig().simTheme.ordinal());
        themePanel.add(themeLabel);
        themePanel.add(this.themeComboBox);
        contents.add(themePanel);

        JPanel pauseOnImagePanel = new JPanel(new FlowLayout());
        JLabel pauseOnImageLabel = new JLabel("Pause with Image Sources");

        pauseOnImageCheckBox.setSelected(config.pauseOnImages);

        pauseOnImagePanel.add(pauseOnImageCheckBox);
        pauseOnImagePanel.add(pauseOnImageLabel);

        contents.add(pauseOnImagePanel);

        pipelineTimeoutComboBox = new EnumComboBox<>(
                "Pipeline Process Timeout: ", PipelineTimeout.class,
                PipelineTimeout.values(), PipelineTimeout::getCoolName, PipelineTimeout::fromCoolName
        );
        pipelineTimeoutComboBox.setSelectedEnum(config.pipelineTimeout);
        contents.add(pipelineTimeoutComboBox);

        pipelineFpsComboBox = new EnumComboBox<>(
                "Pipeline Max FPS: ", PipelineFps.class,
                PipelineFps.values(), PipelineFps::getCoolName, PipelineFps::fromCoolName
        );
        pipelineFpsComboBox.setSelectedEnum(config.pipelineMaxFps);
        contents.add(this.pipelineFpsComboBox);

        videoRecordingSize = new SizeFields(
                config.videoRecordingSize, false,
                "Video Recording Size: "
        );
        videoRecordingSize.onChange.doPersistent(() ->
                acceptButton.setEnabled(this.videoRecordingSize.getValid())
        );
        contents.add(this.videoRecordingSize);

        JPanel acceptPanel = new JPanel(new FlowLayout());
        acceptPanel.add(acceptButton);

        acceptButton.addActionListener(e -> {
            this.eocvSim.onMainUpdate.doOnce(this::applyChanges);
            close();
        });

        contents.add(acceptPanel);
        contents.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        configuration.add(this.contents);
        configuration.setResizable(false);
        configuration.setLocationRelativeTo(null);
        configuration.setVisible(true);
    }

    private void applyChanges() {
        Config config = eocvSim.configManager.getConfig();

        Theme userSelectedTheme = Theme.valueOf(themeComboBox.getSelectedItem().toString().replace(" ", "_"));
        Theme beforeTheme = config.simTheme;

        //save user modifications to config
        config.simTheme = userSelectedTheme;
        config.pauseOnImages = pauseOnImageCheckBox.isSelected();
        config.pipelineTimeout = pipelineTimeoutComboBox.getSelectedEnum();
        config.pipelineMaxFps = pipelineFpsComboBox.getSelectedEnum();
        config.videoRecordingSize = videoRecordingSize.getCurrentSize();

        eocvSim.configManager.saveToFile(); //update config file

        if (userSelectedTheme != beforeTheme)
            eocvSim.restart();
    }

    public void close() {
        configuration.setVisible(false);
        configuration.dispose();
    }

}