package com.chordquiz.app.di

import com.chordquiz.app.audio.AudioRecorderManager
import com.chordquiz.app.audio.ChordRecognizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideAudioRecorderManager(): AudioRecorderManager = AudioRecorderManager()

    @Provides
    @Singleton
    fun provideChordRecognizer(): ChordRecognizer = ChordRecognizer()
}
