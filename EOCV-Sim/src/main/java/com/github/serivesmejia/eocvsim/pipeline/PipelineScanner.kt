/*
 * Copyright (c) 2021 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.serivesmejia.eocvsim.pipeline

import com.github.serivesmejia.eocvsim.util.Log
import com.github.serivesmejia.eocvsim.util.ReflectUtil
import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import org.openftc.easyopencv.OpenCvPipeline

@Suppress("UNCHECKED_CAST")
class PipelineScanner(val scanInPackage: String = "org.firstinspires") {

    fun lookForPipelines(callback: (Class<OpenCvPipeline>) -> Unit) {
        Log.info("PipelineScanner", "Scanning for pipelines...")
        val scanResult = scanClasspath(scanInPackage)

        //iterate over the results of the scan
        for (routeClassInfo in scanResult.allClasses) {

            val foundClass: Class<*> = try {
                Class.forName(routeClassInfo.name)
            } catch (e1: ClassNotFoundException) {
                e1.printStackTrace()
                continue  //continue because we couldn't get the class...
            }

            if(ReflectUtil.hasSuperclass(foundClass, OpenCvPipeline::class.java)) {
                Log.info("PipelineScanner", "Found pipeline class ${foundClass.canonicalName}")
                callback(foundClass as Class<OpenCvPipeline>);
            }

        }
    }

    fun scanClasspath(inPackage: String): ScanResult {
        //Scan for all classes in the specified package
        val classGraph = ClassGraph().enableAllInfo().acceptPackages(inPackage)
        return classGraph.scan()
    }

}