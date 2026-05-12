/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 */

package io.github.deltacv.vision.external.source;

import org.openftc.easyopencv.OpenCvViewport;

public interface ViewportVisionSourceProvider extends VisionSourceProvider {
    OpenCvViewport viewport();
}

