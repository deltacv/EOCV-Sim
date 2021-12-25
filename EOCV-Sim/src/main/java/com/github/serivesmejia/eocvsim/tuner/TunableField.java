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

package com.github.serivesmejia.eocvsim.tuner;

import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel;
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanelConfig;
import com.github.serivesmejia.eocvsim.util.event.EventHandler;
import org.openftc.easyopencv.OpenCvPipeline;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class TunableField<T> {

    protected Field reflectionField;
    protected TunableFieldPanel fieldPanel;

    protected OpenCvPipeline pipeline;
    protected AllowMode allowMode;
    protected EOCVSim eocvSim;

    protected Object initialFieldValue;

    private int guiFieldAmount = 1;
    private int guiComboBoxAmount = 0;

    public final EventHandler onValueChange = new EventHandler("TunableField-ValueChange");

    private TunableFieldPanel.Mode recommendedMode = null;

    public TunableField(OpenCvPipeline instance, Field reflectionField, EOCVSim eocvSim, AllowMode allowMode) throws IllegalAccessException {
        this.reflectionField = reflectionField;
        this.pipeline = instance;
        this.allowMode = allowMode;
        this.eocvSim = eocvSim;

        initialFieldValue = reflectionField.get(instance);
    }

    public TunableField(OpenCvPipeline instance, Field reflectionField, EOCVSim eocvSim) throws IllegalAccessException {
        this(instance, reflectionField, eocvSim, AllowMode.TEXT);
    }

    public abstract void init();

    public abstract void update();

    public abstract void updateGuiFieldValues();

    public void setPipelineFieldValue(T newValue) throws IllegalAccessException {
        if (hasChanged()) { //execute if value is not the same to save resources
            reflectionField.set(pipeline, newValue);
            onValueChange.run();
        }
    }

    public abstract void setGuiFieldValue(int index, String newValue) throws IllegalAccessException;

    public void setGuiComboBoxValue(int index, String newValue) throws IllegalAccessException { }

    public final void setTunableFieldPanel(TunableFieldPanel fieldPanel) {
        this.fieldPanel = fieldPanel;
    }

    protected final void setRecommendedPanelMode(TunableFieldPanel.Mode mode) {
        recommendedMode = mode;
    }

    public final void evalRecommendedPanelMode() {
        TunableFieldPanelConfig configPanel = fieldPanel.panelOptions.getConfigPanel();
        TunableFieldPanelConfig.ConfigSource configSource = configPanel.getLocalConfig().getSource();
        //only apply the recommendation if user hasn't
        //configured a global or specific field config
        if(recommendedMode != null && fieldPanel != null && configSource == TunableFieldPanelConfig.ConfigSource.GLOBAL_DEFAULT) {
            fieldPanel.setMode(recommendedMode);
        }
    }

    public abstract T getValue();

    public abstract Object getGuiFieldValue(int index);

    public Object[] getGuiComboBoxValues(int index) {
        return new Object[0];
    }

    public final int getGuiFieldAmount() {
        return guiFieldAmount;
    }

    public final void setGuiFieldAmount(int amount) {
        this.guiFieldAmount = amount;
    }

    public final int getGuiComboBoxAmount() {
        return guiComboBoxAmount;
    }

    public final void setGuiComboBoxAmount(int amount) {
        this.guiComboBoxAmount = amount;
    }

    public final String getFieldName() {
        return reflectionField.getName();
    }

    public final AllowMode getAllowMode() {
        return allowMode;
    }

    public final boolean isOnlyNumbers() {
        return getAllowMode() == TunableField.AllowMode.ONLY_NUMBERS ||
                getAllowMode() == TunableField.AllowMode.ONLY_NUMBERS_DECIMAL;
    }

    public abstract boolean hasChanged();

    public final EOCVSim getEOCVSim() {
        return eocvSim;
    }

    public enum AllowMode {ONLY_NUMBERS, ONLY_NUMBERS_DECIMAL, TEXT}

}
