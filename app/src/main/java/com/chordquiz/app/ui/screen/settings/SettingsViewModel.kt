package com.chordquiz.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chordquiz.app.data.preferences.UserPreferencesRepository
import com.chordquiz.app.domain.model.Difficulty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(Settings(hapticFeedbackEnabled = true))
    val uiState: StateFlow<Settings> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            userPreferencesRepository.hapticFeedbackEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(hapticFeedbackEnabled = enabled)
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.autoContinueDelaySeconds.collect { delay ->
                _uiState.value = _uiState.value.copy(autoContinueDelaySeconds = delay)
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.difficulty.collect { difficulty ->
                _uiState.value = _uiState.value.copy(difficulty = difficulty)
            }
        }
    }

    fun toggleHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setHapticFeedbackEnabled(enabled)
        }
    }

    fun setAutoContinueDelay(seconds: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoContinueDelaySeconds(seconds.coerceIn(1, 5))
        }
    }

    fun setDifficulty(difficulty: Difficulty) {
        viewModelScope.launch {
            userPreferencesRepository.setDifficulty(difficulty)
        }
    }
}
