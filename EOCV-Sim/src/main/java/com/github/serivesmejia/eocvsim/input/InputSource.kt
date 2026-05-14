/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
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

    abstract val sourceSize: Size

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

