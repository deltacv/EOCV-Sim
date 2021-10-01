package io.github.deltacv.easyvision.node

enum class Category { CV_BASICS, MATH, MISC}

annotation class AddNode(val name: String, val category: Category, val description: String = "")
