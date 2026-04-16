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

package com.github.serivesmejia.eocvsim.gui.component.tuner;

import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.gui.component.tuner.element.TunableComboBox;
import com.github.serivesmejia.eocvsim.gui.component.tuner.element.TunableSlider;
import com.github.serivesmejia.eocvsim.gui.component.tuner.element.TunableTextField;
import com.github.serivesmejia.eocvsim.tuner.*;

import javax.swing.*;
import javax.swing.border.SoftBevelBorder;
import java.awt.*;
import java.util.List;

@SuppressWarnings("unchecked")
public class TunableFieldPanel extends JPanel {

    public final TunableField tunableField;

    public TunableTextField[] fields;
    public JPanel fieldsPanel;

    public TunableSlider[] sliders;
    public JPanel slidersPanel;

    public JComboBox[] comboBoxes;

    public TunableFieldPanelOptions panelOptions = null;
    private final EOCVSim eocvSim;

    private Mode mode;
    private boolean reevalConfigRequested = false;

    private boolean hasBeenShown = false;

    public enum Mode { TEXTBOXES, SLIDERS }

    public TunableFieldPanel(TunableField tunableField, EOCVSim eocvSim) {
        super();

        this.tunableField = tunableField;
        this.eocvSim = eocvSim;

        tunableField.setTunableFieldPanel(this);

        init();
    }

    private void init() {
        setBorder(new SoftBevelBorder(SoftBevelBorder.RAISED));

        panelOptions = new TunableFieldPanelOptions(this, eocvSim);

        List<TunableValue<?>> values = tunableField.getTunableValues();
        
        boolean hasFieldsOrSliders = false;
        for (TunableValue<?> val : values) {
            if (val instanceof TunableNumber || val instanceof TunableString) {
                hasFieldsOrSliders = true;
                break;
            }
        }

        if(hasFieldsOrSliders) {
            add(panelOptions);
        }

        JLabel fieldNameLabel = new JLabel();
        fieldNameLabel.setText(tunableField.getFieldName());

        add(fieldNameLabel);

        fields = new TunableTextField[values.size()];
        sliders = new TunableSlider[values.size()];
        comboBoxes = new JComboBox[values.size()];

        fieldsPanel = new JPanel();
        slidersPanel = new JPanel(new GridBagLayout());
        
        for (int i = 0 ; i < values.size() ; i++) {
            TunableValue<?> value = values.get(i);
            
            if (value instanceof TunableNumber || value instanceof TunableString) {
                TunableTextField field = new TunableTextField(value, eocvSim);
                fields[i] = field;
                field.setEditable(true);
                fieldsPanel.add(field);
                
                if (value instanceof TunableNumber) {
                    JLabel sliderLabel = new JLabel("0");
                    TunableSlider slider = new TunableSlider((TunableNumber) value, eocvSim, sliderLabel);
                    sliders[i] = slider;

                    GridBagConstraints cSlider = new GridBagConstraints();
                    cSlider.gridx = 0;
                    cSlider.gridy = i;

                    GridBagConstraints cLabel = new GridBagConstraints();
                    cLabel.gridx = 1;
                    cLabel.gridy = i;

                    slidersPanel.add(slider, cSlider);
                    slidersPanel.add(sliderLabel, cLabel);
                }
            } else if (value instanceof TunableEnum) {
                TunableComboBox comboBox = new TunableComboBox((TunableEnum<?>) value, eocvSim);
                add(comboBox);
                comboBoxes[i] = comboBox;
            }
        }

        setMode(Mode.TEXTBOXES);
    }

    public void showFieldPanel() {
        if(hasBeenShown) return;
        hasBeenShown = true;

        panelOptions.getConfigPanel().updateFieldGuiFromConfig();
        if (!tunableField.isOnlyNumbers()) {
            setMode(Mode.SLIDERS);
        }
    }

    public void setFieldValue(int index, Object value) {
        if(index >= fields.length) return;
        if(fields[index] == null) return;
        
        if(fields[index].isFocusOwner()) return;
        if(sliders[index] != null && sliders[index].isFocusOwner()) return;

        String text;
        TunableValue<?> tv = (TunableValue<?>) tunableField.getTunableValues().get(index);
        
        if(tv instanceof TunableNumber && ((TunableNumber)tv).isOnlyNumbers()) {
            text = String.valueOf((int) Math.round(Double.parseDouble(value.toString())));
        } else {
            text = value.toString();
        }

        fields[index].setText(text);

        try {
            if (sliders[index] != null) sliders[index].setScaledValue(Double.parseDouble(value.toString()));
        } catch(NumberFormatException ignored) {}
    }

    public void setComboBoxSelection(int index, Object selection) {
        if (comboBoxes[index] != null) {
            comboBoxes[index].setSelectedItem(selection.toString());
        }
    }

    protected void requestAllConfigReeval() {
        reevalConfigRequested = true;
    }

    public void setMode(Mode mode) {
        switch(mode) {
            case TEXTBOXES:
                if(this.mode == Mode.SLIDERS) {
                    remove(slidersPanel);
                }

                for(int i = 0 ; i < fields.length ; i++) {
                    if (fields[i] != null) fields[i].setInControl(true);
                    if (sliders[i] != null) sliders[i].setInControl(false);
                    if (fields[i] != null) setFieldValue(i, ((TunableValue<?>) tunableField.getTunableValues().get(i)).getValue());
                }

                add(fieldsPanel);
                break;
            case SLIDERS:
                if(this.mode == Mode.TEXTBOXES) {
                    remove(fieldsPanel);
                }

                for(int i = 0 ; i < fields.length ; i++) {
                    if (fields[i] != null) fields[i].setInControl(false);
                    if (sliders[i] != null) sliders[i].setInControl(true);
                    if (fields[i] != null) setFieldValue(i, ((TunableValue<?>) tunableField.getTunableValues().get(i)).getValue());
                }

                add(slidersPanel);
                break;
        }

        this.mode = mode;

        if(panelOptions.getMode() != mode) {
            panelOptions.setMode(mode);
        }

        revalidate(); repaint();
    }

    public void setSlidersRange(double min, double max) {
        if(sliders == null) return;
        for(TunableSlider slider : sliders) {
            if (slider != null) slider.setScaledBounds(min, max);
        }
    }

    public Mode getMode() { return this.mode; }

    public boolean hasRequestedAllConfigReeval() {
        boolean current = reevalConfigRequested;
        reevalConfigRequested = false;

        return current;
    }

}