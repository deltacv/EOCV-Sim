package io.github.deltacv.easyvision.codegen

open class Scope(private val tabsCount: Int = 1) {

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

    fun instanceVariable(vis: Visibility, type: String, name: String,
                         defaultValue: String? = null,
                         isStatic: Boolean = false, isFinal: Boolean = false) {
        newStatement()

        val modifiers = if(isStatic) "static " else "" +
                        if(isFinal) "final " else " "

        val ending = if(defaultValue != null) "= $defaultValue;" else ";"

        builder.append("${vis.name.lowercase()} $modifiers$type $name $ending")
    }
    
    fun localVariable(
        type: String, name: String,
        defaultValue: String? = null
    ) {
        newStatement()
        
        val ending = if(defaultValue != null) "= $defaultValue;" else ";"
        
        builder.append("$type $name $ending")
    }

    fun methodCall(className: String, methodName: String, vararg parameters: String) {
        newStatement()
        
        builder.append("$className.$methodName(${parameters.csv()});")
    }

    fun method(
        vis: Visibility, returnType: String, name: String, body: Scope,
        vararg parameters: Parameter,
        isStatic: Boolean = false, isFinal: Boolean = false, isOverride: Boolean = true
    ) {
        newStatement()
        builder.appendLine()

        val static = if(isStatic) "static " else ""
        val final = if(isFinal) "final " else ""

        if(isOverride) {
            builder.append("$tabs@Override").appendLine()
        }

        builder.append("""
            |$tabs${vis.name.lowercase()} $static$final$returnType $name(${parameters.csv()}) {
            |$tabs$body
            |$tabs}
        """.trimMargin())
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
            |$tabs$body$endWhitespaceLine
            |$tabs}
        """.trimMargin())
    }

    fun scope(scope: Scope) {
        newStatement()
        builder.appendLine().append(scope)
    }

    fun newStatement() {
        if(builder.isNotEmpty()) {
            builder.appendLine()
        }

        builder.append(tabs)
    }

    fun clear() = builder.clear()

    fun get() = builder.toString()

    override fun toString() = get()

}

data class Parameter(val type: String, val name: String)

fun Array<out String>.csv(): String {
    val builder = StringBuilder()

    for((i, parameter) in this.withIndex()) {
        builder.append(parameter)

        if(i < this.size - 1) {
            builder.append(", ")
        }
    }

    return builder.toString()
}

fun Array<out Parameter>.csv(): String {
    val stringArray = this.map { "${it.type} ${it.name}" }.toTypedArray()
    return stringArray.csv()
}