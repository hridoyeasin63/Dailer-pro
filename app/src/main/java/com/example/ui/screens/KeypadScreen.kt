package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ContactEntity
import com.example.data.repository.PhoneNumberUtilsHelper
import com.example.telecom.SimAccountInfo
import com.example.ui.KeypadSuggestion
import com.example.ui.components.ContactAvatar
import com.example.ui.components.getCallTypeVisual
import com.example.ui.theme.CallAcceptGreen

private data class KeypadKey(
    val digit: Char,
    val subText: String
)

private val keypadRows = listOf(
    listOf(
        KeypadKey('1', ""),
        KeypadKey('2', "ABC"),
        KeypadKey('3', "DEF")
    ),
    listOf(
        KeypadKey('4', "GHI"),
        KeypadKey('5', "JKL"),
        KeypadKey('6', "MNO")
    ),
    listOf(
        KeypadKey('7', "PQRS"),
        KeypadKey('8', "TUV"),
        KeypadKey('9', "WXYZ")
    ),
    listOf(
        KeypadKey('*', ""),
        KeypadKey('0', "+"),
        KeypadKey('#', "")
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadScreen(
    dialedNumber: String,
    suggestions: List<KeypadSuggestion> = emptyList(),
    matchingContacts: List<ContactEntity> = emptyList(),
    availableSims: List<SimAccountInfo>,
    defaultSimSlot: Int,
    onDigitPress: (Char) -> Unit,
    onDigitLongPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onCallClick: (String, Int?) -> Unit,
    onSelectSimSlot: (Int) -> Unit = {},
    onAddContactWithNumber: (String) -> Unit,
    onContactSuggestionClick: (ContactEntity) -> Unit = {},
    onSuggestionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val formattedNumber = remember(dialedNumber) {
        PhoneNumberUtilsHelper.formatForDisplay(dialedNumber)
    }

    val resolvedSuggestions = remember(suggestions, matchingContacts) {
        if (suggestions.isNotEmpty()) {
            suggestions
        } else {
            matchingContacts.map { KeypadSuggestion.Contact(it) }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth
        val isVeryCompactHeight = screenHeight < 620.dp
        val isCompactHeight = screenHeight < 720.dp

        val keyButtonHeight: Dp = when {
            isVeryCompactHeight -> 46.dp
            isCompactHeight -> 52.dp
            else -> 60.dp
        }
        val keyButtonWidth: Dp = ((screenWidth - 40.dp) / 3).coerceIn(64.dp, 94.dp)
        val digitFontSize: TextUnit = when {
            isVeryCompactHeight -> 20.sp
            isCompactHeight -> 24.sp
            else -> 28.sp
        }
        val subTextFontSize: TextUnit = if (isVeryCompactHeight) 9.sp else 10.sp
        val rowSpacing: Dp = if (isVeryCompactHeight) 4.dp else if (isCompactHeight) 6.dp else 10.dp
        val callButtonSize: Dp = when {
            isVeryCompactHeight -> 56.dp
            isCompactHeight -> 62.dp
            else -> 68.dp
        }
        val callIconSize: Dp = if (isVeryCompactHeight) 26.dp else 30.dp

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP SECTION: Suggestions area taking all space above dialed number display
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                if (dialedNumber.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("keypad_suggestions_list"),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        item {
                            TextButton(
                                onClick = { onAddContactWithNumber(dialedNumber) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("keypad_add_to_contacts_button")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Add to contacts",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                        items(resolvedSuggestions, key = { it.key }) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSuggestionClick(item.phoneNumber)
                                        if (item is KeypadSuggestion.Contact) {
                                            onContactSuggestionClick(item.contact)
                                        }
                                    }
                                    .testTag("keypad_match_item_${item.key}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ContactAvatar(
                                        name = item.title,
                                        photoUri = item.photoUri,
                                        colorIndex = item.avatarColorIndex,
                                        size = 36.dp,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (item is KeypadSuggestion.Contact) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "Contact • " + PhoneNumberUtilsHelper.formatForDisplay(item.phoneNumber),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else if (item is KeypadSuggestion.History) {
                                                val visual = getCallTypeVisual(item.callType)
                                                Icon(
                                                    imageVector = visual.icon,
                                                    contentDescription = null,
                                                    tint = visual.tint,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "${visual.label} • " + PhoneNumberUtilsHelper.formatForDisplay(item.phoneNumber),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    IconButton(
                                        onClick = { onCallClick(item.phoneNumber, null) },
                                        modifier = Modifier.testTag("keypad_match_call_${item.key}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "Call ${item.title}",
                                            tint = CallAcceptGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = "Enter a phone number",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // 2. FIXED BOTTOM DIALER SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isVeryCompactHeight) 2.dp else 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 2a. Dialed Number Display (directly above the dialpad keys)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = if (isVeryCompactHeight) 2.dp else 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (dialedNumber.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(44.dp))
                    }

                    val numberFontSize = when {
                        formattedNumber.length > 16 -> if (isVeryCompactHeight) 18.sp else 22.sp
                        formattedNumber.length > 12 -> if (isVeryCompactHeight) 22.sp else 26.sp
                        isVeryCompactHeight -> 26.sp
                        isCompactHeight -> 30.sp
                        else -> 36.sp
                    }

                    Text(
                        text = formattedNumber.ifEmpty { " " },
                        fontSize = numberFontSize,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("keypad_number_display")
                    )

                    // Single '×' Delete Button (Tap to delete 1 digit, Long press to clear all)
                    if (dialedNumber.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .combinedClickable(
                                    onClick = onBackspace,
                                    onLongClick = onClear
                                )
                                .testTag("keypad_backspace_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete digit (Long press to clear)",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isVeryCompactHeight) 2.dp else 6.dp))

                // 2b. Dialpad 4x3 Grid (directly above call button)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(rowSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    keypadRows.forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowKeys.forEach { keyItem ->
                                ResponsiveDialpadButton(
                                    digit = keyItem.digit,
                                    subText = keyItem.subText,
                                    width = keyButtonWidth,
                                    height = keyButtonHeight,
                                    digitFontSize = digitFontSize,
                                    subTextFontSize = subTextFontSize,
                                    onClick = { onDigitPress(keyItem.digit) },
                                    onLongClick = { onDigitLongPress(keyItem.digit) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isVeryCompactHeight) 6.dp else 10.dp))

                // 2c. Call Button(s) at bottom:
                // If dual SIM phone: 2 buttons side by side (SIM 1 & SIM 2)
                // If 1 SIM phone: 1 Call button
                if (availableSims.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val sim1 = availableSims.find { it.slotIndex == 1 } ?: availableSims.firstOrNull()
                        val sim2 = availableSims.find { it.slotIndex == 2 } ?: availableSims.getOrNull(1)

                        // SIM 1 Call Button
                        Surface(
                            onClick = { onCallClick(dialedNumber, 1) },
                            shape = RoundedCornerShape(28.dp),
                            color = CallAcceptGreen,
                            contentColor = Color.White,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("keypad_call_button_sim1")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call with SIM 1",
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "SIM 1",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (sim1 != null && sim1.displayName.isNotBlank() && sim1.displayName != "SIM 1") {
                                        Text(
                                            text = sim1.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // SIM 2 Call Button
                        Surface(
                            onClick = { onCallClick(dialedNumber, 2) },
                            shape = RoundedCornerShape(28.dp),
                            color = CallAcceptGreen,
                            contentColor = Color.White,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("keypad_call_button_sim2")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call with SIM 2",
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "SIM 2",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (sim2 != null && sim2.displayName.isNotBlank() && sim2.displayName != "SIM 2") {
                                        Text(
                                            text = sim2.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Single SIM: 1 large centered Call button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            onClick = { onCallClick(dialedNumber, null) },
                            shape = CircleShape,
                            color = CallAcceptGreen,
                            contentColor = Color.White,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(callButtonSize)
                                .testTag("keypad_call_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call entered number",
                                    modifier = Modifier.size(callIconSize)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResponsiveDialpadButton(
    digit: Char,
    subText: String,
    width: Dp,
    height: Dp,
    digitFontSize: TextUnit,
    subTextFontSize: TextUnit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "dial_btn_scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(width = width, height = height)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("keypad_key_$digit"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit.toString(),
                fontSize = digitFontSize,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subText.isNotEmpty()) {
                Text(
                    text = subText,
                    fontSize = subTextFontSize,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
