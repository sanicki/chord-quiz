package com.chordquiz.app.di

import com.chordquiz.app.data.repository.GroupsRepository
import com.chordquiz.app.data.repository.GroupsRepositoryImpl
import com.chordquiz.app.data.repository.SavedPatternsRepository
import com.chordquiz.app.data.repository.SavedPatternsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGroupsRepository(impl: GroupsRepositoryImpl): GroupsRepository

    @Binds
    @Singleton
    abstract fun bindSavedPatternsRepository(impl: SavedPatternsRepositoryImpl): SavedPatternsRepository
}
