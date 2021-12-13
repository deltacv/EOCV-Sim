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

package com.github.serivesmejia.eocvsim.gui.dialog.source;

import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.gui.DialogFactory;
import com.github.serivesmejia.eocvsim.input.SourceType;

import javax.swing.*;
import java.awt.*;

public class CreateSource {

    public static volatile boolean alreadyOpened = false;
    public volatile JDialog chooseSource = null;

    EOCVSim eocvSim = null;

    private volatile JFrame parent = null;

    public CreateSource(JFrame parent, EOCVSim eocvSim) {

        chooseSource = new JDialog(parent);

        this.parent = parent;
        this.eocvSim = eocvSim;

        eocvSim.visualizer.childDialogs.add(chooseSource);
        initChooseSource();

    }

    private void initChooseSource() {

        alreadyOpened = true;

        chooseSource.setModal(true);

        chooseSource.setTitle("Select source type");
        chooseSource.setSize(300, 150);

        JPanel contentsPane = new JPanel(new GridLayout(2, 1));

        JPanel dropDownPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        SourceType[] sourceTypes = SourceType.values();
        String[] sourceTypesStr = new String[sourceTypes.length - 1];

        for (int i = 0; i < sourceTypes.length - 1; i++) {
            sourceTypesStr[i] = sourceTypes[i].coolName;
        }

        JComboBox<String> dropDown = new JComboBox<>(sourceTypesStr);
        dropDownPanel.add(dropDown);
        contentsPane.add(dropDownPanel);

        JPanel buttonsPanel = new JPanel(new FlowLayout());
        JButton nextButton = new JButton("Next");

        buttonsPanel.add(nextButton);

        JButton cancelButton = new JButton("Cancel");
        buttonsPanel.add(cancelButton);

        contentsPane.add(buttonsPanel);

        contentsPane.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        chooseSource.getContentPane().add(contentsPane, BorderLayout.CENTER);

        cancelButton.addActionListener(e -> close());

        nextButton.addActionListener(e -> {
            close();
            SourceType sourceType = SourceType.fromCoolName(dropDown.getSelectedItem().toString());
            DialogFactory.createSourceDialog(eocvSim, sourceType);
        });

        chooseSource.pack();
        chooseSource.setLocationRelativeTo(null);
        chooseSource.setResizable(false);
        chooseSource.setVisible(true);
    }

    public void close() {
        alreadyOpened = false;
        chooseSource.setVisible(false);
        chooseSource.dispose();
    }

}
