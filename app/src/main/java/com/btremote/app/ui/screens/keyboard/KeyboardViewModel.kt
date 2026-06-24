package com.btremote.app.ui.screens.keyboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btremote.app.bluetooth.ConnectionState
import com.btremote.app.bluetooth.ConsumerUsage
import com.btremote.app.bluetooth.HidKeyCode
import com.btremote.app.bluetooth.HidServiceController
import com.btremote.app.data.AppPreferences
import com.btremote.app.data.HostProfileRepository
import com.btremote.app.data.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class KeyboardViewModel @Inject constructor(
    private val controller: HidServiceController,
    private val prefs: PreferencesRepository,
    private val profileRepo: HostProfileRepository
) : ViewModel() {

    val preferences: StateFlow<AppPreferences?> = prefs.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val effectiveTargetOs: StateFlow<String> =
        combine(prefs.preferences, profileRepo.state) { p, profilesState ->
            val profileOs = profilesState.active?.targetOs
            if (!profileOs.isNullOrBlank() && profileOs != "auto") profileOs else p.targetOs
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "auto")

    val connectionState: StateFlow<ConnectionState> = controller.connectionState

    private sealed class KbAction {
        data class Press(val code: HidKeyCode, val modifier: Int) : KbAction()
        data class Type(val char: Char) : KbAction()
        data class Consumer(val usage: ConsumerUsage) : KbAction()
    }

    private val queue = Channel<KbAction>(capacity = Channel.UNLIMITED)

    init {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            queue.consumeAsFlow().collect { action ->
                val sender = controller.reportSender
                if (!sender.isReady()) return@collect
                when (action) {
                    is KbAction.Press -> sender.pressAndRelease(action.code, action.modifier)
                    is KbAction.Type -> sender.typeChar(action.char)
                    is KbAction.Consumer -> sender.sendConsumerKey(action.usage)
                }
            }
        }
    }

    fun pressKey(code: HidKeyCode, modifier: Int = 0) {
        queue.trySend(KbAction.Press(code, modifier))
    }

    fun typeText(text: String) {
        for (c in text) queue.trySend(KbAction.Type(c))
    }

    fun typeChar(c: Char) {
        queue.trySend(KbAction.Type(c))
    }

    fun consumer(usage: ConsumerUsage) {
        queue.trySend(KbAction.Consumer(usage))
    }

    fun setDirectInputMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setDirectInputMode(enabled) }
    }

    override fun onCleared() {
        queue.close()
        super.onCleared()
    }
}
