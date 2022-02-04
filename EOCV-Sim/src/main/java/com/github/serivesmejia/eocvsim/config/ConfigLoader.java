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

package com.github.serivesmejia.eocvsim.config;

import com.github.serivesmejia.eocvsim.util.SysUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;

public class ConfigLoader {

    public static final String CONFIG_SAVEFILE_NAME = "eocvsim_config.json";

    public static final File CONFIG_SAVEFILE = new File(SysUtil.getEOCVSimFolder() + File.separator + CONFIG_SAVEFILE_NAME);
    public static final File OLD_CONFIG_SAVEFILE = new File(SysUtil.getAppData() + File.separator + CONFIG_SAVEFILE_NAME);

    Logger logger = LoggerFactory.getLogger(getClass());

    static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public Config loadFromFile(File file) throws FileNotFoundException {
        if (!file.exists()) throw new FileNotFoundException();

        String jsonConfig = SysUtil.loadFileStr(file);
        if (jsonConfig.trim().equals("")) return null;

        try {
            return gson.fromJson(jsonConfig, Config.class);
        } catch (Exception ex) {
            logger.info("Gson exception while parsing config file", ex);
            return null;
        }
    }

    public Config loadFromFile() throws FileNotFoundException {
        SysUtil.migrateFile(OLD_CONFIG_SAVEFILE, CONFIG_SAVEFILE);
        return loadFromFile(CONFIG_SAVEFILE);
    }

    public void saveToFile(File file, Config conf) {
        String jsonConfig = gson.toJson(conf);
        SysUtil.saveFileStr(file, jsonConfig);
    }

    public void saveToFile(Config conf) {
        saveToFile(CONFIG_SAVEFILE, conf);
    }

}
