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
import com.github.serivesmejia.eocvsim.tuner.TunableField;

import javax.swing.*;
import javax.swing.border.SoftBevelBorder;
import java.awt.*;

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
        //nice look
        setBorder(new SoftBevelBorder(SoftBevelBorder.RAISED));

        panelOptions = new TunableFieldPanelOptions(this, eocvSim);

        if(tunableField.getGuiFieldAmount() > 0) {
            add(panelOptions);
        }

        JLabel fieldNameLabel = new JLabel();
        fieldNameLabel.setText(tunableField.getFieldName());

        add(fieldNameLabel);

        int fieldAmount = tunableField.getGuiFieldAmount();

        fields = new TunableTextField[fieldAmount];
        sliders = new TunableSlider[fieldAmount];

        fieldsPanel = new JPanel();
        slidersPanel = new JPanel(new GridBagLayout());

        for (int i = 0 ; i < tunableField.getGuiFieldAmount() ; i++) {
            //add the tunable field as a field
            TunableTextField field = new TunableTextField(i, tunableField, eocvSim);
            fields[i] = field;

            field.setEditable(true);
            fieldsPanel.add(field);

            //add the tunable field as a slider
            JLabel sliderLabel = new JLabel("0");
            TunableSlider slider = new TunableSlider(i, tunableField, eocvSim, sliderLabel);
            sliders[i] = slider;

            GridBagConstraints cSlider = new GridBagConstraints();
            cSlider.gridx = 0;
            cSlider.gridy = i;

            GridBagConstraints cLabel = new GridBagConstraints();
            cLabel.gridy = 1;
            cLabel.gridy = i;

            slidersPanel.add(slider, cSlider);
            slidersPanel.add(sliderLabel, cLabel);
        }

        setMode(Mode.TEXTBOXES);

        comboBoxes = new JComboBox[tunableField.getGuiComboBoxAmount()];

        for (int i = 0; i < comboBoxes.length; i++) {
            TunableComboBox comboBox = new TunableComboBox(i, tunableField, eocvSim);
            add(comboBox);

            comboBoxes[i] = comboBox;
        }
    }

    //method that should be called when this panel is added to the visualizer gui
    public void showFieldPanel() {
        if(hasBeenShown) return;
        hasBeenShown = true;

        //updates the slider ranges from config
        panelOptions.getConfigPanel().updateFieldGuiFromConfig();
        tunableField.evalRecommendedPanelMode();
    }

    public void setFieldValue(int index, Object value) {
        if(index >= fields.length) return;

        fields[index].setText(value.toString());
        try {
            sliders[index].setScaledValue(Double.parseDouble(value.toString()));
        } catch(NumberFormatException ignored) {}
    }

    public void setComboBoxSelection(int index, Object selection) {
        comboBoxes[index].setSelectedItem(selection.toString());
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

                for(int i = 0 ; i < tunableField.getGuiFieldAmount() ; i++) {
                    fields[i].setInControl(true);
                    sliders[i].setInControl(false);
                    setFieldValue(i, tunableField.getGuiFieldValue(i));
                }

                add(fieldsPanel);
                break;
            case SLIDERS:
                if(this.mode == Mode.TEXTBOXES) {
                    remove(fieldsPanel);
                }

                for(int i = 0 ; i < tunableField.getGuiFieldAmount() ; i++) {
                    fields[i].setInControl(false);
                    sliders[i].setInControl(true);
                    setFieldValue(i, tunableField.getGuiFieldValue(i));
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
            slider.setScaledBounds(min, max);
        }
    }

    public Mode getMode() { return this.mode; }

    public boolean hasRequestedAllConfigReeval() {
        boolean current = reevalConfigRequested;
        reevalConfigRequested = false;

        return current;
    }

}