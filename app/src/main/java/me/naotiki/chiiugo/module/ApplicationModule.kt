package me.naotiki.chiiugo.module

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.naotiki.chiiugo.data.repository.InstalledAppRepository
import me.naotiki.chiiugo.data.repository.impl.InstalledAppRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ApplicationModule {
    @Binds
    @Singleton
    abstract fun bindToDoRepository(impl: InstalledAppRepositoryImpl): InstalledAppRepository
}
