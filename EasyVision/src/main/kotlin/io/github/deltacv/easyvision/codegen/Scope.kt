package io.github.deltacv.easyvision.codegen

enum class Visibility {
    PUBLIC, PRIVATE, PROTECTED
}

class Scope(private val tabs: Int = 1) {

    private var builder = StringBuilder()

    fun instanceVariable(vis: Visibility, type: String, name: String,
                         defaultValue: String? = null,
                         isStatic: Boolean = false, isFinal: Boolean = false) {
        newStatement()

        val modifiers = if(isStatic) "static " else "" +
                        if(isFinal) "final " else " "

        val ending = if(defaultValue != null) "= $defaultValue;" else ";"

        builder.append("${vis.name.lowercase()} $modifiers$type $name $ending")
    }
    
    fun localVariable(type: String, name: String,
                      defaultValue: String? = null) {
        newStatement()
        
        val ending = if(defaultValue != null) "= $defaultValue;" else ";"
        
        builder.append("$type $name $ending")
    }

    fun methodCall(className: String, methodName: String, vararg parameters: String) {
        newStatement()
        
        builder.append("$className.$methodName(")
        
        for((i, parameter) in parameters.withIndex()) {
            builder.append(parameter)

            if(i < parameters.size - 1) {
                builder.append(", ")
            }
        }

        builder.append(");")
    }
    
    private fun newStatement() {
        if(builder.isNotEmpty()) {
            builder.appendLine()
        }

        insertTabs()
    }
    
    private fun insertTabs() {
        repeat(tabs) {
            builder.append("\t")
        }
    }

    fun get() = builder.toString()

}