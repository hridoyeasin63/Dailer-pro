package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dialer_settings")

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ContactSortOrder {
    FIRST_NAME,
    LAST_NAME
}

enum class ContactNameFormat {
    FIRST_NAME_FIRST,
    LAST_NAME_FIRST
}

data class DialerSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val defaultSimSlot: Int = 0, // 0 = Ask every time, 1 = SIM 1, 2 = SIM 2
    val rememberSimPreference: Boolean = false,
    val callerIdEnabled: Boolean = true,
    val swipeToAnswerEnabled: Boolean = true,
    val callVibrationEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val dialPadSoundsEnabled: Boolean = true,
    val proximitySensorEnabled: Boolean = true,
    val contactSortOrder: ContactSortOrder = ContactSortOrder.FIRST_NAME,
    val contactNameFormat: ContactNameFormat = ContactNameFormat.FIRST_NAME_FIRST,
    val defaultAccount: String = "Device & SIM Storage",
    val showMissedCalls: Boolean = true,
    val showOutgoingCalls: Boolean = true,
    val showIncomingCalls: Boolean = true,
    val defaultDialerPromptDismissed: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_SIM_SLOT = intPreferencesKey("default_sim_slot")
        val REMEMBER_SIM = booleanPreferencesKey("remember_sim")
        val CALLER_ID = booleanPreferencesKey("caller_id")
        val SWIPE_TO_ANSWER = booleanPreferencesKey("swipe_to_answer")
        val CALL_VIBRATION = booleanPreferencesKey("call_vibration")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val DIAL_PAD_SOUNDS = booleanPreferencesKey("dial_pad_sounds")
        val PROXIMITY_SENSOR = booleanPreferencesKey("proximity_sensor")
        val CONTACT_SORT_ORDER = stringPreferencesKey("contact_sort_order")
        val CONTACT_NAME_FORMAT = stringPreferencesKey("contact_name_format")
        val DEFAULT_ACCOUNT = stringPreferencesKey("default_account")
        val SHOW_MISSED = booleanPreferencesKey("show_missed")
        val SHOW_OUTGOING = booleanPreferencesKey("show_outgoing")
        val SHOW_INCOMING = booleanPreferencesKey("show_incoming")
        val DEFAULT_DIALER_DISMISSED = booleanPreferencesKey("default_dialer_dismissed")
    }

    val settingsFlow: Flow<DialerSettings> = context.dataStore.data.map { prefs ->
        DialerSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { AppThemeMode.valueOf(it) }.getOrDefault(AppThemeMode.SYSTEM)
            } ?: AppThemeMode.SYSTEM,
            defaultSimSlot = prefs[Keys.DEFAULT_SIM_SLOT] ?: 0,
            rememberSimPreference = prefs[Keys.REMEMBER_SIM] ?: false,
            callerIdEnabled = prefs[Keys.CALLER_ID] ?: true,
            swipeToAnswerEnabled = prefs[Keys.SWIPE_TO_ANSWER] ?: true,
            callVibrationEnabled = prefs[Keys.CALL_VIBRATION] ?: true,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            dialPadSoundsEnabled = prefs[Keys.DIAL_PAD_SOUNDS] ?: true,
            proximitySensorEnabled = prefs[Keys.PROXIMITY_SENSOR] ?: true,
            contactSortOrder = prefs[Keys.CONTACT_SORT_ORDER]?.let {
                runCatching { ContactSortOrder.valueOf(it) }.getOrDefault(ContactSortOrder.FIRST_NAME)
            } ?: ContactSortOrder.FIRST_NAME,
            contactNameFormat = prefs[Keys.CONTACT_NAME_FORMAT]?.let {
                runCatching { ContactNameFormat.valueOf(it) }.getOrDefault(ContactNameFormat.FIRST_NAME_FIRST)
            } ?: ContactNameFormat.FIRST_NAME_FIRST,
            defaultAccount = prefs[Keys.DEFAULT_ACCOUNT] ?: "Device & SIM Storage",
            showMissedCalls = prefs[Keys.SHOW_MISSED] ?: true,
            showOutgoingCalls = prefs[Keys.SHOW_OUTGOING] ?: true,
            showIncomingCalls = prefs[Keys.SHOW_INCOMING] ?: true,
            defaultDialerPromptDismissed = prefs[Keys.DEFAULT_DIALER_DISMISSED] ?: false
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDefaultSimSlot(slot: Int, remember: Boolean = slot != 0) {
        context.dataStore.edit {
            it[Keys.DEFAULT_SIM_SLOT] = slot
            it[Keys.REMEMBER_SIM] = remember
        }
    }

    suspend fun setCallerIdEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CALLER_ID] = enabled }
    }

    suspend fun setSwipeToAnswerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SWIPE_TO_ANSWER] = enabled }
    }

    suspend fun setCallVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CALL_VIBRATION] = enabled }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setDialPadSoundsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DIAL_PAD_SOUNDS] = enabled }
    }

    suspend fun setProximitySensorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PROXIMITY_SENSOR] = enabled }
    }

    suspend fun setContactSortOrder(order: ContactSortOrder) {
        context.dataStore.edit { it[Keys.CONTACT_SORT_ORDER] = order.name }
    }

    suspend fun setContactNameFormat(format: ContactNameFormat) {
        context.dataStore.edit { it[Keys.CONTACT_NAME_FORMAT] = format.name }
    }

    suspend fun setDefaultAccount(account: String) {
        context.dataStore.edit { it[Keys.DEFAULT_ACCOUNT] = account }
    }

    suspend fun setShowMissedCalls(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_MISSED] = show }
    }

    suspend fun setShowOutgoingCalls(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_OUTGOING] = show }
    }

    suspend fun setShowIncomingCalls(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_INCOMING] = show }
    }

    suspend fun setDefaultDialerPromptDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[Keys.DEFAULT_DIALER_DISMISSED] = dismissed }
    }
}
