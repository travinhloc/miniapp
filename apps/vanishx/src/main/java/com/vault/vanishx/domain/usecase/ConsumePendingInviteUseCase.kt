package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.invite.PendingInviteStore
import com.vault.vanishx.domain.model.InvitePendingCodec
import javax.inject.Inject

/**
 * Persists a deep-link invite for the Message Request gate. Does **not** auto-Accept
 * (story 14.4) — Home peeks and opens Join.
 */
class ConsumePendingInviteUseCase @Inject constructor(
    private val pendingInviteStore: PendingInviteStore,
) {
    fun captureIfInvite(uri: String?): Boolean {
        val canonical = InvitePendingCodec.canonicalize(uri) ?: return false
        pendingInviteStore.save(canonical)
        return true
    }
}
