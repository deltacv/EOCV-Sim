/*
 * Copyright (c) 2026 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.plugin.api.impl

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.HookApi

class SimpleHookApiImpl(owner: EOCVSimPlugin) : HookApi(owner) {

    private val onceHooks = mutableListOf<OnceHook>()
    private val persistentHooks = mutableListOf<PersistentHook>()

    override fun once(hook: OnceHook) = apiImpl {
        onceHooks += hook
    }

    override fun attach(hook: PersistentHook) = apiImpl {
        persistentHooks += hook
    }

    override fun runListeners() = apiImpl {
        // run & clear once hooks
        val toRunOnce = onceHooks.toList()
        onceHooks.clear()
        toRunOnce.forEach { it() }

        // run persistent hooks
        val iterator = persistentHooks.iterator()
        while (iterator.hasNext()) {
            val hook = iterator.next()
            hook {
                iterator.remove() // allow self-removal
            }
        }
    }

    override fun disableApi() {
        onceHooks.clear()
        persistentHooks.clear()
    }
}
