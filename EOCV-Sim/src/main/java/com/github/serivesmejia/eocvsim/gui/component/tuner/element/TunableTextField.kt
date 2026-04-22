package com.github.serivesmejia.eocvsim.gui.component.tuner.element

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.tuner.TunableNumber
import com.github.serivesmejia.eocvsim.tuner.TunableString
import com.github.serivesmejia.eocvsim.tuner.TunableValue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import org.koin.core.qualifier.named
import javax.swing.JTextField
import javax.swing.border.Border
import javax.swing.border.LineBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DocumentFilter
import java.awt.Color
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

class TunableTextField(val tunableValue: TunableValue<*>) : JTextField(), KoinComponent {

    private val pipelineManager: PipelineManager by inject()
    private val onMainUpdate: EventHandler by inject(named("onMainLoop"))


    private val validCharsIfNumber = mutableListOf<Char>()
    private val initialBorder: Border = this.border
    @Volatile private var hasValidText = true
    var isInControl = false

    init {
        text = tunableValue.value.toString()

        val plusW = (text.length / 5f).toInt() * 10
        preferredSize = Dimension(40 + plusW, preferredSize.height)

        tunableValue.onValueChange.attach {
            if (!isInControl) {
                text = tunableValue.value.toString()
            }
        }

        val isNumber = tunableValue is TunableNumber

        if (isNumber) {
            val numValue = tunableValue

            //add all valid characters for non decimal numeric fields
            validCharsIfNumber.addAll(listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-'))

            //allow dots for decimal numeric fields
            if (!numValue.isOnlyNumbers) {
                validCharsIfNumber.add('.')
            }

            (document as AbstractDocument).documentFilter = object : DocumentFilter() {
                @Throws(BadLocationException::class)
                override fun replace(
                    fb: FilterBypass,
                    offset: Int,
                    length: Int,
                    text: String,
                    attrs: AttributeSet?
                ) {
                    val filteredText = text.replace(" ".toRegex(), "")

                    for (c in filteredText.toCharArray()) {
                        if (!isNumberCharacter(c)) return
                    }

                    var invalidNumber = false

                    try { //check if entered text is valid number
                        filteredText.toDouble()
                    } catch (ex: NumberFormatException) {
                        invalidNumber = true
                    }

                    hasValidText = !invalidNumber || filteredText.isNotEmpty()

                    if (hasValidText) {
                        setNormalBorder()
                    } else {
                        setRedBorder()
                    }

                    super.replace(fb, offset, length, filteredText, attrs)
                }
            }
        }

        document.addDocumentListener(object : DocumentListener {
            val changeFieldValue = Runnable {
                val currentText = text

                if (!hasValidText || !isNumber || (currentText != null && currentText.trim().isNotEmpty())) {
                    try {
                        if (isNumber) {
                            tunableValue.setFromGui(currentText.toDouble())
                        } else if (tunableValue is TunableString) {
                            tunableValue.setFromGui(currentText)
                        }
                    } catch (e: Exception) {
                        setRedBorder()
                    }
                } else {
                    setRedBorder()
                }
            }

            override fun insertUpdate(e: DocumentEvent) = change()
            override fun removeUpdate(e: DocumentEvent) = change()
            override fun changedUpdate(e: DocumentEvent) = change()

            private fun change() {
                onMainUpdate.once(changeFieldValue)

            }
        })

        //unpausing when typing on any tunable text box
        addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) = execute()
            override fun keyPressed(e: KeyEvent) = execute()
            override fun keyReleased(e: KeyEvent) = execute()

            private fun execute() {
                if (pipelineManager.paused) {
                    pipelineManager.requestSetPaused(false)
                }

            }
        })
    }

    fun setNormalBorder() {
        border = initialBorder
    }

    fun setRedBorder() {
        border = LineBorder(Color(255, 79, 79), 2)
    }

    private fun isNumberCharacter(c: Char): Boolean {
        return validCharsIfNumber.contains(c)
    }

}
