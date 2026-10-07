package com.balu.abdialer

import android.telecom.Call

/**
 * Small process-local bridge between the Telecom service and the Compose UI.
 */
object ActiveCallStore {
    @Volatile
    private var activeCall: Call? = null

    fun setCall(call: Call) {
        activeCall = call
    }

    fun getCall(): Call? = activeCall

    fun clear() {
        activeCall = null
    }
}
