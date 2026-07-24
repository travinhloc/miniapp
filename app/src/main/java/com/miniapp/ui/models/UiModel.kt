package com.miniapp.ui.models

import com.miniapp.domain.models.Model

data class UiModel(
    val id: Int
)

fun Model.toUiModel() = UiModel(id = id ?: -1)
