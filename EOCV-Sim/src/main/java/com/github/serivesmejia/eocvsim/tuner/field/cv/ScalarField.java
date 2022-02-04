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
import org.opencv.core.Scalar;
import org.openftc.easyopencv.OpenCvPipeline;

import java.lang.reflect.Field;
import java.util.Arrays;

@RegisterTunableField
public class ScalarField extends TunableField<Scalar> {

    int scalarSize;
    Scalar scalar;

    double[] lastVal = {};

    volatile boolean hasChanged = false;

    public ScalarField(OpenCvPipeline instance, Field reflectionField, EOCVSim eocvSim) throws IllegalAccessException {
        super(instance, reflectionField, eocvSim, AllowMode.ONLY_NUMBERS_DECIMAL);

        if(initialFieldValue == null) {
            scalar = new Scalar(0, 0, 0);
        } else {
            scalar = (Scalar) initialFieldValue;
        }
        scalarSize = scalar.val.length;

        setGuiFieldAmount(scalarSize);
        setRecommendedPanelMode(TunableFieldPanel.Mode.SLIDERS);
    }

    @Override
    public void init() { }

    @Override
    public void update() {
        try {
            scalar = (Scalar) reflectionField.get(pipeline);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        hasChanged = !Arrays.equals(scalar.val, lastVal);

        if (hasChanged) { //update values in GUI if they changed since last check
            updateGuiFieldValues();
        }

        lastVal = scalar.val.clone();
    }

    @Override
    public void updateGuiFieldValues() {
        for (int i = 0; i < scalar.val.length; i++) {
            fieldPanel.setFieldValue(i, scalar.val[i]);
        }
    }

    @Override
    public void setGuiFieldValue(int index, String newValue) throws IllegalAccessException {
        try {
            scalar.val[index] = Double.parseDouble(newValue);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Parameter should be a valid numeric String");
        }

        setPipelineFieldValue(scalar);

        lastVal = scalar.val.clone();
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