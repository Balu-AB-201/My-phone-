package com.balu.abdialer

import android.telecom.Call

/**
 * Process-local bridge between Telecom and the Compose call UI.
 *
 * The stored call/service are only populated when Android binds AB Dialer
 * as the active in-call service.
 */
object ActiveCallStore {
    @Volatile
    private var activeCall: Call? = null

    @Volatile
    private var inCallService: ABInCallService? = null

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
    }

    fun getCall(): Call? = activeCall

    fun disconnect() {
        runCatching { activeCall?.disconnect() }
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
    }
}
