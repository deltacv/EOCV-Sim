package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin

abstract class InputSourceApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract var paused: Boolean
}

abstract class InputSourceManagerApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract var currentSource: InputSourceApi
}