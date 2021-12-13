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

package com.github.serivesmejia.eocvsim.tuner.field;

import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel;
import com.github.serivesmejia.eocvsim.tuner.TunableField;
import com.github.serivesmejia.eocvsim.tuner.scanner.RegisterTunableField;
import org.openftc.easyopencv.OpenCvPipeline;

import java.lang.reflect.Field;

@RegisterTunableField
public class StringField extends TunableField<String> {

    String value;

    String lastVal = "";

    volatile boolean hasChanged = false;

    public StringField(OpenCvPipeline instance, Field reflectionField, EOCVSim eocvSim) throws IllegalAccessException {
        super(instance, reflectionField, eocvSim, AllowMode.TEXT);

        if(initialFieldValue != null) {
            value = (String) initialFieldValue;
        } else {
            value = "";
        }
    }

    @Override
    public void init() {
        setRecommendedPanelMode(TunableFieldPanel.Mode.TEXTBOXES);
    }

    @Override
    public void update() {
        hasChanged = !value.equals(lastVal);

        if (hasChanged) { //update values in GUI if they changed since last check
            updateGuiFieldValues();
        }

        lastVal = value;
    }

    @Override
    public void updateGuiFieldValues() {
        fieldPanel.setFieldValue(0, value);
    }

    @Override
    public void setGuiFieldValue(int index, String newValue) throws IllegalAccessException {

        value = newValue;

        setPipelineFieldValue(value);

        lastVal = value;

    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public Object getGuiFieldValue(int index) {
        return value;
    }

    @Override
    public boolean hasChanged() {
        hasChanged = !value.equals(lastVal);
        return hasChanged;
    }

}
