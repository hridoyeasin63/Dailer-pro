package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BlockedNumberEntity
import com.example.data.local.CallLogEntity
import com.example.data.local.CallRecordType
import com.example.data.local.ContactEntity
import com.example.data.local.DialerDatabase
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.ContactNameFormat
import com.example.data.preferences.ContactSortOrder
import com.example.data.preferences.DialerSettings
import com.example.data.preferences.SettingsRepository
import com.example.data.repository.DialerRepository
import com.example.data.repository.PhoneNumberUtilsHelper
import com.example.telecom.ActiveCallInfo
import com.example.telecom.CallManager
import com.example.telecom.SimAccountInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PrimaryTab(val route: String, val label: String) {
    FAVORITES("favorites", "Favorites"),
    RECENTS("recents", "Recents"),
    CONTACTS("contacts", "Contacts"),
    KEYPAD("keypad", "Keypad")
}

enum class CallHistoryFilter(val label: String) {
    ALL("All"),
    MISSED("Missed"),
    INCOMING("Incoming"),
    OUTGOING("Outgoing"),
    REJECTED("Rejected"),
    BLOCKED("Blocked")
}

sealed interface AppSubScreen {
    data object None : AppSubScreen
    data object Search : AppSubScreen
    data object ManageFavorites : AppSubScreen
    data class AddEditContact(
        val contactId: Long? = null,
        val prefillPhone: String = "",
        val prefillFavorite: Boolean = false
    ) : AppSubScreen
    data class ContactDetails(val contactId: Long) : AppSubScreen
    data class CallDetails(val callLogId: Long) : AppSubScreen
    data object BlockedNumbers : AppSubScreen
    data object Settings : AppSubScreen
}

data class SearchResultsState(
    val query: String = "",
    val matchingFavorites: List<ContactEntity> = emptyList(),
    val matchingContacts: List<ContactEntity> = emptyList(),
    val matchingCallLogs: List<CallLogEntity> = emptyList()
)

data class PendingSimPrompt(
    val phoneNumber: String,
    val availableSims: List<SimAccountInfo>
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DialerDatabase.getInstance(application)
    val repository = DialerRepository(application, database.dialerDao())
    val settingsRepository = SettingsRepository(application)

    private val _selectedTab = MutableStateFlow(PrimaryTab.FAVORITES)
    val selectedTab: StateFlow<PrimaryTab> = _selectedTab.asStateFlow()

    private val _subScreenStack = MutableStateFlow<List<AppSubScreen>>(emptyList())
    val currentSubScreen: StateFlow<AppSubScreen> = _subScreenStack
        .combine(_selectedTab) { stack, _ -> stack.lastOrNull() ?: AppSubScreen.None }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSubScreen.None)

    private val _dialedNumber = MutableStateFlow("")
    val dialedNumber: StateFlow<String> = _dialedNumber.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _callHistoryFilter = MutableStateFlow(CallHistoryFilter.ALL)
    val callHistoryFilter: StateFlow<CallHistoryFilter> = _callHistoryFilter.asStateFlow()

    private val _selectedCallLogIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCallLogIds: StateFlow<Set<Long>> = _selectedCallLogIds.asStateFlow()

    private val _pendingSimPrompt = MutableStateFlow<PendingSimPrompt?>(null)
    val pendingSimPrompt: StateFlow<PendingSimPrompt?> = _pendingSimPrompt.asStateFlow()

    private val _isDefaultDialer = MutableStateFlow(CallManager.isDefaultDialer(application))
    val isDefaultDialer: StateFlow<Boolean> = _isDefaultDialer.asStateFlow()

    private val _availableSims = MutableStateFlow(CallManager.getAvailableSimAccounts(application))
    val availableSims: StateFlow<List<SimAccountInfo>> = _availableSims.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val settingsState: StateFlow<DialerSettings> = settingsRepository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        DialerSettings()
    )

    val activeCallState: StateFlow<ActiveCallInfo?> = CallManager.activeCallState
    val callErrorBanner: StateFlow<String?> = CallManager.callErrorBanner

    val favoritesState: StateFlow<List<ContactEntity>> = repository.favoriteContactsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val contactsState: StateFlow<List<ContactEntity>> = combine(
        repository.allContactsFlow,
        settingsState
    ) { contacts, settings ->
        when (settings.contactSortOrder) {
            ContactSortOrder.FIRST_NAME -> contacts.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.firstName.ifBlank { it.lastName } }
            )
            ContactSortOrder.LAST_NAME -> contacts.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.lastName.ifBlank { it.firstName } }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCallLogsState: StateFlow<List<CallLogEntity>> = combine(
        repository.allCallLogsFlow,
        _callHistoryFilter,
        settingsState
    ) { logs, filter, settings ->
        logs.filter { entry ->
            val allowedBySettings = when (entry.callType) {
                CallRecordType.MISSED -> settings.showMissedCalls
                CallRecordType.OUTGOING -> settings.showOutgoingCalls
                CallRecordType.INCOMING -> settings.showIncomingCalls
                CallRecordType.REJECTED, CallRecordType.BLOCKED -> true
            }
            if (!allowedBySettings) return@filter false

            when (filter) {
                CallHistoryFilter.ALL -> true
                CallHistoryFilter.MISSED -> entry.callType == CallRecordType.MISSED
                CallHistoryFilter.INCOMING -> entry.callType == CallRecordType.INCOMING
                CallHistoryFilter.OUTGOING -> entry.callType == CallRecordType.OUTGOING
                CallHistoryFilter.REJECTED -> entry.callType == CallRecordType.REJECTED
                CallHistoryFilter.BLOCKED -> entry.callType == CallRecordType.BLOCKED
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedNumbersState: StateFlow<List<BlockedNumberEntity>> = repository.allBlockedNumbersFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val keypadMatchingContacts: StateFlow<List<ContactEntity>> = combine(
        _dialedNumber,
        repository.allContactsFlow
    ) { rawQuery, contacts ->
        val query = rawQuery.trim()
        if (query.isEmpty()) return@combine emptyList()
        val normalizedQuery = PhoneNumberUtilsHelper.normalizeNumber(query)
        val digitsOnly = query.filter { it.isDigit() }

        contacts.filter { contact ->
            val numberMatch = contact.phoneNumber.contains(query, ignoreCase = true) ||
                (normalizedQuery.isNotEmpty() && contact.normalizedNumber.contains(normalizedQuery)) ||
                (digitsOnly.isNotEmpty() && contact.normalizedNumber.contains(digitsOnly))
            val t9Match = digitsOnly.length >= 2 &&
                PhoneNumberUtilsHelper.nameToT9Digits(contact.fullName).contains(digitsOnly)
            val nameMatch = contact.fullName.contains(query, ignoreCase = true)
            numberMatch || t9Match || nameMatch
        }.take(6)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResultsState: StateFlow<SearchResultsState> = combine(
        _searchQuery,
        repository.allContactsFlow,
        repository.allCallLogsFlow
    ) { rawQuery, contacts, callLogs ->
        val q = rawQuery.trim()
        if (q.isEmpty()) {
            return@combine SearchResultsState(
                query = "",
                matchingFavorites = contacts.filter { it.isFavorite }.take(8),
                matchingContacts = contacts.take(15),
                matchingCallLogs = callLogs.take(10)
            )
        }
        val normQ = PhoneNumberUtilsHelper.normalizeNumber(q)
        val matchedContacts = contacts.filter { c ->
            c.fullName.contains(q, ignoreCase = true) ||
                c.phoneNumber.contains(q, ignoreCase = true) ||
                (normQ.isNotEmpty() && c.normalizedNumber.contains(normQ)) ||
                c.company.contains(q, ignoreCase = true)
        }
        val matchedFavorites = matchedContacts.filter { it.isFavorite }
        val matchedLogs = callLogs.filter { log ->
            (log.contactName?.contains(q, ignoreCase = true) == true) ||
                log.phoneNumber.contains(q, ignoreCase = true) ||
                (normQ.isNotEmpty() && log.normalizedNumber.contains(normQ))
        }
        SearchResultsState(
            query = q,
            matchingFavorites = matchedFavorites,
            matchingContacts = matchedContacts,
            matchingCallLogs = matchedLogs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResultsState())

    fun selectTab(tab: PrimaryTab) {
        _selectedTab.value = tab
        _subScreenStack.value = emptyList()
        clearCallLogSelection()
    }

    fun openSubScreen(screen: AppSubScreen) {
        _subScreenStack.update { stack -> stack + screen }
    }

    fun navigateBack(): Boolean {
        if (_selectedCallLogIds.value.isNotEmpty()) {
            clearCallLogSelection()
            return true
        }
        val currentStack = _subScreenStack.value
        return if (currentStack.isNotEmpty()) {
            _subScreenStack.value = currentStack.dropLast(1)
            true
        } else if (_selectedTab.value != PrimaryTab.FAVORITES) {
            _selectedTab.value = PrimaryTab.FAVORITES
            true
        } else {
            false
        }
    }

    fun refreshSystemState(context: Context) {
        _isDefaultDialer.value = CallManager.isDefaultDialer(context)
        _availableSims.value = CallManager.getAvailableSimAccounts(context)
        viewModelScope.launch {
            repository.syncWithSystemProvidersIfPermitted()
        }
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    // --- KEYPAD ACTIONS ---
    fun appendKeypadDigit(context: Context, digit: Char) {
        val settings = settingsState.value
        if (settings.hapticFeedbackEnabled) {
            CallManager.triggerHapticFeedback(context, heavy = false)
        }
        if (settings.dialPadSoundsEnabled) {
            CallManager.playDialpadTone(digit)
        }
        _dialedNumber.update { current -> (current + digit).take(24) }
    }

    fun onKeypadLongPress(context: Context, digit: Char) {
        val settings = settingsState.value
        if (settings.hapticFeedbackEnabled) {
            CallManager.triggerHapticFeedback(context, heavy = true)
        }
        when (digit) {
            '0' -> _dialedNumber.update { current -> (current + "+").take(24) }
            '1' -> showMessage("Voicemail speed dial (*86)")
            else -> appendKeypadDigit(context, digit)
        }
    }

    fun backspaceKeypad() {
        _dialedNumber.update { current ->
            if (current.isNotEmpty()) current.dropLast(1) else ""
        }
    }

    fun clearKeypad() {
        _dialedNumber.value = ""
    }

    fun setDialedNumber(number: String) {
        _dialedNumber.value = number.trim().take(24)
    }

    // --- CALL INITIATION & DUAL-SIM ---
    fun initiateCall(context: Context, phoneNumber: String, explicitSimSlot: Int? = null) {
        val trimmed = phoneNumber.trim()
        if (!PhoneNumberUtilsHelper.isValidPhoneNumber(trimmed)) {
            showMessage(context.getString(com.example.R.string.error_invalid_number))
            return
        }

        val sims = CallManager.getAvailableSimAccounts(context)
        _availableSims.value = sims
        val settings = settingsState.value

        if (explicitSimSlot != null) {
            executeCall(context, trimmed, explicitSimSlot)
            return
        }

        // If user has a remembered/default SIM slot configured, or single SIM, dial immediately
        if (settings.defaultSimSlot in 1..2) {
            executeCall(context, trimmed, settings.defaultSimSlot)
            return
        }

        if (sims.size > 1) {
            _pendingSimPrompt.value = PendingSimPrompt(trimmed, sims)
        } else {
            executeCall(context, trimmed, sims.firstOrNull()?.slotIndex ?: 1)
        }
    }

    fun confirmSimSelectionAndCall(context: Context, simSlot: Int, rememberChoice: Boolean) {
        val prompt = _pendingSimPrompt.value ?: return
        _pendingSimPrompt.value = null
        if (rememberChoice) {
            viewModelScope.launch {
                settingsRepository.setDefaultSimSlot(simSlot, remember = true)
            }
        }
        executeCall(context, prompt.phoneNumber, simSlot)
    }

    fun dismissSimPrompt() {
        _pendingSimPrompt.value = null
    }

    private fun executeCall(context: Context, phoneNumber: String, simSlot: Int) {
        val settings = settingsState.value
        val result = CallManager.placeCall(
            context = context,
            rawNumber = phoneNumber,
            simSlot = simSlot,
            callerIdEnabled = settings.callerIdEnabled,
            hapticEnabled = settings.hapticFeedbackEnabled,
            proximityEnabled = settings.proximitySensorEnabled
        )
        result.onFailure { err ->
            showMessage(err.message ?: context.getString(com.example.R.string.error_call_failed))
        }
    }

    // --- SEARCH ---
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- CALL HISTORY MANAGEMENT ---
    fun setCallHistoryFilter(filter: CallHistoryFilter) {
        _callHistoryFilter.value = filter
    }

    fun toggleCallLogSelection(id: Long) {
        _selectedCallLogIds.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    fun clearCallLogSelection() {
        _selectedCallLogIds.value = emptySet()
    }

    fun deleteSelectedCallLogs() {
        val ids = _selectedCallLogIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteCallLogs(ids)
            _selectedCallLogIds.value = emptySet()
            showMessage("Deleted ${ids.size} call record(s)")
        }
    }

    fun deleteSingleCallLog(id: Long) {
        viewModelScope.launch {
            repository.deleteCallLog(id)
            showMessage("Call record deleted")
        }
    }

    fun deleteCallHistoryForNumber(phoneNumber: String) {
        viewModelScope.launch {
            repository.deleteCallLogsForNumber(phoneNumber)
            showMessage("Deleted call history for $phoneNumber")
        }
    }

    fun clearAllCallHistory() {
        viewModelScope.launch {
            repository.clearAllCallHistory()
            _selectedCallLogIds.value = emptySet()
            showMessage("All call history cleared")
        }
    }

    // --- CONTACTS & FAVORITES ---
    fun saveContact(
        existingId: Long? = null,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        email: String,
        company: String,
        notes: String,
        photoUri: String?,
        isFavorite: Boolean,
        onSaved: (Long) -> Unit = {}
    ) {
        if (!PhoneNumberUtilsHelper.isValidPhoneNumber(phoneNumber)) {
            showMessage("Please enter a valid phone number (e.g. 017XXXXXXXX or +8801XXXXXXXXX)")
            return
        }
        viewModelScope.launch {
            val savedId = repository.saveContact(
                existingId = existingId,
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                email = email,
                company = company,
                notes = notes,
                photoUri = photoUri,
                isFavorite = isFavorite
            )
            showMessage(if (existingId == null) "Contact saved" else "Contact updated")
            onSaved(savedId)
        }
    }

    fun deleteContact(contact: ContactEntity, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteContact(contact)
            showMessage("${contact.fullName} deleted")
            onDeleted()
        }
    }

    fun toggleFavorite(contact: ContactEntity) {
        viewModelScope.launch {
            val newFav = !contact.isFavorite
            repository.toggleFavorite(contact.id, newFav)
            showMessage(
                if (newFav) "${contact.fullName} added to Favorites"
                else "${contact.fullName} removed from Favorites"
            )
        }
    }

    // --- BLOCKING ---
    fun blockNumber(phoneNumber: String, label: String = "") {
        if (!PhoneNumberUtilsHelper.isValidPhoneNumber(phoneNumber)) {
            showMessage("Please enter a valid phone number to block")
            return
        }
        viewModelScope.launch {
            repository.blockNumber(phoneNumber, label)
            showMessage("$phoneNumber blocked")
        }
    }

    fun unblockNumber(phoneNumber: String) {
        viewModelScope.launch {
            repository.unblockNumber(phoneNumber)
            showMessage("$phoneNumber unblocked")
        }
    }

    // --- EXTERNAL INTENTS (SMS, SHARE, VIDEO) ---
    fun sendSmsToNumber(context: Context, phoneNumber: String, body: String = "") {
        runCatching {
            val uri = Uri.parse("smsto:${Uri.encode(phoneNumber)}")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                if (body.isNotEmpty()) putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            showMessage("No messaging app available on this device")
        }
    }

    fun shareContact(context: Context, contact: ContactEntity) {
        runCatching {
            val shareText = buildString {
                appendLine(contact.fullName)
                appendLine("Phone: ${contact.phoneNumber}")
                if (contact.email.isNotBlank()) appendLine("Email: ${contact.email}")
                if (contact.company.isNotBlank()) appendLine("Company: ${contact.company}")
            }.trim()

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, contact.fullName)
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            val chooser = Intent.createChooser(sendIntent, "Share Contact").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    // --- SETTINGS UPDATES ---
    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun updateDefaultSimSlot(slot: Int) {
        viewModelScope.launch { settingsRepository.setDefaultSimSlot(slot) }
    }

    fun updateCallerId(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCallerIdEnabled(enabled) }
    }

    fun updateSwipeToAnswer(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSwipeToAnswerEnabled(enabled) }
    }

    fun updateCallVibration(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCallVibrationEnabled(enabled) }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHapticFeedbackEnabled(enabled) }
    }

    fun updateDialPadSounds(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDialPadSoundsEnabled(enabled) }
    }

    fun updateProximitySensor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setProximitySensorEnabled(enabled) }
    }

    fun updateContactSortOrder(order: ContactSortOrder) {
        viewModelScope.launch { settingsRepository.setContactSortOrder(order) }
    }

    fun updateContactNameFormat(format: ContactNameFormat) {
        viewModelScope.launch { settingsRepository.setContactNameFormat(format) }
    }

    fun updateDefaultAccount(account: String) {
        viewModelScope.launch { settingsRepository.setDefaultAccount(account) }
    }

    fun updateShowMissedCalls(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowMissedCalls(show) }
    }

    fun updateShowOutgoingCalls(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowOutgoingCalls(show) }
    }

    fun updateShowIncomingCalls(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowIncomingCalls(show) }
    }

    fun dismissDefaultDialerBanner() {
        viewModelScope.launch { settingsRepository.setDefaultDialerPromptDismissed(true) }
    }
}
