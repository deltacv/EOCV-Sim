package io.github.deltacv.eocvsim.pipeline.js

import org.mozilla.javascript.NativeArray

object JavascriptUtilContext {

    fun callMethodWith(
        className: String, methodName: String,
        methodParameterTypes: NativeArray,
        methodParameterValues: NativeArray
    ): Any? {
        val clazz = Class.forName(className)

        val methodParameterTypesClasses = methodParameterTypes.map {
            Class.forName(it as String)
        }.toTypedArray()

        val methodParameterJavaValues = methodParameterValues.toTypedArray()

        val method = clazz.getMethod(methodName, *methodParameterTypesClasses)

        return method.invoke(null, methodParameterJavaValues)
    }

}