package com.github.serivesmejia.eocvsim.tuner

import com.github.serivesmejia.eocvsim.util.loggerForThis
import java.util.HashMap

class TunableFieldAcceptorManager(private val acceptors: HashMap<Class<out TunableField<*>>, Class<out TunableFieldAcceptor>>) {

    val logger by loggerForThis()

    fun accept(clazz: Class<*>): Class<out TunableField<*>>? {
        for((fieldClass, acceptorClass) in acceptors) {
            //try getting a constructor for this acceptor
            val acceptorConstructor = try {
                acceptorClass.getConstructor() //get constructor with no params
            } catch(ex: NoSuchMethodException) {
                logger.error("TunableFieldAcceptor ${acceptorClass.typeName} doesn't implement a constructor with zero parameters", ex)
                continue
            }

            val acceptor = acceptorConstructor.newInstance() //create an instance of this acceptor

            if(acceptor.accept(clazz)) { //try accepting the given clazz type
                return fieldClass //wooo someone accepted our type! return to tell who did.
            }
        }

        return null //no one accepted our type... poor clazz :(
    }

}