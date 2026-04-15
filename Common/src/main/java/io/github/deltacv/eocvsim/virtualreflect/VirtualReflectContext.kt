/*
 * Copyright (c) 2022 Sebastian Erives
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

package io.github.deltacv.eocvsim.virtualreflect

/**
 * Context for virtual reflection, which makes for a multi-platform way to reflect on classes, fields, and methods.
 */
interface VirtualReflectContext {

    /**
     * The name of the class this context is reflecting on, including package name
     */
    val name: String

    /**
     * The simple name of the class this context is reflecting on, without package name
     */
    val simpleName: String

    /**
     * The fields of the class this context is reflecting on
     */
    val fields: Array<VirtualField>

    /**
     * Gets a field by its name, or null if it doesn't exist
     */
    fun getField(name: String): VirtualField?

    /**
     * Gets a field by its label, or null if it doesn't exist
     * @see io.github.deltacv.eocvsim.virtualreflect.jvm.Label
     */
    fun getLabeledField(label: String): VirtualField?

}