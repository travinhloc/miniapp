package com.vault.vanishx.domain.usecase

import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import javax.inject.Inject

/** Patch local-only room prefs (mute / favorite / avatar / wallpaper). */
class UpdateRoomLocalPrefsUseCase @Inject constructor(
    private val mailboxRepository: MailboxRepository,
) {
    suspend fun setMuted(roomId: String, muted: Boolean): MailboxRoom =
        patch(roomId) { it.copy(muted = muted) }

    suspend fun setFavorite(roomId: String, favorite: Boolean): MailboxRoom =
        patch(roomId) { it.copy(favorite = favorite) }

    suspend fun setAvatarPath(roomId: String, path: String?): MailboxRoom =
        patch(roomId) { it.copy(avatarLocalPath = path) }

    suspend fun setWallpaperPath(roomId: String, path: String?): MailboxRoom =
        patch(roomId) { it.copy(wallpaperLocalPath = path) }

    private suspend fun patch(roomId: String, block: (MailboxRoom) -> MailboxRoom): MailboxRoom {
        val room = mailboxRepository.getRoom(roomId) ?: error("Room not found")
        val updated = block(room)
        mailboxRepository.upsertRoom(updated)
        return updated
    }
}
