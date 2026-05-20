/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.config;

import com.github.serivesmejia.eocvsim.util.SysUtil;
import com.github.serivesmejia.eocvsim.util.serialization.JacksonJsonSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;

public class ConfigLoader {

    public static final String CONFIG_SAVEFILE_NAME = "eocvsim_config.json";

    public static final File CONFIG_SAVEFILE = new File(SysUtil.getEOCVSimFolder() + File.separator + CONFIG_SAVEFILE_NAME);
    public static final File OLD_CONFIG_SAVEFILE = new File(SysUtil.getAppData() + File.separator + CONFIG_SAVEFILE_NAME);

    Logger logger = LoggerFactory.getLogger(getClass());

    public Config loadFromFile(File file) throws FileNotFoundException {
        if (!file.exists()) throw new FileNotFoundException();

        String jsonConfig = SysUtil.loadFileStr(file);
        if (jsonConfig.trim().equals("")) return null;

        try {
            return JacksonJsonSupport.persistenceMapper.readValue(jsonConfig, Config.class);
        } catch (Exception ex) {
            logger.info("Jackson exception while parsing config file", ex);
            return null;
        }
    }

    public Config loadFromFile() throws FileNotFoundException {
        SysUtil.migrateFile(OLD_CONFIG_SAVEFILE, CONFIG_SAVEFILE);
        return loadFromFile(CONFIG_SAVEFILE);
    }

    public void saveToFile(File file, Config conf) {
        String jsonConfig;

        try {
            jsonConfig = JacksonJsonSupport.persistenceMapper.writeValueAsString(conf);
        } catch (Exception ex) {
            logger.error("Failed to serialize config file", ex);
            return;
        }

        SysUtil.saveFileStr(file, jsonConfig);
    }

    public void saveToFile(Config conf) {
        saveToFile(CONFIG_SAVEFILE, conf);
    }

}

