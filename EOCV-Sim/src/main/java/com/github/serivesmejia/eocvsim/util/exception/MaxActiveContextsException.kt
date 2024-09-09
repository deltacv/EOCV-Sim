package com.github.serivesmejia.eocvsim.util.exception

/**
 * Exception thrown when the maximum amount of active coroutine contexts is reached
 * Coroutine context are used in EOCV-Sim to handle possibly blocking operations
 * from user code (like running a pipeline) in a time-expiring way.
 * @param message the message of the exception
 */
class MaxActiveContextsException(message: String = "") : Exception(message)