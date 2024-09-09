/*
 * Copyright (c) 2024 Sebastian Erives
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

package io.github.deltacv.eocvsim.sandbox.restrictions

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class MethodCallByteCodeChecker(
    bytecode: ByteArray,
    val methodBlacklist: Set<String>
) {

    init {
        // Create a ClassReader to read the bytecode
        val classReader = ClassReader(bytecode)
        // Accept the ClassVisitor to visit the class
        classReader.accept(MethodCheckClassVisitor(), 0)
    }

    private inner class MethodCheckClassVisitor : ClassVisitor(Opcodes.ASM9) {
        lateinit var currentClassName: String

        override fun visit(
            version: Int,
            access: Int,
            name: String?,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            super.visit(version, access, name, signature, superName, interfaces)
            currentClassName = name!!.replace("/", ".")
        }

        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<String>?
        ): MethodVisitor {
            val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
            return MethodCheckMethodVisitor(mv, currentClassName, name!!)
        }
    }

    private inner class MethodCheckMethodVisitor(mv: MethodVisitor?,
                                                 val currentClassName: String, val currentMethodName: String) : MethodVisitor(Opcodes.ASM9, mv) {
        override fun visitMethodInsn(
            opcode: Int,
            owner: String?,
            name: String?,
            descriptor: String?,
            isInterface: Boolean
        ) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            val methodIdentifier = "${owner!!.replace("/", ".")}#$name"

            if (methodBlacklist.contains(methodIdentifier)) {
                throw IllegalAccessError("Unauthorized method call of $methodIdentifier from dynamic code at $currentClassName#$currentMethodName")
            }
        }
    }

}