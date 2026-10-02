package com.example.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Handles actions tapped on active call and missed call notifications.
 */
class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_ACCEPT_CALL -> {
                CallManager.acceptIncomingCall(context)
            }
            ACTION_END_CALL -> {
                val current = CallManager.activeCallState.value
                if (current?.status == CallStatus.RINGING && current.isIncoming) {
                    CallManager.declineIncomingCall(context)
                } else {
                    CallManager.endCall(context)
                }
            }
            ACTION_CALLBACK -> {
                val number = intent.getStringExtra(EXTRA_PHONE_NUMBER).orEmpty()
                if (number.isNotBlank()) {
                    CallManager.placeCall(context, number)
                }
            }
        }
    }

    companion object {
        const val ACTION_ACCEPT_CALL = "com.example.telecom.ACTION_ACCEPT_CALL"
        const val ACTION_END_CALL = "com.example.telecom.ACTION_END_CALL"
        const val ACTION_CALLBACK = "com.example.telecom.ACTION_CALLBACK"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
    }
}
