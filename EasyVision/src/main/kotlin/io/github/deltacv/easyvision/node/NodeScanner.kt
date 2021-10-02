package io.github.deltacv.easyvision.node

import io.github.classgraph.ClassGraph
import io.github.deltacv.easyvision.util.ElapsedTime
import kotlinx.coroutines.*

object NodeScanner {

    val ignoredPackages = arrayOf(
        "java",
        "org.opencv",
        "imgui",
        "io.github.classgraph",
        "org.lwjgl"
    )

    @Suppress("UNCHECKED_CAST") //shut
    fun scan(): List<Class<out Node<*>>> {
        val nodesClasses = mutableListOf<Class<out Node<*>>>()

        println("Scanning for nodes...")

        val classGraph = ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .rejectPackages(*ignoredPackages)

        val scanResult = classGraph.scan()
        val nodeClasses = scanResult.getClassesWithAnnotation(AddNode::class.java.name)

        for(nodeClass in nodeClasses) {
            val clazz = Class.forName(nodeClass.name)

            if(hasSuperclass(clazz, Node::class.java)) {
                nodesClasses.add(clazz as Class<out Node<*>>)
            }
        }

        println("Found ${nodesClasses.size} nodes")

        return nodesClasses
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