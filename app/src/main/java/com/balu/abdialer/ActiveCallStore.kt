package com.balu.abdialer

import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CallUiState(
    val number: String,
    val state: Int
)

object ActiveCallStore {
    @Volatile
    private var activeCall: Call? = null

    @Volatile
    private var inCallService: ABInCallService? = null

    private val _uiState = MutableStateFlow<CallUiState?>(null)
    val uiState: StateFlow<CallUiState?> = _uiState.asStateFlow()

    fun setService(service: ABInCallService) {
        inCallService = service
    }

    fun clearService(service: ABInCallService) {
        if (inCallService === service) {
            inCallService = null
        }
    }

    fun setCall(call: Call) {
        activeCall = call
        publish(call)
    }

    fun updateCall(call: Call) {
        if (activeCall === call) publish(call)
    }

    private fun publish(call: Call) {
        val number = call.details.handle?.schemeSpecificPart
            ?.takeIf { it.isNotBlank() }
            ?: "Unknown number"
        _uiState.value = CallUiState(number, call.state)
    }

    fun getCall(): Call? = activeCall

    fun disconnect() {
        runCatching { activeCall?.disconnect() }
    }

    fun answer() {
        runCatching { activeCall?.answer(0) }
    }

    fun reject() {
        runCatching { activeCall?.reject(false, null) }
    }

    fun playDtmfTone(digit: Char) {
        runCatching { activeCall?.playDtmfTone(digit) }
    }

    fun stopDtmfTone() {
        runCatching { activeCall?.stopDtmfTone() }
    }

    fun setMuted(muted: Boolean) {
        runCatching { inCallService?.setMuted(muted) }
    }

    fun setSpeaker(enabled: Boolean) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            runCatching {
                inCallService?.setAudioRoute(
                    if (enabled) android.telecom.CallAudioState.ROUTE_SPEAKER
                    else android.telecom.CallAudioState.ROUTE_WIRED_OR_EARPIECE
                )
            }
        }
    }

    fun clear() {
        activeCall = null
        _uiState.value = null
    }
}
