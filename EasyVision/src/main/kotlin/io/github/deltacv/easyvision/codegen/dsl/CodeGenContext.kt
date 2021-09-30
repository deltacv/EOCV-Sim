package io.github.deltacv.easyvision.codegen.dsl

import io.github.deltacv.easyvision.codegen.*
import io.github.deltacv.easyvision.codegen.parse.*

class CodeGenContext(val codeGen: CodeGen) {

    fun import(pkg: String) {
        codeGen.importScope.import(pkg)
    }

    fun enum(name: String, vararg values: String) {
        codeGen.classStartScope.enumClass(name, *values)
    }

    fun init(block: ScopeContext.() -> Unit) {
        codeGen.initScope(block)
    }

    fun processFrame(block: ScopeContext.() -> Unit) {
        codeGen.processFrameScope(block)
    }

    fun onViewportTapped(block: ScopeContext.() -> Unit) {
        codeGen.viewportTappedScope(block)
    }

    fun public(name: String, v: Value) =
        codeGen.classStartScope.instanceVariable(Visibility.PUBLIC, name, v)

    fun private(name: String, v: Value) =
        codeGen.classStartScope.instanceVariable(Visibility.PRIVATE, name, v)

    fun protected(name: String, v: Value) =
        codeGen.classStartScope.instanceVariable(Visibility.PROTECTED, name, v)

    fun tryName(name: String) = codeGen.classStartScope.tryName(name)

    operator fun String.invoke(
        vis: Visibility, returnType: String,
        vararg parameters: Parameter,
        isStatic: Boolean = false, isFinal: Boolean = false, isOverride: Boolean = true,
        scopeBlock: ScopeContext.() -> Unit
    ) {
        val s = Scope(2)
        scopeBlock(s.context)

        codeGen.classEndScope.method(
            vis, returnType, this, s, *parameters,
            isStatic = isStatic, isFinal = isFinal, isOverride = isOverride
        )
    }

}