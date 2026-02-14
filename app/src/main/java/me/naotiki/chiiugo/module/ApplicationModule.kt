package me.naotiki.chiiugo.module

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.naotiki.chiiugo.data.llm.LlmSettingsRepository
import me.naotiki.chiiugo.data.llm.LlmSettingsRepositoryImpl
import me.naotiki.chiiugo.data.repository.ConfigRepository
import me.naotiki.chiiugo.data.repository.InstalledAppRepository
import me.naotiki.chiiugo.data.repository.impl.ConfigRepositoryImpl
import me.naotiki.chiiugo.data.repository.impl.InstalledAppRepositoryImpl
import me.naotiki.chiiugo.domain.comment.KoogMascotCommentGenerator
import me.naotiki.chiiugo.domain.comment.KoogPromptClient
import me.naotiki.chiiugo.domain.comment.KoogPromptClientImpl
import me.naotiki.chiiugo.domain.comment.MascotCommentGenerator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ApplicationModule {
    @Binds
    @Singleton
    abstract fun bindInstalledAppRepository(impl: InstalledAppRepositoryImpl): InstalledAppRepository

    @Binds
    @Singleton
    abstract fun bindConfigRepository(impl: ConfigRepositoryImpl): ConfigRepository

    @Binds
    @Singleton
    abstract fun bindLlmSettingsRepository(impl: LlmSettingsRepositoryImpl): LlmSettingsRepository

    @Binds
    @Singleton
    abstract fun bindMascotCommentGenerator(impl: KoogMascotCommentGenerator): MascotCommentGenerator

    @Binds
    @Singleton
    abstract fun bindKoogPromptClient(impl: KoogPromptClientImpl): KoogPromptClient
}
