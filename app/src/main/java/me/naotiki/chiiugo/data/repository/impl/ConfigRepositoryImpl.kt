package me.naotiki.chiiugo.data.repository.impl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.naotiki.chiiugo.data.repository.ConfigRepository
import me.naotiki.chiiugo.ui.component.Config
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mascot_config")

@Singleton
class ConfigRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ConfigRepository {

    private object PreferenceKeys {
        val IMAGE_SIZE = floatPreferencesKey("image_size")
        val MOVE_SPEED_MS = intPreferencesKey("move_speed_ms")
        val TRANSPARENCY = floatPreferencesKey("transparency")
        val AREA_OFFSET_X = floatPreferencesKey("area_offset_x")
        val AREA_OFFSET_Y = floatPreferencesKey("area_offset_y")
        val AREA_SIZE_X = floatPreferencesKey("area_size_x")
        val AREA_SIZE_Y = floatPreferencesKey("area_size_y")
        val BLOCKING_TOUCH = booleanPreferencesKey("blocking_touch")
    }

    private val defaultConfig = Config()

    override val configFlow: Flow<Config> = context.dataStore.data.map { preferences ->
        Config(
            imageSize = preferences[PreferenceKeys.IMAGE_SIZE] ?: defaultConfig.imageSize,
            areaOffset = (preferences[PreferenceKeys.AREA_OFFSET_X] ?: defaultConfig.areaOffset.first) to
                    (preferences[PreferenceKeys.AREA_OFFSET_Y] ?: defaultConfig.areaOffset.second),
            areaSize = (preferences[PreferenceKeys.AREA_SIZE_X] ?: defaultConfig.areaSize.first) to
                    (preferences[PreferenceKeys.AREA_SIZE_Y] ?: defaultConfig.areaSize.second),
            moveSpeedMs = preferences[PreferenceKeys.MOVE_SPEED_MS] ?: defaultConfig.moveSpeedMs,
            transparency = preferences[PreferenceKeys.TRANSPARENCY] ?: defaultConfig.transparency,
            blockingTouch = preferences[PreferenceKeys.BLOCKING_TOUCH] ?: defaultConfig.blockingTouch
        )
    }

    override suspend fun updateImageSize(size: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.IMAGE_SIZE] = size
        }
    }

    override suspend fun updateMoveSpeed(speedMs: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.MOVE_SPEED_MS] = speedMs
        }
    }

    override suspend fun updateTransparency(transparency: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.TRANSPARENCY] = transparency
        }
    }

    override suspend fun updateAreaOffset(offset: Pair<Float, Float>) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.AREA_OFFSET_X] = offset.first
            preferences[PreferenceKeys.AREA_OFFSET_Y] = offset.second
        }
    }

    override suspend fun updateAreaSize(size: Pair<Float, Float>) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.AREA_SIZE_X] = size.first
            preferences[PreferenceKeys.AREA_SIZE_Y] = size.second
        }
    }

    override suspend fun updateBlockingTouch(blocking: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.BLOCKING_TOUCH] = blocking
        }
    }

    override suspend fun updateConfig(config: Config) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.IMAGE_SIZE] = config.imageSize
            preferences[PreferenceKeys.MOVE_SPEED_MS] = config.moveSpeedMs
            preferences[PreferenceKeys.TRANSPARENCY] = config.transparency
            preferences[PreferenceKeys.AREA_OFFSET_X] = config.areaOffset.first
            preferences[PreferenceKeys.AREA_OFFSET_Y] = config.areaOffset.second
            preferences[PreferenceKeys.AREA_SIZE_X] = config.areaSize.first
            preferences[PreferenceKeys.AREA_SIZE_Y] = config.areaSize.second
            preferences[PreferenceKeys.BLOCKING_TOUCH] = config.blockingTouch
        }
    }
}

