package io.github.deltacv.easyvision.codegen.dsl

import io.github.deltacv.easyvision.codegen.CodeGen

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

}