package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.repository.PhoneNumberUtilsHelper
import com.example.telecom.ActiveCallInfo
import com.example.telecom.AudioRouteState
import com.example.telecom.CallStatus
import com.example.ui.components.ContactAvatar
import com.example.ui.components.SimBadge
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.CallDeclineRed
import com.example.ui.theme.CallWarningAmber
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun IncomingCallScreen(
    callInfo: ActiveCallInfo,
    onAcceptClick: () -> Unit,
    onDeclineClick: () -> Unit,
    onRejectWithMessage: (String) -> Unit,
    onSilenceRinger: () -> Unit,
    onBlockCaller: () -> Unit
) {
    var showQuickReplyDialog by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "incoming_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_pulse"
    )

    if (showQuickReplyDialog) {
        val quickReplies = listOf(
            "Can't talk right now, I'll call you right back.",
            "I'm in a meeting. Please send me a text message.",
            "On my way! Call you in 5 minutes.",
            "Sorry, busy right now. Talk soon."
        )
        AlertDialog(
            onDismissRequest = { showQuickReplyDialog = false },
            title = { Text("Decline with message") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickReplies.forEach { reply ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showQuickReplyDialog = false
                                    onRejectWithMessage(reply)
                                },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = reply,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuickReplyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("incoming_call_screen")
    ) {
        // Full screen Contact Photo if available
        if (!callInfo.photoUri.isNullOrBlank()) {
            AsyncImage(
                model = callInfo.photoUri,
                contentDescription = "${callInfo.displayCallerTitle} full screen photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("incoming_fullscreen_photo")
            )
            // Cinematic dark gradient scrim overlay for contrast & readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.72f),
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
        } else {
            // Ambient gradient when no photo is attached
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF07222F),
                                Color(0xFF0D1821),
                                Color(0xFF090E12)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Caller Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = if (callInfo.isRingerSilenced) "Incoming call (Silenced)" else "Incoming call",
                        color = Color(0xFF5DD4FC),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("incoming_call_status_label")
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

                // Show circular pulsing avatar only when there is no full screen photo
                if (callInfo.photoUri.isNullOrBlank()) {
                    Box(modifier = Modifier.scale(pulseScale)) {
                        ContactAvatar(
                            name = callInfo.displayCallerTitle,
                            photoUri = null,
                            colorIndex = callInfo.avatarColorIndex,
                            size = 110.dp,
                            fontSize = 38.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Text(
                    text = callInfo.displayCallerTitle,
                    color = Color.White,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("incoming_caller_name")
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = PhoneNumberUtilsHelper.formatForDisplay(callInfo.phoneNumber),
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.testTag("incoming_caller_number")
                )
                Spacer(modifier = Modifier.height(10.dp))
                SimBadge(
                    simSlot = callInfo.simSlot,
                    carrierName = callInfo.simCarrierName
                )
            }

            // Bottom Actions: Secondary Row (Message, Silence, Block) + Swipe-Up Accept & Decline Sliders
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IncomingSecondaryAction(
                        icon = Icons.AutoMirrored.Filled.Message,
                        label = "Message",
                        onClick = { showQuickReplyDialog = true },
                        testTag = "incoming_message_button"
                    )
                    IncomingSecondaryAction(
                        icon = Icons.Default.NotificationsOff,
                        label = if (callInfo.isRingerSilenced) "Silenced" else "Silence",
                        onClick = onSilenceRinger,
                        testTag = "incoming_silence_button"
                    )
                    IncomingSecondaryAction(
                        icon = Icons.Default.Block,
                        label = "Block",
                        onClick = onBlockCaller,
                        testTag = "incoming_block_button"
                    )
                }

                // Modern Swipe-Up Answer & Decline Controllers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Swipe up to Decline
                    SwipeUpCallButton(
                        actionText = "Decline",
                        subLabel = "Decline",
                        icon = Icons.Default.CallEnd,
                        buttonColor = CallDeclineRed,
                        onTrigger = onDeclineClick,
                        testTag = "incoming_decline_button"
                    )

                    // Swipe up to Answer
                    SwipeUpCallButton(
                        actionText = "Answer",
                        subLabel = "Answer",
                        icon = Icons.Default.Call,
                        buttonColor = CallAcceptGreen,
                        onTrigger = onAcceptClick,
                        testTag = "incoming_accept_button"
                    )
                }
            }
        }
    }
}

/**
 * Modern Full-Screen Bottom-to-Top Swipe Call Action Slider Button.
 * Draggable vertically upwards with cascading animated arrow cues, spring-back physics,
 * full track touch-surface, and direct tap support.
 */
@Composable
private fun SwipeUpCallButton(
    actionText: String,
    subLabel: String,
    icon: ImageVector,
    buttonColor: Color,
    onTrigger: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val maxDragPx = with(density) { 135.dp.toPx() }
    val thresholdPx = with(density) { 50.dp.toPx() }
    val offsetYAnim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val arrowBounceTransition = rememberInfiniteTransition(label = "arrow_bounce_$actionText")
    val chevronAlpha1 by arrowBounceTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ch1_$actionText"
    )
    val chevronAlpha2 by arrowBounceTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ch2_$actionText"
    )
    val arrowOffsetY by arrowBounceTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_y_$actionText"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Vertical track pill extending high upwards
        Box(
            modifier = Modifier
                .width(86.dp)
                .height(208.dp)
                .clip(RoundedCornerShape(43.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            buttonColor.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val newOffset = (offsetYAnim.value + dragAmount).coerceIn(-maxDragPx, 0f)
                                offsetYAnim.snapTo(newOffset)
                                if (newOffset <= -thresholdPx) {
                                    onTrigger()
                                }
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetYAnim.value <= -thresholdPx) {
                                    onTrigger()
                                } else {
                                    offsetYAnim.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f))
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetYAnim.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f))
                            }
                        }
                    )
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            // Upper track guide with animated upward chevrons
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
                    .offset(y = arrowOffsetY.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = chevronAlpha1),
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = chevronAlpha2),
                    modifier = Modifier
                        .size(20.dp)
                        .offset(y = (-6).dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "SWIPE UP",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Draggable action button at bottom of track
            Surface(
                onClick = onTrigger,
                shape = CircleShape,
                color = buttonColor,
                contentColor = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .offset { IntOffset(0, offsetYAnim.value.roundToInt()) }
                    .size(74.dp)
                    .testTag(testTag)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = actionText,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = subLabel,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun IncomingSecondaryAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun ActiveCallScreen(
    callInfo: ActiveCallInfo,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleBluetooth: () -> Unit,
    onToggleHold: () -> Unit,
    onAddCallClick: () -> Unit,
    onSendDtmfDigit: (Char) -> Unit,
    onEndCallClick: () -> Unit
) {
    var isDtmfKeypadOpen by remember { mutableStateOf(false) }
    var showRecordingRestrictionNotice by remember { mutableStateOf(false) }

    if (showRecordingRestrictionNotice) {
        AlertDialog(
            onDismissRequest = { showRecordingRestrictionNotice = false },
            title = { Text("Call Recording Availability") },
            text = {
                Text(
                    "Android 10+ restricts third-party apps from capturing voice call audio streams (VOICE_CALL) " +
                        "to protect user privacy and comply with telecommunications laws. Call recording is only " +
                        "available on devices where the system manufacturer provides a privileged OEM Telecom recorder."
                )
            },
            confirmButton = {
                Button(onClick = { showRecordingRestrictionNotice = false }) {
                    Text("Understood")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("active_call_screen")
    ) {
        // Full screen Contact Photo if available during Outgoing / Active call
        if (!callInfo.photoUri.isNullOrBlank()) {
            AsyncImage(
                model = callInfo.photoUri,
                contentDescription = "${callInfo.displayCallerTitle} full screen photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("active_fullscreen_photo")
            )
            // Scrim overlay to ensure controls, text, and buttons remain ultra legible
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.72f),
                                Color.Black.copy(alpha = 0.50f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
        } else {
            // Ambient gradient when no photo is attached
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0A2E3D),
                                Color(0xFF0E1B24),
                                Color(0xFF091015)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Caller Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                SimBadge(
                    simSlot = callInfo.simSlot,
                    carrierName = callInfo.simCarrierName
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Show circular avatar only if full screen photo is absent
                if (callInfo.photoUri.isNullOrBlank()) {
                    ContactAvatar(
                        name = callInfo.displayCallerTitle,
                        photoUri = null,
                        colorIndex = callInfo.avatarColorIndex,
                        size = 92.dp,
                        fontSize = 34.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }

                Text(
                    text = callInfo.displayCallerTitle,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("active_caller_name")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = PhoneNumberUtilsHelper.formatForDisplay(callInfo.phoneNumber),
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("active_caller_number")
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Call State & Duration Badge
                val statusText = when (callInfo.status) {
                    CallStatus.CONNECTED -> callInfo.formattedDuration
                    CallStatus.ON_HOLD -> "On Hold • ${callInfo.formattedDuration}"
                    else -> callInfo.status.displayLabel
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (callInfo.isOnHold) {
                        CallWarningAmber.copy(alpha = 0.35f)
                    } else {
                        Color.Black.copy(alpha = 0.40f)
                    }
                ) {
                    Text(
                        text = statusText,
                        color = if (callInfo.isOnHold) CallWarningAmber else Color(0xFF5DD4FC),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("active_call_duration_or_status")
                    )
                }

                // Active Audio / Mute Status Pills
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (callInfo.isMuted) {
                        Text(
                            text = "Microphone Muted",
                            color = CallWarningAmber,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Text(
                        text = "Audio: ${callInfo.audioRoute.displayLabel}",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Middle Section: Either In-Call DTMF Keypad or Call Controls Grid
            AnimatedVisibility(
                visible = isDtmfKeypadOpen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                InCallDtmfKeypadCard(
                    dtmfDigits = callInfo.dtmfDigitsTyped,
                    onDigitPress = onSendDtmfDigit,
                    onClose = { isDtmfKeypadOpen = false }
                )
            }

            AnimatedVisibility(
                visible = !isDtmfKeypadOpen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Row 1: Mute, Keypad, Speaker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InCallControlButton(
                            icon = if (callInfo.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (callInfo.isMuted) "Muted" else "Mute",
                            isActive = callInfo.isMuted,
                            onClick = onToggleMute,
                            testTag = "incall_mute_button"
                        )
                        InCallControlButton(
                            icon = Icons.Default.Dialpad,
                            label = "Keypad",
                            isActive = isDtmfKeypadOpen,
                            onClick = { isDtmfKeypadOpen = true },
                            testTag = "incall_keypad_button"
                        )
                        InCallControlButton(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            label = if (callInfo.isSpeakerOn) "Speaker On" else "Speaker",
                            isActive = callInfo.isSpeakerOn,
                            onClick = onToggleSpeaker,
                            testTag = "incall_speaker_button"
                        )
                    }

                    // Row 2: Bluetooth, Hold, Add Call, Record (only if supported or info notice)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InCallControlButton(
                            icon = Icons.Default.BluetoothAudio,
                            label = "Bluetooth",
                            isActive = callInfo.audioRoute == AudioRouteState.BLUETOOTH,
                            onClick = onToggleBluetooth,
                            testTag = "incall_bluetooth_button"
                        )
                        InCallControlButton(
                            icon = if (callInfo.isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                            label = if (callInfo.isOnHold) "Resume" else "Hold",
                            isActive = callInfo.isOnHold,
                            onClick = onToggleHold,
                            testTag = "incall_hold_button"
                        )
                        InCallControlButton(
                            icon = Icons.Default.AddIcCall,
                            label = "Add Call",
                            isActive = false,
                            onClick = onAddCallClick,
                            testTag = "incall_add_call_button"
                        )
                        if (callInfo.isRecordingSupported) {
                            InCallControlButton(
                                icon = Icons.Default.FiberManualRecord,
                                label = "Record",
                                isActive = false,
                                onClick = { showRecordingRestrictionNotice = true },
                                testTag = "incall_record_button"
                            )
                        }
                    }
                }
            }

            // Bottom End Call Button (50% of screen width, center aligned, with swipe up & tap support)
            ActiveCallEndSection(
                onEndCallClick = onEndCallClick
            )
        }
    }
}

/**
 * Centered End Call button occupying 50% of screen width.
 * Direct tap button with no swipe.
 */
@Composable
private fun ActiveCallEndSection(
    onEndCallClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        // 50% width Call End pill button, center aligned, direct tap
        Surface(
            onClick = onEndCallClick,
            shape = RoundedCornerShape(32.dp),
            color = CallDeclineRed,
            contentColor = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(64.dp)
                .testTag("incall_end_call_button")
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "End Call",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InCallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(6.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) Color.White else Color.White.copy(alpha = 0.16f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color(0xFF0A2E3D) else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun InCallDtmfKeypadCard(
    dtmfDigits: String,
    onDigitPress: (Char) -> Unit,
    onClose: () -> Unit
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('*', '0', '#')
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dtmf_keypad_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dtmfDigits.ifEmpty { "DTMF Keypad" },
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("dtmf_digits_display")
                )
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("dtmf_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Close", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { digit ->
                        Box(
                            modifier = Modifier
                                .size(width = 76.dp, height = 50.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .clickable { onDigitPress(digit) }
                                .testTag("dtmf_key_$digit"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = digit.toString(),
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
