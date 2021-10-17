package com.github.serivesmejia.eocvsim.util

import com.github.serivesmejia.eocvsim.tuner.TunableField
import com.github.serivesmejia.eocvsim.tuner.TunableFieldAcceptor
import com.github.serivesmejia.eocvsim.tuner.scanner.RegisterTunableField
import com.qualcomm.robotcore.opmode.Disabled
import com.qualcomm.robotcore.util.ElapsedTime
import io.github.classgraph.ClassGraph
import kotlinx.coroutines.*
import org.openftc.easyopencv.OpenCvPipeline

class ClasspathScan {

    companion object {
        const val TAG = "ClasspathScan"

        val ignoredPackages = arrayOf(
            "java",
            "kotlin",
            "org.opencv",
            "imgui",
            "io.github.classgraph",
            "io.github.deltacv",
            "com.github.serivesmejia.eocvsim.pipeline",
            "org.openftc",
            "org.lwjgl"
        )
    }

    lateinit var scanResult: ScanResult
        private set

    private lateinit var scanResultJob: Job

    @Suppress("UNCHECKED_CAST")
    fun scan() {
        val timer = ElapsedTime()
        val classGraph = ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .rejectPackages(*ignoredPackages)

        Log.info(TAG, "Starting to scan classpath...")

        val scanResult = classGraph.scan()

        Log.info(TAG, "ClassGraph finished scanning (took ${timer.seconds()}s)")
        
        val tunableFieldClassesInfo = scanResult.getClassesWithAnnotation(RegisterTunableField::class.java.name)
        val pipelineClassesInfo = scanResult.getSubclasses(OpenCvPipeline::class.java.name)

        val pipelineClasses = mutableListOf<Class<out OpenCvPipeline>>()

        for(pipelineClassInfo in pipelineClassesInfo) {
            val clazz = Class.forName(pipelineClassInfo.name)

            if(ReflectUtil.hasSuperclass(clazz, OpenCvPipeline::class.java)) {
                if(clazz.isAnnotationPresent(Disabled::class.java)) {
                    Log.info(TAG, "Found @Disabled pipeline ${clazz.typeName}")
                } else {
                    Log.info(TAG, "Found pipeline ${clazz.typeName}")
                    pipelineClasses.add(clazz as Class<out OpenCvPipeline>)
                }
            }
        }

        Log.blank()
        Log.info(TAG, "Found ${pipelineClasses.size} pipelines")
        Log.blank()

        val tunableFieldClasses = mutableListOf<Class<out TunableField<*>>>()
        val tunableFieldAcceptorClasses = mutableMapOf<Class<out TunableField<*>>, Class<out TunableFieldAcceptor>>()

        for(tunableFieldClassInfo in tunableFieldClassesInfo) {
            val clazz = Class.forName(tunableFieldClassInfo.name)

            if(ReflectUtil.hasSuperclass(clazz, TunableField::class.java)) {
                val tunableFieldClass = clazz as Class<out TunableField<*>>

                tunableFieldClasses.add(tunableFieldClass)
                Log.info(TAG, "Found tunable field ${clazz.typeName}")

                for(subclass in clazz.declaredClasses) {
                    if(ReflectUtil.hasSuperclass(subclass, TunableFieldAcceptor::class.java)) {
                        tunableFieldAcceptorClasses[tunableFieldClass] = subclass as Class<out TunableFieldAcceptor>
                        Log.info(TAG, "Found acceptor for this tunable field, ${clazz.typeName}")
                        break
                    }
                }
            }
        }

        Log.blank()
        Log.info(TAG, "Found ${tunableFieldClasses.size} tunable fields and ${tunableFieldAcceptorClasses.size} acceptors")
        Log.blank()

        Log.info(TAG, "Finished scanning (took ${timer.seconds()}s)")

        this.scanResult = ScanResult(
            pipelineClasses.toTypedArray(),
            tunableFieldClasses.toTypedArray(),
            tunableFieldAcceptorClasses.toMap()
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun asyncScan() {
        scanResultJob = GlobalScope.launch(Dispatchers.IO) {
            scan()
        }
    }

    fun join() = runBlocking {
        scanResultJob.join()
    }

}

data class ScanResult(
    val pipelineClasses: Array<Class<out OpenCvPipeline>>,
    val tunableFieldClasses: Array<Class<out TunableField<*>>>,
    val tunableFieldAcceptorClasses: Map<Class<out TunableField<*>>, Class<out TunableFieldAcceptor>>
)