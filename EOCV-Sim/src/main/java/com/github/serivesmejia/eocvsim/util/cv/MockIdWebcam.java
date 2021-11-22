package com.github.serivesmejia.eocvsim.util.cv;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDevice;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MockIdWebcam extends Webcam {

    public MockIdWebcam(int id) {
        super(new MockIdWebcamDevice(id));
    }

    private static class MockIdWebcamDevice implements WebcamDevice {

        private int id;

        private MockIdWebcamDevice(int id) {
            this.id = id;
        }

        @Override
        public String getName() {
            return "Webcam " + id;
        }

        @Override
        public Dimension[] getResolutions() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Dimension getResolution() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setResolution(Dimension size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BufferedImage getImage() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void open() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void dispose() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isOpen() {
            return false;
        }
    }

}
