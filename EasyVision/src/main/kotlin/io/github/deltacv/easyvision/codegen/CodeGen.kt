package io.github.deltacv.easyvision.codegen

enum class Visibility {
    PUBLIC, PRIVATE, PROTECTED
}

class CodeGen(var className: String) {

    val importScope     = Scope(0)
    val classStartScope = Scope(1)

    val initScope           = Scope(1)
    val processFrameScope   = Scope(1)
    val viewportTappedScope = Scope(1)

    init {
        importScope.import("org.openftc.easyopencv.OpenCvPipeline")
    }


    fun gen(): String {
        val mainScope = Scope(0)
        val bodyScope = Scope(1)

        val start = classStartScope.get()
        if(start.isNotBlank()) {
            bodyScope.scope(classStartScope)
        }

        val init = initScope.get()
        if(init.isNotBlank()) {
            bodyScope.method(
                Visibility.PUBLIC, "void", "init", initScope,
                Parameter("Mat", "input"), isOverride = true
            )
        }

        val process = processFrameScope.get()
        if(process.isNotBlank()) {
            bodyScope.method(
                Visibility.PUBLIC, "Mat", "processFrame", processFrameScope,
                Parameter("Mat", "input"), isOverride = true
            )
        }

        val viewportTapped = viewportTappedScope.get()
        if(viewportTapped.isNotBlank()) {
            bodyScope.method(
                Visibility.PUBLIC, "Mat", "onViewportTapped", viewportTappedScope,
                isOverride = true
            )
        }

        mainScope.scope(importScope)
        mainScope.newStatement()
        mainScope.clazz(Visibility.PUBLIC, className, bodyScope, extends = arrayOf("OpenCvPipeline"))

        return mainScope.get()
    }

}