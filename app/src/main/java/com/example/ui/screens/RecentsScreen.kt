package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CallLogEntity
import com.example.data.local.CallRecordType
import com.example.data.local.ContactEntity
import com.example.data.repository.PhoneNumberUtilsHelper
import com.example.ui.CallHistoryFilter
import com.example.ui.components.ContactAvatar
import com.example.ui.components.SimBadge
import com.example.ui.components.formatCallDurationLong
import com.example.ui.components.formatCallTimestamp
import com.example.ui.components.getCallTypeVisual
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.CallDeclineRed

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecentsScreen(
    callLogs: List<CallLogEntity>,
    activeFilter: CallHistoryFilter,
    selectedIds: Set<Long>,
    onFilterChange: (CallHistoryFilter) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClearAllHistory: () -> Unit,
    onCallEntryClick: (CallLogEntity) -> Unit,
    onQuickCallClick: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBlockedNumbers: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    val isSelectionMode = selectedIds.isNotEmpty()

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear all call history?") },
            text = {
                Text("This will permanently remove all incoming, outgoing, and missed call records from your call history.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllDialog = false
                        onClearAllHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CallDeclineRed),
                    modifier = Modifier.testTag("confirm_clear_all_calls_button")
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (isSelectionMode) {
            TopAppBar(
                title = { Text("${selectedIds.size} selected") },
                navigationIcon = {
                    IconButton(onClick = onClearSelection) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onDeleteSelected,
                        modifier = Modifier.testTag("delete_selected_calls_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete selected call records",
                            tint = CallDeclineRed
                        )
                    }
                }
            )
        } else {
            TopAppBar(
                title = {
                    Text(
                        text = "Recents",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier.testTag("recents_search_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search call history")
                    }
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("recents_more_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear all call history") },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    showClearAllDialog = true
                                },
                                modifier = Modifier.testTag("menu_clear_all_history")
                            )
                            DropdownMenuItem(
                                text = { Text("Blocked numbers") },
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenBlockedNumbers()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenSettings()
                                }
                            )
                        }
                    }
                }
            )
        }

        // Filter Chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CallHistoryFilter.entries.forEach { filter ->
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.label) },
                    modifier = Modifier.testTag("call_filter_${filter.name.lowercase()}")
                )
            }
        }

        if (callLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No recent calls",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag("recents_empty_text")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Calls matching '${activeFilter.label}' will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("recents_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(callLogs, key = { it.id }) { log ->
                    val isSelected = selectedIds.contains(log.id)
                    CallHistoryRowCard(
                        callLog = log,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                onToggleSelect(log.id)
                            } else {
                                onCallEntryClick(log)
                            }
                        },
                        onLongClick = { onToggleSelect(log.id) },
                        onCallClick = { onQuickCallClick(log.phoneNumber) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallHistoryRowCard(
    callLog: CallLogEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val visual = getCallTypeVisual(callLog.callType)
    val (dateStr, timeStr) = remember(callLog.timestamp) { formatCallTimestamp(callLog.timestamp) }
    val title = callLog.contactName?.takeIf { it.isNotBlank() }
        ?: PhoneNumberUtilsHelper.formatForDisplay(callLog.phoneNumber)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("call_log_item_${callLog.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                ContactAvatar(
                    name = title,
                    photoUri = callLog.photoUri,
                    colorIndex = callLog.avatarColorIndex,
                    size = 48.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (callLog.callType == CallRecordType.MISSED) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (callLog.callType == CallRecordType.MISSED) CallDeclineRed else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = visual.label,
                        tint = visual.tint,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${visual.label} • $dateStr, $timeStr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SimBadge(
                        simSlot = callLog.simSlot,
                        carrierName = callLog.simCarrierName
                    )
                    if (callLog.durationSeconds > 0L &&
                        (callLog.callType == CallRecordType.INCOMING || callLog.callType == CallRecordType.OUTGOING)
                    ) {
                        Text(
                            text = "Duration: ${formatCallDurationLong(callLog.durationSeconds)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(
                onClick = onCallClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CallAcceptGreen.copy(alpha = 0.14f))
                    .testTag("call_log_dial_button_${callLog.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call $title",
                    tint = CallAcceptGreen
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailsScreen(
    callLog: CallLogEntity,
    historyForContact: List<CallLogEntity>,
    matchedContact: ContactEntity?,
    isBlocked: Boolean,
    onCallClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    onAddOrEditContactClick: () -> Unit,
    onToggleFavoriteClick: () -> Unit,
    onToggleBlockClick: () -> Unit,
    onDeleteSingleCallLog: () -> Unit,
    onDeleteAllHistoryForNumber: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val visual = getCallTypeVisual(callLog.callType)
    val (dateStr, timeStr) = remember(callLog.timestamp) { formatCallTimestamp(callLog.timestamp) }
    val displayName = matchedContact?.fullName ?: callLog.contactName ?: "Unknown Number"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call Details") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("call_details_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onDeleteSingleCallLog,
                        modifier = Modifier.testTag("delete_single_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete this call record",
                            tint = CallDeclineRed
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ContactAvatar(
                            name = displayName,
                            photoUri = matchedContact?.photoUri ?: callLog.photoUri,
                            colorIndex = matchedContact?.avatarColorIndex ?: callLog.avatarColorIndex,
                            size = 84.dp,
                            fontSize = 30.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("call_details_name")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = PhoneNumberUtilsHelper.formatForDisplay(callLog.phoneNumber),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("call_details_number")
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Metadata chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SimBadge(simSlot = callLog.simSlot, carrierName = callLog.simCarrierName)
                            Text(
                                text = "• ${historyForContact.size} call(s) with this contact",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // Specific Call Record details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = visual.icon,
                                    contentDescription = null,
                                    tint = visual.tint,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = visual.label,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "$dateStr at $timeStr",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Duration",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCallDurationLong(callLog.durationSeconds),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onCallClick(callLog.phoneNumber) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("call_details_call_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CallAcceptGreen)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Call")
                        }
                        OutlinedButton(
                            onClick = { onMessageClick(callLog.phoneNumber) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("call_details_message_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Message")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAddOrEditContactClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("call_details_add_contact_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (matchedContact == null) "Add to Contacts" else "Edit Contact")
                        }
                        if (matchedContact != null) {
                            OutlinedButton(
                                onClick = onToggleFavoriteClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("call_details_favorite_button")
                            ) {
                                Icon(
                                    imageVector = if (matchedContact.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (matchedContact.isFavorite) "Favorited" else "Add to Favorites")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onToggleBlockClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("call_details_block_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = CallDeclineRed
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBlocked) "Unblock Number" else "Block Number",
                                color = CallDeclineRed
                            )
                        }
                        OutlinedButton(
                            onClick = onDeleteAllHistoryForNumber,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("call_details_delete_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = CallDeclineRed
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Delete Call History",
                                color = CallDeclineRed
                            )
                        }
                    }
                }
            }

            // Recent calls with this number
            if (historyForContact.size > 1) {
                item {
                    Text(
                        text = "All Calls with $displayName (${historyForContact.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(historyForContact, key = { it.id }) { entry ->
                    val itemVisual = getCallTypeVisual(entry.callType)
                    val (d, t) = formatCallTimestamp(entry.timestamp)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = itemVisual.icon,
                                    contentDescription = null,
                                    tint = itemVisual.tint,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(itemVisual.label, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "$d, $t • ${entry.simCarrierName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = formatCallDurationLong(entry.durationSeconds),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
