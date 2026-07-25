package com.vault.vanishx.data.local.db

import android.content.Context
import androidx.room.Room
import com.miniapp.core.common.DispatchersProvider
import com.vault.vanishx.domain.repository.LocalDatabaseWiper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Owns the SQLCipher-backed Room database lifecycle, including wipe for Panic later.
 */
@Singleton
class VanishxLocalDatabase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val passphraseStore: DatabasePassphraseStore,
    private val dispatchersProvider: DispatchersProvider,
) : LocalDatabaseWiper {

    private val mutex = Mutex()

    @Volatile
    private var database: VanishxDatabase? = null

    suspend fun <T> withDatabase(block: suspend (VanishxDatabase) -> T): T =
        withContext(dispatchersProvider.io) {
            mutex.withLock {
                val db = openLocked()
                ensureMetaLocked(db)
                block(db)
            }
        }

    override suspend fun wipe() = withContext(dispatchersProvider.io) {
        mutex.withLock {
            database?.close()
            database = null
            deleteDatabaseFiles()
            passphraseStore.clear()
        }
    }

    private fun openLocked(): VanishxDatabase {
        database?.let { return it }
        System.loadLibrary("sqlcipher")
        val passphrase = passphraseStore.getOrCreatePassphrase()
        val factory = SupportOpenHelperFactory(passphrase.copyOf(), null, false)
        return Room.databaseBuilder(
            context,
            VanishxDatabase::class.java,
            VanishxDatabase.NAME,
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
            .also { database = it }
    }

    private suspend fun ensureMetaLocked(db: VanishxDatabase) {
        if (db.metaDao().get(META_CRYPTO_SCHEME) == null) {
            db.metaDao().upsert(
                MetaEntity(key = META_CRYPTO_SCHEME, value = CRYPTO_SCHEME_VERSION),
            )
        }
    }

    private fun deleteDatabaseFiles() {
        context.deleteDatabase(VanishxDatabase.NAME)
        val base = context.getDatabasePath(VanishxDatabase.NAME)
        listOf("", "-shm", "-wal", "-journal").forEach { suffix ->
            File(base.path + suffix).takeIf { it.exists() }?.delete()
        }
    }

    companion object {
        const val META_CRYPTO_SCHEME = "crypto_scheme_version"
        const val CRYPTO_SCHEME_VERSION = "1"
    }
}
