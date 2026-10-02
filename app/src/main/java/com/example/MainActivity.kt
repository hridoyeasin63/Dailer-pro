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
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.AppThemeMode
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
import com.example.ui.screens.IncomingCallScreen
import com.example.ui.screens.KeypadScreen
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

@Composable
fun PhoneDialerApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val currentSubScreen by viewModel.currentSubScreen.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val isDefaultDialer by viewModel.isDefaultDialer.collectAsStateWithLifecycle()
    val availableSims by viewModel.availableSims.collectAsStateWithLifecycle()

    val contacts by viewModel.contactsState.collectAsStateWithLifecycle()
    val callLogs by viewModel.filteredCallLogsState.collectAsStateWithLifecycle()
    val activeFilter by viewModel.callHistoryFilter.collectAsStateWithLifecycle()
    val selectedCallLogIds by viewModel.selectedCallLogIds.collectAsStateWithLifecycle()
    val blockedNumbers by viewModel.blockedNumbersState.collectAsStateWithLifecycle()

    val dialedNumber by viewModel.dialedNumber.collectAsStateWithLifecycle()
    val keypadMatches by viewModel.keypadMatchingContacts.collectAsStateWithLifecycle()
    val keypadSuggestions by viewModel.keypadSuggestions.collectAsStateWithLifecycle()
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

    BackHandler(enabled = currentSubScreen != AppSubScreen.None || selectedCallLogIds.isNotEmpty() || selectedTab != PrimaryTab.KEYPAD) {
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

    // REQUIRE DEFAULT DIALER: App does not operate until set as default phone app
    if (!isDefaultDialer) {
        DefaultDialerRequiredGateScreen(
            onRequestDefaultDialer = requestDefaultDialerRole
        )
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

        is AppSubScreen.AddEditContact -> {
            val existing = sub.contactId?.let { id ->
                contacts.find { it.id == id }
            }
            AddEditContactScreen(
                existingContact = existing,
                prefillPhone = sub.prefillPhone,
                onSave = { first, last, phone, email, company, notes, photo ->
                    viewModel.saveContact(
                        existingId = sub.contactId,
                        firstName = first,
                        lastName = last,
                        phoneNumber = phone,
                        email = email,
                        company = company,
                        notes = notes,
                        photoUri = photo,
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
                            PrimaryTab.KEYPAD -> {
                                KeypadScreen(
                                    dialedNumber = dialedNumber,
                                    suggestions = keypadSuggestions,
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
                                    onCallClick = { number, simSlot ->
                                        viewModel.initiateCall(context, number, explicitSimSlot = simSlot)
                                    },
                                    onSelectSimSlot = viewModel::updateDefaultSimSlot,
                                    onAddContactWithNumber = { number ->
                                        viewModel.openSubScreen(
                                            AppSubScreen.AddEditContact(prefillPhone = number)
                                        )
                                    },
                                    onContactSuggestionClick = { contact ->
                                        viewModel.setDialedNumber(contact.phoneNumber)
                                    },
                                    onSuggestionClick = { number ->
                                        viewModel.setDialedNumber(number)
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
                                        viewModel.initiateCall(context, number, forceSimPromptIfDualSim = true)
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
                                    onCallClick = { number ->
                                        viewModel.initiateCall(context, number, forceSimPromptIfDualSim = true)
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
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-screen blocking gate displayed when the app is not set as default dialer.
 * Guarantees that the app will not work without being set as default phone app.
 */
@Composable
private fun DefaultDialerRequiredGateScreen(
    onRequestDefaultDialer: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        val isShortScreen = maxHeight < 580.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(if (isShortScreen) 72.dp else 96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (isShortScreen) 36.dp else 48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.default_dialer_required_title),
                style = if (isShortScreen) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.default_dialer_required_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GateFeatureRow(icon = Icons.Default.Phone, text = "Dial, receive & manage phone calls")
                    GateFeatureRow(icon = Icons.Default.Security, text = "Screen spam & block unwanted numbers")
                    GateFeatureRow(icon = Icons.Default.Lock, text = "Private, on-device contact storage")
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onRequestDefaultDialer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("gate_set_default_dialer_button"),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text(
                    text = stringResource(R.string.set_default_phone_app),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun GateFeatureRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getTabIcon(tab: PrimaryTab, selected: Boolean): ImageVector {
    return when (tab) {
        PrimaryTab.KEYPAD -> if (selected) Icons.Filled.Dialpad else Icons.Outlined.Dialpad
        PrimaryTab.RECENTS -> if (selected) Icons.Filled.History else Icons.Outlined.History
        PrimaryTab.CONTACTS -> if (selected) Icons.Filled.People else Icons.Outlined.PeopleOutline
    }
}
