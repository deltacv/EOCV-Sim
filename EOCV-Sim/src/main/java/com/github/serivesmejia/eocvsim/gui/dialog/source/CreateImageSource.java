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
import com.github.serivesmejia.eocvsim.input.source.ImageSource;
import com.github.serivesmejia.eocvsim.util.CvUtil;
import com.github.serivesmejia.eocvsim.util.FileFilters;
import com.github.serivesmejia.eocvsim.util.StrUtil;
import org.opencv.core.Size;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;

public class CreateImageSource {

    public JDialog createImageSource;

    public JTextField nameTextField = null;

    public SizeFields sizeFieldsInput = null;

    public FileSelector imageFileSelector = null;

    private File initialFile = null;

    public JButton createButton = null;
    public boolean selectedValidImage = false;
    private EOCVSim eocvSim = null;

    public CreateImageSource(JFrame parent, EOCVSim eocvSim, File initialFile) {
        createImageSource = new JDialog(parent);

        this.eocvSim = eocvSim;
        this.initialFile = initialFile;

        eocvSim.visualizer.childDialogs.add(createImageSource);

        initCreateImageSource();
    }

    public void initCreateImageSource() {

        createImageSource.setModal(true);

        createImageSource.setTitle("Create image source");
        createImageSource.setSize(370, 200);

        JPanel contentsPanel = new JPanel(new GridLayout(4, 1));

        //file select part

        imageFileSelector = new FileSelector(18, FileFilters.imagesFilter);

        imageFileSelector.onFileSelect.doPersistent(() ->
            imageFileSelected(imageFileSelector.getLastSelectedFile())
        );


        if(initialFile != null)
            SwingUtilities.invokeLater(() ->
                imageFileSelector.setLastSelectedFile(initialFile)
            );

        contentsPanel.add(imageFileSelector);

        // Size part

        sizeFieldsInput = new SizeFields();
        sizeFieldsInput.onChange.doPersistent(this::updateCreateBtt);

        contentsPanel.add(sizeFieldsInput);
        contentsPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        //Name part

        JPanel namePanel = new JPanel(new FlowLayout());

        JLabel nameLabel = new JLabel("Source name: ");
        nameLabel.setHorizontalAlignment(JLabel.LEFT);

        nameTextField = new JTextField("ImageSource-" + (eocvSim.inputSourceManager.sources.size() + 1), 15);

        namePanel.add(nameLabel);
        namePanel.add(nameTextField);

        contentsPanel.add(namePanel);

        // Bottom buttons

        JPanel buttonsPanel = new JPanel(new FlowLayout());
        createButton = new JButton("Create");
        createButton.setEnabled(selectedValidImage);

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

        createButton.addActionListener(e -> {
            createSource(nameTextField.getText(), imageFileSelector.getLastSelectedFile().getAbsolutePath(), sizeFieldsInput.getCurrentSize());
            close();
        });

        cancelButton.addActionListener(e -> close());

        createImageSource.pack();
        createImageSource.setLocationRelativeTo(null);
        createImageSource.setResizable(false);
        createImageSource.setVisible(true);

    }

    public void imageFileSelected(File f) {
        String fileAbsPath = f.getAbsolutePath();

        if (CvUtil.checkImageValid(fileAbsPath)) {

            String fileName = StrUtil.getFileBaseName(f.getName());
            if(!fileName.trim().equals("")) {
                nameTextField.setText(eocvSim.inputSourceManager.tryName(fileName));
            }

            Size size = CvUtil.scaleToFit(CvUtil.getImageSize(fileAbsPath), EOCVSim.DEFAULT_EOCV_SIZE);

            sizeFieldsInput.getWidthTextField().setText(String.valueOf(Math.round(size.width)));
            sizeFieldsInput.getHeightTextField().setText(String.valueOf(Math.round(size.height)));

            selectedValidImage = true;
        } else {
            imageFileSelector.getDirTextField().setText("Unable to load selected file.");
            selectedValidImage = false;
        }

        updateCreateBtt();
    }

    public void close() {
        createImageSource.setVisible(false);
        createImageSource.dispose();
    }

    public void createSource(String sourceName, String imgPath, Size size) {
        eocvSim.onMainUpdate.doOnce(() ->
                eocvSim.inputSourceManager.addInputSource(
                        sourceName,
                        new ImageSource(imgPath, size),
                        false
                )
        );
    }

    public void updateCreateBtt() {
        createButton.setEnabled(!nameTextField.getText().trim().equals("")
                && sizeFieldsInput.getValid()
                && selectedValidImage
                && !eocvSim.inputSourceManager.isNameOnUse(nameTextField.getText()));
    }

}
