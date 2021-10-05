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

import com.github.sarxos.webcam.Webcam;
import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.gui.component.input.SizeFields;
import com.github.serivesmejia.eocvsim.input.source.CameraSource;
import com.github.serivesmejia.eocvsim.util.CvUtil;
import com.github.serivesmejia.eocvsim.util.Log;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class CreateCameraSource {

    public static int VISIBLE_CHARACTERS_COMBO_BOX = 22;

    public JDialog createCameraSource = null;

    public JComboBox<String> camerasComboBox = null;
    public SizeFields sizeFieldsInput = null;
    public JTextField nameTextField = null;

    public JButton createButton = null;

    public boolean wasCancelled = false;

    private final EOCVSim eocvSim;

    private State state = State.INITIAL;

    JLabel statusLabel = new JLabel();

    enum State { INITIAL, CLICKED_TEST, TEST_SUCCESSFUL, TEST_FAILED, NO_WEBCAMS }

    public CreateCameraSource(JFrame parent, EOCVSim eocvSim) {
        createCameraSource = new JDialog(parent);

        this.eocvSim = eocvSim;
        eocvSim.visualizer.childDialogs.add(createCameraSource);

        initCreateImageSource();
    }

    public void initCreateImageSource() {
        java.util.List<Webcam> webcams = Webcam.getWebcams();

        createCameraSource.setModal(true);

        createCameraSource.setTitle("Create camera source");
        createCameraSource.setSize(350, 280);

        JPanel contentsPanel = new JPanel(new GridLayout(5, 1));

        // Camera id part
        JPanel idPanel = new JPanel(new FlowLayout());

        JLabel idLabel = new JLabel("Camera: ");
        idLabel.setHorizontalAlignment(JLabel.LEFT);

        camerasComboBox = new JComboBox<>();
        if(webcams.isEmpty()) {
            camerasComboBox.addItem("No Cameras Detected");
            state = State.NO_WEBCAMS;
        } else {
            for(Webcam webcam : webcams) {
                // limit the webcam name to certain characters and append dots in the end if needed
                String dots = webcam.getName().length() > VISIBLE_CHARACTERS_COMBO_BOX ? "..." : "";

                camerasComboBox.addItem(
                        // https://stackoverflow.com/a/27060643
                        String.format("%1." + VISIBLE_CHARACTERS_COMBO_BOX + "s", webcam.getName()).trim() + dots
                );
            }

            SwingUtilities.invokeLater(() -> camerasComboBox.setSelectedIndex(0));
        }

        idPanel.add(idLabel);
        idPanel.add(camerasComboBox);

        contentsPanel.add(idPanel);

        //Name part

        JPanel namePanel = new JPanel(new FlowLayout());

        JLabel nameLabel = new JLabel("Source Name: ");

        nameTextField = new JTextField("CameraSource-" + (eocvSim.inputSourceManager.sources.size() + 1), 15);

        namePanel.add(nameLabel);
        namePanel.add(nameTextField);

        contentsPanel.add(namePanel);

        // Size part
        sizeFieldsInput = new SizeFields();
        sizeFieldsInput.onChange.doPersistent(this::updateCreateBtt);

        contentsPanel.add(sizeFieldsInput);

        contentsPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        // Status label part
        statusLabel.setHorizontalAlignment(JLabel.CENTER);

        contentsPanel.add(statusLabel);

        // Bottom buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout());
        createButton = new JButton();

        buttonsPanel.add(createButton);

        JButton cancelButton = new JButton("Cancel");
        buttonsPanel.add(cancelButton);

        contentsPanel.add(buttonsPanel);

        //Add contents
        contentsPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        createCameraSource.getContentPane().add(contentsPanel, BorderLayout.CENTER);

        // Additional stuff & events
        createButton.addActionListener(e -> {
            if(state == State.TEST_SUCCESSFUL) {
                createSource(
                        nameTextField.getText(),
                        camerasComboBox.getSelectedIndex(),
                        sizeFieldsInput.getCurrentSize()
                );
                close();
            } else {
                state = State.CLICKED_TEST;
                updateState();

                eocvSim.onMainUpdate.doOnce(() -> {
                    if (testCamera(camerasComboBox.getSelectedIndex())) {
                        if (wasCancelled) return;

                        SwingUtilities.invokeLater(() -> {
                            state = State.TEST_SUCCESSFUL;
                            updateState();
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            state = State.TEST_FAILED;
                            updateState();
                        });
                    }
                });
            }
        });

        camerasComboBox.addActionListener((e) -> {
            String sourceName = webcams.get(camerasComboBox.getSelectedIndex()).getName();
            if(!eocvSim.inputSourceManager.isNameOnUse(sourceName)) {
                nameTextField.setText(sourceName);
            }

            state = State.INITIAL;
            updateCreateBtt();
        });

        nameTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                changed();
            }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void insertUpdate(DocumentEvent e) {
                changed();
            }
            public void changed() {
                updateCreateBtt();
            }
        });

        cancelButton.addActionListener(e -> {
            wasCancelled = true;
            close();
        });

        updateState();

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
        camera.open(camerasComboBox.getSelectedIndex());

        boolean wasOpened = camera.isOpened();

        if(wasOpened) {
            Mat m = new Mat();
            try {
                camera.read(m);
                Size size = CvUtil.scaleToFit(m.size(), EOCVSim.DEFAULT_EOCV_SIZE);

                SwingUtilities.invokeLater(() -> {
                    sizeFieldsInput.getWidthTextField().setText(String.format("%.0f", size.width));
                    sizeFieldsInput.getHeightTextField().setText(String.format("%.0f", size.height));
                });
            } catch (Exception e) {
                Log.warn("CreateCameraSource", "Threw exception when trying to open camera", e);
                wasOpened = false;
            }

            m.release();
        }

        camera.release();

        return wasOpened;
    }

    private void updateState() {
        switch(state) {
            case INITIAL:
                statusLabel.setText("Click \"test\" to test camera.");
                createButton.setText("Test");
                break;

            case CLICKED_TEST:
                statusLabel.setText("Trying to open camera, please wait...");
                camerasComboBox.setEnabled(false);
                createButton.setEnabled(false);
                break;

            case TEST_SUCCESSFUL:
                camerasComboBox.setEnabled(true);
                createButton.setEnabled(true);
                statusLabel.setText("Camera was opened successfully.");
                createButton.setText("Create");
                break;

            case TEST_FAILED:
                camerasComboBox.setEnabled(true);
                createButton.setEnabled(true);
                statusLabel.setText("Failed to open camera, try another one.");
                createButton.setText("Test");
                break;

            case NO_WEBCAMS:
                statusLabel.setText("No cameras detected.");
                createButton.setText("Test");
                nameTextField.setText("");

                createButton.setEnabled(false);
                nameTextField.setEnabled(false);
                camerasComboBox.setEnabled(false);
                sizeFieldsInput.getWidthTextField().setEnabled(false);
                sizeFieldsInput.getHeightTextField().setEnabled(false);
                break;
        }
    }

    public void createSource(String sourceName, int index, Size size) {
        eocvSim.onMainUpdate.doOnce(() -> eocvSim.inputSourceManager.addInputSource(
                sourceName,
                new CameraSource(index, size),
                true
        ));
    }

    public void updateCreateBtt() {
        createButton.setEnabled(!nameTextField.getText().trim().equals("")
                && sizeFieldsInput.getValid()
                && !eocvSim.inputSourceManager.isNameOnUse(nameTextField.getText()));

        updateState();
    }

}
