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

package com.github.serivesmejia.eocvsim.input

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import org.koin.core.component.KoinComponent
import org.opencv.core.Mat
import org.opencv.core.Size
import javax.swing.filechooser.FileFilter

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
abstract class InputSource : Comparable<InputSource>, KoinComponent {

    // Computed property (no backing field) so Jackson field-based persistence only
    // stores the actual source state and does not capture derived values.
    open val hasSlowInitialization: Boolean get() = false

    @Transient var isDefault = false

    @Transient open var name: String = ""

    @Transient private var beforeIsPaused = false
    @Transient @set:JvmName("setPausedValue") var isPaused = false
        set(value) {
            field = value
            if (beforeIsPaused != value) {
                if (value) onPause() else onResume()
            }
            beforeIsPaused = value
        }

    @JsonProperty
    @JvmField
    var createdOn = -1L

    abstract fun init(): Boolean
    abstract fun reset()

    open fun cleanIfDirty() {}

    abstract fun close()

    abstract fun onPause()
    abstract fun onResume()

    open fun setSize(size: Size) {}

    open fun getSize() = Size()

    open fun update(): Mat? = null

    fun cloneSource(): InputSource {
        val source = internalCloneSource()
        source.createdOn = createdOn
        return source
    }

    protected abstract fun internalCloneSource(): InputSource

    abstract val fileFilters: FileFilter?
    abstract val captureTimeNanos: Long

    val creationTime: Long get() = createdOn

    override fun compareTo(other: InputSource): Int {
        return if (createdOn > other.createdOn) 1 else -1
    }

}
