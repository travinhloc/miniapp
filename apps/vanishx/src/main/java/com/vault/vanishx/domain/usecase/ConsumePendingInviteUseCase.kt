package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.invite.PendingInviteStore
import com.vault.vanishx.domain.model.InviteUriCodec
import com.vault.vanishx.domain.model.MailboxRoom
import timber.log.Timber
import javax.inject.Inject

/**
 * After identity bootstrap: join any pending invite saved from a cold-start deep link.
 */
class ConsumePendingInviteUseCase @Inject constructor(
    private val pendingInviteStore: PendingInviteStore,
    private val joinRoom: JoinRoomUseCase,
) {
    suspend operator fun invoke(): MailboxRoom? {
        val raw = pendingInviteStore.consume() ?: return null
        return runCatching {
            joinRoom(raw)
        }.onFailure { e ->
            Timber.w(e, "Pending invite join failed; restoring URI")
            pendingInviteStore.save(raw)
        }.getOrNull()
    }

    fun captureIfInvite(uri: String?): Boolean {
        val trimmed = uri?.trim().orEmpty()
        val valid = trimmed.isNotEmpty() && InviteUriCodec.parse(trimmed) != null
        if (valid) {
            pendingInviteStore.save(trimmed)
        }
        return valid
    }
}
