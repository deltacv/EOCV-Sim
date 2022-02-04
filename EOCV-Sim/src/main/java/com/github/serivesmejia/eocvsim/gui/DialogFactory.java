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

package com.github.serivesmejia.eocvsim.gui;

import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.gui.dialog.*;
import com.github.serivesmejia.eocvsim.gui.dialog.SplashScreen;
import com.github.serivesmejia.eocvsim.gui.dialog.source.CreateCameraSource;
import com.github.serivesmejia.eocvsim.gui.dialog.source.CreateImageSource;
import com.github.serivesmejia.eocvsim.gui.dialog.source.CreateSource;
import com.github.serivesmejia.eocvsim.gui.dialog.source.CreateVideoSource;
import com.github.serivesmejia.eocvsim.input.SourceType;
import com.github.serivesmejia.eocvsim.util.event.EventHandler;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.function.IntConsumer;

public class DialogFactory {

    private DialogFactory() { }

    public static void createYesOrNo(Component parent, String message, String submessage, IntConsumer result) {
        JPanel panel = new JPanel();

        JLabel label1 = new JLabel(message);
        panel.add(label1);

        if (!submessage.trim().equals("")) {
            JLabel label2 = new JLabel(submessage);
            panel.add(label2);
            panel.setLayout(new GridLayout(2, 1));
        }

        SwingUtilities.invokeLater(() -> result.accept(
                JOptionPane.showConfirmDialog(parent, panel, "Confirm",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                )
        ));
    }

    public static FileChooser createFileChooser(Component parent, FileChooser.Mode mode, FileFilter... filters) {
        FileChooser fileChooser = new FileChooser(parent, mode, "", filters);
        invokeLater(fileChooser::init);
        return fileChooser;
    }

    public static FileChooser createFileChooser(Component parent, FileChooser.Mode mode, String initialFileName, FileFilter... filters) {
        FileChooser fileChooser = new FileChooser(parent, mode, initialFileName, filters);
        invokeLater(fileChooser::init);
        return fileChooser;
    }

    public static FileChooser createFileChooser(Component parent, FileFilter... filters) {
        return createFileChooser(parent, null, "", filters);
    }

    public static FileChooser createFileChooser(Component parent, FileChooser.Mode mode) {
        return createFileChooser(parent, mode, new FileFilter[0]);
    }

    public static FileChooser createFileChooser(Component parent) {
        return createFileChooser(parent, null, new FileFilter[0]);
    }

    public static void createSourceDialog(EOCVSim eocvSim,
                                          SourceType type,
                                          File initialFile) {
        invokeLater(() -> {
            switch (type) {
                case IMAGE:
                    new CreateImageSource(eocvSim.visualizer.frame, eocvSim, initialFile);
                    break;
                case CAMERA:
                    new CreateCameraSource(eocvSim.visualizer.frame, eocvSim);
                    break;
                case VIDEO:
                    new CreateVideoSource(eocvSim.visualizer.frame, eocvSim, initialFile);
            }
        });
    }

    public static void createSourceDialog(EOCVSim eocvSim, SourceType type) {
        createSourceDialog(eocvSim, type, null);
    }

    public static void createSourceDialog(EOCVSim eocvSim) {
        invokeLater(() -> new CreateSource(eocvSim.visualizer.frame, eocvSim));
    }

    public static void createConfigDialog(EOCVSim eocvSim) {
        invokeLater(() -> new Configuration(eocvSim.visualizer.frame, eocvSim));
    }

    public static void createAboutDialog(EOCVSim eocvSim) {
        invokeLater(() -> new About(eocvSim.visualizer.frame, eocvSim));
    }

    public static void createOutput(EOCVSim eocvSim, boolean wasManuallyOpened) {
        invokeLater(() -> {
             if(!Output.Companion.isAlreadyOpened())
                new Output(eocvSim.visualizer.frame, eocvSim, Output.Companion.getLatestIndex(), wasManuallyOpened);
        });
    }

    public static void createOutput(EOCVSim eocvSim) {
        createOutput(eocvSim, false);
    }

    public static void createBuildOutput(EOCVSim eocvSim) {
        invokeLater(() -> {
            if(!Output.Companion.isAlreadyOpened())
                new Output(eocvSim.visualizer.frame, eocvSim, 1);
        });
    }

    public static void createPipelineOutput(EOCVSim eocvSim) {
        invokeLater(() -> {
            if(!Output.Companion.isAlreadyOpened())
                new Output(eocvSim.visualizer.frame, eocvSim, 0);
        });
    }

    public static void createSplashScreen(EventHandler closeHandler) {
        invokeLater(() -> new SplashScreen(closeHandler));
    }

    public static FileAlreadyExists.UserChoice createFileAlreadyExistsDialog(EOCVSim eocvSim) {
        return new FileAlreadyExists(eocvSim.visualizer.frame, eocvSim).run();
    }

    private static void invokeLater(Runnable runn) {
        SwingUtilities.invokeLater(runn);
    }

    public static class FileChooser {

        private final JFileChooser chooser;
        private final Component parent;

        private final Mode mode;

        private final ArrayList<FileChooserCloseListener> closeListeners = new ArrayList<>();

        public FileChooser(Component parent, Mode mode, String initialFileName, FileFilter... filters) {

            if (mode == null) mode = Mode.FILE_SELECT;

            chooser = new JFileChooser();

            this.parent = parent;
            this.mode = mode;

            if (mode == Mode.DIRECTORY_SELECT) {
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                // disable the "All files" option.
                chooser.setAcceptAllFileFilterUsed(false);
            }

            chooser.setSelectedFile(new File(chooser.getSelectedFile(), initialFileName));

            if (filters != null) {
                for (FileFilter filter : filters) {
                    if(filter != null) chooser.addChoosableFileFilter(filter);
                }
                if(filters.length > 0) {
                    chooser.setFileFilter(filters[0]);
                }
            }

        }

        protected void init() {

            int returnVal;

            if (mode == Mode.SAVE_FILE_SELECT) {
                returnVal = chooser.showSaveDialog(parent);
            } else {
                returnVal = chooser.showOpenDialog(parent);
            }

            executeCloseListeners(returnVal, chooser.getSelectedFile(), chooser.getFileFilter());

        }

        public void addCloseListener(FileChooserCloseListener listener) {
            this.closeListeners.add(listener);
        }

        private void executeCloseListeners(int OPTION, File selectedFile, FileFilter selectedFileFilter) {
            for (FileChooserCloseListener listener : closeListeners) {
                listener.onClose(OPTION, selectedFile, selectedFileFilter);
            }
        }

        public void close() {
            chooser.setVisible(false);
            executeCloseListeners(JFileChooser.CANCEL_OPTION, new File(""), new FileNameExtensionFilter("", ""));
        }

        public enum Mode {FILE_SELECT, DIRECTORY_SELECT, SAVE_FILE_SELECT}

        public interface FileChooserCloseListener {
            void onClose(int OPTION, File selectedFile, FileFilter selectedFileFilter);
        }

    }

}
