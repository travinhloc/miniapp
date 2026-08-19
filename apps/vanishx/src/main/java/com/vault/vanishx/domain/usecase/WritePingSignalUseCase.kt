package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteRoomSignal
import com.vault.vanishx.domain.repository.IdentityRepository
import java.util.UUID
import javax.inject.Inject

/** Handshake Ping → RTDB `/signals` for FCM fan-out (Epic 15.3). */
class WritePingSignalUseCase @Inject constructor(
    private val remote: MailboxRemoteDataSource,
    private val identityRepository: IdentityRepository,
) {
    suspend operator fun invoke(roomId: String) {
        val me = identityRepository.ensureIdentity()
        remote.writeRoomSignal(
            roomId,
            RemoteRoomSignal(
                signalId = UUID.randomUUID().toString(),
                type = RemoteRoomSignal.TYPE_PING,
                fromPub = me.publicKeyBase64,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }
}
