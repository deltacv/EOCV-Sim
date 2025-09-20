/*
 * Copyright (c) 2025 Miriam Sinton-Remes
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of Miriam Sinton-Remes nor the names of her contributors may be used to
 * endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.vision.opencv;

import org.opencv.core.Point;

/**
 * Stores the center Point and radius of a circle
 * @see ColorBlobLocatorProcessor
 */
public class Circle {
    private final float x;
    private final float y;
    private final float radius;

    /**
     * Create a new Circle
     * @param center the center Point of the Circle
     * @param radius the radius of the Circle
     */
    public Circle(Point center, float radius) {
        this((float) center.x, (float) center.y, radius);
    }

    /**
     * Create a new Circle
     * @param x the x position of the Circle
     * @param y the y position of the Circle
     * @param radius the radius of the circle
     */
    public Circle(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    /**
     * Get the center Point of the Circle
     * @return the center Point of the Circle
     */
    public Point getCenter() {
        return new Point(x, y);
    }

    /**
     * Get the radius of the Circle
     * @return the radius of the Circle
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Get the x position of the Circle
     * @return the x position of the Circle
     */
    public float getX() {
        return x;
    }

    /**
     * Get the y position of the Circle
     * @return the y position of the Circle
     */
    public float getY() {
        return y;
    }
}
