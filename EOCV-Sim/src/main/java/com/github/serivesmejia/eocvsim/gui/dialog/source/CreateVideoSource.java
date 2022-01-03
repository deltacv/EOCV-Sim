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
import com.github.serivesmejia.eocvsim.input.source.VideoSource;
import com.github.serivesmejia.eocvsim.util.CvUtil;
import com.github.serivesmejia.eocvsim.util.FileFilters;
import com.github.serivesmejia.eocvsim.util.StrUtil;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;

public class CreateVideoSource {

    public JDialog createVideoSource = null;

    public JTextField nameTextField = null;

    public FileSelector fileSelectorVideo = null;

    public SizeFields sizeFieldsInput = null;

    public JButton createButton = null;
    public boolean selectedValidVideo = false;

    public File initialFile = null;

    private EOCVSim eocvSim = null;

    public CreateVideoSource(JFrame parent, EOCVSim eocvSim, File initialFile) {

        createVideoSource = new JDialog(parent);

        this.eocvSim = eocvSim;
        this.initialFile = initialFile;

        eocvSim.visualizer.childDialogs.add(createVideoSource);

        initCreateImageSource();

    }

    public void initCreateImageSource() {

        createVideoSource.setModal(true);

        createVideoSource.setTitle("Create video source");
        createVideoSource.setSize(370, 200);

        JPanel contentsPanel = new JPanel(new GridLayout(4, 1));

        //file select
        fileSelectorVideo = new FileSelector(18, FileFilters.videoMediaFilter);

        fileSelectorVideo.onFileSelect.doPersistent(() ->
            videoFileSelected(fileSelectorVideo.getLastSelectedFile())
        );

        if(initialFile != null)
            SwingUtilities.invokeLater(() ->
                fileSelectorVideo.setLastSelectedFile(initialFile)
            );

        contentsPanel.add(fileSelectorVideo);

        // Size part

        sizeFieldsInput = new SizeFields();
        sizeFieldsInput.onChange.doPersistent(this::updateCreateBtt);

        contentsPanel.add(sizeFieldsInput);

        contentsPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        //Name part

        JPanel namePanel = new JPanel(new FlowLayout());

        JLabel nameLabel = new JLabel("Source name: ");
        nameLabel.setHorizontalAlignment(JLabel.LEFT);

        nameTextField = new JTextField("VideoSource-" + (eocvSim.inputSourceManager.sources.size() + 1), 15);

        namePanel.add(nameLabel);
        namePanel.add(nameTextField);

        contentsPanel.add(namePanel);

        // Bottom buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout());
        createButton = new JButton("Create");
        createButton.setEnabled(selectedValidVideo);

        buttonsPanel.add(createButton);

        JButton cancelButton = new JButton("Cancel");
        buttonsPanel.add(cancelButton);

        contentsPanel.add(buttonsPanel);

        //Add contents
        createVideoSource.getContentPane().add(contentsPanel, BorderLayout.CENTER);

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

        createButton.addActionListener(e -> {
            createSource(nameTextField.getText(), fileSelectorVideo.getLastSelectedFile().getAbsolutePath(), sizeFieldsInput.getCurrentSize());
            close();
        });

        // Status label part
        cancelButton.addActionListener(e -> close());

        createVideoSource.pack();
        createVideoSource.setLocationRelativeTo(null);
        createVideoSource.setResizable(false);
        createVideoSource.setVisible(true);
    }

    public void videoFileSelected(File f) {

        String fileAbsPath = f.getAbsolutePath();

        Mat videoMat = CvUtil.readOnceFromVideo(fileAbsPath);

        if (videoMat != null && !videoMat.empty()) {

            String fileName = StrUtil.getFileBaseName(f.getName());
            if(!fileName.trim().equals("")) {
                nameTextField.setText(eocvSim.inputSourceManager.tryName(fileName));
            }

            Size newSize = CvUtil.scaleToFit(videoMat.size(), EOCVSim.DEFAULT_EOCV_SIZE);

            this.sizeFieldsInput.getWidthTextField().setText(String.valueOf(Math.round(newSize.width)));
            this.sizeFieldsInput.getHeightTextField().setText(String.valueOf(Math.round(newSize.height)));

            selectedValidVideo = true;
        } else {
            fileSelectorVideo.getDirTextField().setText("Unable to load selected file.");
            selectedValidVideo = false;
        }

        if(videoMat != null) videoMat.release();

        updateCreateBtt();

    }

    public void close() {
        createVideoSource.setVisible(false);
        createVideoSource.dispose();
    }

    public void createSource(String sourceName, String videoPath, Size size) {
        eocvSim.onMainUpdate.doOnce(() -> {
            eocvSim.inputSourceManager.addInputSource(
                sourceName,
                new VideoSource(videoPath, size),
                true
            );
        });
    }

    public void updateCreateBtt() {
        createButton.setEnabled(!nameTextField.getText().trim().equals("")
                && sizeFieldsInput.getValid()
                && selectedValidVideo
                && !eocvSim.inputSourceManager.isNameOnUse(nameTextField.getText()));
    }

}
