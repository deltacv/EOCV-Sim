/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.workspace.config;

import java.util.ArrayList;

/**
 * A class that holds the configuration of a workspace.
 * This class is used to serialize and deserialize the workspace configuration.
 * The workspace configuration is stored in a file called "eocvsim_workspace.json" in the workspace directory.
 * The workspace configuration is used to store the paths of the sources and resources directories, the excluded paths and file extensions, and the EOCV-Sim version.
 */
public class WorkspaceConfig {

    public String sourcesPath = ".";
    public String resourcesPath = ".";
    public ArrayList<String> excludedPaths = new ArrayList<>();
    public ArrayList<String> excludedFileExtensions = new ArrayList<>();

    public String eocvSimVersion = "";

}
