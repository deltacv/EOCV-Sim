package io.github.deltacv.easyvision.node

import io.github.classgraph.ClassGraph

typealias CategorizedNodes = Map<Category, MutableList<Class<out Node<*>>>>

object NodeScanner {

    val ignoredPackages = arrayOf(
        "java",
        "org.opencv",
        "imgui",
        "io.github.classgraph",
        "org.lwjgl"
    )

    @Suppress("UNCHECKED_CAST") //shut
    fun scan(): CategorizedNodes {
        val nodes = mutableMapOf<Category, MutableList<Class<out Node<*>>>>()

        println("Scanning for nodes...")

        val classGraph = ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .rejectPackages(*ignoredPackages)

        val scanResult = classGraph.scan()
        val nodeClasses = scanResult.getClassesWithAnnotation(RegisterNode::class.java.name)

        for(nodeClass in nodeClasses) {
            val clazz = Class.forName(nodeClass.name)

            val regAnnotation = clazz.getDeclaredAnnotation(RegisterNode::class.java)

            if(hasSuperclass(clazz, Node::class.java)) {
                val nodeClazz = clazz as Class<out Node<*>>

                var list = nodes[regAnnotation.category]

                if(list == null) {
                    list = mutableListOf(nodeClazz)
                    nodes[regAnnotation.category] = list
                } else {
                    list.add(nodeClazz)
                }
            }
        }

        println("Found ${nodeClasses.size} nodes")

        return nodes
    }

}

fun hasSuperclass(clazz: Class<*>, superClass: Class<*>): Boolean {
    return try {
        clazz.asSubclass(superClass)
        true
    } catch (ex: ClassCastException) {
        false
    }
}