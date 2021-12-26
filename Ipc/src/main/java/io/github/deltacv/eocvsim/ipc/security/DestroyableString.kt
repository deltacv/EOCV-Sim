package io.github.deltacv.eocvsim.ipc.security

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.*

class DestroyableString(val chars: ByteArray) {

    @OptIn(DelicateCoroutinesApi::class)
    companion object {
        private var secureRandom: SecureRandom? = null
        private val random = Random()
        
        private val allowedCharacters = "ABCDEFGHIJKLMNOPQRSTUWXYZabcdefghijklmnopqrstuwxyz0123456789-_+=$&#@#"

        init {
            GlobalScope.launch(Dispatchers.IO) {
                secureRandom = SecureRandom()
                val seed = secureRandom!!.nextLong()

                synchronized(random) {
                    random.setSeed(seed)
                }
            }
        }
        
        fun random(length: Int? = null): DestroyableString {
            val byteBuffer: ByteBuffer
            
            synchronized(random) {
                val characters = length ?: random.nextInt(240) + 16
                byteBuffer = ByteBuffer.allocate(characters * 2)

                repeat(characters) {
                    byteBuffer.putChar(
                        allowedCharacters[
                                random.nextInt(allowedCharacters.length - 1)
                        ]
                    )
                }
            }

            return DestroyableString(byteBuffer.array())
        }
    }

    fun destroy() {
        for(i in chars.indices) {
            chars[i] = 0x00
        }
    }

    override fun toString() = String(chars)

}