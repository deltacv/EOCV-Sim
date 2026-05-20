/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.util

import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Object to store file filters for JFileChooser
 */
object FileFilters {

    /**
     * Filter for images
     */
    @JvmField val imagesFilter = FileNameExtensionFilter("Images",
            "jpg", "jpeg", "jpe", "jp2", "bmp", "png", "tiff", "tif")

    /**
     * Filter for videos
     */
    @JvmField val videoMediaFilter = FileNameExtensionFilter("Video Media",
            "avi", "mkv", "mov", "mp4")

    /**
     * Filter for recorded videos
     */
    @JvmField val recordedVideoFilter = FileNameExtensionFilter("AVI (*.avi)", "avi")

    /**
     * Filter for log files
     */
    @JvmField val logFileFilter = FileNameExtensionFilter("Log File (*.log)", "log")

}
