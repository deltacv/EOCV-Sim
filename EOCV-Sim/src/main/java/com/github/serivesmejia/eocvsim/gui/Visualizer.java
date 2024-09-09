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

import com.formdev.flatlaf.FlatLaf;
import com.github.serivesmejia.eocvsim.Build;
import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.gui.component.CollapsiblePanelX;
import com.github.serivesmejia.eocvsim.gui.component.visualizer.*;
import com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode.OpModeSelectorPanel;
import com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline.SourceSelectorPanel;
import io.github.deltacv.vision.external.gui.SwingOpenCvViewport;
import com.github.serivesmejia.eocvsim.gui.component.tuner.ColorPicker;
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel;
import com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline.PipelineSelectorPanel;
import com.github.serivesmejia.eocvsim.gui.theme.Theme;
import com.github.serivesmejia.eocvsim.pipeline.compiler.PipelineCompiler;
import com.github.serivesmejia.eocvsim.util.event.EventHandler;
import com.github.serivesmejia.eocvsim.workspace.util.VSCodeLauncher;
import com.github.serivesmejia.eocvsim.workspace.util.template.GradleWorkspaceTemplate;
import kotlin.Unit;
import org.opencv.core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class Visualizer {

    public final EventHandler onInitFinished = new EventHandler("OnVisualizerInitFinish");
    public final EventHandler onPluginGuiAttachment = new EventHandler("OnPLuginGuiAttachment");

    public final ArrayList<AsyncPleaseWaitDialog> pleaseWaitDialogs = new ArrayList<>();

    public final ArrayList<JFrame> childFrames = new ArrayList<>();
    public final ArrayList<JDialog> childDialogs = new ArrayList<>();

    private final EOCVSim eocvSim;
    public JFrame frame;

    public SwingOpenCvViewport viewport = null;

    public TopMenuBar menuBar = null;
    public JPanel tunerMenuPanel;

    public JPanel rightContainer = null;

    public PipelineOpModeSwitchablePanel pipelineOpModeSwitchablePanel = null;

    public PipelineSelectorPanel pipelineSelectorPanel = null;
    public SourceSelectorPanel sourceSelectorPanel = null;

    public OpModeSelectorPanel opModeSelectorPanel = null;

    public TelemetryPanel telemetryPanel;

    public JPanel tunerCollapsible;

    private String title = "EasyOpenCV Simulator v" + Build.standardVersionString;
    private String titleMsg = "No pipeline";
    private String beforeTitle = "";
    private String beforeTitleMsg = "";

    public ColorPicker colorPicker = null;

    private volatile boolean hasFinishedInitializing = false;

    Logger logger = LoggerFactory.getLogger(getClass());

    public Visualizer(EOCVSim eocvSim) {
        this.eocvSim = eocvSim;
    }

    public void init(Theme theme) {
        if(Taskbar.isTaskbarSupported()){
            try {
                //set icon for mac os (and other systems which do support this method)
                Taskbar.getTaskbar().setIconImage(Icons.INSTANCE.getImage("ico_eocvsim").getImage());
            } catch (final UnsupportedOperationException e) {
                logger.warn("Setting the Taskbar icon image is not supported on this platform");
            } catch (final SecurityException e) {
                logger.error("Security exception while setting TaskBar icon", e);
            }
        }

        try {
            theme.install();
        } catch (Exception e) {
            logger.error("Failed to install theme " + theme.name(), e);
        }

        Icons.INSTANCE.setDark(FlatLaf.isLafDark());

        if(Build.isDev) {
            title += "-dev ";
        }

        //instantiate all swing elements after theme installation
        frame = new JFrame();

        String fpsMeterDescriptor = "deltacv EOCV-Sim v" + Build.standardVersionString;
        if(Build.isDev) fpsMeterDescriptor += "-dev";

        onPluginGuiAttachment.run();

        viewport = new SwingOpenCvViewport(new Size(1080, 720), fpsMeterDescriptor);
        viewport.setDark(FlatLaf.isLafDark());

        colorPicker = new ColorPicker(viewport);

        JLayeredPane skiaPanel = viewport.skiaPanel();
        skiaPanel.setLayout(new BorderLayout());

        frame.add(skiaPanel);

        menuBar = new TopMenuBar(this, eocvSim);

        tunerMenuPanel = new JPanel();

        pipelineOpModeSwitchablePanel = new PipelineOpModeSwitchablePanel(eocvSim);
        pipelineOpModeSwitchablePanel.disableSwitching();

        pipelineSelectorPanel = pipelineOpModeSwitchablePanel.getPipelineSelectorPanel();
        sourceSelectorPanel = pipelineOpModeSwitchablePanel.getSourceSelectorPanel();

        opModeSelectorPanel = pipelineOpModeSwitchablePanel.getOpModeSelectorPanel();

        telemetryPanel = new TelemetryPanel();

        rightContainer = new JPanel();

        /*
         * TOP MENU BAR
         */
        
        frame.setJMenuBar(menuBar);

        /*
         * IMG VISUALIZER & SCROLL PANE
         */

        rightContainer.setLayout(new BoxLayout(rightContainer, BoxLayout.Y_AXIS));
        // add pretty border
        rightContainer.setBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, UIManager.getColor("Separator.foreground"))
        );

        pipelineOpModeSwitchablePanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        rightContainer.add(pipelineOpModeSwitchablePanel);

        /*
         * TELEMETRY
         */

        JPanel telemetryWithInsets = new JPanel();
        telemetryWithInsets.setLayout(new BoxLayout(telemetryWithInsets, BoxLayout.LINE_AXIS));
        telemetryWithInsets.setBorder(new EmptyBorder(0, 20, 20, 20));

        telemetryWithInsets.add(telemetryPanel);

        rightContainer.add(telemetryWithInsets);

        //global
        frame.getContentPane().setDropTarget(new InputSourceDropTarget(eocvSim));

        tunerCollapsible = new CollapsiblePanelX("Variable Tuner", null, null);
        tunerCollapsible.setLayout(new BoxLayout(tunerCollapsible, BoxLayout.LINE_AXIS));
        tunerCollapsible.setVisible(false);

        JScrollPane tunerScrollPane = new JScrollPane(tunerMenuPanel);
        tunerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        tunerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        tunerCollapsible.add(tunerScrollPane);

        frame.add(tunerCollapsible, BorderLayout.SOUTH);
        frame.add(rightContainer, BorderLayout.EAST);

        //initialize other various stuff of the frame
        frame.setSize(780, 645);
        frame.setMinimumSize(frame.getSize());
        frame.setTitle("EasyOpenCV Simulator - No Pipeline");

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        frame.setIconImage(Icons.INSTANCE.getImage("ico_eocvsim").getImage());

        frame.setLocationRelativeTo(null);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setVisible(true);

        onInitFinished.run();
        onInitFinished.setCallRightAway(true);

        registerListeners();

        hasFinishedInitializing = true;

        if(!PipelineCompiler.Companion.getIS_USABLE()) {
            compilingUnsupported();
        }
    }

    public void initAsync(Theme simTheme) {
        SwingUtilities.invokeLater(() -> init(simTheme));
    }

    private void registerListeners() {

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                eocvSim.onMainUpdate.doOnce((Runnable) eocvSim::destroy);
            }
        });

        //handling onViewportTapped evts
        viewport.getComponent().addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(!colorPicker.isPicking())
                    eocvSim.pipelineManager.callViewportTapped();
            }
        });

        //VIEWPORT RESIZE HANDLING
        // imgScrollPane.addMouseWheelListener(e -> {
        //    if (isCtrlPressed) { //check if control key is pressed
                // double scale = viewport.getViewportScale() - (0.15 * e.getPreciseWheelRotation());
                // viewport.setViewportScale(scale);
        //    }
        // });

        // stop color-picking mode when changing pipeline
        // TODO: find out why this breaks everything?????
        eocvSim.pipelineManager.onPipelineChange.doPersistent(() -> colorPicker.stopPicking());
    }

    public boolean hasFinishedInit() { return hasFinishedInitializing; }

    public void joinInit() {
        while (!hasFinishedInitializing) {
            Thread.yield();
        }
    }

    public void close() {
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(false);
            viewport.deactivate();

            //close all asyncpleasewait dialogs
            for (AsyncPleaseWaitDialog dialog : pleaseWaitDialogs) {
                if (dialog != null) {
                    dialog.destroyDialog();
                }
            }

            pleaseWaitDialogs.clear();

            //close all opened frames
            for (JFrame frame : childFrames) {
                if (frame != null && frame.isVisible()) {
                    frame.setVisible(false);
                    frame.dispose();
                }
            }

            childFrames.clear();

            //close all opened dialogs
            for (JDialog dialog : childDialogs) {
                if (dialog != null && dialog.isVisible()) {
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            }

            childDialogs.clear();
            frame.dispose();
            viewport.deactivate();
        });
    }

    private void setFrameTitle(String title, String titleMsg) {
        frame.setTitle(title + " - " + titleMsg);
    }

    public void setTitle(String title) {
        this.title = title;
        if (!beforeTitle.equals(title)) setFrameTitle(title, titleMsg);
        beforeTitle = title;
    }

    public void setTitleMessage(String titleMsg) {
        this.titleMsg = titleMsg;
        if (!beforeTitleMsg.equals(title)) setFrameTitle(title, titleMsg);
        beforeTitleMsg = titleMsg;
    }

    public void updateTunerFields(List<TunableFieldPanel> fields) {
        tunerMenuPanel.removeAll();
        tunerMenuPanel.repaint();

        for (TunableFieldPanel fieldPanel : fields) {
            tunerMenuPanel.add(fieldPanel);
            fieldPanel.showFieldPanel();
        }

        tunerCollapsible.setVisible(!fields.isEmpty());
    }

    public void asyncCompilePipelines() {
        if(PipelineCompiler.Companion.getIS_USABLE()) {
            menuBar.workspCompile.setEnabled(false);
            pipelineSelectorPanel.getButtonsPanel().getPipelineCompileBtt().setEnabled(false);

            eocvSim.pipelineManager.compiledPipelineManager.asyncCompile(true, (result) -> {
                menuBar.workspCompile.setEnabled(true);
                pipelineSelectorPanel.getButtonsPanel().getPipelineCompileBtt().setEnabled(true);

                return Unit.INSTANCE;
            });
        } else {
            compilingUnsupported();
        }
    }

    public void compilingUnsupported() {
        asyncPleaseWaitDialog(
                "Runtime pipeline builds are not supported on this JVM",
                "For further info, check the EOCV-Sim GitHub repo",
                "Close",
                new Dimension(320, 160),
                true, true
        );
    }

    public void selectPipelinesWorkspace() {
        DialogFactory.createFileChooser(
                frame, DialogFactory.FileChooser.Mode.DIRECTORY_SELECT
        ).addCloseListener((OPTION, selectedFile, selectedFileFilter) -> {
            if (OPTION == JFileChooser.APPROVE_OPTION) {
                if(!selectedFile.exists()) selectedFile.mkdir();

                eocvSim.onMainUpdate.doOnce(() ->
                        eocvSim.workspaceManager.setWorkspaceFile(selectedFile)
                );
            }
        });
    }

    public void createVSCodeWorkspace() {
        DialogFactory.createFileChooser(frame, DialogFactory.FileChooser.Mode.DIRECTORY_SELECT)
        .addCloseListener((OPTION, selectedFile, selectedFileFilter) -> {
            if(OPTION == JFileChooser.APPROVE_OPTION) {
                if(!selectedFile.exists()) selectedFile.mkdir();

                if(selectedFile.isDirectory() && selectedFile.listFiles().length == 0) {
                    eocvSim.workspaceManager.createWorkspaceWithTemplateAsync(
                            selectedFile, GradleWorkspaceTemplate.INSTANCE,

                            () -> {
                                askOpenVSCode();
                                return Unit.INSTANCE; // weird kotlin interop
                            }
                    );
                } else {
                    asyncPleaseWaitDialog(
                            "The selected directory must be empty", "Select an empty directory or create a new one",
                            "Retry", new Dimension(320, 160), true, true
                    ).onCancel(this::createVSCodeWorkspace);
                }
            }
        });
    }

    public void askOpenVSCode() {
        DialogFactory.createYesOrNo(frame, "A new workspace was created. Do you want to open VS Code?", "",
            (result) -> {
                if(result == 0) {
                    VSCodeLauncher.INSTANCE.asyncLaunch(eocvSim.workspaceManager.getWorkspaceFile());
                }
            }
        );
    }

    // PLEASE WAIT DIALOGS

    public boolean pleaseWaitDialog(JDialog diag, String message, String subMessage, String cancelBttText, Dimension size, boolean cancellable, AsyncPleaseWaitDialog apwd, boolean isError) {
        final JDialog dialog = diag == null ? new JDialog(this.frame) : diag;

        boolean addSubMessage = subMessage != null;

        int rows = 3;
        if (!addSubMessage) {
            rows--;
        }

        dialog.setModal(true);
        dialog.setLayout(new GridLayout(rows, 1));

        if (isError) {
            dialog.setTitle("Operation failed");
        } else {
            dialog.setTitle("Operation in progress");
        }

        JLabel msg = new JLabel(message);
        msg.setHorizontalAlignment(JLabel.CENTER);
        msg.setVerticalAlignment(JLabel.CENTER);

        dialog.add(msg);

        JLabel subMsg = null;
        if (addSubMessage) {

            subMsg = new JLabel(subMessage);
            subMsg.setHorizontalAlignment(JLabel.CENTER);
            subMsg.setVerticalAlignment(JLabel.CENTER);

            dialog.add(subMsg);

        }

        JPanel exitBttPanel = new JPanel(new FlowLayout());
        JButton cancelBtt = new JButton(cancelBttText);

        cancelBtt.setEnabled(cancellable);

        exitBttPanel.add(cancelBtt);

        boolean[] cancelled = {false};

        cancelBtt.addActionListener(e -> {
            cancelled[0] = true;
            dialog.setVisible(false);
            dialog.dispose();
        });

        dialog.add(exitBttPanel);

        if (apwd != null) {
            apwd.msg = msg;
            apwd.subMsg = subMsg;
            apwd.cancelBtt = cancelBtt;
        }
        
        if(size == null) size = new Dimension(400, 200);
        dialog.setSize(size);

        dialog.setLocationRelativeTo(null);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        dialog.setVisible(true);

        return cancelled[0];
    }

    public void pleaseWaitDialog(JDialog dialog, String message, String subMessage, String cancelBttText, Dimension size, boolean cancellable) {
        pleaseWaitDialog(dialog, message, subMessage, cancelBttText, size, cancellable, null, false);
    }

    public void pleaseWaitDialog(String message, String subMessage, String cancelBttText, Dimension size, boolean cancellable) {
        pleaseWaitDialog(null, message, subMessage, cancelBttText, size, cancellable, null, false);
    }

    public AsyncPleaseWaitDialog asyncPleaseWaitDialog(String message, String subMessage, String cancelBttText, Dimension size, boolean cancellable, boolean isError) {
        AsyncPleaseWaitDialog rPWD = new AsyncPleaseWaitDialog(message, subMessage, cancelBttText, size, cancellable, isError, eocvSim);
        SwingUtilities.invokeLater(rPWD);

        return rPWD;
    }

    public AsyncPleaseWaitDialog asyncPleaseWaitDialog(String message, String subMessage, String cancelBttText, Dimension size, boolean cancellable) {
        AsyncPleaseWaitDialog rPWD = new AsyncPleaseWaitDialog(message, subMessage, cancelBttText, size, cancellable, false, eocvSim);
        SwingUtilities.invokeLater(rPWD);

        return rPWD;
    }

    public class AsyncPleaseWaitDialog implements Runnable {

        public volatile JDialog dialog = new JDialog(frame);

        public volatile JLabel msg = null;
        public volatile JLabel subMsg = null;

        public volatile JButton cancelBtt = null;

        public volatile boolean wasCancelled = false;
        public volatile boolean isError;

        public volatile String initialMessage;
        public volatile String initialSubMessage;

        public volatile boolean isDestroyed = false;

        String message;
        String subMessage;
        String cancelBttText;

        Dimension size;

        boolean cancellable;

        private final ArrayList<Runnable> onCancelRunnables = new ArrayList<>();

        public AsyncPleaseWaitDialog(String message, String subMessage, String cancelBttText, Dimension size, boolean cancellable, boolean isError, EOCVSim eocvSim) {
            this.message = message;
            this.subMessage = subMessage;
            this.initialMessage = message;
            this.initialSubMessage = subMessage;
            this.cancelBttText = cancelBttText;

            this.size = size;
            this.cancellable = cancellable;

            this.isError = isError;

            eocvSim.visualizer.pleaseWaitDialogs.add(this);
        }

        public void onCancel(Runnable runn) {
            onCancelRunnables.add(runn);
        }

        @Override
        public void run() {
            wasCancelled = pleaseWaitDialog(dialog, message, subMessage, cancelBttText, size, cancellable, this, isError);

            if (wasCancelled) {
                for (Runnable runn : onCancelRunnables) {
                    runn.run();
                }
            }
        }

        public void destroyDialog() {
            if (!isDestroyed) {
                dialog.setVisible(false);
                dialog.dispose();
                isDestroyed = true;
            }
        }

    }

}
