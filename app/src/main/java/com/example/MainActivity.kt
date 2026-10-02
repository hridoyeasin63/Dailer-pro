package com.example

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.AppThemeMode
import com.example.data.repository.PhoneNumberUtilsHelper
import com.example.telecom.CallManager
import com.example.telecom.CallStatus
import com.example.ui.AppSubScreen
import com.example.ui.MainViewModel
import com.example.ui.PrimaryTab
import com.example.ui.components.DualSimChooserDialog
import com.example.ui.screens.ActiveCallScreen
import com.example.ui.screens.AddEditContactScreen
import com.example.ui.screens.BlockedNumbersScreen
import com.example.ui.screens.CallDetailsScreen
import com.example.ui.screens.ContactDetailsScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.IncomingCallScreen
import com.example.ui.screens.KeypadScreen
import com.example.ui.screens.ManageFavoritesScreen
import com.example.ui.screens.RecentsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            val settings by viewModel.settingsState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (settings.themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                PhoneDialerApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSystemState(this)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_DIAL, Intent.ACTION_VIEW, Intent.ACTION_CALL -> {
                val number = intent.data?.schemeSpecificPart
                if (!number.isNullOrBlank()) {
                    viewModel.selectTab(PrimaryTab.KEYPAD)
                    viewModel.setDialedNumber(number)
                }
            }
        }
    }
}

fun getRequiredDialerPermissions(): Array<String> {
    val list = mutableListOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        list.add(Manifest.permission.ANSWER_PHONE_CALLS)
        list.add(Manifest.permission.READ_PHONE_NUMBERS)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        list.add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    return list.toTypedArray()
}

fun hasAllCorePermissions(context: Context): Boolean {
    val core = listOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.CALL_PHONE
    )
    return core.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun PhoneDialerApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val currentSubScreen by viewModel.currentSubScreen.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val isDefaultDialer by viewModel.isDefaultDialer.collectAsStateWithLifecycle()
    val availableSims by viewModel.availableSims.collectAsStateWithLifecycle()

    val favorites by viewModel.favoritesState.collectAsStateWithLifecycle()
    val contacts by viewModel.contactsState.collectAsStateWithLifecycle()
    val callLogs by viewModel.filteredCallLogsState.collectAsStateWithLifecycle()
    val activeFilter by viewModel.callHistoryFilter.collectAsStateWithLifecycle()
    val selectedCallLogIds by viewModel.selectedCallLogIds.collectAsStateWithLifecycle()
    val blockedNumbers by viewModel.blockedNumbersState.collectAsStateWithLifecycle()

    val dialedNumber by viewModel.dialedNumber.collectAsStateWithLifecycle()
    val keypadMatches by viewModel.keypadMatchingContacts.collectAsStateWithLifecycle()
    val searchState by viewModel.searchResultsState.collectAsStateWithLifecycle()

    val activeCall by viewModel.activeCallState.collectAsStateWithLifecycle()
    val callErrorBanner by viewModel.callErrorBanner.collectAsStateWithLifecycle()
    val pendingSimPrompt by viewModel.pendingSimPrompt.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var showPermissionRationaleDialog by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshSystemState(context)
    }

    val defaultDialerRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSystemState(context)
    }

    val requestDefaultDialerRole: () -> Unit = {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                        defaultDialerRoleLauncher.launch(intent)
                    } else {
                        viewModel.showMessage("Phone is already set as your default dialer")
                    }
                }
            } else {
                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                }
                defaultDialerRoleLauncher.launch(intent)
            }
        }.onFailure {
            viewModel.showMessage("Default dialer role request is not supported on this device")
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(callErrorBanner) {
        callErrorBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            CallManager.clearErrorBanner()
        }
    }

    BackHandler(enabled = currentSubScreen != AppSubScreen.None || selectedCallLogIds.isNotEmpty() || selectedTab != PrimaryTab.FAVORITES) {
        viewModel.navigateBack()
    }

    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            title = { Text(stringResource(R.string.permission_rationale_title)) },
            text = { Text(stringResource(R.string.permission_rationale_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationaleDialog = false
                        permissionLauncher.launch(getRequiredDialerPermissions())
                    },
                    modifier = Modifier.testTag("grant_permissions_confirm_button")
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionRationaleDialog = false },
                    modifier = Modifier.testTag("grant_permissions_dismiss_button")
                ) {
                    Text("Not now")
                }
            }
        )
    }

    // Dual-SIM Selector Dialog
    pendingSimPrompt?.let { prompt ->
        DualSimChooserDialog(
            phoneNumber = prompt.phoneNumber,
            sims = prompt.availableSims,
            onSimSelected = { slot, rememberChoice ->
                viewModel.confirmSimSelectionAndCall(context, slot, rememberChoice)
            },
            onDismiss = { viewModel.dismissSimPrompt() }
        )
    }

    // Full-screen Active or Incoming Call UI overrides normal screen when a call is active
    val currentCall = activeCall
    if (currentCall != null) {
        if (currentCall.status == CallStatus.RINGING && currentCall.isIncoming) {
            IncomingCallScreen(
                callInfo = currentCall,
                onAcceptClick = {
                    CallManager.acceptIncomingCall(context, settings.hapticFeedbackEnabled)
                },
                onDeclineClick = {
                    CallManager.declineIncomingCall(context, settings.hapticFeedbackEnabled)
                },
                onRejectWithMessage = { replyText ->
                    CallManager.declineIncomingCall(context, settings.hapticFeedbackEnabled, replyText)
                    viewModel.sendSmsToNumber(context, currentCall.phoneNumber, replyText)
                },
                onSilenceRinger = {
                    CallManager.silenceIncomingRinger(context)
                },
                onBlockCaller = {
                    viewModel.blockNumber(currentCall.phoneNumber)
                    CallManager.declineIncomingCall(context, settings.hapticFeedbackEnabled)
                }
            )
        } else {
            ActiveCallScreen(
                callInfo = currentCall,
                onToggleMute = { CallManager.toggleMute(context) },
                onToggleSpeaker = { CallManager.toggleSpeaker(context) },
                onToggleBluetooth = { CallManager.toggleBluetoothRoute(context) },
                onToggleHold = { CallManager.toggleHold() },
                onAddCallClick = {
                    viewModel.selectTab(PrimaryTab.KEYPAD)
                    viewModel.showMessage("Switch to keypad to dial second party")
                },
                onSendDtmfDigit = { digit ->
                    CallManager.sendDtmfTone(
                        context = context,
                        digit = digit,
                        playTone = settings.dialPadSoundsEnabled,
                        haptic = settings.hapticFeedbackEnabled
                    )
                },
                onEndCallClick = {
                    CallManager.endCall(context, settings.hapticFeedbackEnabled)
                }
            )
        }
        return
    }

    // Sub-screen navigation layer
    when (val sub = currentSubScreen) {
        is AppSubScreen.Search -> {
            SearchScreen(
                searchState = searchState,
                onQueryChange = viewModel::updateSearchQuery,
                onContactClick = { contact ->
                    viewModel.openSubScreen(AppSubScreen.ContactDetails(contact.id))
                },
                onCallLogClick = { log ->
                    viewModel.openSubScreen(AppSubScreen.CallDetails(log.id))
                },
                onQuickCallClick = { number ->
                    viewModel.initiateCall(context, number)
                },
                onBack = { viewModel.navigateBack() }
            )
            return
        }

        is AppSubScreen.ManageFavorites -> {
            ManageFavoritesScreen(
                allContacts = contacts,
                onToggleFavorite = viewModel::toggleFavorite,
                onCreateNewFavoriteContact = {
                    viewModel.openSubScreen(
                        AppSubScreen.AddEditContact(
                            contactId = null,
                            prefillFavorite = true
                        )
                    )
                },
                onBack = { viewModel.navigateBack() }
            )
            return
        }

        is AppSubScreen.AddEditContact -> {
            val existing = sub.contactId?.let { id ->
                contacts.find { it.id == id }
            }
            AddEditContactScreen(
                existingContact = existing,
                prefillPhone = sub.prefillPhone,
                prefillFavorite = sub.prefillFavorite,
                onSave = { first, last, phone, email, company, notes, photo, fav ->
                    viewModel.saveContact(
                        existingId = sub.contactId,
                        firstName = first,
                        lastName = last,
                        phoneNumber = phone,
                        email = email,
                        company = company,
                        notes = notes,
                        photoUri = photo,
                        isFavorite = fav,
                        onSaved = { viewModel.navigateBack() }
                    )
                },
                onCancel = { viewModel.navigateBack() }
            )
            return
        }

        is AppSubScreen.ContactDetails -> {
            val contact = contacts.find { it.id == sub.contactId }
            if (contact == null) {
                LaunchedEffect(Unit) { viewModel.navigateBack() }
            } else {
                val contactHistory by viewModel.repository
                    .getCallLogsForNumberFlow(contact.normalizedNumber)
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val isBlocked by viewModel.repository
                    .isNumberBlockedFlow(contact.normalizedNumber)
                    .collectAsStateWithLifecycle(initialValue = contact.isBlocked)

                ContactDetailsScreen(
                    contact = contact,
                    recentCalls = contactHistory,
                    isBlocked = isBlocked,
                    onCallClick = { number -> viewModel.initiateCall(context, number) },
                    onMessageClick = { number -> viewModel.sendSmsToNumber(context, number) },
                    onVideoCallClick = { number -> viewModel.initiateCall(context, number) },
                    onToggleFavorite = { viewModel.toggleFavorite(contact) },
                    onEditClick = {
                        viewModel.openSubScreen(AppSubScreen.AddEditContact(contactId = contact.id))
                    },
                    onShareClick = { viewModel.shareContact(context, contact) },
                    onToggleBlockClick = {
                        if (isBlocked) {
                            viewModel.unblockNumber(contact.phoneNumber)
                        } else {
                            viewModel.blockNumber(contact.phoneNumber, contact.fullName)
                        }
                    },
                    onDeleteContactClick = {
                        viewModel.deleteContact(contact) {
                            viewModel.navigateBack()
                        }
                    },
                    onBack = { viewModel.navigateBack() }
                )
            }
            return
        }

        is AppSubScreen.CallDetails -> {
            val callLog by viewModel.repository
                .getCallLogByIdFlow(sub.callLogId)
                .collectAsStateWithLifecycle(initialValue = null)
            val currentLog = callLog
            if (currentLog != null) {
                val historyForNumber by viewModel.repository
                    .getCallLogsForNumberFlow(currentLog.normalizedNumber)
                    .collectAsStateWithLifecycle(initialValue = listOf(currentLog))
                val isBlocked by viewModel.repository
                    .isNumberBlockedFlow(currentLog.normalizedNumber)
                    .collectAsStateWithLifecycle(initialValue = false)
                val matchedContact = contacts.find { it.normalizedNumber == currentLog.normalizedNumber }

                CallDetailsScreen(
                    callLog = currentLog,
                    historyForContact = historyForNumber,
                    matchedContact = matchedContact,
                    isBlocked = isBlocked,
                    onCallClick = { number -> viewModel.initiateCall(context, number) },
                    onMessageClick = { number -> viewModel.sendSmsToNumber(context, number) },
                    onAddOrEditContactClick = {
                        if (matchedContact != null) {
                            viewModel.openSubScreen(AppSubScreen.AddEditContact(contactId = matchedContact.id))
                        } else {
                            viewModel.openSubScreen(AppSubScreen.AddEditContact(prefillPhone = currentLog.phoneNumber))
                        }
                    },
                    onToggleFavoriteClick = {
                        matchedContact?.let { viewModel.toggleFavorite(it) }
                    },
                    onToggleBlockClick = {
                        if (isBlocked) {
                            viewModel.unblockNumber(currentLog.phoneNumber)
                        } else {
                            viewModel.blockNumber(currentLog.phoneNumber, matchedContact?.fullName ?: "")
                        }
                    },
                    onDeleteSingleCallLog = {
                        viewModel.deleteSingleCallLog(currentLog.id)
                        viewModel.navigateBack()
                    },
                    onDeleteAllHistoryForNumber = {
                        viewModel.deleteCallHistoryForNumber(currentLog.phoneNumber)
                        viewModel.navigateBack()
                    },
                    onBack = { viewModel.navigateBack() }
                )
            }
            return
        }

        is AppSubScreen.BlockedNumbers -> {
            BlockedNumbersScreen(
                blockedNumbers = blockedNumbers,
                onAddBlockedNumber = viewModel::blockNumber,
                onUnblockNumber = viewModel::unblockNumber,
                onBack = { viewModel.navigateBack() }
            )
            return
        }

        is AppSubScreen.Settings -> {
            SettingsScreen(
                settings = settings,
                isDefaultDialer = isDefaultDialer,
                availableSims = availableSims,
                onRequestDefaultDialer = requestDefaultDialerRole,
                onRequestPermissions = {
                    showPermissionRationaleDialog = true
                },
                onOpenBlockedNumbers = {
                    viewModel.openSubScreen(AppSubScreen.BlockedNumbers)
                },
                onUpdateTheme = viewModel::updateThemeMode,
                onUpdateDefaultSim = viewModel::updateDefaultSimSlot,
                onUpdateCallerId = viewModel::updateCallerId,
                onUpdateSwipeToAnswer = viewModel::updateSwipeToAnswer,
                onUpdateCallVibration = viewModel::updateCallVibration,
                onUpdateHapticFeedback = viewModel::updateHapticFeedback,
                onUpdateDialPadSounds = viewModel::updateDialPadSounds,
                onUpdateProximitySensor = viewModel::updateProximitySensor,
                onUpdateSortOrder = viewModel::updateContactSortOrder,
                onUpdateNameFormat = viewModel::updateContactNameFormat,
                onUpdateDefaultAccount = viewModel::updateDefaultAccount,
                onUpdateShowMissedCalls = viewModel::updateShowMissedCalls,
                onUpdateShowOutgoingCalls = viewModel::updateShowOutgoingCalls,
                onUpdateShowIncomingCalls = viewModel::updateShowIncomingCalls,
                onTriggerIncomingCallDiagnostic = {
                    CallManager.onIncomingCallReceived(
                        context = context,
                        phoneNumber = "+8801712345678",
                        simSlot = 1,
                        callerIdEnabled = settings.callerIdEnabled,
                        vibrateOnRing = settings.callVibrationEnabled
                    )
                },
                onBack = { viewModel.navigateBack() }
            )
            return
        }

        AppSubScreen.None -> Unit
    }

    // Main Responsive Navigation Shell (NavigationBar on Compact, NavigationRail on Wide/Landscape)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
                        PrimaryTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = getTabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(modifier = Modifier.testTag("side_navigation_rail")) {
                        PrimaryTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = getTabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    Crossfade(targetState = selectedTab, label = "main_tab_crossfade") { tab ->
                        when (tab) {
                            PrimaryTab.FAVORITES -> {
                                FavoritesScreen(
                                    favorites = favorites,
                                    allContactsCount = contacts.size,
                                    nameFormat = settings.contactNameFormat,
                                    showDefaultDialerBanner = !isDefaultDialer && !settings.defaultDialerPromptDismissed,
                                    onRequestDefaultDialer = requestDefaultDialerRole,
                                    onDismissDefaultDialerBanner = viewModel::dismissDefaultDialerBanner,
                                    onOpenSearch = { viewModel.openSubScreen(AppSubScreen.Search) },
                                    onAddFavoriteClick = {
                                        if (contacts.isEmpty()) {
                                            viewModel.openSubScreen(
                                                AppSubScreen.AddEditContact(prefillFavorite = true)
                                            )
                                        } else {
                                            viewModel.openSubScreen(AppSubScreen.ManageFavorites)
                                        }
                                    },
                                    onManageFavoritesClick = {
                                        viewModel.openSubScreen(AppSubScreen.ManageFavorites)
                                    },
                                    onContactClick = { contact ->
                                        viewModel.openSubScreen(AppSubScreen.ContactDetails(contact.id))
                                    },
                                    onQuickCallClick = { number ->
                                        viewModel.initiateCall(context, number)
                                    },
                                    onOpenBlockedNumbers = {
                                        viewModel.openSubScreen(AppSubScreen.BlockedNumbers)
                                    },
                                    onOpenSettings = {
                                        viewModel.openSubScreen(AppSubScreen.Settings)
                                    }
                                )
                            }

                            PrimaryTab.RECENTS -> {
                                RecentsScreen(
                                    callLogs = callLogs,
                                    activeFilter = activeFilter,
                                    selectedIds = selectedCallLogIds,
                                    onFilterChange = viewModel::setCallHistoryFilter,
                                    onToggleSelect = viewModel::toggleCallLogSelection,
                                    onClearSelection = viewModel::clearCallLogSelection,
                                    onDeleteSelected = viewModel::deleteSelectedCallLogs,
                                    onClearAllHistory = viewModel::clearAllCallHistory,
                                    onCallEntryClick = { log ->
                                        viewModel.openSubScreen(AppSubScreen.CallDetails(log.id))
                                    },
                                    onQuickCallClick = { number ->
                                        viewModel.initiateCall(context, number)
                                    },
                                    onOpenSearch = { viewModel.openSubScreen(AppSubScreen.Search) },
                                    onOpenBlockedNumbers = {
                                        viewModel.openSubScreen(AppSubScreen.BlockedNumbers)
                                    },
                                    onOpenSettings = {
                                        viewModel.openSubScreen(AppSubScreen.Settings)
                                    }
                                )
                            }

                            PrimaryTab.CONTACTS -> {
                                ContactsScreen(
                                    contacts = contacts,
                                    nameFormat = settings.contactNameFormat,
                                    onAddContactClick = {
                                        viewModel.openSubScreen(AppSubScreen.AddEditContact())
                                    },
                                    onContactClick = { contact ->
                                        viewModel.openSubScreen(AppSubScreen.ContactDetails(contact.id))
                                    },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onCallClick = { number ->
                                        viewModel.initiateCall(context, number)
                                    },
                                    onMessageClick = { number ->
                                        viewModel.sendSmsToNumber(context, number)
                                    },
                                    onShareContact = { contact ->
                                        viewModel.shareContact(context, contact)
                                    },
                                    onBlockContact = { contact ->
                                        if (contact.isBlocked) {
                                            viewModel.unblockNumber(contact.phoneNumber)
                                        } else {
                                            viewModel.blockNumber(contact.phoneNumber, contact.fullName)
                                        }
                                    },
                                    onDeleteContact = { contact ->
                                        viewModel.deleteContact(contact)
                                    },
                                    onOpenSearch = { viewModel.openSubScreen(AppSubScreen.Search) },
                                    onOpenBlockedNumbers = {
                                        viewModel.openSubScreen(AppSubScreen.BlockedNumbers)
                                    },
                                    onOpenSettings = {
                                        viewModel.openSubScreen(AppSubScreen.Settings)
                                    }
                                )
                            }

                            PrimaryTab.KEYPAD -> {
                                KeypadScreen(
                                    dialedNumber = dialedNumber,
                                    matchingContacts = keypadMatches,
                                    availableSims = availableSims,
                                    defaultSimSlot = settings.defaultSimSlot,
                                    onDigitPress = { digit ->
                                        viewModel.appendKeypadDigit(context, digit)
                                    },
                                    onDigitLongPress = { digit ->
                                        viewModel.onKeypadLongPress(context, digit)
                                    },
                                    onBackspace = viewModel::backspaceKeypad,
                                    onClear = viewModel::clearKeypad,
                                    onPasteNumber = { pasted ->
                                        val cleaned = pasted.filter {
                                            it.isDigit() || it == '+' || it == '*' || it == '#'
                                        }
                                        if (cleaned.isNotEmpty()) {
                                            viewModel.setDialedNumber(cleaned)
                                        }
                                    },
                                    onCallClick = { number ->
                                        viewModel.initiateCall(context, number)
                                    },
                                    onSelectSimSlot = viewModel::updateDefaultSimSlot,
                                    onAddContactWithNumber = { number ->
                                        viewModel.openSubScreen(
                                            AppSubScreen.AddEditContact(prefillPhone = number)
                                        )
                                    },
                                    onContactSuggestionClick = { contact ->
                                        viewModel.setDialedNumber(contact.phoneNumber)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getTabIcon(tab: PrimaryTab, selected: Boolean): ImageVector {
    return when (tab) {
        PrimaryTab.FAVORITES -> if (selected) Icons.Filled.Star else Icons.Outlined.StarOutline
        PrimaryTab.RECENTS -> if (selected) Icons.Filled.History else Icons.Outlined.History
        PrimaryTab.CONTACTS -> if (selected) Icons.Filled.People else Icons.Outlined.PeopleOutline
        PrimaryTab.KEYPAD -> if (selected) Icons.Filled.Dialpad else Icons.Outlined.Dialpad
    }
}
