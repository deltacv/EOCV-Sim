package io.github.deltacv.easyvision.codegen

import io.github.deltacv.easyvision.codegen.dsl.ScopeContext

class Scope(private val tabsCount: Int = 1) {

    private var builder = StringBuilder()

    private val tabs by lazy {
        val builder = StringBuilder()

        repeat(tabsCount) {
            builder.append("\t")
        }

        builder.toString()
    }

    private val imports = mutableListOf<String>()

    fun import(pkg: String) {
        if(!imports.contains(pkg)) {
            newStatement()

            imports.add(pkg)

            builder.append("import $pkg;")
        }
    }

    fun instanceVariable(vis: Visibility, name: String,
                         variable: Value,
                         isStatic: Boolean = false, isFinal: Boolean = false) {
        newStatement()

        val modifiers = if(isStatic) " static" else "" +
                        if(isFinal) " final" else ""

        val ending = if(variable.value != null) "= ${variable.value};" else ";"

        builder.append("$tabs${vis.name.lowercase()}$modifiers ${variable.type} $name $ending")
    }
    
    fun localVariable(name: String, variable: Value) {
        newStatement()

        val ending = if(variable.value != null) "= ${variable.value};" else ";"
        
        builder.append("$tabs${variable.type} $name $ending")
    }

    fun variableSet(name: String, v: Value) {
        newStatement()

        builder.append("$tabs$name = ${v.value!!};")
    }

    fun instanceVariableSet(name: String, v: Value) {
        newStatement()

        builder.append("${tabs}this.$name = ${v.value!!};")
    }

    fun methodCall(className: String, methodName: String, vararg parameters: Value) {
        newStatement()
        
        builder.append("$tabs$className.$methodName(${parameters.csv()});")
    }

    fun methodCall(methodName: String, vararg parameters: Value) {
        newStatement()

        builder.append("$tabs$methodName(${parameters.csv()});")
    }

    fun method(
        vis: Visibility, returnType: String, name: String, body: Scope,
        vararg parameters: Parameter,
        isStatic: Boolean = false, isFinal: Boolean = false, isOverride: Boolean = false
    ) {
        newLineIfNotBlank()

        val static = if(isStatic) "static " else ""
        val final = if(isFinal) "final " else ""

        if(isOverride) {
            builder.append("$tabs@Override").appendLine()
        }

        builder.append("""
            |$tabs${vis.name.lowercase()} $static$final$returnType $name(${parameters.csv()}) {
            |$body
            |$tabs}
        """.trimMargin())
    }

    fun returnMethod(value: Value? = null) {
        newStatement()

        if(value != null) {
            builder.append("${tabs}return ${value.value!!};")
        } else {
            builder.append("${tabs}return;")
        }
    }

    fun clazz(vis: Visibility, name: String, body: Scope,
              extends: Array<String> = arrayOf(), implements: Array<String> = arrayOf(),
              isStatic: Boolean = false, isFinal: Boolean = false) {

        newStatement()

        val static = if(isStatic) "static " else ""
        val final = if(isFinal) "final " else ""

        val e = if(extends.isNotEmpty()) "extends ${extends.csv()} " else ""
        val i = if(implements.isNotEmpty()) "implements ${implements.csv()} " else ""

        val endWhitespaceLine = if(!body.get().endsWith("\n")) "\n" else ""

        builder.append("""
            |$tabs${vis.name.lowercase()} $static$final$name $e$i{
            |$body$endWhitespaceLine
            |$tabs}
        """.trimMargin())
    }

    fun enumClass(name: String, vararg values: String) {
        newStatement()

        builder.append("${tabs}enum $name { ${values.csv()} }")
    }

    fun scope(scope: Scope) {
        newLineIfNotBlank()
        builder.append(scope)
    }

    fun newStatement() {
        if(builder.isNotEmpty()) {
            builder.appendLine()
        }
    }

    fun newLineIfNotBlank() {
        val str = get()

        if(!str.endsWith("\n\n") && str.endsWith("\n")) {
            builder.appendLine()
        } else if(!str.endsWith("\n\n")) {
            builder.append("\n")
        }
    }

    fun clear() = builder.clear()

    fun get() = builder.toString()

    override fun toString() = get()

    internal val context = ScopeContext(this)

    operator fun invoke(block: ScopeContext.() -> Unit) {
        block(context)
    }

}

data class Parameter(val type: String, val name: String)