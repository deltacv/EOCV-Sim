package com.github.serivesmejia.eocvsim;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Bootstrap {

    public static void main(String[] args) throws Exception {
        log("======================================");
        log("Starting EOCV-Sim Bootstrap");
        log("======================================");

        log("Runtime:");
        log("  java.version = " + System.getProperty("java.version"));
        log("  os.name      = " + System.getProperty("os.name"));
        log("  java.home    = " + System.getProperty("java.home"));
        log("  JAVA_HOME    = " + System.getenv("JAVA_HOME"));

        if (Boolean.getBoolean("eocvsim.bypass.bootstrap")) {
            log("Bootstrap bypass enabled");
            launchApp(args);
            return;
        }

        int current = getJavaMajorVersion(System.getProperty("java.version"));
        log("Detected runtime Java major: " + current);

        if (current >= 25) {
            log("Java 25+ detected → launching directly");
            launchApp(args);
            return;
        }

        log("Java 25+ NOT detected");

        File detected = findJava25();

        if (detected != null) {
            log("Autodetected Java 25+: " + detected.getAbsolutePath());
        } else {
            log("No Java 25+ installations detected");
        }

        File chosen = promptUser(detected, current);

        if (chosen == null) {
            log("User exited bootstrap");
            System.exit(0);
            return;
        }

        log("User selected: " + chosen.getAbsolutePath());

        relaunch(chosen, args);
    }

    // ---------------- UI ----------------

    private static File promptUser(File detected, int currentJava) {

        boolean hasDetected = detected != null;

        log("Opening selection UI (detected=" + hasDetected + ")");

        final File[] result = new File[1];
        final Object lock = new Object();

        JFrame dialog = new JFrame("EOCV-Sim: Java 25 Required");

        try {
            URL icon = Bootstrap.class.getResource("/images/icon/ico_eocvsim_new_128.png");
            if (icon != null) {
                dialog.setIconImage(new ImageIcon(icon).getImage());
            }
        } catch (Exception ignored) { }
        
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel();
        root.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

        // ---------------- TITLE ----------------
        JLabel title = new JLabel("EOCV-Sim requires Java 25 or newer");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        // ---------------- INFO TEXT ----------------
        JTextArea info = new JTextArea();
        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setOpaque(false);
        info.setFocusable(false);

        StringBuilder text = new StringBuilder();
        text.append("EOCV-Sim was started with Java ")
                .append(currentJava)
                .append(", but requires Java 25 or newer to run.\n\n");
        if (!hasDetected) {
            text.append("No compatible Java installation was found automatically.\n\n");
        }
        text.append("Select a Java 25+ installation or continue with the detected one if it is correct.");
        info.setText(text.toString());
        info.setMaximumSize(new Dimension(420, 120));

        // ---------------- DETECTED AREAS ----------------
        JTextArea detectedLabel = null;
        JTextArea detectedPath = null;

        if (hasDetected) {
            detectedLabel = new JTextArea("Autodetected installation:");
            detectedLabel.setEditable(false);
            detectedLabel.setOpaque(false);
            detectedLabel.setFocusable(false);
            detectedLabel.setFont(info.getFont().deriveFont(Font.BOLD));

            detectedPath = new JTextArea(detected.getAbsolutePath());
            detectedPath.setEditable(false);
            detectedPath.setOpaque(false);
            detectedPath.setFocusable(false);
            detectedPath.setFont(info.getFont().deriveFont(Font.BOLD));
            detectedPath.setForeground(new Color(0, 120, 215));
        }

        // ---------------- BUTTONS ----------------
        JButton continueBtn = new JButton("Continue with detected");
        JButton selectBtn = new JButton("Select from disk");
        JButton exitBtn = new JButton("Exit");

        continueBtn.setEnabled(hasDetected);

        continueBtn.setPreferredSize(new Dimension(180, 30));
        selectBtn.setPreferredSize(new Dimension(160, 30));
        exitBtn.setPreferredSize(new Dimension(360, 30));

        // ---------------- ACTIONS ----------------
        continueBtn.addActionListener(e -> {
            log("User: continue with detected");
            result[0] = detected;
            dialog.dispose();
            synchronized (lock) { lock.notifyAll(); }
        });

        selectBtn.addActionListener(e -> {
            log("User: browse disk");

            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                log("Selected: " + selected.getAbsolutePath());

                if (isJava25OrNewer(selected)) {
                    log("Valid Java 25+ selected");
                    result[0] = selected;
                    dialog.dispose();
                    synchronized (lock) { lock.notifyAll(); }
                } else {
                    log("Invalid Java selected");
                    JOptionPane.showMessageDialog(
                            dialog,
                            "This directory is not a valid Java 25 or newer installation.",
                            "Invalid Java",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        exitBtn.addActionListener(e -> {
            log("User exit");
            result[0] = null;
            dialog.dispose();
            synchronized (lock) { lock.notifyAll(); }
        });

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                synchronized (lock) { lock.notifyAll(); }
            }
        });

        // ---------------- LAYOUT ----------------
        root.add(title);
        root.add(Box.createVerticalStrut(10));
        root.add(info);

        if (hasDetected) {
            root.add(Box.createVerticalStrut(8));
            root.add(detectedLabel);
            root.add(Box.createVerticalStrut(2));
            root.add(detectedPath);
        }

        root.add(Box.createVerticalStrut(12));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        row1.add(continueBtn);
        row1.add(selectBtn);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        row2.add(exitBtn);

        JPanel buttonBlock = new JPanel();
        buttonBlock.setLayout(new BoxLayout(buttonBlock, BoxLayout.Y_AXIS));
        buttonBlock.add(row1);
        buttonBlock.add(row2);

        root.add(buttonBlock);

        dialog.setContentPane(root);

        dialog.setMinimumSize(new Dimension(460, 260));
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setResizable(false);

        dialog.setVisible(true);

        synchronized (lock) {
            try { lock.wait(); } catch (InterruptedException ignored) {}
        }

        return result[0];
    }

    // ---------------- LAUNCH ----------------

    private static void launchApp(String[] args) throws Exception {
        log("Launching Main via reflection");

        Class.forName("com.github.serivesmejia.eocvsim.Main")
                .getMethod("main", String[].class)
                .invoke(null, (Object) args);
    }

    private static void relaunch(File javaHome, String[] args) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();

        String javaBin = javaHome.getAbsolutePath()
                + File.separator + "bin"
                + File.separator + "java";

        if (os.contains("win")) javaBin += ".exe";

        File jar = new File(
                Bootstrap.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .getPath()
        );

        log("Relaunching:");
        log("  Java: " + javaBin);
        log("  Jar : " + jar.getAbsolutePath());

        List<String> cmd = new ArrayList<>();
        cmd.add(javaBin);
        cmd.add("-cp");
        cmd.add(jar.getAbsolutePath());
        cmd.add("com.github.serivesmejia.eocvsim.Main");

        Collections.addAll(cmd, args);

        new ProcessBuilder(cmd).inheritIO().start();
        System.exit(0);
    }

    // ---------------- DETECTION ----------------

    private static File findJava25() {
        File jdks = new File(System.getProperty("user.home"), ".jdks");
        log("Scanning: " + jdks.getAbsolutePath());
        return scan(jdks);
    }

    private static File scan(File root) {
        if (root == null || !root.exists()) return null;

        File[] files = root.listFiles();
        if (files == null) return null;

        for (File f : files) {
            log("Checking: " + f.getName());
            if (isJava25OrNewer(f)) {
                log("Matched Java 25+: " + f.getAbsolutePath());
                return f;
            }
        }

        return null;
    }

    // ---------------- VERSION ----------------

    private static int getJavaMajorVersion(String version) {
        try {
            if (version.startsWith("1.")) return Integer.parseInt(version.split("\\.")[1]);
            return Integer.parseInt(version.split("\\.")[0]);
        } catch (Exception e) {
            return -1;
        }
    }

    private static int getJavaMajorVersion(File javaHome) {
        File bin = new File(javaHome, "bin/java");
        if (!bin.exists()) bin = new File(javaHome, "bin/java.exe");
        if (!bin.exists()) return -1;

        try {
            Process p = new ProcessBuilder(bin.getAbsolutePath(), "-version")
                    .redirectErrorStream(true)
                    .start();

            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = r.readLine();

            int q1 = line.indexOf('"');
            int q2 = line.indexOf('"', q1 + 1);

            String v = line.substring(q1 + 1, q2);

            if (v.startsWith("1.")) return Integer.parseInt(v.split("\\.")[1]);
            return Integer.parseInt(v.split("\\.")[0]);

        } catch (Exception e) {
            log("Failed reading version: " + javaHome);
            return -1;
        }
    }

    private static boolean isJava25OrNewer(File home) {
        return getJavaMajorVersion(home) >= 25;
    }

    // ---------------- LOG ----------------

    private static void log(String msg) {
        System.out.println("[EOCV-SIM BOOTSTRAP] " + msg);
    }
}