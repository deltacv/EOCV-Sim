/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.util

import com.github.serivesmejia.eocvsim.util.orchestration.Orchestrable
import com.github.serivesmejia.eocvsim.util.orchestration.Orchestrator
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.util.ElapsedTime
import io.github.classgraph.ClassGraph
import org.deltacv.common.util.loggerForThis
import org.firstinspires.ftc.vision.VisionProcessor
import org.openftc.easyopencv.OpenCvPipeline
import java.net.URI

class InitClasspathScan : ClasspathScan(), Orchestrable {
    override fun wire(orchestrator: Orchestrator) =
        orchestrator.register(this) {
            phase(Orchestrator.Phase.INIT) {
                target { scan(scanForEmbeddedPlugins = true) }
            }
        }
}

/**
 * Classpath scanner using ClassGraph.
 * It scans for OpenCvPipelines, OpModes, VisionProcessors and TunableFields
 */
open class ClasspathScan {

    companion object {
        val ignoredPackages = arrayOf(
            "java",
            "kotlin",
            "org.opencv",
            "imgui",
            "io.github.classgraph",
            "org.deltacv",
            "com.github.serivesmejia.eocvsim.pipeline",
            "org.firstinspires.ftc.vision",
            "org.lwjgl",
            "org.apache",
            "org.codehaus",
            "com.google"
        )
    }

    val logger by loggerForThis()

    var scanResult: EOCVSimScanResult? = null
        private set

    /**
     * Perform the classpath scan using ClassGraph, surprisingly fast due to
     * the miracles of that library, as it uses bytecode scanning instead of reflection.
     *
     * This method will scan for OpenCvPipelines, OpModes, VisionProcessors and TunableFields
     * @param jarFile the jar file to scan, if null, the classpath will be scanned
     * @param classLoader the classloader to use, if null, the system classloader will be used
     * @param addProcessorsAsPipelines if true, VisionProcessors will be wrapped as pipelines
     */
    @Suppress("UNCHECKED_CAST")
    fun scan(
        jarFile: String? = null,
        classLoader: ClassLoader? = null,
        addProcessorsAsPipelines: Boolean = true,
        scanForEmbeddedPlugins: Boolean = false
    ): EOCVSimScanResult {
        val timer = ElapsedTime()
        val classGraph = ClassGraph()
            .enableClassInfo()
            // .verbose()
            .enableAnnotationInfo()
            .rejectPackages(*ignoredPackages)

        if(scanForEmbeddedPlugins) {
            classGraph.acceptPaths("/embedded_plugins")
        }

        if (jarFile != null) {
            classGraph.overrideClasspath("$jarFile!/")
            logger.info("Starting to scan for classes in $jarFile...")
        } else {
            logger.info("Starting to scan classpath...")
        }

        if (classLoader != null) {
            classGraph.overrideClassLoaders(classLoader)
        }

        val pipelineClasses = mutableListOf<Class<*>>()
        val embeddedPlugins = mutableListOf<URI>()

        classGraph.scan().use { scanResult ->

            logger.info("ClassGraph finished scanning (took ${timer.seconds()}s)")

            // i...don't even know how to name this, sorry, future readers
            // but classgraph for some reason does not have a recursive search for subclasses...
            fun searchPipelinesOfSuperclass(superclass: String) {
                logger.trace("searchPipelinesOfSuperclass: {}", superclass)

                val superclassClazz = if (classLoader != null) {
                    classLoader.loadClass(superclass)
                } else Class.forName(superclass)

                val pipelineClassesInfo = if (superclassClazz.isInterface)
                    scanResult.getClassesImplementing(superclass)
                else scanResult.getSubclasses(superclass)

                for (pipelineClassInfo in pipelineClassesInfo) {
                    logger.trace("pipelineClassInfo: {}", pipelineClassInfo.name)

                    for (pipelineSubclassInfo in pipelineClassInfo.subclasses) {
                        searchPipelinesOfSuperclass(pipelineSubclassInfo.name) // naming is my passion
                    }

                    if (pipelineClassInfo.isAbstract || pipelineClassInfo.isInterface) {
                        continue // nope'd outta here
                    }

                    val clazz = if (classLoader != null) {
                        classLoader.loadClass(pipelineClassInfo.name)
                    } else Class.forName(pipelineClassInfo.name)

                    logger.trace("class {} super {}", clazz.typeName, clazz.superclass.typeName)

                    if (!pipelineClasses.contains(clazz) && ReflectUtil.hasSuperclass(clazz, superclassClazz)) {
                        if (clazz.isAnnotationPresent(Disabled::class.java)) {
                            logger.debug("Found @Disabled pipeline ${clazz.typeName}")
                        } else {
                            logger.info("Found pipeline ${clazz.typeName}")
                            pipelineClasses.add(clazz)
                        }
                    }
                }
            }

            // start recursive hell
            searchPipelinesOfSuperclass(OpenCvPipeline::class.java.name)

            if (jarFile != null) {
                // When specifying a jar file, we consequently remove
                // EOCV-Sim from the scan classpath, ClassGraph does not know
                // that OpMode and LinearOpMode are subclasses of OpenCvPipeline,
                // so we have to scan them manually...
                searchPipelinesOfSuperclass(OpMode::class.java.name)
                searchPipelinesOfSuperclass(LinearOpMode::class.java.name)
            }

            if (addProcessorsAsPipelines) {
                logger.info("Searching for VisionProcessors...")
                searchPipelinesOfSuperclass(VisionProcessor::class.java.name)
            }

            logger.info("Found ${pipelineClasses.size} pipelines")

            if (scanForEmbeddedPlugins) {
                scanResult
                    .getResourcesWithExtension("jar")
                    .filter { it.path.startsWith("embedded_plugins/") }
                    .forEach {
                        logger.info("Found embedded plugin: {}", it.path)
                        embeddedPlugins.add(it.uri)
                    }

                logger.info("Found {} embedded plugins", embeddedPlugins.size)
            }
        }

        logger.info("Finished scanning (took ${timer.seconds()}s)")

        this.scanResult = EOCVSimScanResult(
            pipelineClasses, embeddedPlugins
        )

        return this.scanResult!!
    }

}

/**
 * Result of the classpath scan
 * @param pipelineClasses the found OpenCvPipelines
 */
data class EOCVSimScanResult(
    val pipelineClasses: List<Class<*>>,
    val embeddedPlugins: List<URI>
)
