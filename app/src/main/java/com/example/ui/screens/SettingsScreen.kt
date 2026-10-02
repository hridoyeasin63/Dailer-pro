package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.ContactNameFormat
import com.example.data.preferences.ContactSortOrder
import com.example.data.preferences.DialerSettings
import com.example.telecom.SimAccountInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: DialerSettings,
    isDefaultDialer: Boolean,
    availableSims: List<SimAccountInfo>,
    onRequestDefaultDialer: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenBlockedNumbers: () -> Unit,
    onUpdateTheme: (AppThemeMode) -> Unit,
    onUpdateDefaultSim: (Int) -> Unit,
    onUpdateCallerId: (Boolean) -> Unit,
    onUpdateSwipeToAnswer: (Boolean) -> Unit,
    onUpdateCallVibration: (Boolean) -> Unit,
    onUpdateHapticFeedback: (Boolean) -> Unit,
    onUpdateDialPadSounds: (Boolean) -> Unit,
    onUpdateProximitySensor: (Boolean) -> Unit,
    onUpdateSortOrder: (ContactSortOrder) -> Unit,
    onUpdateNameFormat: (ContactNameFormat) -> Unit,
    onUpdateDefaultAccount: (String) -> Unit,
    onUpdateShowMissedCalls: (Boolean) -> Unit,
    onUpdateShowOutgoingCalls: (Boolean) -> Unit,
    onUpdateShowIncomingCalls: (Boolean) -> Unit,
    onTriggerIncomingCallDiagnostic: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = { Text("Privacy Policy") },
            text = {
                Text(
                    "Phone stores your contacts, call history, and blocked numbers strictly on your device. " +
                        "No contacts or call logs are ever uploaded to external servers. All calling, caller ID, " +
                        "and call screening actions use official Android Telecom APIs."
                )
            },
            confirmButton = {
                Button(onClick = { showPrivacyPolicyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Open-Source Licenses") },
            text = {
                Text(
                    "• Android Jetpack Compose & Material 3 (Apache License 2.0)\n" +
                        "• AndroidX Room Persistence Library (Apache License 2.0)\n" +
                        "• AndroidX DataStore Preferences (Apache License 2.0)\n" +
                        "• Kotlin Coroutines (Apache License 2.0)\n" +
                        "• Coil Image Loader (Apache License 2.0)"
                )
            },
            confirmButton = {
                Button(onClick = { showLicensesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Default Phone App Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDefaultDialer) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneCallback,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isDefaultDialer) {
                                        "Phone is your Default Dialer App"
                                    } else {
                                        "Set as default phone app"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isDefaultDialer) {
                                        "Full incoming call handling, in-call controls, and CallScreeningService are active."
                                    } else {
                                        "Some features (full-screen incoming calls & automatic call blocking) require default dialer status."
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        if (!isDefaultDialer) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRequestDefaultDialer,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .testTag("settings_set_default_dialer_button")
                            ) {
                                Text("Set as default phone app")
                            }
                        }
                    }
                }
            }

            // 1. CALLING SECTION
            item {
                SettingsSectionCard(title = "Calling", icon = Icons.Default.Call) {
                    Text(
                        text = "Default SIM (${availableSims.size} detected)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.defaultSimSlot == 0,
                            onClick = { onUpdateDefaultSim(0) },
                            label = { Text("Ask every time") },
                            modifier = Modifier.testTag("sim_pref_ask")
                        )
                        FilterChip(
                            selected = settings.defaultSimSlot == 1,
                            onClick = { onUpdateDefaultSim(1) },
                            label = { Text("SIM 1") },
                            modifier = Modifier.testTag("sim_pref_1")
                        )
                        FilterChip(
                            selected = settings.defaultSimSlot == 2,
                            onClick = { onUpdateDefaultSim(2) },
                            label = { Text("SIM 2") },
                            modifier = Modifier.testTag("sim_pref_2")
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    SettingsSwitchRow(
                        title = "Caller ID",
                        subtitle = "Match incoming & outgoing numbers with local contacts",
                        checked = settings.callerIdEnabled,
                        onCheckedChange = onUpdateCallerId,
                        testTag = "switch_caller_id"
                    )
                    SettingsSwitchRow(
                        title = "Answer / Decline Quick Actions",
                        subtitle = "Show message reply and silence actions on incoming calls",
                        checked = settings.swipeToAnswerEnabled,
                        onCheckedChange = onUpdateSwipeToAnswer,
                        testTag = "switch_answer_decline"
                    )
                    SettingsSwitchRow(
                        title = "Call Vibration",
                        subtitle = "Vibrate on incoming calls and call connection",
                        checked = settings.callVibrationEnabled,
                        onCheckedChange = onUpdateCallVibration,
                        testTag = "switch_call_vibration"
                    )
                    SettingsSwitchRow(
                        title = "Haptic Feedback",
                        subtitle = "Subtle vibration for dial pad, call, accept, and end call",
                        checked = settings.hapticFeedbackEnabled,
                        onCheckedChange = onUpdateHapticFeedback,
                        testTag = "switch_haptic_feedback"
                    )
                    SettingsSwitchRow(
                        title = "Dial Pad Sounds",
                        subtitle = "Play DTMF tones when pressing keypad digits",
                        checked = settings.dialPadSoundsEnabled,
                        onCheckedChange = onUpdateDialPadSounds,
                        testTag = "switch_dialpad_sounds"
                    )
                    SettingsSwitchRow(
                        title = "Proximity Sensor Behavior",
                        subtitle = "Turn off screen when holding phone to ear during active call",
                        checked = settings.proximitySensorEnabled,
                        onCheckedChange = onUpdateProximitySensor,
                        testTag = "switch_proximity_sensor"
                    )
                }
            }

            // 2. APPEARANCE SECTION
            item {
                SettingsSectionCard(title = "Appearance", icon = Icons.Default.DarkMode) {
                    Text(
                        text = "App Theme",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.themeMode == AppThemeMode.SYSTEM,
                            onClick = { onUpdateTheme(AppThemeMode.SYSTEM) },
                            label = { Text("System Default") },
                            modifier = Modifier.testTag("theme_chip_system")
                        )
                        FilterChip(
                            selected = settings.themeMode == AppThemeMode.LIGHT,
                            onClick = { onUpdateTheme(AppThemeMode.LIGHT) },
                            label = { Text("Light") },
                            modifier = Modifier.testTag("theme_chip_light")
                        )
                        FilterChip(
                            selected = settings.themeMode == AppThemeMode.DARK,
                            onClick = { onUpdateTheme(AppThemeMode.DARK) },
                            label = { Text("Dark") },
                            modifier = Modifier.testTag("theme_chip_dark")
                        )
                    }
                }
            }

            // 3. CONTACTS SECTION
            item {
                SettingsSectionCard(title = "Contacts", icon = Icons.Default.People) {
                    Text(
                        text = "Sort Contacts By",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.contactSortOrder == ContactSortOrder.FIRST_NAME,
                            onClick = { onUpdateSortOrder(ContactSortOrder.FIRST_NAME) },
                            label = { Text("First name") },
                            modifier = Modifier.testTag("sort_first_name_chip")
                        )
                        FilterChip(
                            selected = settings.contactSortOrder == ContactSortOrder.LAST_NAME,
                            onClick = { onUpdateSortOrder(ContactSortOrder.LAST_NAME) },
                            label = { Text("Last name") },
                            modifier = Modifier.testTag("sort_last_name_chip")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Name Format",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.contactNameFormat == ContactNameFormat.FIRST_NAME_FIRST,
                            onClick = { onUpdateNameFormat(ContactNameFormat.FIRST_NAME_FIRST) },
                            label = { Text("First name first") },
                            modifier = Modifier.testTag("format_first_name_chip")
                        )
                        FilterChip(
                            selected = settings.contactNameFormat == ContactNameFormat.LAST_NAME_FIRST,
                            onClick = { onUpdateNameFormat(ContactNameFormat.LAST_NAME_FIRST) },
                            label = { Text("Last name first") },
                            modifier = Modifier.testTag("format_last_name_chip")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Default Account for New Contacts",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val accounts = listOf("Device & SIM Storage", "Local Phone DB")
                        accounts.forEach { acc ->
                            FilterChip(
                                selected = settings.defaultAccount == acc,
                                onClick = { onUpdateDefaultAccount(acc) },
                                label = { Text(acc) }
                            )
                        }
                    }
                }
            }

            // 4. CALL HISTORY SECTION
            item {
                SettingsSectionCard(title = "Call History", icon = Icons.Default.SimCard) {
                    SettingsSwitchRow(
                        title = "Show missed calls",
                        subtitle = "Include missed calls in Recents list",
                        checked = settings.showMissedCalls,
                        onCheckedChange = onUpdateShowMissedCalls,
                        testTag = "switch_show_missed"
                    )
                    SettingsSwitchRow(
                        title = "Show outgoing calls",
                        subtitle = "Include outgoing calls in Recents list",
                        checked = settings.showOutgoingCalls,
                        onCheckedChange = onUpdateShowOutgoingCalls,
                        testTag = "switch_show_outgoing"
                    )
                    SettingsSwitchRow(
                        title = "Show incoming calls",
                        subtitle = "Include answered incoming calls in Recents list",
                        checked = settings.showIncomingCalls,
                        onCheckedChange = onUpdateShowIncomingCalls,
                        testTag = "switch_show_incoming"
                    )
                }
            }

            // 5. PRIVACY & PERMISSIONS SECTION
            item {
                SettingsSectionCard(title = "Privacy", icon = Icons.Default.Security) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenBlockedNumbers)
                            .padding(vertical = 8.dp)
                            .testTag("settings_open_blocked_numbers"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Blocked numbers", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Manage numbers rejected by CallScreeningService",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.Block, contentDescription = null)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRequestPermissions)
                            .padding(vertical = 8.dp)
                            .testTag("settings_manage_permissions"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Permission management", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Review Contacts, Call Log, Phone, and Notification permissions",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.Lock, contentDescription = null)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    OutlinedButton(
                        onClick = onTriggerIncomingCallDiagnostic,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_test_incoming_call_button")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Incoming Call Handler (+8801712345678)")
                    }
                }
            }

            // 6. ABOUT SECTION
            item {
                SettingsSectionCard(title = "About", icon = Icons.Default.Info) {
                    Text(
                        text = "App Version: 1.0.0 (Build 1)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Developer: Android Telecom & Communications Engineering",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showPrivacyPolicyDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_privacy_policy_button")
                        ) {
                            Text("Privacy Policy")
                        }
                        OutlinedButton(
                            onClick = { showLicensesDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_licenses_button")
                        ) {
                            Text("Open-Source Licenses")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
