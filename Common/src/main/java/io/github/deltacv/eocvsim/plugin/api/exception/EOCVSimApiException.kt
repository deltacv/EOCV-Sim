package io.github.deltacv.eocvsim.plugin.api.exception

import io.github.deltacv.eocvsim.plugin.api.Api

class EOCVSimApiException(message: String, api: Api) : RuntimeException(message)