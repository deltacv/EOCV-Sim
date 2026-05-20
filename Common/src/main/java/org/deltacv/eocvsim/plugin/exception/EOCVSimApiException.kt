/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.plugin.exception

import org.deltacv.eocvsim.plugin.api.Api

class EOCVSimApiException(message: String, val api: Api) : RuntimeException("Exception thrown by API ${api::class.simpleName}: $message")