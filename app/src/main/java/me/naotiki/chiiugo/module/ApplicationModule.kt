package me.naotiki.chiiugo.module

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import android.content.Context
import me.naotiki.chiiugo.data.llm.LlmSettingsRepository
import me.naotiki.chiiugo.data.llm.LlmSettingsRepositoryImpl
import me.naotiki.chiiugo.data.repository.ConfigRepository
import me.naotiki.chiiugo.data.repository.InstalledAppRepository
import me.naotiki.chiiugo.data.repository.impl.ConfigRepositoryImpl
import me.naotiki.chiiugo.data.repository.impl.InstalledAppRepositoryImpl
import me.naotiki.chiiugo.domain.comment.KoogMascotCommentGenerator
import me.naotiki.chiiugo.domain.comment.KoogPromptClient
import me.naotiki.chiiugo.domain.comment.KoogPromptClientImpl
import me.naotiki.chiiugo.domain.comment.MascotCommentOrchestrator
import me.naotiki.chiiugo.domain.comment.MascotCommentGenerator
import me.naotiki.chiiugo.domain.context.ContextEventRepository
import me.naotiki.chiiugo.domain.screen.ScreenCaptureBridge
import me.naotiki.chiiugo.domain.screen.ScreenCaptureController
import me.naotiki.chiiugo.domain.screen.ScreenCaptureManager
import me.naotiki.chiiugo.domain.screen.ScreenCaptureSource
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

    companion object {
        @Provides
        @Singleton
        fun provideScreenCaptureManager(
            @ApplicationContext context: Context
        ): ScreenCaptureManager {
            return ScreenCaptureManager(context)
        }

        @Provides
        @Singleton
        fun provideScreenCaptureBridge(): ScreenCaptureBridge {
            return ScreenCaptureBridge()
        }

        @Provides
        @Singleton
        fun provideScreenCaptureSource(bridge: ScreenCaptureBridge): ScreenCaptureSource = bridge

        @Provides
        @Singleton
        fun provideScreenCaptureController(bridge: ScreenCaptureBridge): ScreenCaptureController = bridge

        @Provides
        @Singleton
        fun provideMascotCommentOrchestrator(
            contextEventRepository: ContextEventRepository,
            llmSettingsRepository: LlmSettingsRepository,
            commentGenerator: MascotCommentGenerator,
            screenCaptureSource: ScreenCaptureSource
        ): MascotCommentOrchestrator {
            return MascotCommentOrchestrator(
                contextEventRepository = contextEventRepository,
                llmSettingsRepository = llmSettingsRepository,
                commentGenerator = commentGenerator,
                screenCaptureSource = screenCaptureSource
            )
        }
    }
}
