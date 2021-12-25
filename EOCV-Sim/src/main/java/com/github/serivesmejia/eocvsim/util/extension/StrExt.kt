package com.github.serivesmejia.eocvsim.util.extension

fun String.removeFromEnd(rem: String): String {
    if(endsWith(rem)) {
        return substring(0, length - rem.length).trim()
    }
    return trim()
}

fun String.tabIndent() = replace("(?m)^", "\t")

fun String.removeIndentFromFirstLine(): String {
    val lines = split("\n")

    if(lines.size == 1) {
        return lines[0].replace("\t", "")
    } else {
        var str = ""
        for(i in 0..lines.size.zeroBased) {
            val line = if(i == 0) {
                lines[i].replace("\t", "")
            } else {
                lines[i]
            }

            str += line + "\n"
        }

        return str.trim()
    }
}