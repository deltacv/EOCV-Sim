package io.github.deltacv.vision.external.source;

import org.openftc.easyopencv.OpenCvViewport;

public interface ViewportAndSourceHander extends VisionSourceHander {

    OpenCvViewport viewport();

}
