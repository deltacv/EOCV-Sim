package io.github.deltacv.easyvision.node

enum class Category(val properName: String) {
    CV_BASICS("Basic OpenCV Operations"),
    MATH("Math Operations"),
    MISC("Miscellaneous")
}

annotation class RegisterNode(val name: String, val category: Category, val description: String = "")
