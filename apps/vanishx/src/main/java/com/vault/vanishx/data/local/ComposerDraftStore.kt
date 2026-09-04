package com.vault.vanishx.data.local

import com.vault.vanishx.data.local.db.MetaEntity
import com.vault.vanishx.data.local.db.VanishxLocalDatabase
import javax.inject.Inject
import javax.inject.Singleton

/** Per-room composer text drafts in SQLCipher `meta` (survives process death). */
@Singleton
class ComposerDraftStore @Inject constructor(
    private val localDatabase: VanishxLocalDatabase,
) {
    suspend fun get(roomId: String): String {
        if (roomId.isBlank()) return ""
        return localDatabase.withDatabase { db ->
            db.metaDao().get(metaKey(roomId))?.value.orEmpty()
        }
    }

    suspend fun set(roomId: String, draft: String) {
        if (roomId.isBlank()) return
        val trimmed = draft.take(MAX_DRAFT_CHARS)
        localDatabase.withDatabase { db ->
            val key = metaKey(roomId)
            if (trimmed.isBlank()) {
                db.metaDao().delete(key)
            } else {
                db.metaDao().upsert(MetaEntity(key = key, value = trimmed))
            }
        }
    }

    suspend fun clear(roomId: String) = set(roomId, "")

    companion object {
        private const val KEY_PREFIX = "composer_draft:"
        private const val MAX_DRAFT_CHARS = 8_000

        fun metaKey(roomId: String): String = "$KEY_PREFIX$roomId"
    }
}
