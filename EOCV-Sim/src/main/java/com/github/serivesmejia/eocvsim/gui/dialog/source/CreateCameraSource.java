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
import com.github.serivesmejia.eocvsim.gui.component.input.EnumComboBox;
import com.github.serivesmejia.eocvsim.gui.util.WebcamDriver;
import com.github.serivesmejia.eocvsim.input.source.CameraSource;
import io.github.deltacv.steve.Webcam;
import io.github.deltacv.steve.WebcamRotation;
import io.github.deltacv.steve.opencv.OpenCvWebcam;
import io.github.deltacv.steve.opencv.OpenCvWebcamBackend;
import io.github.deltacv.steve.openimaj.OpenIMAJWebcamBackend;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class CreateCameraSource {

    public static int VISIBLE_CHARACTERS_COMBO_BOX = 22;

    public JDialog createCameraSource = null;

    public JComboBox<String> camerasComboBox = null;
    public JComboBox<String> dimensionsComboBox = null;
    public EnumComboBox<WebcamRotation> rotationComboBox = null;
    public JTextField nameTextField = null;

    public JButton createButton = null;

    public boolean wasCancelled = false;
    
    private final EOCVSim eocvSim;

    private State state = State.INITIAL;

    JLabel statusLabel = new JLabel();

    private java.util.List<Webcam> webcams = null;

    // making it static so that we don't have to re-guess the sizes every time the frame is opened
    private static HashMap<String, java.util.List<Size>> sizes = new HashMap<>();

    private HashMap<String, Integer> indexes = new HashMap<>();

    private static final Executor executor = Executors.newFixedThreadPool(3);

    Logger logger = LoggerFactory.getLogger(getClass());

    enum State { INITIAL, CLICKED_TEST, TEST_SUCCESSFUL, TEST_FAILED, NO_WEBCAMS, UNSUPPORTED }

    public CreateCameraSource(JFrame parent, EOCVSim eocvSim) {
        createCameraSource = new JDialog(parent);

        this.eocvSim = eocvSim;
        eocvSim.visualizer.childDialogs.add(createCameraSource);

        initCreateImageSource();
    }

    private boolean usingOpenCvDiscovery = false;

    public void initCreateImageSource() {
        WebcamDriver preferredDriver = eocvSim.getConfig().preferredWebcamDriver;

        if(preferredDriver == WebcamDriver.OpenIMAJ) {
            try {
                Webcam.Companion.setBackend(OpenIMAJWebcamBackend.INSTANCE);
                webcams = Webcam.Companion.getAvailableWebcams();
            } catch (Throwable e) {
                logger.error("OpenIMAJ failed to discover cameras with error", e);
                logger.warn("OpenIMAJ is unusable, falling back to OpenCV webcam discovery");

                Webcam.Companion.setBackend(OpenCvWebcamBackend.INSTANCE);
                webcams = Webcam.Companion.getAvailableWebcams();

                usingOpenCvDiscovery = true;
            }

            if (!usingOpenCvDiscovery && webcams.isEmpty()) {
                logger.warn("OpenIMAJ returned 0 cameras, trying with OpenCV webcam discovery");

                Webcam.Companion.setBackend(OpenCvWebcamBackend.INSTANCE);
                webcams = Webcam.Companion.getAvailableWebcams();

                usingOpenCvDiscovery = true;
            }
        } else {
            Webcam.Companion.setBackend(OpenCvWebcamBackend.INSTANCE);
            webcams = Webcam.Companion.getAvailableWebcams();
            usingOpenCvDiscovery = true;
        }

        createCameraSource.setModal(true);

        createCameraSource.setTitle("Create camera source");

        JPanel contentsPanel = new JPanel(new GridBagLayout());

        // Camera id part
        JLabel idLabel = new JLabel("Available cameras: ", JLabel.RIGHT);

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
                        int camIndex = index;
                        executor.execute(() -> {
                            java.util.List<Size> resolutions = webcam.getSupportedResolutions();

                            if (resolutions.size() == 0) {
                                // remove webcam from list since it didn't return valid res

                                ArrayList<Webcam> newWebcams = new ArrayList<>();

                                for (int i = 0; i < webcams.size(); i++) {
                                    if (i != camIndex) {
                                        newWebcams.add(webcams.get(i));
                                    } else {
                                        newWebcams.add(null);
                                    }
                                }

                                webcams = newWebcams;
                                logger.warn("Webcam " + webcam.getName() + " didn't return any available resolutions, therefore it's unavailable.");
                            } else {
                                sizes.put(name, resolutions);
                            }
                        });
                    }
                }

                index++;
            }

            SwingUtilities.invokeLater(() -> camerasComboBox.setSelectedIndex(0));
        }

        JPanel fieldsPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(7, 0, 0, 7);

        fieldsPanel.add(idLabel, gbc);

        gbc.gridx = 1;
        fieldsPanel.add(camerasComboBox, gbc);

        //Name part
        JLabel nameLabel = new JLabel("Source name: ", JLabel.RIGHT);

        nameTextField = new JTextField("CameraSource-" + (eocvSim.inputSourceManager.sources.size() + 1), 15);

        gbc.gridx = 0;
        gbc.gridy = 1;

        fieldsPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        fieldsPanel.add(nameTextField, gbc);

        // Size part
        JLabel sizeLabel = new JLabel("Suggested resolutions: ", JLabel.RIGHT);
        dimensionsComboBox = new JComboBox<>();

        gbc.gridx = 0;
        gbc.gridy = 2;

        fieldsPanel.add(sizeLabel, gbc);

        gbc.gridx = 1;
        fieldsPanel.add(dimensionsComboBox, gbc);

        // Webcam rotation combo box

        JLabel rotationLabel = new JLabel("Camera rotation: ", JLabel.RIGHT);
        rotationComboBox = new EnumComboBox<WebcamRotation>(
                "",
                WebcamRotation.class,
                WebcamRotation.values(),
                WebcamRotation::getDisplayName,
                WebcamRotation::fromDisplayName
        );

        gbc.gridx = 0;
        gbc.gridy = 3;

        fieldsPanel.add(rotationLabel, gbc);
        gbc.gridx = 1;
        fieldsPanel.add(rotationComboBox.getComboBox(), gbc);

        gbc.gridx = 0;
        gbc.gridy = 0;

        contentsPanel.add(fieldsPanel, gbc);

        contentsPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        // Status label part
        statusLabel.setHorizontalAlignment(JLabel.CENTER);

        gbc.gridy = 1;
        contentsPanel.add(statusLabel, gbc);

        // Bottom buttons

        JPanel bottomButtonsPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbcBtts = new GridBagConstraints();
        gbcBtts.insets = new Insets(0, 0, 0, 10);

        createButton = new JButton();
        JButton cancelButton = new JButton("Cancel");

        bottomButtonsPanel.add(createButton, gbcBtts);

        gbcBtts.gridx = 1;
        gbcBtts.insets = new Insets(0, 0, 0, 0);
        bottomButtonsPanel.add(cancelButton, gbcBtts);

        gbc.insets = new Insets(10, 0, 0, 0);
        gbc.gridx = 0;
        gbc.gridy = 2;
        contentsPanel.add(bottomButtonsPanel, gbc);

        //Add contents
        contentsPanel.setBorder(BorderFactory.createEmptyBorder(8, 15, 15, 0));

        createCameraSource.getContentPane().add(contentsPanel, BorderLayout.CENTER);

        // Additional stuff & events
        createButton.addActionListener(e -> {
            if(state == State.TEST_SUCCESSFUL) {
                Webcam webcam = webcams.get(getSelectedIndex());

                Size dim = sizes.get(camerasComboBox.getSelectedItem()
                ).get(dimensionsComboBox.getSelectedIndex()); //oh god again...

                if(usingOpenCvDiscovery) {
                    int index;
                    if(webcam instanceof OpenCvWebcam) {
                        index = webcam.getIndex();
                    } else {
                        index = camerasComboBox.getSelectedIndex();
                    }

                    createSource(
                            nameTextField.getText(),
                            index,
                            new Size(dim.width, dim.height),
                            rotationComboBox.getSelectedEnum()
                    );
                } else {
                    createSource(
                            nameTextField.getText(),
                            webcam.getName(),
                            new Size(dim.width, dim.height),
                            rotationComboBox.getSelectedEnum()
                    );
                }
                close();
            } else {
                state = State.CLICKED_TEST;
                updateState();

                eocvSim.onMainUpdate.doOnce(() -> {
                    Webcam webcam = webcams.get(getSelectedIndex());

                    Size dim = sizes.get(
                            camerasComboBox.getSelectedItem()
                    ).get(dimensionsComboBox.getSelectedIndex()); //oh god again...

                    webcam.setResolution(dim);

                    if (testCamera(webcam)) {
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

            Runnable runn = () -> {
                java.util.List<Size> webcamSizes = null;

                // TODO: Add a timeout to this abomination, maybe...?

                boolean firstLoop = true;
                while (webcamSizes == null) {
                    webcamSizes = sizes.get(camerasComboBox.getSelectedItem());

                    if (webcamSizes == null) {
                        if (firstLoop) {
                            dimensionsComboBox.addItem("Calculating...");
                            setInteractables(false);
                        }

                        Thread.yield();
                    }

                    firstLoop = false;
                }

                dimensionsComboBox.removeAllItems();

                if (webcamSizes.size() == 0) {
                    state = State.UNSUPPORTED;
                } else {
                    for (Size dim : webcamSizes) {
                        dimensionsComboBox.addItem(Math.round(dim.width) + "x" + Math.round(dim.height));
                    }

                    state = State.INITIAL;
                }

                updateCreateBtt();
                updateState();
            };

            if(sizes.get(camerasComboBox.getSelectedItem()) == null) {
                new Thread(runn).start();
            } else {
                runn.run();
            }
        });

        nameTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateCreateBtt(); }
            public void removeUpdate(DocumentEvent e) { updateCreateBtt(); }
            public void insertUpdate(DocumentEvent e) { updateCreateBtt(); }
        });

        cancelButton.addActionListener(e -> {
            wasCancelled = true;
            close();
        });

        updateState();

        createCameraSource.pack();
        createCameraSource.setResizable(false);
        createCameraSource.setLocationRelativeTo(null);
        createCameraSource.setVisible(true);
    }

    public void close() {
        createCameraSource.setVisible(false);
        createCameraSource.dispose();
    }

    public boolean testCamera(Webcam webcam) {
        webcam.open();

        boolean wasOpened = webcam.isOpen();

        if(wasOpened) {
            Mat m = new Mat();
            try {
                webcam.read(m);
            } catch (Exception e) {
                logger.warn("Threw exception when trying to open camera", e);
                wasOpened = false;
            }

            m.release();
            webcam.close();
        }

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

    public void createSource(String sourceName, String camName, Size size, WebcamRotation rotation) {
        eocvSim.onMainUpdate.doOnce(() -> eocvSim.inputSourceManager.addInputSource(
                sourceName,
                new CameraSource(camName, size, rotation),
                true
        ));
    }

    public void createSource(String sourceName, int camIndex, Size size, WebcamRotation rotation) {
        eocvSim.onMainUpdate.doOnce(() -> eocvSim.inputSourceManager.addInputSource(
                sourceName,
                new CameraSource(camIndex, size, rotation),
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