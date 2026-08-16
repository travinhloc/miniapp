package com.vault.vanishx.di.modules.main

import com.vault.vanishx.data.invite.ClipboardInviteAccess
import com.vault.vanishx.data.invite.ClipboardInviteReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
abstract class MainActivityModule {
    @Binds
    abstract fun bindClipboardInviteAccess(impl: ClipboardInviteReader): ClipboardInviteAccess
}
