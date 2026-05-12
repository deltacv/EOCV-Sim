/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.util.extension

import javax.swing.JTextField
import javax.swing.text.AbstractDocument
import javax.swing.text.DocumentFilter

val JTextField.abstractDocument: AbstractDocument
    get() {
        return (document as AbstractDocument)
    }

var JTextField.documentFilter: DocumentFilter
    get() {
        return abstractDocument.documentFilter
    }
    set(value) {
        abstractDocument.documentFilter = value
    }
