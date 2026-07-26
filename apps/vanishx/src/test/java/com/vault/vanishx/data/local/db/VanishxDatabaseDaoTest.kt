package com.vault.vanishx.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Schema / DAO smoke on plain in-memory Room.
 * Production path uses SQLCipher (see [VanishxLocalDatabase]); native libs are not loaded in JVM unit tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class VanishxDatabaseDaoTest {

    private lateinit var database: VanishxDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, VanishxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `write and read meta row`() {
        runBlocking {
            database.metaDao().upsert(MetaEntity(key = "crypto_scheme_version", value = "1"))
            database.metaDao().get("crypto_scheme_version")?.value shouldBe "1"
        }
    }

    @Test
    fun `write and read room row`() {
        runBlocking {
            database.mailboxRoomDao().upsert(
                MailboxRoomEntity(
                    id = "room-1",
                    roomKey = "key-1",
                    createdAt = 1L,
                    expiresAt = 2L,
                    title = "temp",
                    status = "active",
                    role = "creator",
                    peerPub = "peer-pub",
                ),
            )
            val rooms = database.mailboxRoomDao().getByStatus("active")
            rooms.size shouldBe 1
            rooms.first().id shouldBe "room-1"
            rooms.first().peerPub shouldBe "peer-pub"

            database.blockedPeerDao().upsert(
                BlockedPeerEntity(peerPub = "peer-pub", blockedAt = 9L),
            )
            database.blockedPeerDao().get("peer-pub")?.blockedAt shouldBe 9L
        }
    }
}
