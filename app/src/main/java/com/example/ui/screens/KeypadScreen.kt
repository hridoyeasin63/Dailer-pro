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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ContactEntity
import com.example.data.repository.PhoneNumberUtilsHelper
import com.example.telecom.SimAccountInfo
import com.example.ui.components.ContactAvatar
import com.example.ui.theme.CallAcceptGreen

private data class KeypadKey(
    val digit: Char,
    val subText: String
)

private val keypadRows = listOf(
    listOf(
        KeypadKey('1', "OO"),
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
    matchingContacts: List<ContactEntity>,
    availableSims: List<SimAccountInfo>,
    defaultSimSlot: Int,
    onDigitPress: (Char) -> Unit,
    onDigitLongPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onPasteNumber: (String) -> Unit,
    onCallClick: (String) -> Unit,
    onSelectSimSlot: (Int) -> Unit,
    onAddContactWithNumber: (String) -> Unit,
    onContactSuggestionClick: (ContactEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val formattedNumber = remember(dialedNumber) {
        PhoneNumberUtilsHelper.formatForDisplay(dialedNumber)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Matching Contacts Above Keypad
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (dialedNumber.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("keypad_suggestions_list"),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        TextButton(
                            onClick = { onAddContactWithNumber(dialedNumber) },
                            modifier = Modifier.testTag("keypad_add_to_contacts_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create new contact for $formattedNumber")
                        }
                    }
                    items(matchingContacts, key = { it.id }) { contact ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onContactSuggestionClick(contact) }
                                .testTag("keypad_match_contact_${contact.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ContactAvatar(
                                    name = contact.fullName,
                                    photoUri = contact.photoUri,
                                    colorIndex = contact.avatarColorIndex,
                                    size = 40.dp,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.fullName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${PhoneNumberUtilsHelper.formatForDisplay(contact.phoneNumber)} • Mobile",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { onCallClick(contact.phoneNumber) },
                                    modifier = Modifier.testTag("keypad_match_call_${contact.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call ${contact.fullName}",
                                        tint = CallAcceptGreen
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Enter a number or T9 contact name",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Entered Number Display & Copy/Paste/Clear Bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Copy & Paste affordances
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text.orEmpty()
                            if (clip.isNotBlank()) {
                                onPasteNumber(clip)
                            }
                        },
                        modifier = Modifier.testTag("keypad_paste_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste phone number",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (dialedNumber.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(dialedNumber))
                            },
                            modifier = Modifier.testTag("keypad_copy_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy phone number",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Selectable Number Display
                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formattedNumber,
                        style = if (formattedNumber.length > 14) {
                            MaterialTheme.typography.headlineMedium
                        } else {
                            MaterialTheme.typography.displayMedium
                        },
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("keypad_number_display")
                    )
                }

                // Backspace & Clear controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dialedNumber.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = onBackspace,
                                    onLongClick = onClear
                                )
                                .testTag("keypad_backspace_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace (Long press to clear)",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.testTag("keypad_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear number",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Keypad 4x3 Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                keypadRows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowKeys.forEach { keyItem ->
                            DialpadButton(
                                digit = keyItem.digit,
                                subText = keyItem.subText,
                                onClick = { onDigitPress(keyItem.digit) },
                                onLongClick = { onDigitLongPress(keyItem.digit) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Call Row with SIM switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SIM Slot quick switcher
                val activeSimLabel = when (defaultSimSlot) {
                    1 -> "SIM 1"
                    2 -> "SIM 2"
                    else -> if (availableSims.size > 1) "Ask SIM" else "SIM 1"
                }
                AssistChip(
                    onClick = {
                        val nextSlot = when (defaultSimSlot) {
                            1 -> 2
                            2 -> 0
                            else -> 1
                        }
                        onSelectSimSlot(nextSlot)
                    },
                    label = { Text(activeSimLabel) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.SimCard,
                            contentDescription = "Switch SIM",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("keypad_sim_selector_chip")
                )

                // Large Emerald Call Button
                Surface(
                    onClick = { onCallClick(dialedNumber) },
                    shape = CircleShape,
                    color = CallAcceptGreen,
                    contentColor = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("keypad_call_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call entered number",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(76.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialpadButton(
    digit: Char,
    subText: String,
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
            .size(width = 88.dp, height = 64.dp)
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = digit.toString(),
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subText.isNotEmpty()) {
                Text(
                    text = subText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
