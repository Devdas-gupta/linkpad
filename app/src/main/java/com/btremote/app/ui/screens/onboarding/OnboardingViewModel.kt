package com.btremote.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btremote.app.data.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: PreferencesRepository
) : ViewModel() {
    fun complete() {
        viewModelScope.launch { prefs.setOnboardingComplete(true) }
    }
}
