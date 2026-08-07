package com.vault.vanishx.data.repository

import com.vault.vanishx.data.local.db.BlockedPeerEntity
import com.vault.vanishx.data.local.db.VanishxLocalDatabase
import com.vault.vanishx.domain.repository.BlockRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockRepositoryImpl @Inject constructor(
    private val localDatabase: VanishxLocalDatabase,
) : BlockRepository {

    override suspend fun isBlocked(peerPub: String): Boolean =
        localDatabase.withDatabase { db ->
            db.blockedPeerDao().get(peerPub) != null
        }

    override suspend fun block(peerPub: String, blockedAt: Long) {
        localDatabase.withDatabase { db ->
            db.blockedPeerDao().upsert(
                BlockedPeerEntity(peerPub = peerPub, blockedAt = blockedAt),
            )
        }
    }

    override suspend fun unblock(peerPub: String) {
        localDatabase.withDatabase { db ->
            db.blockedPeerDao().delete(peerPub)
        }
    }
}
