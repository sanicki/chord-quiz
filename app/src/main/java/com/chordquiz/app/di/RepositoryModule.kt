package com.chordquiz.app.di

import com.chordquiz.app.data.repository.ChordRepository
import com.chordquiz.app.data.repository.ChordRepositoryImpl
import com.chordquiz.app.data.repository.InstrumentRepository
import com.chordquiz.app.data.repository.InstrumentRepositoryImpl
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
    abstract fun bindInstrumentRepository(impl: InstrumentRepositoryImpl): InstrumentRepository

    @Binds
    @Singleton
    abstract fun bindChordRepository(impl: ChordRepositoryImpl): ChordRepository
}
