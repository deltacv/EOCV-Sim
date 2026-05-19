/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.sandbox.restrictions

import org.objectweb.asm.*

class MethodCallByteCodeChecker(
    bytecode: ByteArray,
    private val methodBlacklist: Set<String>
) {

    init {
        val classReader = ClassReader(bytecode)
        classReader.accept(MethodCheckClassVisitor(), 0)
    }

    private inner class MethodCheckClassVisitor : ClassVisitor(Opcodes.ASM9) {

        private lateinit var currentClassName: String

        override fun visit(
            version: Int,
            access: Int,
            name: String?,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            super.visit(version, access, name, signature, superName, interfaces)

            currentClassName = name!!.replace('/', '.')
        }

        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {

            val mv = super.visitMethod(
                access,
                name,
                descriptor,
                signature,
                exceptions
            )

            return MethodCheckMethodVisitor(
                mv,
                currentClassName,
                name!!
            )
        }
    }

    private inner class MethodCheckMethodVisitor(
        mv: MethodVisitor?,
        private val currentClassName: String,
        private val currentMethodName: String
    ) : MethodVisitor(Opcodes.ASM9, mv) {

        private fun checkMethod(owner: String, name: String) {
            val methodIdentifier =
                "${owner.replace('/', '.')}#$name"

            if(methodBlacklist.contains(methodIdentifier)) {
                throw IllegalAccessError(
                    "Unauthorized method call of $methodIdentifier " +
                            "from dynamic code at " +
                            "$currentClassName#$currentMethodName"
                )
            }
        }

        override fun visitMethodInsn(
            opcode: Int,
            owner: String?,
            name: String?,
            descriptor: String?,
            isInterface: Boolean
        ) {
            super.visitMethodInsn(
                opcode,
                owner,
                name,
                descriptor,
                isInterface
            )

            checkMethod(owner!!, name!!)
        }

        override fun visitInvokeDynamicInsn(
            name: String?,
            descriptor: String?,
            bootstrapMethodHandle: Handle?,
            bootstrapMethodArguments: Array<out Any>?
        ) {

            super.visitInvokeDynamicInsn(
                name,
                descriptor,
                bootstrapMethodHandle,
                bootstrapMethodArguments
            )

            /*
             * Check bootstrap method itself
             */
            bootstrapMethodHandle?.let {
                checkMethod(it.owner, it.name)
            }

            /*
             * Check bootstrap arguments
             *
             * Lambdas and method references commonly store
             * target methods here as Handle instances.
             */
            bootstrapMethodArguments?.forEach { arg ->

                when(arg) {

                    is Handle -> {
                        checkMethod(arg.owner, arg.name)
                    }

                    is ConstantDynamic -> {

                        /*
                         * ConstantDynamic also has a bootstrap
                         * method and bootstrap args.
                         */

                        val bsm = arg.bootstrapMethod

                        checkMethod(
                            bsm.owner,
                            bsm.name
                        )

                        for(i in 0 until arg.bootstrapMethodArgumentCount) {

                            val nestedArg =
                                arg.getBootstrapMethodArgument(i)

                            if(nestedArg is Handle) {
                                checkMethod(
                                    nestedArg.owner,
                                    nestedArg.name
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}