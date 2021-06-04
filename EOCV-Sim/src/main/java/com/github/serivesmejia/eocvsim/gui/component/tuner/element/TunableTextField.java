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
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;

public class TunableTextField extends JTextField {

    private final ArrayList<Character> validCharsIfNumber = new ArrayList<>();

    private final TunableField tunableField;
    private final int index;
    private final EOCVSim eocvSim;

    private final Border initialBorder;

    private volatile boolean hasValidText = true;

    private boolean inControl = false;

    public TunableTextField(int index, TunableField tunableField, EOCVSim eocvSim) {
        super();

        this.initialBorder = this.getBorder();

        this.tunableField = tunableField;
        this.index = index;
        this.eocvSim = eocvSim;

        setText(tunableField.getGuiFieldValue(index).toString());

        int plusW = Math.round(getText().length() / 5f) * 10;
        this.setPreferredSize(new Dimension(40 + plusW, getPreferredSize().height));

        tunableField.onValueChange.doPersistent(() -> {
            if(!inControl) {
                setText(tunableField.getGuiFieldValue(index).toString());
            }
        });

        if (tunableField.isOnlyNumbers()) {

            //add all valid characters for non decimal numeric fields
            Collections.addAll(validCharsIfNumber, '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-');

            //allow dots for decimal numeric fields
            if (tunableField.getAllowMode() == TunableField.AllowMode.ONLY_NUMBERS_DECIMAL) {
                validCharsIfNumber.add('.');
            }

            ((AbstractDocument) getDocument()).setDocumentFilter(new DocumentFilter() {

                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                    text = text.replace(" ", "");

                    for (char c : text.toCharArray()) {
                        if (!isNumberCharacter(c)) return;
                    }

                    boolean invalidNumber = false;

                    try { //check if entered text is valid number
                        Double.valueOf(text);
                    } catch (NumberFormatException ex) {
                        invalidNumber = true;
                    }

                    hasValidText = !invalidNumber || !text.isEmpty();

                    if (hasValidText) {
                        setNormalBorder();
                    } else {
                        setRedBorder();
                    }

                    super.replace(fb, offset, length, text, attrs);
                }

            });

        }

        getDocument().addDocumentListener(new DocumentListener() {

            Runnable changeFieldValue = () -> {
                if ((!hasValidText || !tunableField.isOnlyNumbers() || !getText().trim().equals(""))) {
                    try {
                        tunableField.setGuiFieldValue(index, getText());
                    } catch (Exception e) {
                        setRedBorder();
                    }
                } else {
                    setRedBorder();
                }
            };

            @Override
            public void insertUpdate(DocumentEvent e) {
                change();
            }
            @Override
            public void removeUpdate(DocumentEvent e) { change(); }
            @Override
            public void changedUpdate(DocumentEvent e) { change(); }

            public void change() {
                eocvSim.onMainUpdate.doOnce(changeFieldValue);
            }
        });

        //unpausing when typing on any tunable text box
        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                execute();
            }
            @Override
            public void keyPressed(KeyEvent e) {
                execute();
            }
            @Override
            public void keyReleased(KeyEvent e) { execute(); }

            public void execute() {
                if (eocvSim.pipelineManager.getPaused()) {
                    eocvSim.pipelineManager.requestSetPaused(false);
                }
            }
        });
    }

    public void setNormalBorder() {
        setBorder(initialBorder);
    }

    public void setRedBorder() {
        setBorder(new LineBorder(new Color(255, 79, 79), 2));
    }

    public void setInControl(boolean inControl) {
        this.inControl = inControl;
    }

    private boolean isNumberCharacter(char c) {
        for (char validC : validCharsIfNumber) {
            if (c == validC) return true;
        }
        return false;
    }

    public boolean isInControl() { return inControl; }

}
