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
import com.github.serivesmejia.eocvsim.input.source.CameraSource;
import com.github.serivesmejia.eocvsim.util.cv.CameraUtil;
import com.github.serivesmejia.eocvsim.util.Log;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

public class CreateCameraSource {

    public static int VISIBLE_CHARACTERS_COMBO_BOX = 22;

    public JDialog createCameraSource = null;

    public JComboBox<String> camerasComboBox = null;
    public JComboBox<String> dimensionsComboBox = null;
    public JTextField nameTextField = null;

    public JButton createButton = null;

    public boolean wasCancelled = false;

    private final EOCVSim eocvSim;

    private State state = State.INITIAL;

    JLabel statusLabel = new JLabel();

    private java.util.List<Webcam> webcams = null;

    // making it static so that we don't have to re-guess the sizes every time the frame is opened
    private static HashMap<String, Size[]> sizes = new HashMap<>();

    private HashMap<String, Integer> indexes = new HashMap<>();

    enum State { INITIAL, CLICKED_TEST, TEST_SUCCESSFUL, TEST_FAILED, NO_WEBCAMS, UNSUPPORTED }

    public CreateCameraSource(JFrame parent, EOCVSim eocvSim) {
        createCameraSource = new JDialog(parent);

        this.eocvSim = eocvSim;
        eocvSim.visualizer.childDialogs.add(createCameraSource);

        initCreateImageSource();
    }

    private boolean usingOpenCvDiscovery = false;

    public void initCreateImageSource() {
        try {
            webcams = Webcam.getWebcams();
        } catch(Throwable e) {
            Log.warn("CreateCameraSource", "webcam-capture is unusable, falling basck to OpenCV webcam discovery");
            webcams = CameraUtil.findWebcamsOpenCv();

            usingOpenCvDiscovery = true;
        }

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
            int index = 0;

            for(Webcam webcam : webcams.toArray(new Webcam[0])) {
                if(webcam == null) continue;

                // limit the webcam name to certain characters and append dots in the end if needed
                String dots = webcam.getName().length() > VISIBLE_CHARACTERS_COMBO_BOX ? "..." : "";

                // https://stackoverflow.com/a/27060643
                String name = String.format("%1." + VISIBLE_CHARACTERS_COMBO_BOX + "s", webcam.getName()).trim() + dots;

                camerasComboBox.removeItem(name);
                camerasComboBox.addItem(name);

                if(!indexes.containsKey(name)) {
                    indexes.put(name, index);

                    if(!sizes.containsKey(name)) {
                        Size[] resolutions = CameraUtil.getResolutionsOf(index);

                        if(resolutions.length == 0) {
                            ArrayList<Webcam> newWebcams = new ArrayList<>();

                            for(int i = 0 ; i < webcams.size() ; i++) {
                                if(i != index) {
                                    newWebcams.add(webcams.get(i));
                                } else {
                                    newWebcams.add(null);
                                }
                            }

                            webcams = newWebcams;
                            Log.warn("CreateCameraSource", "Webcam " + webcam.getName() + " didn't return any available resolutions, therefore it's unavailable.");
                            continue;
                        }

                        sizes.put(name, resolutions);
                    }
                }

                index++;
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

        JPanel sizePanel = new JPanel(new FlowLayout());

        JLabel sizeLabel = new JLabel("Available resolutions: ");
        sizePanel.add(sizeLabel);

        dimensionsComboBox = new JComboBox<>();
        sizePanel.add(dimensionsComboBox);

        contentsPanel.add(sizePanel);

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
                Webcam webcam = webcams.get(getSelectedIndex());

                Size dim = sizes.get(
                        camerasComboBox.getSelectedItem()
                )[dimensionsComboBox.getSelectedIndex()]; //oh god again...

                if(usingOpenCvDiscovery) {
                    createSource(
                            nameTextField.getText(),
                            camerasComboBox.getSelectedIndex(),
                            new Size(dim.width, dim.height)
                    );
                } else {
                    createSource(
                            nameTextField.getText(),
                            webcam.getName(),
                            new Size(dim.width, dim.height)
                    );
                }
                close();
            } else {
                state = State.CLICKED_TEST;
                updateState();

                eocvSim.onMainUpdate.doOnce(() -> {
                    if (testCamera(getSelectedIndex())) {
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
            Webcam webcam = webcams.get(getSelectedIndex());

            if(webcam == null) {
                state = State.UNSUPPORTED;
                updateState();
                return;
            }

            nameTextField.setText(eocvSim.inputSourceManager.tryName(webcam.getName()));

            dimensionsComboBox.removeAllItems();
            Size[] webcamSizes = sizes.get(camerasComboBox.getSelectedItem());

            if(webcamSizes.length == 0) {
                state = State.UNSUPPORTED;
            } else {
                for(Size dim : webcamSizes) {
                    dimensionsComboBox.addItem(Math.round(dim.width) + "x" + Math.round(dim.height));
                }

                state = State.INITIAL;
            }

            updateCreateBtt();
            updateState();
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
        camera.open(camIndex);

        boolean wasOpened = camera.isOpened();

        if(wasOpened) {
            Mat m = new Mat();
            try {
                camera.read(m);
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

                setInteractables(true);
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

                setInteractables(false);
                break;
            case UNSUPPORTED:
                statusLabel.setText("This camera is currently unavailable.");
                createButton.setText("Test");
                nameTextField.setText("");

                setInteractables(false);
                camerasComboBox.setEnabled(true);
                break;
        }
    }

    private void setInteractables(boolean value) {
        createButton.setEnabled(value);
        nameTextField.setEnabled(value);
        camerasComboBox.setEnabled(value);
        dimensionsComboBox.setEnabled(value);
    }

    public void createSource(String sourceName, String camName, Size size) {
        eocvSim.onMainUpdate.doOnce(() -> eocvSim.inputSourceManager.addInputSource(
                sourceName,
                new CameraSource(camName, size),
                true
        ));
    }

    public void createSource(String sourceName, int camIndex, Size size) {
        eocvSim.onMainUpdate.doOnce(() -> eocvSim.inputSourceManager.addInputSource(
                sourceName,
                new CameraSource(camIndex, size),
                true
        ));
    }

    public void updateCreateBtt() {
        createButton.setEnabled(!nameTextField.getText().trim().equals("")
                && !eocvSim.inputSourceManager.isNameOnUse(nameTextField.getText()));
    }

    public int getSelectedIndex() {
        return indexes.get(camerasComboBox.getSelectedItem());
    }

}