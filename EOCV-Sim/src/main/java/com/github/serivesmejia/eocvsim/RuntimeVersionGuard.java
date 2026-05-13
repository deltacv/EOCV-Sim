package com.github.serivesmejia.eocvsim;

import javax.swing.JOptionPane;
import java.awt.GraphicsEnvironment;
import java.util.Locale;

public final class RuntimeVersionGuard {

    private static final int REQUIRED_JAVA_VERSION = 25;

    private RuntimeVersionGuard() {
    }

    public static void ensureJavaOrExit() {
        int currentVersion = getJavaFeatureVersion();

        if (currentVersion == REQUIRED_JAVA_VERSION) {
            return;
        }

        String message = "EOCV-Sim requires Java " + REQUIRED_JAVA_VERSION +
                " to run.\n\nDetected Java version: " + getDisplayJavaVersion() +
                "\n\nPlease install Java " + REQUIRED_JAVA_VERSION + " and start EOCV-Sim again.";

        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(message);
        } else {
            JOptionPane.showMessageDialog(
                    null,
                    message,
                    "Unsupported Java version",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        System.exit(1);
    }

    private static int getJavaFeatureVersion() {
        String specVersion = System.getProperty("java.specification.version", "0");
        return parseFeatureVersion(specVersion);
    }

    private static int parseFeatureVersion(String version) {
        if (version == null) {
            return 0;
        }

        version = version.trim();
        if (version.isEmpty()) {
            return 0;
        }

        if (version.startsWith("1.")) {
            version = version.substring(2);
        }

        int dot = version.indexOf('.');
        int dash = version.indexOf('-');
        int end = version.length();

        if (dot >= 0) {
            end = dot;
        }
        if (dash >= 0 && dash < end) {
            end = dash;
        }

        try {
            return Integer.parseInt(version.substring(0, end).toLowerCase(Locale.ROOT));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String getDisplayJavaVersion() {
        return System.getProperty("java.version", "unknown");
    }
}


