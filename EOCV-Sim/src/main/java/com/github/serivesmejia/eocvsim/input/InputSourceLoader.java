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

package com.github.serivesmejia.eocvsim.input;

import com.github.serivesmejia.eocvsim.input.source.CameraSource;
import com.github.serivesmejia.eocvsim.input.source.CameraSourceAdapter;
import com.github.serivesmejia.eocvsim.input.source.ImageSource;
import com.github.serivesmejia.eocvsim.input.source.VideoSource;
import com.github.serivesmejia.eocvsim.util.SysUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class InputSourceLoader {

    Logger logger = LoggerFactory.getLogger(getClass());

    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(CameraSource.class, new CameraSourceAdapter())
            .setPrettyPrinting()
            .create();

    public static final String SOURCES_SAVEFILE_NAME = "eocvsim_sources.json";

    public static final File SOURCES_SAVEFILE = new File(SysUtil.getEOCVSimFolder() + File.separator + SOURCES_SAVEFILE_NAME);
    public static final File SOURCES_SAVEFILE_OLD = new File(SysUtil.getAppData() + File.separator + SOURCES_SAVEFILE_NAME);

    public static final InputSourcesContainer.SourcesFileVersion CURRENT_FILE_VERSION = InputSourcesContainer.SourcesFileVersion.SEIS;

    public HashMap<String, InputSource> loadedInputSources = new HashMap<>();

    public InputSourcesContainer.SourcesFileVersion fileVersion = null;

    public void saveInputSource(String name, InputSource source) {
        loadedInputSources.put(name, source);
    }

    public void deleteInputSource(String name) {
        loadedInputSources.remove(name);
    }

    public void saveInputSourcesToFile() {
        saveInputSourcesToFile(SOURCES_SAVEFILE);
    }

    public void saveInputSourcesToFile(File f) {

        InputSourcesContainer sourcesContainer = new InputSourcesContainer();

        //updates file version to most recent since it will be regenerated at this point
        if(fileVersion != null)
            sourcesContainer.sourcesFileVersion = fileVersion.ordinal() < CURRENT_FILE_VERSION.ordinal()
                                                ? CURRENT_FILE_VERSION : fileVersion;

        for (Map.Entry<String, InputSource> entry : loadedInputSources.entrySet()) {
            if (!entry.getValue().isDefault) {
                InputSource source = entry.getValue().cloneSource();
                sourcesContainer.classifySource(entry.getKey(), source);
            }
        }

        saveInputSourcesToFile(f, sourcesContainer);

    }

    public void saveInputSourcesToFile(File file, InputSourcesContainer sourcesContainer) {
        String jsonInputSources = gson.toJson(sourcesContainer);
        SysUtil.saveFileStr(file, jsonInputSources);
    }

    public void saveInputSourcesToFile(InputSourcesContainer sourcesContainer) {
        saveInputSourcesToFile(SOURCES_SAVEFILE, sourcesContainer);
    }

    public void loadInputSourcesFromFile() {
        SysUtil.migrateFile(SOURCES_SAVEFILE_OLD, SOURCES_SAVEFILE);
        loadInputSourcesFromFile(SOURCES_SAVEFILE);
    }

    public void loadInputSourcesFromFile(File f) {

        if (!f.exists()) return;

        String jsonSources = SysUtil.loadFileStr(f);
        if (jsonSources.trim().equals("")) return;

        InputSourcesContainer sources;

        try {
            sources = gson.fromJson(jsonSources, InputSourcesContainer.class);
        } catch (Exception ex) {
            logger.error("Error while parsing sources file, it will be replaced and fixed later on, but the user created sources will be deleted.", ex);
            return;
        }

        sources.updateAllSources();
        fileVersion = sources.sourcesFileVersion;

        saveInputSourcesToFile(sources); //to make sure version gets declared in case it was an older file

        logger.info("InputSources file version is " + sources.sourcesFileVersion);

        loadedInputSources = sources.allSources;

    }

    static class InputSourcesContainer {

        public transient HashMap<String, InputSource> allSources = new HashMap<>();

        public HashMap<String, ImageSource> imageSources = new HashMap<>();
        public HashMap<String, CameraSource> cameraSources = new HashMap<>();
        public HashMap<String, VideoSource> videoSources = new HashMap<>();

        @Expose
        public SourcesFileVersion sourcesFileVersion = null;

        enum SourcesFileVersion { DOS, SEIS, SIETE }

        public void updateAllSources() {

            if(sourcesFileVersion == null) sourcesFileVersion = SourcesFileVersion.DOS;

            allSources.clear();

            for (Map.Entry<String, ImageSource> entry : imageSources.entrySet()) {
                allSources.put(entry.getKey(), entry.getValue());
            }

            for (Map.Entry<String, CameraSource> entry : cameraSources.entrySet()) {
                allSources.put(entry.getKey(), entry.getValue());
            }

            //check if file version is bigger than DOS, we should have video sources section
            //declared in any file with a version greater than that
            if(sourcesFileVersion.ordinal() >= 1) {
                for (Map.Entry<String, VideoSource> entry : videoSources.entrySet()) {
                    allSources.put(entry.getKey(), entry.getValue());
                }
            }

        }

        public void classifySource(String sourceName, InputSource source) {

            switch (SourceType.fromClass(source.getClass())) {
                case IMAGE:
                    imageSources.put(sourceName, (ImageSource) source);
                    break;
                case CAMERA:
                    cameraSources.put(sourceName, (CameraSource) source);
                    break;
                case VIDEO:
                    videoSources.put(sourceName, (VideoSource) source);
                    break;
            }

        }

    }

}
