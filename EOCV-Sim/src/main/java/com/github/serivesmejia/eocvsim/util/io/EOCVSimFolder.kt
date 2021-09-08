package com.github.serivesmejia.eocvsim.util.io

import com.github.serivesmejia.eocvsim.util.SysUtil
import java.io.File

object EOCVSimFolder : File(SysUtil.getAppData().absolutePath + separator + ".eocvsim") {

    val lock by lazy { lockDirectory() }

    val couldLock get() = lock != null && lock!!.isLocked

    init {
        mkdir()
    }

}