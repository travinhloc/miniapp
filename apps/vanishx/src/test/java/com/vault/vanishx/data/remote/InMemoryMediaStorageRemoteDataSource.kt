package com.vault.vanishx.data.remote

class InMemoryMediaStorageRemoteDataSource : MediaStorageRemoteDataSource {
    private val blobs = linkedMapOf<String, ByteArray>()

    override suspend fun upload(
        roomId: String,
        messageId: String,
        attId: String,
        ciphertext: ByteArray,
    ) {
        blobs[key(roomId, messageId, attId)] = ciphertext.copyOf()
    }

    override suspend fun download(
        roomId: String,
        messageId: String,
        attId: String,
        maxBytes: Long,
    ): ByteArray {
        val bytes = blobs[key(roomId, messageId, attId)]
            ?: error("Missing blob")
        require(bytes.size <= maxBytes) { "Blob too large" }
        return bytes.copyOf()
    }

    override suspend fun delete(roomId: String, messageId: String, attId: String) {
        blobs.remove(key(roomId, messageId, attId))
    }

    override suspend fun deleteRoomPrefix(roomId: String) {
        val prefix = "rooms/$roomId/att/"
        blobs.keys.filter { it.startsWith(prefix) }.toList().forEach { blobs.remove(it) }
    }

    private fun key(roomId: String, messageId: String, attId: String) =
        "rooms/$roomId/att/$messageId/$attId"
}
