package com.github.serivesmejia.eocvsim.util.exception.handling

import com.github.serivesmejia.eocvsim.util.Log
import kotlin.system.exitProcess

class EOCVSimUncaughtExceptionHandler private constructor() : Thread.UncaughtExceptionHandler {

    companion object {
        const val MAX_UNCAUGHT_EXCEPTIONS_BEFORE_CRASH = 3

        @JvmStatic fun register() {
            Thread.setDefaultUncaughtExceptionHandler(EOCVSimUncaughtExceptionHandler())
        }

        private const val TAG = "EOCVSimUncaughtExceptionHandler"
    }

    private var uncaughtExceptionsCount = 0

    override fun uncaughtException(t: Thread, e: Throwable) {
        //we don't want the whole app to crash on a simple interrupted exception right?
        if(e is InterruptedException) {
            Log.warn(TAG, "Uncaught InterruptedException thrown in Thread ${t.name}, it will be interrupted", e)
            t.interrupt()
            return
        }

        uncaughtExceptionsCount++

        Log.error(TAG,"Uncaught exception thrown in \"${t.name}\" thread", e)
        Log.blank()

        //Exit if uncaught exception happened in the main thread
        //since we would be basically in a deadlock state if that happened
        //or if we have a lotta uncaught exceptions.
        if(t.name.equals("main", true) || uncaughtExceptionsCount > MAX_UNCAUGHT_EXCEPTIONS_BEFORE_CRASH) {
            CrashReport(e).saveCrashReport()

            Log.warn(TAG, "If this error persists, open an issue on EOCV-Sim's GitHub attaching the crash report file.")
            Log.blank()
            Log.warn(TAG, "The application will exit now (exit code 1)")

            exitProcess(1)
        } else {
            //if not, eocv sim might still be working (i.e a crash from a MatPoster thread)
            //so we might not need to exit in this point, but we'll need to send a warning
            //to the user
            Log.warn(TAG, "If this error persists, open an issue on EOCV-Sim's GitHub.")
            Log.blank()
            Log.warn(TAG, "The application might not work as expected from this point")
        }
    }

}