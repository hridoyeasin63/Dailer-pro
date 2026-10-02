package com.example.telecom

import android.telecom.PhoneAccountHandle

enum class CallStatus(val displayLabel: String) {
    CALLING("Calling..."),
    RINGING("Incoming call"),
    CONNECTED("Connected"),
    ON_HOLD("On hold"),
    DISCONNECTED("Call ended"),
    FAILED("Call failed"),
    BUSY("Line busy"),
    REJECTED("Call declined")
}

enum class AudioRouteState(val displayLabel: String) {
    EARPIECE("Earpiece"),
    SPEAKER("Speaker"),
    BLUETOOTH("Bluetooth"),
    WIRED_HEADSET("Headset")
}

data class SimAccountInfo(
    val slotIndex: Int, // 1 or 2
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val phoneNumber: String = "",
    val accountHandle: PhoneAccountHandle? = null
)

data class ActiveCallInfo(
    val callId: String = "call_1",
    val phoneNumber: String,
    val callerName: String?, // null if unknown caller
    val photoUri: String? = null,
    val avatarColorIndex: Int = 0,
    val status: CallStatus = CallStatus.CALLING,
    val isIncoming: Boolean = false,
    val isMuted: Boolean = false,
    val audioRoute: AudioRouteState = AudioRouteState.EARPIECE,
    val isBluetoothAvailable: Boolean = false,
    val isOnHold: Boolean = false,
    val canHold: Boolean = true,
    val canAddCall: Boolean = true,
    val isRecordingSupported: Boolean = false,
    val simSlot: Int = 1,
    val simCarrierName: String = "SIM 1",
    val connectTimestampMillis: Long? = null,
    val durationSeconds: Long = 0L,
    val dtmfDigitsTyped: String = "",
    val isRingerSilenced: Boolean = false,
    val statusDetailMessage: String? = null
) {
    val isSpeakerOn: Boolean
        get() = audioRoute == AudioRouteState.SPEAKER

    val isBluetoothOn: Boolean
        get() = audioRoute == AudioRouteState.BLUETOOTH

    val displayCallerTitle: String
        get() = callerName?.takeIf { it.isNotBlank() } ?: "Unknown Number"

    val formattedDuration: String
        get() {
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            val hours = mins / 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, mins % 60, secs)
            } else {
                String.format("%02d:%02d", mins, secs)
            }
        }
}
