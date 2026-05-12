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
    @JsonProperty @JvmField var imgPath: String = "",
    @JsonProperty @JvmField var size: Size = Size()

) : InputSource() {

    @Transient private var img: MatRecycler.RecyclableMat? = null
    @Transient private var lastCloneTo: MatRecycler.RecyclableMat? = null

    @Transient private var initialized = false

    @Transient private var matRecycler = MatRecycler(2)

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

        if (this.size.area() != 0.0) {
            Imgproc.resize(img, img, this.size, 0.0, 0.0, Imgproc.INTER_AREA)
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

    override fun internalCloneSource() = ImageSource(imgPath, size)

    override fun setSize(size: Size) {
        this.size = size
    }

    override fun getSize() = size


    override val fileFilters: FileFilter get() = FileFilters.imagesFilter
    override val captureTimeNanos: Long get() = System.nanoTime()


    override fun toString(): String {
        return "ImageSource(\"$imgPath\", $size)"

    }

}
