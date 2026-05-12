/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.util

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
