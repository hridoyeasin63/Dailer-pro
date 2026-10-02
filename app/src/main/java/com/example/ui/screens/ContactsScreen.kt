package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CallLogEntity
import com.example.data.local.ContactEntity
import com.example.data.preferences.ContactNameFormat
import com.example.data.repository.PhoneNumberUtilsHelper
import com.example.ui.components.ContactAvatar
import com.example.ui.components.formatCallDurationLong
import com.example.ui.components.formatCallTimestamp
import com.example.ui.components.getCallTypeVisual
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.CallDeclineRed
import com.example.ui.theme.CallWarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contacts: List<ContactEntity>,
    nameFormat: ContactNameFormat,
    onAddContactClick: () -> Unit,
    onContactClick: (ContactEntity) -> Unit,
    onToggleFavorite: (ContactEntity) -> Unit,
    onCallClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    onShareContact: (ContactEntity) -> Unit,
    onBlockContact: (ContactEntity) -> Unit,
    onDeleteContact: (ContactEntity) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBlockedNumbers: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var topMenuExpanded by remember { mutableStateOf(false) }
    val lastNameFirst = nameFormat == ContactNameFormat.LAST_NAME_FIRST

    val groupedContacts = remember(contacts, lastNameFirst) {
        contacts.groupBy { contact ->
            val display = contact.displayName(lastNameFirst)
            val firstChar = display.firstOrNull()?.uppercaseChar() ?: '#'
            if (firstChar.isLetter()) firstChar.toString() else "#"
        }.toSortedMap()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Contacts",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${contacts.size} saved contact(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier.testTag("contacts_search_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search contacts")
                    }
                    IconButton(
                        onClick = onAddContactClick,
                        modifier = Modifier.testTag("contacts_top_add_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact")
                    }
                    Box {
                        IconButton(
                            onClick = { topMenuExpanded = true },
                            modifier = Modifier.testTag("contacts_more_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = topMenuExpanded,
                            onDismissRequest = { topMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Blocked numbers") },
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                                onClick = {
                                    topMenuExpanded = false
                                    onOpenBlockedNumbers()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    topMenuExpanded = false
                                    onOpenSettings()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddContactClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_contact_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create new contact")
            }
        }
    ) { innerPadding ->
        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your address book is empty",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag("contacts_empty_text")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add contacts with Bangladesh (+880) or international numbers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onAddContactClick,
                        modifier = Modifier.testTag("empty_add_contact_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Contact")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("contacts_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedContacts.forEach { (letter, groupList) ->
                    item(key = "header_$letter") {
                        Text(
                            text = letter,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(groupList, key = { it.id }) { contact ->
                        ContactListRow(
                            contact = contact,
                            lastNameFirst = lastNameFirst,
                            onClick = { onContactClick(contact) },
                            onToggleFavorite = { onToggleFavorite(contact) },
                            onCallClick = { onCallClick(contact.phoneNumber) },
                            onMessageClick = { onMessageClick(contact.phoneNumber) },
                            onShareClick = { onShareContact(contact) },
                            onBlockClick = { onBlockContact(contact) },
                            onDeleteClick = { onDeleteContact(contact) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactListRow(
    contact: ContactEntity,
    lastNameFirst: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCallClick: () -> Unit,
    onMessageClick: () -> Unit,
    onShareClick: () -> Unit,
    onBlockClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var rowMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("contact_item_${contact.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(
                name = contact.fullName,
                photoUri = contact.photoUri,
                colorIndex = contact.avatarColorIndex,
                size = 48.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.displayName(lastNameFirst),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (contact.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = CallWarningAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (contact.isBlocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Blocked",
                            tint = CallDeclineRed,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = PhoneNumberUtilsHelper.formatForDisplay(contact.phoneNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onCallClick,
                modifier = Modifier.testTag("contact_call_button_${contact.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call ${contact.fullName}",
                    tint = CallAcceptGreen
                )
            }

            Box {
                IconButton(
                    onClick = { rowMenuExpanded = true },
                    modifier = Modifier.testTag("contact_row_menu_${contact.id}")
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Contact actions")
                }
                DropdownMenu(
                    expanded = rowMenuExpanded,
                    onDismissRequest = { rowMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (contact.isFavorite) "Remove from Favorites" else "Add to Favorites") },
                        leadingIcon = {
                            Icon(
                                if (contact.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            rowMenuExpanded = false
                            onToggleFavorite()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Message") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null) },
                        onClick = {
                            rowMenuExpanded = false
                            onMessageClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Contact") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            rowMenuExpanded = false
                            onShareClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (contact.isBlocked) "Unblock Contact" else "Block Contact") },
                        leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                        onClick = {
                            rowMenuExpanded = false
                            onBlockClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Contact", color = CallDeclineRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = CallDeclineRed) },
                        onClick = {
                            rowMenuExpanded = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactScreen(
    existingContact: ContactEntity?,
    prefillPhone: String = "",
    prefillFavorite: Boolean = false,
    onSave: (
        firstName: String,
        lastName: String,
        phoneNumber: String,
        email: String,
        company: String,
        notes: String,
        photoUri: String?,
        isFavorite: Boolean
    ) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler(onBack = onCancel)
    val context = LocalContext.current

    var firstName by rememberSaveable(existingContact?.id) {
        mutableStateOf(existingContact?.firstName ?: "")
    }
    var lastName by rememberSaveable(existingContact?.id) {
        mutableStateOf(existingContact?.lastName ?: "")
    }
    var phoneNumber by rememberSaveable(existingContact?.id, prefillPhone) {
        mutableStateOf(existingContact?.phoneNumber ?: prefillPhone)
    }
    var email by rememberSaveable(existingContact?.id) {
        mutableStateOf(existingContact?.email ?: "")
    }
    var company by rememberSaveable(existingContact?.id) {
        mutableStateOf(existingContact?.company ?: "")
    }
    var notes by rememberSaveable(existingContact?.id) {
        mutableStateOf(existingContact?.notes ?: "")
    }
    var photoUri by rememberSaveable(existingContact?.id) {
        mutableStateOf(existingContact?.photoUri)
    }
    var isFavorite by rememberSaveable(existingContact?.id, prefillFavorite) {
        mutableStateOf(existingContact?.isFavorite ?: prefillFavorite)
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            photoUri = uri.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (existingContact == null) "Add Contact" else "Edit Contact")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("add_contact_cancel_icon")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("add_contact_cancel_button")
                    ) {
                        Text("CANCEL")
                    }
                    Button(
                        onClick = {
                            if (firstName.isBlank() && lastName.isBlank()) {
                                validationError = "Please enter a contact name."
                                return@Button
                            }
                            if (!PhoneNumberUtilsHelper.isValidPhoneNumber(phoneNumber)) {
                                validationError = "Enter a valid phone number (e.g., 01712345678 or +8801712345678)."
                                return@Button
                            }
                            validationError = null
                            onSave(
                                firstName,
                                lastName,
                                phoneNumber,
                                email,
                                company,
                                notes,
                                photoUri,
                                isFavorite
                            )
                        },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("add_contact_save_button")
                    ) {
                        Text("SAVE")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Photo Selector
            Box(
                modifier = Modifier
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("contact_photo_picker_button"),
                contentAlignment = Alignment.BottomEnd
            ) {
                ContactAvatar(
                    name = "$firstName $lastName".ifBlank { "?" },
                    photoUri = photoUri,
                    colorIndex = existingContact?.avatarColorIndex ?: 0,
                    size = 96.dp,
                    fontSize = 34.sp
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Choose photo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Text(
                text = if (photoUri == null) "Tap to add profile photo" else "Tap to change photo",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (validationError != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_validation_error_banner")
                ) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_first_name")
            )

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_last_name")
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it
                        validationError = null
                    },
                    label = { Text("Phone Number") },
                    placeholder = { Text("017XXXXXXXX or +8801XXXXXXXXX") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_phone_number")
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {
                            if (!phoneNumber.startsWith("+880")) {
                                phoneNumber = if (phoneNumber.startsWith("0")) {
                                    "+880" + phoneNumber.removePrefix("0")
                                } else {
                                    "+880$phoneNumber"
                                }
                            }
                        },
                        label = { Text("+880 (BD)") },
                        modifier = Modifier.testTag("prefix_bd_chip")
                    )
                    AssistChip(
                        onClick = {
                            if (!phoneNumber.startsWith("+1")) {
                                phoneNumber = "+1$phoneNumber"
                            }
                        },
                        label = { Text("+1 (INTL)") }
                    )
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_email")
            )

            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                label = { Text("Company") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_company")
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_notes")
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = CallWarningAmber
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Add to Favorites",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Switch(
                        checked = isFavorite,
                        onCheckedChange = { isFavorite = it },
                        modifier = Modifier.testTag("input_favorite_switch")
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("CANCEL")
                }
                Button(
                    onClick = {
                        if (firstName.isBlank() && lastName.isBlank()) {
                            validationError = "Please enter a contact name."
                            return@Button
                        }
                        if (!PhoneNumberUtilsHelper.isValidPhoneNumber(phoneNumber)) {
                            validationError = "Enter a valid phone number (e.g., 01712345678 or +8801712345678)."
                            return@Button
                        }
                        validationError = null
                        onSave(
                            firstName,
                            lastName,
                            phoneNumber,
                            email,
                            company,
                            notes,
                            photoUri,
                            isFavorite
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bottom_save_contact_button")
                ) {
                    Text("SAVE")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailsScreen(
    contact: ContactEntity,
    recentCalls: List<CallLogEntity>,
    isBlocked: Boolean,
    onCallClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    onVideoCallClick: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleBlockClick: () -> Unit,
    onDeleteContactClick: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Contact?") },
            text = { Text("Are you sure you want to delete ${contact.fullName} from your contacts?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteContactClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CallDeclineRed),
                    modifier = Modifier.testTag("confirm_delete_contact_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Details") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("contact_details_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.testTag("contact_details_edit_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit contact")
                    }
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.testTag("contact_details_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share contact")
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("contact_details_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete contact",
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
            // Header Card with Large Profile Photo & Quick Actions
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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ContactAvatar(
                            name = contact.fullName,
                            photoUri = contact.photoUri,
                            colorIndex = contact.avatarColorIndex,
                            size = 104.dp,
                            fontSize = 36.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = contact.fullName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("contact_details_name")
                        )
                        if (contact.company.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = contact.company,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))

                        // Quick Actions: CALL, MESSAGE, VIDEO, FAVORITE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            QuickActionPill(
                                icon = Icons.Default.Call,
                                label = "CALL",
                                active = true,
                                onClick = { onCallClick(contact.phoneNumber) },
                                testTag = "contact_quick_call_button"
                            )
                            QuickActionPill(
                                icon = Icons.AutoMirrored.Filled.Message,
                                label = "MESSAGE",
                                active = false,
                                onClick = { onMessageClick(contact.phoneNumber) },
                                testTag = "contact_quick_message_button"
                            )
                            QuickActionPill(
                                icon = Icons.Default.Videocam,
                                label = "VIDEO",
                                active = false,
                                onClick = { onVideoCallClick(contact.phoneNumber) },
                                testTag = "contact_quick_video_button"
                            )
                            QuickActionPill(
                                icon = if (contact.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                label = "FAVORITE",
                                active = contact.isFavorite,
                                onClick = onToggleFavorite,
                                testTag = "contact_quick_favorite_button"
                            )
                        }
                    }
                }
            }

            // Contact Information Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Contact Info",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCallClick(contact.phoneNumber) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = PhoneNumberUtilsHelper.formatForDisplay(contact.phoneNumber),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.testTag("contact_details_phone")
                                )
                                Text(
                                    text = "Mobile",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onMessageClick(contact.phoneNumber) }) {
                                Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Message")
                            }
                        }

                        if (contact.email.isNotBlank()) {
                            HorizontalDivider()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(contact.email, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "Email",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (contact.notes.isNotBlank()) {
                            HorizontalDivider()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Notes,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(contact.notes, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "Notes",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Secondary Management Actions (Edit, Share, Block, Delete)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onToggleBlockClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("contact_details_block_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = CallDeclineRed
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBlocked) "Unblock" else "Block",
                            color = CallDeclineRed
                        )
                    }
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = CallDeclineRed
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Contact", color = CallDeclineRed)
                    }
                }
            }

            // Recent Activity Section
            item {
                Text(
                    text = "Recent Activity (${recentCalls.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (recentCalls.isEmpty()) {
                item {
                    Text(
                        text = "No call history with ${contact.fullName} yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(recentCalls, key = { it.id }) { log ->
                    val visual = getCallTypeVisual(log.callType)
                    val (d, t) = formatCallTimestamp(log.timestamp)
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
                                    imageVector = visual.icon,
                                    contentDescription = null,
                                    tint = visual.tint,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(visual.label, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "$d, $t • ${log.simCarrierName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = formatCallDurationLong(log.durationSeconds),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    active: Boolean,
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
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
