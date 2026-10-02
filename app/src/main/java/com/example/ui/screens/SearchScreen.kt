package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CallLogEntity
import com.example.data.local.ContactEntity
import com.example.data.repository.PhoneNumberUtilsHelper
import com.example.ui.SearchResultsState
import com.example.ui.components.ContactAvatar
import com.example.ui.components.formatCallTimestamp
import com.example.ui.components.getCallTypeVisual
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.CallWarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchState: SearchResultsState,
    onQueryChange: (String) -> Unit,
    onContactClick: (ContactEntity) -> Unit,
    onCallLogClick: (CallLogEntity) -> Unit,
    onQuickCallClick: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("search_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    OutlinedTextField(
                        value = searchState.query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Search contacts, numbers, history...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchState.query.isNotEmpty()) {
                                IconButton(
                                    onClick = { onQueryChange("") },
                                    modifier = Modifier.testTag("search_clear_button")
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                            .testTag("global_search_input")
                    )
                }
            )
        }
    ) { innerPadding ->
        val hasAnyResults = searchState.matchingFavorites.isNotEmpty() ||
            searchState.matchingContacts.isNotEmpty() ||
            searchState.matchingCallLogs.isNotEmpty()

        if (!hasAnyResults && searchState.query.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No matches for \"${searchState.query}\"",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag("search_no_results_text")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (PhoneNumberUtilsHelper.isValidPhoneNumber(searchState.query)) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onQuickCallClick(searchState.query) }
                                .testTag("search_direct_dial_card"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Call ${searchState.query}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    tint = CallAcceptGreen
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("search_results_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Direct dial option when typing a valid number
                if (searchState.query.isNotBlank() && PhoneNumberUtilsHelper.isValidPhoneNumber(searchState.query)) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onQuickCallClick(searchState.query) }
                                .testTag("search_dial_number_row"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Dial \"${searchState.query}\"",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = CallAcceptGreen
                                )
                            }
                        }
                    }
                }

                if (searchState.matchingFavorites.isNotEmpty()) {
                    item {
                        Text(
                            text = "Favorites (${searchState.matchingFavorites.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(searchState.matchingFavorites, key = { "fav_${it.id}" }) { fav ->
                        SearchContactItem(
                            contact = fav,
                            onClick = { onContactClick(fav) },
                            onCallClick = { onQuickCallClick(fav.phoneNumber) }
                        )
                    }
                }

                if (searchState.matchingContacts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Contacts (${searchState.matchingContacts.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(searchState.matchingContacts, key = { "contact_${it.id}" }) { contact ->
                        SearchContactItem(
                            contact = contact,
                            onClick = { onContactClick(contact) },
                            onCallClick = { onQuickCallClick(contact.phoneNumber) }
                        )
                    }
                }

                if (searchState.matchingCallLogs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Call History (${searchState.matchingCallLogs.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(searchState.matchingCallLogs, key = { "log_${it.id}" }) { log ->
                        val visual = getCallTypeVisual(log.callType)
                        val (d, t) = formatCallTimestamp(log.timestamp)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCallLogClick(log) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = visual.icon,
                                    contentDescription = null,
                                    tint = visual.tint,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.contactName ?: log.phoneNumber,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${log.phoneNumber} • $d, $t",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onQuickCallClick(log.phoneNumber) }) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call",
                                        tint = CallAcceptGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchContactItem(
    contact: ContactEntity,
    onClick: () -> Unit,
    onCallClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("search_contact_result_${contact.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(
                name = contact.fullName,
                photoUri = contact.photoUri,
                colorIndex = contact.avatarColorIndex,
                size = 42.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (contact.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = CallWarningAmber,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Text(
                    text = PhoneNumberUtilsHelper.formatForDisplay(contact.phoneNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onCallClick) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call ${contact.fullName}",
                    tint = CallAcceptGreen
                )
            }
        }
    }
}
