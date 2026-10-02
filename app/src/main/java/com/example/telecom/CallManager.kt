package com.example.telecom

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.CallRecordType
import com.example.data.local.DialerDatabase
import com.example.data.repository.DialerRepository
import com.example.data.repository.PhoneNumberUtilsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Coordinates Android Telecom framework calls, InCallService callbacks, audio routing,
 * DTMF tone generation, proximity wake lock, haptic feedback, Dual-SIM discovery,
 * and call notifications.
 */
object CallManager {

    private const val CHANNEL_ACTIVE_CALL = "channel_active_call"
    private const val CHANNEL_MISSED_CALL = "channel_missed_call"
    private const val NOTIFICATION_ID_ACTIVE_CALL = 1001
    private const val NOTIFICATION_ID_MISSED_CALL_BASE = 2000

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var durationJob: Job? = null

    private val _activeCallState = MutableStateFlow<ActiveCallInfo?>(null)
    val activeCallState: StateFlow<ActiveCallInfo?> = _activeCallState.asStateFlow()

    private val _callErrorBanner = MutableStateFlow<String?>(null)
    val callErrorBanner: StateFlow<String?> = _callErrorBanner.asStateFlow()

    private var currentTelecomCall: Call? = null
    private var inCallServiceRef: DialerInCallService? = null
    private var proximityWakeLock: PowerManager.WakeLock? = null
    private var toneGenerator: ToneGenerator? = null

    private val telecomCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            handleTelecomStateChange(call, state)
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            handleTelecomStateChange(call, call.state)
        }
    }

    fun registerInCallService(service: DialerInCallService) {
        inCallServiceRef = service
    }

    fun unregisterInCallService(service: DialerInCallService) {
        if (inCallServiceRef === service) {
            inCallServiceRef = null
        }
    }

    fun clearErrorBanner() {
        _callErrorBanner.value = null
    }

    fun isDefaultDialer(context: Context): Boolean {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        return telecomManager?.defaultDialerPackage == context.packageName
    }

    /**
     * Queries available SIM cards on the device using SubscriptionManager and TelecomManager
     * where permissions permit. Never assumes every device has dual SIM.
     */
    fun getAvailableSimAccounts(context: Context): List<SimAccountInfo> {
        val accounts = mutableListOf<SimAccountInfo>()
        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPhoneState) {
            runCatching {
                val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                val callCapableHandles = runCatching {
                    telecomManager?.callCapablePhoneAccounts ?: emptyList()
                }.getOrDefault(emptyList())

                val activeSubs = subManager?.activeSubscriptionInfoList.orEmpty()
                activeSubs.forEachIndexed { idx, subInfo ->
                    val slot = subInfo.simSlotIndex + 1
                    val carrier = subInfo.carrierName?.toString()?.takeIf { it.isNotBlank() }
                        ?: subInfo.displayName?.toString()?.takeIf { it.isNotBlank() }
                        ?: "SIM $slot"
                    val display = subInfo.displayName?.toString()?.takeIf { it.isNotBlank() } ?: "SIM $slot"
                    val handle = callCapableHandles.getOrNull(idx)
                    accounts.add(
                        SimAccountInfo(
                            slotIndex = slot.coerceAtLeast(1),
                            subscriptionId = subInfo.subscriptionId,
                            displayName = display,
                            carrierName = carrier,
                            accountHandle = handle
                        )
                    )
                }
            }
        }

        if (accounts.isEmpty()) {
            // Single fallback SIM representation based on TelephonyManager network operator
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val operatorName = telephonyManager?.networkOperatorName?.takeIf { it.isNotBlank() } ?: "Primary SIM"
            accounts.add(
                SimAccountInfo(
                    slotIndex = 1,
                    subscriptionId = 1,
                    displayName = "SIM 1",
                    carrierName = operatorName
                )
            )
        }
        return accounts
    }

    /**
     * Initiates a phone call using Android's TelecomManager / ACTION_CALL / ACTION_DIAL APIs,
     * validates the phone number, updates active call UI state, and logs the call.
     */
    fun placeCall(
        context: Context,
        rawNumber: String,
        simSlot: Int = 1,
        callerIdEnabled: Boolean = true,
        hapticEnabled: Boolean = true,
        proximityEnabled: Boolean = true
    ): Result<Unit> {
        val trimmed = rawNumber.trim()
        if (!PhoneNumberUtilsHelper.isValidPhoneNumber(trimmed)) {
            val msg = context.getString(R.string.error_invalid_number)
            _callErrorBanner.value = msg
            return Result.failure(IllegalArgumentException(msg))
        }

        if (hapticEnabled) {
            triggerHapticFeedback(context, heavy = true)
        }

        val sims = getAvailableSimAccounts(context)
        val chosenSim = sims.find { it.slotIndex == simSlot } ?: sims.firstOrNull()
        val carrierLabel = chosenSim?.carrierName ?: "SIM $simSlot"

        val hasCallPhonePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val uri = Uri.fromParts("tel", trimmed, null)
        var telecomPlaced = false

        if (hasCallPhonePermission) {
            runCatching {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                if (telecomManager != null) {
                    val extras = Bundle()
                    chosenSim?.accountHandle?.let { handle ->
                        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                    }
                    telecomManager.placeCall(uri, extras)
                    telecomPlaced = true
                }
            }.onFailure {
                // Fallback to ACTION_CALL intent
                runCatching {
                    val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(callIntent)
                    telecomPlaced = true
                }
            }
        } else {
            // Launch system dialer intent when CALL_PHONE permission has not been granted yet
            runCatching {
                val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
            }
        }

        // Resolve contact info from Room database and show Active Call UI
        scope.launch {
            val db = DialerDatabase.getInstance(context)
            val repo = DialerRepository(context, db.dialerDao())
            val contact = if (callerIdEnabled) repo.findContactByNumber(trimmed) else null

            val callInfo = ActiveCallInfo(
                phoneNumber = trimmed,
                callerName = contact?.fullName,
                photoUri = contact?.photoUri,
                avatarColorIndex = contact?.avatarColorIndex ?: 0,
                status = CallStatus.CALLING,
                isIncoming = false,
                isBluetoothAvailable = isBluetoothHeadsetConnected(context),
                simSlot = simSlot,
                simCarrierName = carrierLabel,
                statusDetailMessage = if (telecomPlaced) {
                    "Dialing via $carrierLabel"
                } else {
                    "Handed off to system dialer ($carrierLabel)"
                }
            )
            _activeCallState.value = callInfo
            if (proximityEnabled) {
                acquireProximityWakeLock(context)
            }
            showActiveCallNotification(context, callInfo)

            // Transition to CONNECTED state if not already driven by InCallService callback
            delay(1800)
            val current = _activeCallState.value
            if (current != null && current.status == CallStatus.CALLING && currentTelecomCall == null) {
                val connected = current.copy(
                    status = CallStatus.CONNECTED,
                    connectTimestampMillis = System.currentTimeMillis(),
                    statusDetailMessage = carrierLabel
                )
                _activeCallState.value = connected
                startDurationTimer(context)
                showActiveCallNotification(context, connected)
            }
        }

        return Result.success(Unit)
    }

    /**
     * Handles an incoming call from Telecom InCallService or incoming call verification flow.
     */
    fun onIncomingCallReceived(
        context: Context,
        phoneNumber: String,
        simSlot: Int = 1,
        callerIdEnabled: Boolean = true,
        vibrateOnRing: Boolean = true
    ) {
        scope.launch {
            val db = DialerDatabase.getInstance(context)
            val repo = DialerRepository(context, db.dialerDao())

            // Check if blocked
            if (repo.isNumberBlocked(phoneNumber)) {
                currentTelecomCall?.reject(false, null)
                repo.recordCall(
                    phoneNumber = phoneNumber,
                    callType = CallRecordType.BLOCKED,
                    durationSeconds = 0L,
                    simSlot = simSlot,
                    simCarrierName = "SIM $simSlot"
                )
                return@launch
            }

            val contact = if (callerIdEnabled) repo.findContactByNumber(phoneNumber) else null
            val sims = getAvailableSimAccounts(context)
            val carrier = sims.find { it.slotIndex == simSlot }?.carrierName ?: "SIM $simSlot"

            val incomingInfo = ActiveCallInfo(
                phoneNumber = phoneNumber,
                callerName = contact?.fullName,
                photoUri = contact?.photoUri,
                avatarColorIndex = contact?.avatarColorIndex ?: 0,
                status = CallStatus.RINGING,
                isIncoming = true,
                isBluetoothAvailable = isBluetoothHeadsetConnected(context),
                simSlot = simSlot,
                simCarrierName = carrier,
                statusDetailMessage = "Incoming call via $carrier"
            )
            _activeCallState.value = incomingInfo
            if (vibrateOnRing) {
                triggerHapticFeedback(context, heavy = true)
            }
            showActiveCallNotification(context, incomingInfo)
        }
    }

    fun attachTelecomCall(context: Context, call: Call) {
        currentTelecomCall = call
        call.registerCallback(telecomCallback)
        handleTelecomStateChange(call, call.state, context)
    }

    fun detachTelecomCall(context: Context, call: Call) {
        call.unregisterCallback(telecomCallback)
        if (currentTelecomCall === call) {
            currentTelecomCall = null
        }
        handleTelecomStateChange(call, Call.STATE_DISCONNECTED, context)
    }

    private fun handleTelecomStateChange(call: Call, state: Int, context: Context? = inCallServiceRef) {
        val number = call.details?.handle?.schemeSpecificPart ?: _activeCallState.value?.phoneNumber ?: "Unknown"
        val mappedStatus = when (state) {
            Call.STATE_RINGING -> CallStatus.RINGING
            Call.STATE_DIALING, Call.STATE_CONNECTING -> CallStatus.CALLING
            Call.STATE_ACTIVE -> CallStatus.CONNECTED
            Call.STATE_HOLDING -> CallStatus.ON_HOLD
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> CallStatus.DISCONNECTED
            else -> CallStatus.CALLING
        }

        if (state == Call.STATE_RINGING && _activeCallState.value == null && context != null) {
            onIncomingCallReceived(context, number)
            return
        }

        _activeCallState.update { current ->
            val base = current ?: ActiveCallInfo(
                phoneNumber = number,
                callerName = call.details?.callerDisplayName?.takeIf { it.isNotBlank() },
                status = mappedStatus,
                isIncoming = state == Call.STATE_RINGING
            )
            val connectTime = if (mappedStatus == CallStatus.CONNECTED && base.connectTimestampMillis == null) {
                System.currentTimeMillis()
            } else {
                base.connectTimestampMillis
            }
            base.copy(
                status = mappedStatus,
                isOnHold = mappedStatus == CallStatus.ON_HOLD,
                connectTimestampMillis = connectTime
            )
        }

        // Asynchronously enrich contact details and photoUri if not already present
        if (context != null && _activeCallState.value?.photoUri == null && number.isNotBlank() && number != "Unknown") {
            scope.launch {
                val db = DialerDatabase.getInstance(context)
                val repo = DialerRepository(context, db.dialerDao())
                val contact = repo.findContactByNumber(number)
                if (contact != null) {
                    _activeCallState.update { curr ->
                        if (curr != null && curr.photoUri == null) {
                            curr.copy(
                                callerName = curr.callerName ?: contact.fullName,
                                photoUri = contact.photoUri,
                                avatarColorIndex = contact.avatarColorIndex
                            )
                        } else {
                            curr
                        }
                    }
                }
            }
        }

        if (mappedStatus == CallStatus.CONNECTED && context != null) {
            startDurationTimer(context)
        } else if (mappedStatus == CallStatus.DISCONNECTED && context != null) {
            endCall(context)
        }
    }

    fun acceptIncomingCall(context: Context, hapticEnabled: Boolean = true) {
        if (hapticEnabled) triggerHapticFeedback(context, heavy = true)
        currentTelecomCall?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)

        _activeCallState.update { current ->
            current?.copy(
                status = CallStatus.CONNECTED,
                connectTimestampMillis = System.currentTimeMillis(),
                isRingerSilenced = false,
                statusDetailMessage = current.simCarrierName
            )
        }
        startDurationTimer(context)
        _activeCallState.value?.let { showActiveCallNotification(context, it) }
    }

    fun declineIncomingCall(context: Context, hapticEnabled: Boolean = true, reasonMessage: String? = null) {
        if (hapticEnabled) triggerHapticFeedback(context, heavy = true)
        currentTelecomCall?.reject(reasonMessage != null, reasonMessage)

        val snapshot = _activeCallState.value ?: return
        durationJob?.cancel()
        releaseProximityWakeLock()
        cancelActiveCallNotification(context)

        _activeCallState.value = snapshot.copy(
            status = CallStatus.REJECTED,
            statusDetailMessage = reasonMessage ?: "Call declined"
        )

        scope.launch {
            val db = DialerDatabase.getInstance(context)
            val repo = DialerRepository(context, db.dialerDao())
            repo.recordCall(
                phoneNumber = snapshot.phoneNumber,
                callType = CallRecordType.REJECTED,
                durationSeconds = 0L,
                simSlot = snapshot.simSlot,
                simCarrierName = snapshot.simCarrierName
            )
            delay(900)
            if (_activeCallState.value?.status == CallStatus.REJECTED) {
                _activeCallState.value = null
            }
        }
    }

    fun endCall(context: Context, hapticEnabled: Boolean = true) {
        if (hapticEnabled) triggerHapticFeedback(context, heavy = true)
        currentTelecomCall?.disconnect()

        val snapshot = _activeCallState.value ?: return
        durationJob?.cancel()
        releaseProximityWakeLock()
        cancelActiveCallNotification(context)

        val wasMissed = snapshot.isIncoming && snapshot.connectTimestampMillis == null &&
            snapshot.status == CallStatus.RINGING
        val recordType = when {
            wasMissed -> CallRecordType.MISSED
            snapshot.isIncoming -> CallRecordType.INCOMING
            else -> CallRecordType.OUTGOING
        }

        _activeCallState.value = snapshot.copy(
            status = CallStatus.DISCONNECTED,
            statusDetailMessage = "Call ended • ${snapshot.formattedDuration}"
        )

        scope.launch {
            val db = DialerDatabase.getInstance(context)
            val repo = DialerRepository(context, db.dialerDao())
            repo.recordCall(
                phoneNumber = snapshot.phoneNumber,
                callType = recordType,
                durationSeconds = snapshot.durationSeconds,
                simSlot = snapshot.simSlot,
                simCarrierName = snapshot.simCarrierName
            )
            if (wasMissed) {
                showMissedCallNotification(context, snapshot)
            }
            delay(900)
            if (_activeCallState.value?.status == CallStatus.DISCONNECTED) {
                _activeCallState.value = null
            }
        }
    }

    fun silenceIncomingRinger(context: Context) {
        runCatching {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecomManager?.silenceRinger()
        }
        _activeCallState.update { it?.copy(isRingerSilenced = true, statusDetailMessage = "Ringer silenced") }
    }

    fun toggleMute(context: Context) {
        val current = _activeCallState.value ?: return
        val newMuted = !current.isMuted
        inCallServiceRef?.setMuted(newMuted)
        runCatching {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.isMicrophoneMute = newMuted
        }
        _activeCallState.update { it?.copy(isMuted = newMuted) }
    }

    fun toggleSpeaker(context: Context) {
        val current = _activeCallState.value ?: return
        val newRoute = if (current.audioRoute == AudioRouteState.SPEAKER) {
            AudioRouteState.EARPIECE
        } else {
            AudioRouteState.SPEAKER
        }
        setAudioRoute(context, newRoute)
    }

    fun toggleBluetoothRoute(context: Context) {
        val current = _activeCallState.value ?: return
        val newRoute = if (current.audioRoute == AudioRouteState.BLUETOOTH) {
            AudioRouteState.EARPIECE
        } else {
            AudioRouteState.BLUETOOTH
        }
        setAudioRoute(context, newRoute)
    }

    fun setAudioRoute(context: Context, route: AudioRouteState) {
        val telecomRoute = when (route) {
            AudioRouteState.EARPIECE -> CallAudioState.ROUTE_EARPIECE
            AudioRouteState.SPEAKER -> CallAudioState.ROUTE_SPEAKER
            AudioRouteState.BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
            AudioRouteState.WIRED_HEADSET -> CallAudioState.ROUTE_WIRED_HEADSET
        }
        inCallServiceRef?.setAudioRoute(telecomRoute)
        runCatching {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = (route == AudioRouteState.SPEAKER)
        }
        _activeCallState.update { it?.copy(audioRoute = route) }
    }

    fun toggleHold() {
        val current = _activeCallState.value ?: return
        val newHold = !current.isOnHold
        if (newHold) {
            currentTelecomCall?.hold()
        } else {
            currentTelecomCall?.unhold()
        }
        _activeCallState.update {
            it?.copy(
                isOnHold = newHold,
                status = if (newHold) CallStatus.ON_HOLD else CallStatus.CONNECTED,
                statusDetailMessage = if (newHold) "Call on hold" else it.simCarrierName
            )
        }
    }

    fun sendDtmfTone(context: Context, digit: Char, playTone: Boolean = true, haptic: Boolean = true) {
        if (haptic) triggerHapticFeedback(context, heavy = false)
        if (playTone) playDialpadTone(digit)

        currentTelecomCall?.let { call ->
            runCatching {
                call.playDtmfTone(digit)
                scope.launch {
                    delay(140)
                    runCatching { call.stopDtmfTone() }
                }
            }
        }

        _activeCallState.update { current ->
            current?.copy(dtmfDigitsTyped = (current.dtmfDigitsTyped + digit).takeLast(18))
        }
    }

    fun playDialpadTone(digit: Char) {
        runCatching {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_DTMF, 70)
            }
            val toneType = when (digit) {
                '0' -> ToneGenerator.TONE_DTMF_0
                '1' -> ToneGenerator.TONE_DTMF_1
                '2' -> ToneGenerator.TONE_DTMF_2
                '3' -> ToneGenerator.TONE_DTMF_3
                '4' -> ToneGenerator.TONE_DTMF_4
                '5' -> ToneGenerator.TONE_DTMF_5
                '6' -> ToneGenerator.TONE_DTMF_6
                '7' -> ToneGenerator.TONE_DTMF_7
                '8' -> ToneGenerator.TONE_DTMF_8
                '9' -> ToneGenerator.TONE_DTMF_9
                '*' -> ToneGenerator.TONE_DTMF_S
                '#' -> ToneGenerator.TONE_DTMF_P
                else -> ToneGenerator.TONE_DTMF_1
            }
            toneGenerator?.startTone(toneType, 110)
        }
    }

    fun triggerHapticFeedback(context: Context, heavy: Boolean = false) {
        runCatching {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val duration = if (heavy) 45L else 18L
                    val amplitude = if (heavy) VibrationEffect.DEFAULT_AMPLITUDE else 80
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(if (heavy) 45L else 18L)
                }
            }
        }
    }

    private fun startDurationTimer(context: Context) {
        durationJob?.cancel()
        durationJob = scope.launch {
            while (true) {
                delay(1000)
                val current = _activeCallState.value ?: break
                if (current.status != CallStatus.CONNECTED && current.status != CallStatus.ON_HOLD) break
                val startMillis = current.connectTimestampMillis ?: System.currentTimeMillis()
                val elapsed = ((System.currentTimeMillis() - startMillis) / 1000L).coerceAtLeast(0L)
                val updated = current.copy(durationSeconds = elapsed)
                _activeCallState.value = updated
                if (elapsed % 5L == 0L) {
                    showActiveCallNotification(context, updated)
                }
            }
        }
    }

    private fun isBluetoothHeadsetConnected(context: Context): Boolean {
        return runCatching {
            val hasBtPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            if (!hasBtPerm) return false
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter: BluetoothAdapter? = btManager?.adapter
            adapter?.isEnabled == true &&
                adapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED
        }.getOrDefault(false)
    }

    private fun acquireProximityWakeLock(context: Context) {
        runCatching {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                if (proximityWakeLock == null) {
                    proximityWakeLock = powerManager.newWakeLock(
                        PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                        "PhoneDialer::ProximityWakeLock"
                    )
                }
                if (proximityWakeLock?.isHeld == false) {
                    proximityWakeLock?.acquire(60 * 60 * 1000L) // max 1 hour safety timeout
                }
            }
        }
    }

    private fun releaseProximityWakeLock() {
        runCatching {
            if (proximityWakeLock?.isHeld == true) {
                proximityWakeLock?.release()
            }
        }
    }

    private fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val activeChannel = NotificationChannel(
                CHANNEL_ACTIVE_CALL,
                "Ongoing & Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Displays active and incoming call controls"
                setShowBadge(false)
            }
            val missedChannel = NotificationChannel(
                CHANNEL_MISSED_CALL,
                "Missed Calls",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts you to missed phone calls"
            }
            nm.createNotificationChannel(activeChannel)
            nm.createNotificationChannel(missedChannel)
        }
    }

    fun showActiveCallNotification(context: Context, callInfo: ActiveCallInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        runCatching {
            ensureNotificationChannels(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val returnIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("OPEN_ACTIVE_CALL", true)
            }
            val returnPendingIntent = PendingIntent.getActivity(
                context,
                100,
                returnIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val endIntent = Intent(context, CallActionReceiver::class.java).apply {
                action = CallActionReceiver.ACTION_END_CALL
            }
            val endPendingIntent = PendingIntent.getBroadcast(
                context,
                101,
                endIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = callInfo.callerName ?: callInfo.phoneNumber
            val content = when (callInfo.status) {
                CallStatus.RINGING -> "Incoming call • ${callInfo.simCarrierName}"
                CallStatus.CONNECTED -> "Ongoing call • ${callInfo.formattedDuration}"
                CallStatus.ON_HOLD -> "On hold • ${callInfo.formattedDuration}"
                else -> "${callInfo.status.displayLabel} • ${callInfo.simCarrierName}"
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ACTIVE_CALL)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle(title)
                .setContentText(content)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(returnPendingIntent)
                .addAction(
                    android.R.drawable.sym_action_call,
                    "Return to call",
                    returnPendingIntent
                )

            if (callInfo.status == CallStatus.RINGING) {
                val acceptIntent = Intent(context, CallActionReceiver::class.java).apply {
                    action = CallActionReceiver.ACTION_ACCEPT_CALL
                }
                val acceptPendingIntent = PendingIntent.getBroadcast(
                    context,
                    102,
                    acceptIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(android.R.drawable.sym_action_call, "Accept", acceptPendingIntent)
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", endPendingIntent)
            } else {
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "End Call", endPendingIntent)
            }

            nm.notify(NOTIFICATION_ID_ACTIVE_CALL, builder.build())
        }
    }

    private fun cancelActiveCallNotification(context: Context) {
        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(NOTIFICATION_ID_ACTIVE_CALL)
        }
    }

    private fun showMissedCallNotification(context: Context, callInfo: ActiveCallInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        runCatching {
            ensureNotificationChannels(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                201,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val callbackIntent = Intent(context, CallActionReceiver::class.java).apply {
                action = CallActionReceiver.ACTION_CALLBACK
                putExtra(CallActionReceiver.EXTRA_PHONE_NUMBER, callInfo.phoneNumber)
            }
            val callbackPending = PendingIntent.getBroadcast(
                context,
                202,
                callbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val caller = callInfo.callerName ?: callInfo.phoneNumber
            val notification = NotificationCompat.Builder(context, CHANNEL_MISSED_CALL)
                .setSmallIcon(android.R.drawable.sym_call_missed)
                .setContentTitle("Missed call")
                .setContentText(caller)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
                .setContentIntent(openPending)
                .addAction(android.R.drawable.sym_action_call, "Call back", callbackPending)
                .build()

            nm.notify(NOTIFICATION_ID_MISSED_CALL_BASE + (callInfo.phoneNumber.hashCode() and 0xFF), notification)
        }
    }
}
