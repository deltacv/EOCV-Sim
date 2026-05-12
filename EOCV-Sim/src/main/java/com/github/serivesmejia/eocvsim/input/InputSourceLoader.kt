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
import com.github.serivesmejia.eocvsim.input.source.*
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.serialization.JacksonJsonSupport
import org.slf4j.LoggerFactory
import java.io.File

class InputSourceLoader {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val SOURCES_SAVEFILE_NAME = "eocvsim_sources.json"

        @JvmStatic
        val SOURCES_SAVEFILE = File(SysUtil.getEOCVSimFolder().toString() + File.separator + SOURCES_SAVEFILE_NAME)
        @JvmStatic
        val SOURCES_SAVEFILE_OLD = File(SysUtil.getAppData().toString() + File.separator + SOURCES_SAVEFILE_NAME)

        @JvmStatic
        val CURRENT_FILE_VERSION = InputSourcesContainer.SourcesFileVersion.SEIS
    }

    var loadedInputSources = HashMap<String, InputSource>()

    var fileVersion: InputSourcesContainer.SourcesFileVersion? = null

    fun saveInputSource(name: String, source: InputSource) {
        loadedInputSources[name] = source
    }

    fun deleteInputSource(name: String) {
        loadedInputSources.remove(name)
    }

    fun saveInputSourcesToFile() {
        saveInputSourcesToFile(SOURCES_SAVEFILE)
    }

    fun saveInputSourcesToFile(f: File) {
        val sourcesContainer = InputSourcesContainer()

        // updates file version to most recent since it will be regenerated at this point
        fileVersion?.let {
            sourcesContainer.sourcesFileVersion = if (it.ordinal < CURRENT_FILE_VERSION.ordinal)
                CURRENT_FILE_VERSION else it
        }

        for ((key, value) in loadedInputSources) {
            if (!value.isDefault) {
                val source = value.cloneSource()
                sourcesContainer.classifySource(key, source)
            }
        }

        saveInputSourcesToFile(f, sourcesContainer)
    }

    fun saveInputSourcesToFile(file: File, sourcesContainer: InputSourcesContainer) {
        val jsonInputSources = JacksonJsonSupport.persistenceMapper.writeValueAsString(sourcesContainer)
        SysUtil.saveFileStr(file, jsonInputSources)
    }

    fun saveInputSourcesToFile(sourcesContainer: InputSourcesContainer) {
        saveInputSourcesToFile(SOURCES_SAVEFILE, sourcesContainer)
    }

    fun loadInputSourcesFromFile() {
        SysUtil.migrateFile(SOURCES_SAVEFILE_OLD, SOURCES_SAVEFILE)
        loadInputSourcesFromFile(SOURCES_SAVEFILE)
    }

    fun loadInputSourcesFromFile(f: File) {
        if (!f.exists()) return

        val jsonSources = SysUtil.loadFileStr(f)
        if (jsonSources.trim() == "") return

        val sources: InputSourcesContainer
        try {
            sources = JacksonJsonSupport.persistenceMapper.readValue(jsonSources, InputSourcesContainer::class.java)
        } catch (ex: Exception) {
            logger.error("Error while parsing sources file, it will be replaced and fixed later on, but the user created sources will be deleted.", ex)
            return
        }

        sources.updateAllSources()
        fileVersion = sources.sourcesFileVersion

        saveInputSourcesToFile(sources) // to make sure version gets declared in case it was an older file

        logger.info("InputSources file version is ${sources.sourcesFileVersion}")

        loadedInputSources = sources.allSources
    }

    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE
    )
    class InputSourcesContainer {

        @Transient var allSources = HashMap<String, InputSource>()

        @JsonProperty var imageSources = HashMap<String, ImageSource>()
        @JsonProperty var cameraSources = HashMap<String, CameraSource>()
        @JsonProperty var videoSources = HashMap<String, VideoSource>()
        @JsonProperty var httpSources = HashMap<String, HttpSource>()

        @JsonProperty var sourcesFileVersion: SourcesFileVersion? = null

        enum class SourcesFileVersion { DOS, SEIS, SIETE }

        fun updateAllSources() {
            if (sourcesFileVersion == null) sourcesFileVersion = SourcesFileVersion.DOS

            allSources.clear()

            allSources.putAll(imageSources)
            allSources.putAll(cameraSources)
            allSources.putAll(httpSources)

            // check if file version is bigger than DOS, we should have video sources section
            // declared in any file with a version greater than that
            if (sourcesFileVersion!!.ordinal >= 1) {
                allSources.putAll(videoSources)
            }
        }

        fun classifySource(sourceName: String, source: InputSource) {
            when (SourceType.fromClass(source.javaClass)) {
                SourceType.IMAGE -> imageSources[sourceName] = source as ImageSource
                SourceType.CAMERA -> cameraSources[sourceName] = source as CameraSource
                SourceType.VIDEO -> videoSources[sourceName] = source as VideoSource
                SourceType.HTTP -> httpSources[sourceName] = source as HttpSource
                else -> {}
            }
        }
    }
}
