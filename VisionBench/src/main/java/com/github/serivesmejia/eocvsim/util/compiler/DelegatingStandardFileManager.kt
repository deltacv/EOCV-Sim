/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.util.compiler

import java.io.File
import java.util.*
import javax.tools.ForwardingJavaFileManager
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.StandardJavaFileManager

open class DelegatingStandardFileManager(
    val delegate: StandardJavaFileManager
) : ForwardingJavaFileManager<StandardJavaFileManager>(delegate), StandardJavaFileManager {

    override fun getJavaFileObjectsFromFiles(files: MutableIterable<File>): MutableIterable<JavaFileObject> =
        delegate.getJavaFileObjectsFromFiles(files) ?: Collections.emptyList()

    override fun getJavaFileObjects(vararg files: File): MutableIterable<JavaFileObject> =
        delegate.getJavaFileObjects(*files) ?: Collections.emptyList()

    override fun getJavaFileObjects(vararg names: String): MutableIterable<JavaFileObject> =
        delegate.getJavaFileObjects(*names) ?: Collections.emptyList()

    override fun getJavaFileObjectsFromStrings(names: MutableIterable<String>): MutableIterable<JavaFileObject> =
        delegate.getJavaFileObjectsFromStrings(names) ?: Collections.emptyList()

    override fun setLocation(location: JavaFileManager.Location, files: MutableIterable<File>) =
        delegate.setLocation(location, files)

    override fun getLocation(location: JavaFileManager.Location): MutableIterable<File> =
        delegate.getLocation(location) ?: Collections.emptyList()

}
