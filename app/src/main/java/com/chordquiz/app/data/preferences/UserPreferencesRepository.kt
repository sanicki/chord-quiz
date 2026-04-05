package com.chordquiz.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chordquiz.app.data.model.Instrument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val lastInstrumentKey = stringPreferencesKey("last_instrument_id")
    private val hapticFeedbackKey = booleanPreferencesKey("haptic_feedback_enabled")

    val lastInstrumentId: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[lastInstrumentKey] ?: Instrument.GUITAR.id }

    val hapticFeedbackEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[hapticFeedbackKey] ?: true }

    suspend fun setLastInstrumentId(id: String) {
        context.dataStore.edit { prefs -> prefs[lastInstrumentKey] = id }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[hapticFeedbackKey] = enabled }
    }
}
