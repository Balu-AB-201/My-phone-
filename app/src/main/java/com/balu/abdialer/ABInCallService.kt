package com.balu.abdialer

import android.telecom.Call
import android.telecom.InCallService

/**
 * System call bridge for AB Dialer.
 *
 * Android only binds this service for the selected/default dialer.
 * The UI remains separate so the liquid-glass Compose layer is not coupled
 * to the Telecom framework lifecycle.
 */
class ABInCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        ActiveCallStore.setCall(call)
    }

    override fun onCallRemoved(call: Call) {
        if (ActiveCallStore.getCall() === call) {
            ActiveCallStore.clear()
        }
        super.onCallRemoved(call)
    }
}
