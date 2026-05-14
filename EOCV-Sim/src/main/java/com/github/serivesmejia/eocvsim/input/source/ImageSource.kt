/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.input.source

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.serivesmejia.eocvsim.input.InputSource
import com.github.serivesmejia.eocvsim.util.FileFilters
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.openftc.easyopencv.MatRecycler
import javax.swing.filechooser.FileFilter

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
class ImageSource @JvmOverloads constructor(
    @field:JsonProperty @JvmField var imgPath: String = "",
    @field:JsonProperty @JvmField var size: Size = Size()

) : InputSource() {

    @Transient private var img: MatRecycler.RecyclableMat? = null
    @Transient private var lastCloneTo: MatRecycler.RecyclableMat? = null

    @Transient private var initialized = false

    @Transient private var matRecycler = MatRecycler(2)

    override val sourceSize get() = size

    override fun init(): Boolean {
        if (initialized) return false
        initialized = true

        readImage()

        return img != null && !img!!.empty()
    }

    override fun onPause() {}

    override fun onResume() {}

    override fun reset() {
        if (!initialized) return

        lastCloneTo?.returnMat()
        lastCloneTo = null

        img?.returnMat()
        img = null

        matRecycler.releaseAll()

        initialized = false
    }

    override fun close() {
        img?.let {
            matRecycler.returnMat(it)
            img = null
        }

        lastCloneTo?.let {
            it.returnMat()
            lastCloneTo = null
        }

        matRecycler.releaseAll()
    }

    fun readImage() {
        val readMat = Imgcodecs.imread(imgPath)

        if (img == null) img = matRecycler.takeMatOrNull()

        if (readMat.empty()) {
            return
        }

        readMat.copyTo(img)
        readMat.release()

        if (this.sourceSize.area() != 0.0) {
            Imgproc.resize(img, img, this.sourceSize, 0.0, 0.0, Imgproc.INTER_AREA)
        } else {
            this.size = img!!.size()
        }

        Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2RGB)
    }

    override fun update(): Mat? {
        if (lastCloneTo == null) lastCloneTo = matRecycler.takeMatOrNull()

        if (img == null || lastCloneTo == null) return null

        lastCloneTo!!.release()
        img!!.copyTo(lastCloneTo)

        return lastCloneTo
    }

    override fun cleanIfDirty() {
        readImage()
    }

    override fun internalCloneSource() = ImageSource(imgPath, sourceSize)

    override val fileFilters: FileFilter get() = FileFilters.imagesFilter
    override val captureTimeNanos: Long get() = System.nanoTime()


    override fun toString(): String {
        return "ImageSource(\"$imgPath\", $sourceSize)"

    }

}

