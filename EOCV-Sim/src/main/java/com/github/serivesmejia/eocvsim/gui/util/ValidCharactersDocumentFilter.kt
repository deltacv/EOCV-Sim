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

package com.github.serivesmejia.eocvsim.gui.util

import javax.swing.JTextField
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DocumentFilter

class ValidCharactersDocumentFilter(val validCharacters: Array<Char>) : DocumentFilter() {

    @get:Synchronized
    @Volatile var valid = false
        private set

    @get:Synchronized
    @Volatile var lastValid = 0.0
        private set

    @Volatile private var lastText = ""

    @Throws(BadLocationException::class)
    @Synchronized override fun replace(fb: FilterBypass?, offset: Int, length: Int, text: String, attrs: AttributeSet?) {
        val newText = text.replace(" ", "")

        for (c in newText.toCharArray()) {
            if(!isValidCharacter(c)) return
        }

        valid = try {
            lastValid = newText.toDouble()
            lastText = newText
            newText != ""
        } catch (ex: NumberFormatException) {
            false
        }

        super.replace(fb, offset, length, newText, attrs)
    }

    private fun isValidCharacter(c: Char): Boolean {
        for (validC in validCharacters) {
            if (c == validC) return true
        }
        return false
    }

}