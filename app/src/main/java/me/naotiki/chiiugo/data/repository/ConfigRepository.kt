package me.naotiki.chiiugo.data.repository

import kotlinx.coroutines.flow.Flow
import me.naotiki.chiiugo.ui.component.Config

interface ConfigRepository {
    val configFlow: Flow<Config>

    suspend fun updateImageSize(size: Float)
    suspend fun updateMoveSpeed(speedMs: Int)
    suspend fun updateTransparency(transparency: Float)
    suspend fun updateAreaOffset(offset: Pair<Float, Float>)
    suspend fun updateAreaSize(size: Pair<Float, Float>)
    suspend fun updateBlockingTouch(blocking: Boolean)
    suspend fun updateConfig(config: Config)
}

