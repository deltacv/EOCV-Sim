/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.vision.external.source;

public interface VisionSourceProvider {

    VisionSource get(String name);

}
