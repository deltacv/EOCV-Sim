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
import com.github.serivesmejia.eocvsim.tuner.TunableField;
import com.github.serivesmejia.eocvsim.tuner.scanner.RegisterTunableField;
import org.opencv.core.Point;
import org.openftc.easyopencv.OpenCvPipeline;

import java.lang.reflect.Field;

@RegisterTunableField
public class PointField extends TunableField<Point> {

    Point point;

    double[] lastXY = {0, 0};

    volatile boolean hasChanged = false;

    public PointField(OpenCvPipeline instance, Field reflectionField, EOCVSim eocvSim) throws IllegalAccessException {

        super(instance, reflectionField, eocvSim, AllowMode.ONLY_NUMBERS_DECIMAL);

        if(initialFieldValue != null) {
            Point p = (Point) initialFieldValue;
            point = new Point(p.x, p.y);
        } else {
            point = new Point(0, 0);
        }

        setGuiFieldAmount(2);

    }

    @Override
    public void init() { }

    @Override
    public void update() {

        hasChanged = point.x != lastXY[0] || point.y != lastXY[1];

        if (hasChanged) { //update values in GUI if they changed since last check
            updateGuiFieldValues();
        }

        lastXY = new double[]{point.x, point.y};

    }

    @Override
    public void updateGuiFieldValues() {
        fieldPanel.setFieldValue(0, point.x);
        fieldPanel.setFieldValue(1, point.y);
    }

    @Override
    public void setGuiFieldValue(int index, String newValue) throws IllegalAccessException {

        try {
            double value = Double.parseDouble(newValue);
            if (index == 0) {
                point.x = value;
            } else {
                point.y = value;
            }
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Parameter should be a valid numeric String");
        }

        setPipelineFieldValue(point);

        lastXY = new double[]{point.x, point.y};

    }

    @Override
    public Point getValue() {
        return point;
    }

    @Override
    public Object getGuiFieldValue(int index) {
        return index == 0 ? point.x : point.y;
    }

    @Override
    public boolean hasChanged() {
        hasChanged = point.x != lastXY[0] || point.y != lastXY[1];
        return hasChanged;
    }

}