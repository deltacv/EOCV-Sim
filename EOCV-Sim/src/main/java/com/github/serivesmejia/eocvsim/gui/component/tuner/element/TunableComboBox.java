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

package com.github.serivesmejia.eocvsim.gui.component.tuner.element;

import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.tuner.TunableField;

import javax.swing.*;
import java.util.Objects;

public class TunableComboBox extends JComboBox<String> {

    private final TunableField tunableField;
    private final int index;

    private final EOCVSim eocvSim;

    public TunableComboBox(int index, TunableField tunableField, EOCVSim eocvSim) {
        super();

        this.tunableField = tunableField;
        this.index = index;
        this.eocvSim = eocvSim;

        init();
    }

    private void init() {
        for (Object obj : tunableField.getGuiComboBoxValues(index)) {
            this.addItem(obj.toString());
        }

        addItemListener(evt -> eocvSim.onMainUpdate.doOnce(() -> {
            try {
                tunableField.setGuiComboBoxValue(index, Objects.requireNonNull(getSelectedItem()).toString());
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }

            if (eocvSim.pipelineManager.getPaused()) {
                eocvSim.pipelineManager.requestSetPaused(false);
            }
        }));
    }

}