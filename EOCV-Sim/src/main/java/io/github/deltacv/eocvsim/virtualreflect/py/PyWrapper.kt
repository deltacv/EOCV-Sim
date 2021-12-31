package io.github.deltacv.eocvsim.virtualreflect.py

import org.python.util.PythonInterpreter

interface PyWrapper {

    val name: String?
    val interpreter: PythonInterpreter

}