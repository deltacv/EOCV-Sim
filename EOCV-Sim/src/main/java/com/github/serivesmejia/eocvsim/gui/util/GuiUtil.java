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

package com.github.serivesmejia.eocvsim.gui.util;

import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.gui.DialogFactory;
import com.github.serivesmejia.eocvsim.gui.dialog.FileAlreadyExists;
import com.github.serivesmejia.eocvsim.util.CvUtil;
import com.github.serivesmejia.eocvsim.util.SysUtil;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GuiUtil {

    static Logger logger = LoggerFactory.getLogger(GuiUtil.class);

    public static void jTextFieldOnlyNumbers(JTextField field, int minNumber, int onMinNumberChangeTo) {

        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            final Pattern regEx = Pattern.compile("\\d*");

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                Matcher matcher = regEx.matcher(text);
                if (!matcher.matches()) {
                    return;
                }

                if (field.getText().length() == 0) {
                    try {
                        int number = Integer.parseInt(text);
                        if (number <= minNumber) {
                            text = String.valueOf(onMinNumberChangeTo);
                        }
                    } catch (NumberFormatException ex) {
                    }
                }

                super.replace(fb, offset, length, text, attrs);
            }
        });

    }

    public static ImageIcon scaleImage(ImageIcon icon, int w, int h) {

        int nw = icon.getIconWidth();
        int nh = icon.getIconHeight();

        if (icon.getIconWidth() > w) {
            nw = w;
            nh = (nw * icon.getIconHeight()) / icon.getIconWidth();
        }

        if (nh > h) {
            nh = h;
            nw = (icon.getIconWidth() * nh) / icon.getIconHeight();
        }

        return new ImageIcon(icon.getImage().getScaledInstance(nw, nh, Image.SCALE_SMOOTH));

    }

    public static ImageIcon loadImageIcon(String path) throws IOException {
        return new ImageIcon(loadBufferedImage(path));
    }

    public static BufferedImage loadBufferedImage(String path) throws IOException {
        return ImageIO.read(GuiUtil.class.getResourceAsStream(path));
    }

    public static void saveBufferedImage(File file, BufferedImage bufferedImage, String format) throws IOException {
        ImageIO.write(bufferedImage, format, file);
    }

    public static void saveBufferedImage(File file, BufferedImage bufferedImage) throws IOException {
        saveBufferedImage(file, bufferedImage, "jpg");
    }

    public static void catchSaveBufferedImage(File file, BufferedImage bufferedImage, String format) {
        try {
            saveBufferedImage(file, bufferedImage, format);
        } catch (IOException e) {
            logger.error("Failed to save buffered image", e);
        }
    }

    public static void catchSaveBufferedImage(File file, BufferedImage bufferedImage) {
        catchSaveBufferedImage(file, bufferedImage, "jpg");
    }

    public static void invertBufferedImageColors(BufferedImage input) {

        for (int x = 0; x < input.getWidth(); x++) {
            for (int y = 0; y < input.getHeight(); y++) {

                int rgba = input.getRGB(x, y);
                Color col = new Color(rgba, true);

                if (col.getAlpha() <= 0) continue;

                col = new Color(255 - col.getRed(),
                        255 - col.getGreen(),
                        255 - col.getBlue());

                input.setRGB(x, y, col.getRGB());

            }
        }

    }

    public static void saveBufferedImageFileChooser(Component parent, BufferedImage bufferedImage, EOCVSim eocvSim) {

        FileNameExtensionFilter jpegFilter = new FileNameExtensionFilter("JPEG (*.jpg)", "jpg", "jpeg");
        FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("PNG (*.png)", "png");

        String[] validExts = {"jpg", "jpeg", "png"};

        DialogFactory.createFileChooser(parent, DialogFactory.FileChooser.Mode.SAVE_FILE_SELECT, jpegFilter, pngFilter)

                .addCloseListener((MODE, selectedFile, selectedFileFilter) -> {
                    if (MODE == JFileChooser.APPROVE_OPTION) {

                        Optional<String> extension = SysUtil.getExtensionByStringHandling(selectedFile.getName());

                        boolean saveImage;

                        if (!selectedFile.exists()) {
                            saveImage = true;
                        } else {
                            FileAlreadyExists.UserChoice userChoice = DialogFactory.createFileAlreadyExistsDialog(eocvSim); //create confirm dialog
                            saveImage = userChoice == FileAlreadyExists.UserChoice.REPLACE;
                        }

                        String ext = "";

                        if (saveImage) {
                            if (selectedFileFilter instanceof FileNameExtensionFilter) { //if user selected an extension

                                //get selected extension
                                ext = ((FileNameExtensionFilter) selectedFileFilter).getExtensions()[0];

                                selectedFile = new File(selectedFile + "." + ext); //append extension to file
                                catchSaveBufferedImage(selectedFile, bufferedImage, ext);

                            } else if (extension.isPresent() && Arrays.asList(validExts).contains(extension.get())) { //if user gave a extension to file and it's valid (jpg, jpeg or png)

                                ext = extension.get(); //get the extension
                                catchSaveBufferedImage(selectedFile, bufferedImage, ext);

                            } else { //default to jpg if the conditions are not met

                                selectedFile = new File(selectedFile + ".jpg"); //append default extension to file
                                catchSaveBufferedImage(selectedFile, bufferedImage);

                            }
                        }

                    }
                });

    }

    public static void saveMatFileChooser(Component parent, Mat mat, EOCVSim eocvSim) {

        Mat clonedMat = mat.clone();

        BufferedImage img = CvUtil.matToBufferedImage(clonedMat);
        clonedMat.release();

        saveBufferedImageFileChooser(parent, img, eocvSim);

    }

    public static ListModel<String> isToListModel(InputStream is, Charset charset) throws UnsupportedEncodingException {

        DefaultListModel<String> listModel = new DefaultListModel<>();
        String isStr = SysUtil.loadIsStr(is, charset);

        String[] lines = isStr.split("\n");

        for(int i = 0 ; i < lines.length ; i++) {
            listModel.add(i, lines[i]);
        }

        return listModel;

    }

    public static class NoSelectionModel extends DefaultListSelectionModel {

        @Override
        public void setAnchorSelectionIndex(final int anchorIndex) {}

        @Override
        public void setLeadAnchorNotificationEnabled(final boolean flag) {}

        @Override
        public void setLeadSelectionIndex(final int leadIndex) {}

        @Override
        public void setSelectionInterval(final int index0, final int index1) { }
    }

}
