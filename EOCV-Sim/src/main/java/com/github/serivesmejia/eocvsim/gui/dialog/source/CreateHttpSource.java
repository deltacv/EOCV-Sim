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
import com.github.serivesmejia.eocvsim.gui.component.input.FileSelector;
import com.github.serivesmejia.eocvsim.gui.component.input.SizeFields;
import com.github.serivesmejia.eocvsim.input.source.HttpSource;
import com.github.serivesmejia.eocvsim.input.source.ImageSource;
import com.github.serivesmejia.eocvsim.util.FileFilters;
import com.github.serivesmejia.eocvsim.util.StrUtil;
import io.github.deltacv.vision.external.util.CvUtil;
import org.opencv.core.Size;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;

public class CreateHttpSource {

    public JDialog createImageSource;

    public JTextField nameTextField = null;

    public JTextField urlField = null;

    public JButton createButton = null;

    private EOCVSim eocvSim = null;

    public CreateHttpSource(JFrame parent, EOCVSim eocvSim) {
        createImageSource = new JDialog(parent);

        this.eocvSim = eocvSim;

        eocvSim.visualizer.childDialogs.add(createImageSource);

        initCreateImageSource();
    }

    public void initCreateImageSource() {
        createImageSource.setModal(false);
        // createImageSource.setFocusableWindowState(false);
        createImageSource.setAlwaysOnTop(true);

        createImageSource.setTitle("Create HTTP Source");
        createImageSource.setSize(370, 80);

        JPanel contentsPanel = new JPanel();
        contentsPanel.setLayout(new BoxLayout(contentsPanel, BoxLayout.Y_AXIS));

        contentsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(7, 0, 0, 7);

        //file select part

        JLabel urlLabel = new JLabel("URL: ");
        urlLabel.setHorizontalAlignment(JLabel.LEFT);
        gbc.gridx = 0;
        fieldsPanel.add(urlLabel, gbc);

        urlField = new JTextField("http://", 15);
        gbc.gridx = 1;
        fieldsPanel.add(urlField, gbc);

        //Name part
        gbc.gridy = 1;

        JLabel nameLabel = new JLabel("Source name: ");
        nameLabel.setHorizontalAlignment(JLabel.LEFT);

        gbc.gridx = 0;
        fieldsPanel.add(nameLabel, gbc);

        nameTextField = new JTextField("HttpSource-" + (eocvSim.inputSourceManager.sources.size() + 1), 15);

        gbc.gridx = 1;
        fieldsPanel.add(nameTextField, gbc);

        contentsPanel.add(fieldsPanel);

        // Bottom buttons

        JPanel buttonsPanel = new JPanel(new FlowLayout());
        createButton = new JButton("Create");

        buttonsPanel.add(createButton);

        JButton cancelButton = new JButton("Cancel");
        buttonsPanel.add(cancelButton);

        contentsPanel.add(buttonsPanel);

        //Add contents

        createImageSource.getContentPane().add(contentsPanel, BorderLayout.CENTER);

        // Additional stuff & events

        nameTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                changed();
            }

            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            public void changed() {
                updateCreateBtt();
            }
        });

        urlField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                changed();
            }

            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            public void changed() {
                updateCreateBtt();
            }
        });

        createButton.addActionListener(e -> {
            createSource(nameTextField.getText(), urlField.getText());
            close();
        });

        cancelButton.addActionListener(e -> close());

        createImageSource.pack();
        createImageSource.setLocationRelativeTo(null);
        createImageSource.setAlwaysOnTop(true);
        createImageSource.setResizable(false);
        createImageSource.setVisible(true);

    }

    public void close() {
        createImageSource.setVisible(false);
        createImageSource.dispose();
    }

    public void createSource(String sourceName, String imgPath) {
        eocvSim.onMainUpdate.doOnce(() ->
                eocvSim.inputSourceManager.addInputSource(
                        sourceName,
                        new HttpSource(imgPath),
                        false
                )
        );
    }

    public void updateCreateBtt() {
        createButton.setEnabled(!nameTextField.getText().trim().isEmpty()
                && !urlField.getText().trim().isEmpty()
                && !eocvSim.inputSourceManager.isNameOnUse(nameTextField.getText()));
    }

}
