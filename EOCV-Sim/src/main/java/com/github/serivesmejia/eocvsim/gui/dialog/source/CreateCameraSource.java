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
import com.github.serivesmejia.eocvsim.gui.component.input.SizeFields;
import com.github.serivesmejia.eocvsim.gui.util.GuiUtil;
import com.github.serivesmejia.eocvsim.input.source.CameraSource;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class CreateCameraSource {

    public JDialog createCameraSource = null;

    public JTextField cameraIdField = null;
    public JButton createButton = null;

    public SizeFields sizeFieldsInput = null;

    public JTextField nameTextField = null;

    public boolean wasCancelled = false;
    private boolean validCameraIdNumber = true;

    private final EOCVSim eocvSim;

    private volatile String initialSourceName = "";

    public CreateCameraSource(JFrame parent, EOCVSim eocvSim) {
        createCameraSource = new JDialog(parent);

        this.eocvSim = eocvSim;
        eocvSim.visualizer.childDialogs.add(createCameraSource);

        initCreateImageSource();
    }

    public void initCreateImageSource() {

        createCameraSource.setModal(true);

        createCameraSource.setTitle("Create camera source");
        createCameraSource.setSize(350, 230);

        JPanel contentsPanel = new JPanel(new GridLayout(5, 1));

        // Camera id part

        JPanel idPanel = new JPanel(new FlowLayout());

        JLabel idLabel = new JLabel("Camera ID: ");
        idLabel.setHorizontalAlignment(JLabel.CENTER);

        cameraIdField = new JTextField("0", 4);

        idPanel.add(idLabel);
        idPanel.add(cameraIdField);

        contentsPanel.add(idPanel);

        //Name part

        JPanel namePanel = new JPanel(new FlowLayout());

        JLabel nameLabel = new JLabel("Source name: ");

        nameTextField = new JTextField("CameraSource-" + (eocvSim.inputSourceManager.sources.size() + 1), 15);
        initialSourceName = nameTextField.getName();

        namePanel.add(nameLabel);
        namePanel.add(nameTextField);

        contentsPanel.add(namePanel);

        // Size part
        sizeFieldsInput = new SizeFields();
        sizeFieldsInput.onChange.doPersistent(this::updateCreateBtt);

        contentsPanel.add(sizeFieldsInput);

        contentsPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        // Status label part
        JLabel statusLabel = new JLabel("Click \"create\" to test camera.");
        statusLabel.setHorizontalAlignment(JLabel.CENTER);

        contentsPanel.add(statusLabel);

        // Bottom buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout());
        createButton = new JButton("Create");

        buttonsPanel.add(createButton);

        JButton cancelButton = new JButton("Cancel");
        buttonsPanel.add(cancelButton);

        contentsPanel.add(buttonsPanel);

        //Add contents

        contentsPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        createCameraSource.getContentPane().add(contentsPanel, BorderLayout.CENTER);

        // Additional stuff & events

        GuiUtil.jTextFieldOnlyNumbers(cameraIdField, -100, 0);

        createButton.addActionListener(e -> {

            int camId = Integer.parseInt(cameraIdField.getText());

            statusLabel.setText("Trying to open camera, please wait...");
            cameraIdField.setEditable(false);
            createButton.setEnabled(false);

            eocvSim.onMainUpdate.doOnce(() -> {
                if (testCamera(camId)) {
                    close();
                    if (wasCancelled) return;
                    createSource(nameTextField.getText(), camId, sizeFieldsInput.getCurrentSize());
                } else {
                    cameraIdField.setEditable(true);
                    createButton.setEnabled(true);
                    statusLabel.setText("Failed to open camera, try with another index.");
                }
            });

        });

        cameraIdField.getDocument().addDocumentListener(new DocumentListener() {
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
                try {
                    Integer.parseInt(cameraIdField.getText());

                    String sourceName = "Camera " + cameraIdField.getText();
                    if(!eocvSim.inputSourceManager.isNameOnUse(sourceName)) {
                        nameTextField.setText(sourceName);
                    }

                    validCameraIdNumber = true;
                } catch (Exception ex) {
                    validCameraIdNumber = false;
                }
                updateCreateBtt();
            }
        });

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

        SwingUtilities.invokeLater(() -> cameraIdField.setText("0"));

        cancelButton.addActionListener(e -> {
            wasCancelled = true;
            close();
        });

        createCameraSource.setResizable(false);
        createCameraSource.setLocationRelativeTo(null);
        createCameraSource.setVisible(true);

    }

    public void close() {
        createCameraSource.setVisible(false);
        createCameraSource.dispose();
    }

    public boolean testCamera(int camIndex) {
        VideoCapture camera = new VideoCapture();
        camera.open(camIndex);

        boolean wasOpened = camera.isOpened();

        camera.release();

        return wasOpened;
    }

    public void createSource(String sourceName, int index, Size size) {
        eocvSim.onMainUpdate.doOnce(() -> {
            eocvSim.inputSourceManager.addInputSource(
                    sourceName,
                    new CameraSource(index, size)
            );

            eocvSim.inputSourceManager.requestSetInputSource(sourceName);
        });
    }

    public void updateCreateBtt() {
        createButton.setEnabled(!nameTextField.getText().trim().equals("")
                && validCameraIdNumber
                && sizeFieldsInput.getValid()
                && !eocvSim.inputSourceManager.isNameOnUse(nameTextField.getText()));
    }

}
