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
import com.github.serivesmejia.eocvsim.gui.util.WebcamDriver;
import com.github.serivesmejia.eocvsim.pipeline.PipelineFps;
import com.github.serivesmejia.eocvsim.pipeline.PipelineTimeout;

import javax.swing.*;
import java.awt.*;

public class Configuration {

    private final EOCVSim eocvSim;
    public JPanel contents = new JPanel(new GridBagLayout());
    public JComboBox<String> themeComboBox = new JComboBox<>();

    public JButton acceptButton = new JButton("Accept");

    public JCheckBox pauseOnImageCheckBox = new JCheckBox();

    public EnumComboBox<PipelineTimeout> pipelineTimeoutComboBox = null;
    public EnumComboBox<PipelineFps> pipelineFpsComboBox = null;

    public EnumComboBox<WebcamDriver> preferredWebcamDriver = null;

    public SizeFields videoRecordingSize = null;
    public EnumComboBox<PipelineFps> videoRecordingFpsComboBox = null;

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
        //configuration.setSize(380, 300);

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.LEFT);

        /*
        UI TAB
         */

        JPanel uiPanel = new JPanel(new GridLayout(1, 1, 1, 8));

        /* THEME SETTING */
        JPanel themePanel = new JPanel(new FlowLayout());
        JLabel themeLabel = new JLabel("Theme: ");

        themeLabel.setHorizontalAlignment(0);

        for (Theme theme : Theme.values()) {
            this.themeComboBox.addItem(theme.toString().replace("_", " "));
        }

        themeComboBox.setSelectedIndex(eocvSim.getConfig().simTheme.ordinal());
        themePanel.add(themeLabel);
        themePanel.add(this.themeComboBox);
        uiPanel.add(themePanel);

        tabbedPane.addTab("Inteface", uiPanel);

        /*
        INPUT SOURCES TAB
         */
        JPanel inputSourcesPanel = new JPanel(new GridLayout(2, 1, 1, 8));

        /* PAUSE WITH IMAGE SOURCES OPTION */
        JPanel pauseOnImagePanel = new JPanel(new FlowLayout());
        JLabel pauseOnImageLabel = new JLabel("Pause with Image Sources");

        pauseOnImageCheckBox.setSelected(config.pauseOnImages);

        pauseOnImagePanel.add(pauseOnImageCheckBox);
        pauseOnImagePanel.add(pauseOnImageLabel);

        inputSourcesPanel.add(pauseOnImagePanel);

        /* PREFERRED WEBCAM DRIVER OPTION */
        preferredWebcamDriver = new EnumComboBox<>(
                "Preferred Webcam Driver: ", WebcamDriver.class,
                WebcamDriver.values()
        );
        preferredWebcamDriver.setSelectedEnum(config.preferredWebcamDriver);
        inputSourcesPanel.add(preferredWebcamDriver);

        tabbedPane.addTab("Input Sources", inputSourcesPanel);

        /*
        PROCESSING TAB
         */
        JPanel processingPanel = new JPanel(new GridLayout(4, 1, 1, 8));

        /* PIPELINE TIMEOUT IN processFrame AND init*/
        pipelineTimeoutComboBox = new EnumComboBox<>(
                "Pipeline Process Timeout: ", PipelineTimeout.class,
                PipelineTimeout.values(), PipelineTimeout::getCoolName, PipelineTimeout::fromCoolName
        );
        pipelineTimeoutComboBox.setSelectedEnum(config.pipelineTimeout);
        processingPanel.add(pipelineTimeoutComboBox);

        /* PIPELINE FPS*/
        pipelineFpsComboBox = new EnumComboBox<>(
                "Pipeline Max FPS: ", PipelineFps.class,
                PipelineFps.values(), PipelineFps::getCoolName, PipelineFps::fromCoolName
        );
        pipelineFpsComboBox.setSelectedEnum(config.pipelineMaxFps);
        processingPanel.add(this.pipelineFpsComboBox);

        /* VIDEO REC SIZE */
        videoRecordingSize = new SizeFields(
                config.videoRecordingSize, false,
                "Video Recording Size: "
        );
        videoRecordingSize.onChange.doPersistent(() ->
                acceptButton.setEnabled(this.videoRecordingSize.getValid())
        );
        processingPanel.add(this.videoRecordingSize);

        /* VIDEO REC FPS */
        videoRecordingFpsComboBox = new EnumComboBox<>(
                "Video Recording FPS: ", PipelineFps.class,
                PipelineFps.values(), PipelineFps::getCoolName, PipelineFps::fromCoolName
        );
        videoRecordingFpsComboBox.setSelectedEnum(config.videoRecordingFps);
        processingPanel.add(videoRecordingFpsComboBox);

        tabbedPane.addTab("Processing", processingPanel);

        GridBagConstraints gbc = new GridBagConstraints();

        /* ADD TABBED PANE TO DIALOG */
        gbc.gridx = 1;
        gbc.gridy = 1;
        contents.add(tabbedPane, gbc);

        /*
        ACCEPT BUTTON AND FINISH SETTING UP DIALOG
         */
        JPanel acceptPanel = new JPanel(new FlowLayout());
        acceptPanel.add(acceptButton);

        acceptButton.addActionListener(e -> {
            this.eocvSim.onMainUpdate.doOnce(this::applyChanges);
            close();
        });

        gbc.gridy = 2;
        contents.add(acceptPanel, gbc);
        contents.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        configuration.add(this.contents);
        configuration.pack();

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
        config.preferredWebcamDriver = preferredWebcamDriver.getSelectedEnum();
        config.pipelineTimeout = pipelineTimeoutComboBox.getSelectedEnum();
        config.pipelineMaxFps = pipelineFpsComboBox.getSelectedEnum();
        config.videoRecordingSize = videoRecordingSize.getCurrentSize();
        config.videoRecordingFps = videoRecordingFpsComboBox.getSelectedEnum();

        eocvSim.configManager.saveToFile(); //update config file

        if (userSelectedTheme != beforeTheme)
            eocvSim.restart();
    }

    public void close() {
        configuration.setVisible(false);
        configuration.dispose();
    }

}