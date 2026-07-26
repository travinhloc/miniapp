package com.vault.vanishx.data.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomSecretsGenerator @Inject constructor() {

    private val random = SecureRandom()

    fun newRoomId(): String = encode(randomBytes(ROOM_ID_ENTROPY_BYTES)).take(ROOM_ID_LENGTH)

    fun newRoomKey(): String = encode(randomBytes(ROOM_KEY_BYTES))

    private fun randomBytes(size: Int): ByteArray =
        ByteArray(size).also { random.nextBytes(it) }

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private companion object {
        const val ROOM_ID_ENTROPY_BYTES = 16
        const val ROOM_KEY_BYTES = 32
        const val ROOM_ID_LENGTH = 22
    }
}
