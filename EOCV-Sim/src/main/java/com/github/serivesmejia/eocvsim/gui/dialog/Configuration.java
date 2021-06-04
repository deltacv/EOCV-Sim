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
import com.github.serivesmejia.eocvsim.gui.component.input.SizeFields;
import com.github.serivesmejia.eocvsim.gui.theme.Theme;
import org.opencv.core.Size;

import javax.swing.*;
import java.awt.*;

public class Configuration {

    private final EOCVSim eocvSim;
    public JPanel contents = new JPanel(new GridLayout(5, 1));
    public JComboBox<String> themeComboBox = new JComboBox<>();

    public JButton acceptButton = new JButton("Accept");

    public JCheckBox storeZoomCheckBox = new JCheckBox();
    public JCheckBox pauseOnImageCheckBox = new JCheckBox();

    public SizeFields videoRecordingSize = null;

    JDialog configuration;

    public Configuration(JFrame parent, EOCVSim eocvSim) {
        configuration = new JDialog(parent);
        this.eocvSim = eocvSim;

        eocvSim.visualizer.childDialogs.add(configuration);

        initConfiguration();
    }

    private void initConfiguration() {

        Config config = eocvSim.configManager.getConfig();

        configuration.setModal(true);

        configuration.setTitle("Settings");
        configuration.setSize(350, 240);

        //theme selection
        JPanel themePanel = new JPanel(new FlowLayout());

        JLabel themeLabel = new JLabel("Theme: ");
        themeLabel.setHorizontalAlignment(JLabel.CENTER);

        //add all themes to combo box
        for (Theme theme : Theme.values()) {
            themeComboBox.addItem(theme.toString().replace("_", " "));
        }

        //select current theme by index
        themeComboBox.setSelectedIndex(eocvSim.configManager.getConfig().simTheme.ordinal());

        themePanel.add(themeLabel);
        themePanel.add(themeComboBox);

        contents.add(themePanel);

        //store zoom option
        JPanel storeZoomPanel = new JPanel(new FlowLayout());
        JLabel storeZoomLabel = new JLabel("Store zoom value");

        storeZoomCheckBox.setSelected(config.storeZoom);

        storeZoomPanel.add(storeZoomCheckBox);
        storeZoomPanel.add(storeZoomLabel);

        contents.add(storeZoomPanel);

        //pause on image option
        JPanel pauseOnImagePanel = new JPanel(new FlowLayout());
        JLabel pauseOnImageLabel = new JLabel("Pause with image sources");

        pauseOnImageCheckBox.setSelected(config.pauseOnImages);

        pauseOnImagePanel.add(pauseOnImageCheckBox);
        pauseOnImagePanel.add(pauseOnImageLabel);

        contents.add(pauseOnImagePanel);

        videoRecordingSize = new SizeFields(config.videoRecordingSize, false, "Video Rec. Size: ");

        videoRecordingSize.onChange.doPersistent(() -> {
            acceptButton.setEnabled(videoRecordingSize.getValid());
        });

        contents.add(videoRecordingSize);

        //accept button
        JPanel acceptPanel = new JPanel(new FlowLayout());
        acceptPanel.add(acceptButton);

        acceptButton.addActionListener((e) -> {
            eocvSim.onMainUpdate.doOnce(this::applyChanges);
            close();
        });

        contents.add(acceptPanel);

        contents.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        configuration.add(contents);

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
        config.storeZoom = storeZoomCheckBox.isSelected();
        config.pauseOnImages = pauseOnImageCheckBox.isSelected();
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