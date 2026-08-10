package com.vault.vanishx.data.remote

interface MediaStorageRemoteDataSource {
    suspend fun upload(roomId: String, messageId: String, attId: String, ciphertext: ByteArray)

    suspend fun download(
        roomId: String,
        messageId: String,
        attId: String,
        maxBytes: Long,
    ): ByteArray

    suspend fun delete(roomId: String, messageId: String, attId: String)

    suspend fun deleteRoomPrefix(roomId: String)
}
