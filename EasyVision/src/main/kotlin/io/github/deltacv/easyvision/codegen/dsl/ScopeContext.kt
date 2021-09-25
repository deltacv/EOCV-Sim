package io.github.deltacv.easyvision.codegen.dsl

import io.github.deltacv.easyvision.codegen.Scope

class ScopeContext(val scope: Scope) {

    operator fun String.invoke(vararg parameters: String) {
        scope.methodCall(this, *parameters)
    }

    fun local(type: String, value: String) {

    }

    fun constructor() {

    }

}