package com.example.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.example.MainActivity

/**
 * Android Telecom InCallService implementation that binds active and ringing system calls
 * to our UI and CallManager when Phone is set as the default dialer app.
 */
class DialerInCallService : InCallService() {

    override fun onCreate() {
        super.onCreate()
        CallManager.registerInCallService(this)
    }

    override fun onDestroy() {
        CallManager.unregisterInCallService(this)
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.attachTelecomCall(this, call)

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("OPEN_ACTIVE_CALL", true)
        }
        runCatching { startActivity(launchIntent) }
    }

    override fun onCallRemoved(call: Call) {
        CallManager.detachTelecomCall(this, call)
        super.onCallRemoved(call)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        audioState ?: return
        val mappedRoute = when (audioState.route) {
            CallAudioState.ROUTE_SPEAKER -> AudioRouteState.SPEAKER
            CallAudioState.ROUTE_BLUETOOTH -> AudioRouteState.BLUETOOTH
            CallAudioState.ROUTE_WIRED_HEADSET -> AudioRouteState.WIRED_HEADSET
            else -> AudioRouteState.EARPIECE
        }
        CallManager.setAudioRoute(this, mappedRoute)
    }
}
