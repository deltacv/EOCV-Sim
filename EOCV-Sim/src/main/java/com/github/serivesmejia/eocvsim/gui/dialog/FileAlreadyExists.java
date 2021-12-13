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

import javax.swing.*;
import java.awt.*;

public class FileAlreadyExists {

    public static volatile boolean alreadyOpened = false;
    public volatile JDialog fileAlreadyExists = null;
    public volatile JPanel contentsPanel = new JPanel();
    public volatile UserChoice userChoice;
    EOCVSim eocvSim = null;

    public FileAlreadyExists(JFrame parent, EOCVSim eocvSim) {

        fileAlreadyExists = new JDialog(parent);

        this.eocvSim = eocvSim;

        eocvSim.visualizer.childDialogs.add(fileAlreadyExists);

    }

    public UserChoice run() {
        fileAlreadyExists.setModal(true);

        fileAlreadyExists.setTitle("Warning");

        JPanel alreadyExistsPanel = new JPanel(new FlowLayout());

        JLabel alreadyExistsLabel = new JLabel("File already exists in the selected directory");
        alreadyExistsPanel.add(alreadyExistsLabel);

        contentsPanel.add(alreadyExistsPanel);

        JPanel replaceCancelPanel = new JPanel(new FlowLayout());

        JButton replaceButton = new JButton("Replace");
        replaceCancelPanel.add(replaceButton);

        replaceButton.addActionListener((e) -> {
            userChoice = UserChoice.REPLACE;
            fileAlreadyExists.setVisible(false);
        });

        JButton cancelButton = new JButton("Cancel");
        replaceCancelPanel.add(cancelButton);

        cancelButton.addActionListener((e) -> {
            userChoice = UserChoice.CANCEL;
            fileAlreadyExists.setVisible(false);
        });

        contentsPanel.add(replaceCancelPanel);

        fileAlreadyExists.add(contentsPanel);

        fileAlreadyExists.setResizable(false);
        fileAlreadyExists.setLocationRelativeTo(null);
        fileAlreadyExists.setVisible(true);

        while (userChoice == UserChoice.NA);

        return userChoice;
    }

    public enum UserChoice {NA, REPLACE, CANCEL}

}
