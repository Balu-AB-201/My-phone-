package com.balu.abdialer

import android.telecom.Call
import android.telecom.InCallService

class ABInCallService : InCallService() {

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            ActiveCallStore.updateCall(call)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ActiveCallStore.setService(this)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callback)
        ActiveCallStore.setCall(call)
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(callback)
        if (ActiveCallStore.getCall() === call) {
            ActiveCallStore.clear()
        }
        super.onCallRemoved(call)
    }

    override fun onDestroy() {
        ActiveCallStore.clearService(this)
        super.onDestroy()
    }
}
