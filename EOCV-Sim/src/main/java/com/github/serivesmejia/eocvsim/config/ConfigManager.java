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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManager {

    public final ConfigLoader configLoader = new ConfigLoader();
    private Config config;

    Logger logger = LoggerFactory.getLogger(getClass());

    public void init() {
        logger.info("Initializing...");

        try {
            config = configLoader.loadFromFile();
            if (config == null) {
                logger.error("Error while parsing config file, it will be replaced and fixed, but the user configurations will be reset");
                throw new NullPointerException(); //for it to be caught later and handle the creation of a new config
            } else {
                logger.info("Loaded config from file successfully");
            }
        } catch (Exception ex) { //handles FileNotFoundException & a NullPointerException thrown above
            config = new Config();
            logger.info("Creating config file...");
            configLoader.saveToFile(config);
        }
    }

    public void saveToFile() {
        configLoader.saveToFile(config);
    }

    public Config getConfig() {
        return config;
    }

}
