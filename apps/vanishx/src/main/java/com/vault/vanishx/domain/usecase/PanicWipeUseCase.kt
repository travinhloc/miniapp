package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.crypto.IdentityKeyStore
import com.vault.vanishx.data.invite.PendingInviteStore
import com.vault.vanishx.data.security.AppLockSession
import com.vault.vanishx.data.security.SecurityPinStore
import com.vault.vanishx.domain.repository.LocalDatabaseWiper
import timber.log.Timber
import javax.inject.Inject

/**
 * Panic wipe: SQLCipher DB + passphrase, identity keyset, pending invite, PIN store → fresh identity.
 */
@Suppress("LongParameterList")
class PanicWipeUseCase @Inject constructor(
    private val localDatabaseWiper: LocalDatabaseWiper,
    private val identityKeyStore: IdentityKeyStore,
    private val pendingInviteStore: PendingInviteStore,
    private val securityPinStore: SecurityPinStore,
    private val appLockSession: AppLockSession,
    private val ensureIdentity: EnsureIdentityUseCase,
    private val wipeRoomMedia: WipeRoomMediaUseCase,
) {
    suspend operator fun invoke() {
        Timber.w("Panic wipe starting")
        localDatabaseWiper.wipe()
        wipeRoomMedia.localAll()
        identityKeyStore.clear()
        pendingInviteStore.clear()
        securityPinStore.clearAll()
        appLockSession.unlock()
        ensureIdentity()
        Timber.w("Panic wipe complete — fresh identity")
    }
}
