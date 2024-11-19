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

package com.github.serivesmejia.eocvsim.tuner.field.cv;

import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel;
import com.github.serivesmejia.eocvsim.tuner.TunableField;
import com.github.serivesmejia.eocvsim.tuner.scanner.RegisterTunableField;
import io.github.deltacv.eocvsim.virtualreflect.VirtualField;
import org.opencv.core.Scalar;
import org.openftc.easyopencv.OpenCvPipeline;

import javax.swing.*;
import java.util.Arrays;

@RegisterTunableField
public class ScalarField extends TunableField<Scalar> {

    int scalarSize;
    Scalar scalar;

    double[] lastVal = {0, 0, 0, 0};

    volatile boolean hasChanged = false;

    public ScalarField(Object instance, VirtualField reflectionField, EOCVSim eocvSim) throws IllegalAccessException {
        super(instance, reflectionField, eocvSim, AllowMode.ONLY_NUMBERS_DECIMAL);

        if(initialFieldValue == null) {
            scalar = new Scalar(0, 0, 0);
        } else {
            scalar = ((Scalar) initialFieldValue).clone();
        }

        scalarSize = scalar.val.length;

        setGuiFieldAmount(4);
        setRecommendedPanelMode(TunableFieldPanel.Mode.SLIDERS);
    }

    @Override
    public void init() {
        reflectionField.set(scalar);
    }

    @Override
    public void update() {
        scalar = (Scalar) reflectionField.get();

        hasChanged = !Arrays.equals(scalar.val, lastVal);

        if (hasChanged) { //update values in GUI if they changed since last check
            updateGuiFieldValues();
        }

        lastVal[0] = scalar.val[0];
        lastVal[1] = scalar.val[1];
        lastVal[2] = scalar.val[2];
        lastVal[3] = scalar.val[3];
    }

    @Override
    public void updateGuiFieldValues() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < scalar.val.length; i++) {
                fieldPanel.setFieldValue(i, scalar.val[i]);
            }
        });
    }

    @Override
    public void setFieldValue(int index, Object newValue) throws IllegalAccessException {
        try {
            double value;

            if(newValue instanceof String) {
                value = Double.parseDouble((String) newValue);
            } else {
                value = (double)newValue;
            }

            scalar.val[index] = value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Parameter should be a valid number", ex);
        }

        setPipelineFieldValue(scalar);

        lastVal[0] = scalar.val[0];
        lastVal[1] = scalar.val[1];
        lastVal[2] = scalar.val[2];
        lastVal[3] = scalar.val[3];
    }

    @Override
    public void setFieldValueFromGui(int index, String newValue) throws IllegalAccessException {
        setFieldValue(index, newValue);
    }

    @Override
    public Scalar getValue() {
        return scalar;
    }

    @Override
    public Object getGuiFieldValue(int index) {
        return scalar.val[index];
    }

    @Override
    public boolean hasChanged() {
        hasChanged = !Arrays.equals(scalar.val, lastVal);
        return hasChanged;
    }

}