package com.example.data.repository

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.BlockedNumberContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.example.data.local.BlockedNumberEntity
import com.example.data.local.CallLogEntity
import com.example.data.local.CallRecordType
import com.example.data.local.ContactEntity
import com.example.data.local.DialerDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Utility for validating, normalizing, and formatting phone numbers,
 * with explicit support for Bangladesh (+880 / 01XXXXXXXXX), US, and International numbers.
 */
object PhoneNumberUtilsHelper {

    fun normalizeNumber(raw: String): String {
        val cleaned = raw.trim().filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (cleaned.isEmpty()) return ""

        // Normalize Bangladesh +8801XXXXXXXXX or 8801XXXXXXXXX to canonical 01XXXXXXXXX for matching
        return when {
            cleaned.startsWith("+8801") && cleaned.length == 14 -> "0" + cleaned.substring(4)
            cleaned.startsWith("8801") && cleaned.length == 13 -> "0" + cleaned.substring(3)
            cleaned.startsWith("+1") && cleaned.length == 12 -> cleaned.substring(2)
            else -> cleaned
        }
    }

    fun isValidPhoneNumber(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return false

        // Allow USSD / MMI codes like *121#, *247#, *#06#
        if ((trimmed.startsWith("*") || trimmed.startsWith("#")) && trimmed.endsWith("#") && trimmed.length >= 3) {
            return true
        }

        // Extract only digits and leading +
        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.length < 3 || digitsOnly.length > 15) return false

        // Check valid characters (digits, spaces, dashes, parentheses, plus, *, #)
        val validChars = trimmed.all {
            it.isDigit() || it == '+' || it == '-' || it == ' ' || it == '(' || it == ')' || it == '*' || it == '#'
        }
        if (!validChars) return false

        // Specific check for Bangladesh numbers if starting with 01 or +880 or 880
        val compact = trimmed.replace("[\\s\\-()]".toRegex(), "")
        if (compact.startsWith("+880")) {
            // +880 followed by 10 digits (e.g., +8801712345678)
            val rest = compact.removePrefix("+880")
            return rest.length in 8..10 && rest.all { it.isDigit() }
        }
        if (compact.startsWith("01") && compact.length == 11) {
            // Bangladesh mobile operators: 013, 014, 015, 016, 017, 018, 019
            val thirdDigit = compact[2]
            return thirdDigit in '3'..'9' && compact.all { it.isDigit() }
        }

        return true
    }

    fun formatForDisplay(raw: String): String {
        val compact = raw.trim().replace("[\\s\\-()]".toRegex(), "")
        return when {
            // Bangladesh +880 1XXX-XXXXXX
            compact.startsWith("+8801") && compact.length == 14 -> {
                "+880 ${compact.substring(4, 8)}-${compact.substring(8)}"
            }
            // Bangladesh local 01XXX-XXXXXX
            compact.startsWith("01") && compact.length == 11 && compact.all { it.isDigit() } -> {
                "${compact.substring(0, 5)}-${compact.substring(5)}"
            }
            // Standard 10-digit number
            compact.length == 10 && compact.all { it.isDigit() } -> {
                "(${compact.substring(0, 3)}) ${compact.substring(3, 6)}-${compact.substring(6)}"
            }
            else -> raw.trim()
        }
    }

    fun t9LettersForDigit(digit: Char): String = when (digit) {
        '2' -> "ABC"
        '3' -> "DEF"
        '4' -> "GHI"
        '5' -> "JKL"
        '6' -> "MNO"
        '7' -> "PQRS"
        '8' -> "TUV"
        '9' -> "WXYZ"
        '0' -> "+"
        else -> ""
    }

    fun nameToT9Digits(name: String): String {
        val sb = StringBuilder()
        for (ch in name.uppercase()) {
            when (ch) {
                in "ABC" -> sb.append('2')
                in "DEF" -> sb.append('3')
                in "GHI" -> sb.append('4')
                in "JKL" -> sb.append('5')
                in "MNO" -> sb.append('6')
                in "PQRS" -> sb.append('7')
                in "TUV" -> sb.append('8')
                in "WXYZ" -> sb.append('9')
                ' ' -> sb.append('0')
            }
        }
        return sb.toString()
    }
}

class DialerRepository(
    private val context: Context,
    private val dao: DialerDao
) {
    val allContactsFlow: Flow<List<ContactEntity>> = dao.getAllContactsFlow()
    val favoriteContactsFlow: Flow<List<ContactEntity>> = dao.getFavoriteContactsFlow()
    val allCallLogsFlow: Flow<List<CallLogEntity>> = dao.getAllCallLogsFlow()
    val allBlockedNumbersFlow: Flow<List<BlockedNumberEntity>> = dao.getAllBlockedNumbersFlow()

    fun getContactByIdFlow(id: Long): Flow<ContactEntity?> = dao.getContactByIdFlow(id)
    fun getCallLogByIdFlow(id: Long): Flow<CallLogEntity?> = dao.getCallLogByIdFlow(id)
    fun getCallLogsForNumberFlow(normalizedNumber: String): Flow<List<CallLogEntity>> =
        dao.getCallLogsForNumberFlow(normalizedNumber)
    fun isNumberBlockedFlow(normalizedNumber: String): Flow<Boolean> =
        dao.isNumberBlockedFlow(normalizedNumber)

    suspend fun findContactByNumber(number: String): ContactEntity? {
        val normalized = PhoneNumberUtilsHelper.normalizeNumber(number)
        if (normalized.isEmpty()) return null
        return dao.findContactByNumber(normalized, number.trim())
    }

    suspend fun getCallCountForNumber(number: String): Int {
        val normalized = PhoneNumberUtilsHelper.normalizeNumber(number)
        return dao.getCallCountForNumber(normalized)
    }

    suspend fun saveContact(
        existingId: Long? = null,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        email: String,
        company: String,
        notes: String,
        photoUri: String?,
        isFavorite: Boolean = false
    ): Long = withContext(Dispatchers.IO) {
        val normalized = PhoneNumberUtilsHelper.normalizeNumber(phoneNumber)
        val isBlocked = dao.isNumberBlocked(normalized)
        val colorIdx = abs((firstName + phoneNumber).hashCode()) % 8

        val entity = if (existingId != null && existingId > 0L) {
            val existing = dao.getContactById(existingId)
            ContactEntity(
                id = existingId,
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                phoneNumber = phoneNumber.trim(),
                normalizedNumber = normalized,
                email = email.trim(),
                company = company.trim(),
                notes = notes.trim(),
                photoUri = photoUri ?: existing?.photoUri,
                avatarColorIndex = existing?.avatarColorIndex ?: colorIdx,
                isFavorite = existing?.isFavorite ?: isFavorite,
                isBlocked = isBlocked,
                preferredSimSlot = existing?.preferredSimSlot,
                systemContactId = existing?.systemContactId,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
        } else {
            val systemId = writeContactToSystemIfPermitted(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                phoneNumber = phoneNumber.trim(),
                email = email.trim(),
                company = company.trim(),
                notes = notes.trim()
            )
            ContactEntity(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                phoneNumber = phoneNumber.trim(),
                normalizedNumber = normalized,
                email = email.trim(),
                company = company.trim(),
                notes = notes.trim(),
                photoUri = photoUri,
                avatarColorIndex = colorIdx,
                isFavorite = isFavorite,
                isBlocked = isBlocked,
                systemContactId = systemId
            )
        }

        val savedId = dao.insertContact(entity)
        dao.updateCallLogsContactInfo(
            normalizedNumber = normalized,
            contactName = entity.fullName,
            photoUri = entity.photoUri
        )
        savedId
    }

    suspend fun deleteContact(contact: ContactEntity) = withContext(Dispatchers.IO) {
        dao.deleteContact(contact)
        dao.updateCallLogsContactInfo(
            normalizedNumber = contact.normalizedNumber,
            contactName = null,
            photoUri = null
        )
    }

    suspend fun toggleFavorite(contactId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        dao.setContactFavorite(contactId, isFavorite)
    }

    suspend fun recordCall(
        phoneNumber: String,
        callType: CallRecordType,
        durationSeconds: Long,
        simSlot: Int = 1,
        simCarrierName: String = "SIM $simSlot",
        timestamp: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val normalized = PhoneNumberUtilsHelper.normalizeNumber(phoneNumber)
        val matchedContact = dao.findContactByNumber(normalized, phoneNumber)
        val colorIdx = matchedContact?.avatarColorIndex ?: (abs(normalized.hashCode()) % 8)

        val logEntity = CallLogEntity(
            phoneNumber = phoneNumber.trim(),
            normalizedNumber = normalized,
            contactName = matchedContact?.fullName,
            photoUri = matchedContact?.photoUri,
            avatarColorIndex = colorIdx,
            callType = callType,
            timestamp = timestamp,
            durationSeconds = durationSeconds,
            simSlot = simSlot,
            simCarrierName = simCarrierName
        )

        writeCallLogToSystemIfPermitted(logEntity)
        dao.insertCallLog(logEntity)
    }

    suspend fun deleteCallLog(callId: Long) = withContext(Dispatchers.IO) {
        dao.deleteCallLogById(callId)
    }

    suspend fun deleteCallLogs(callIds: List<Long>) = withContext(Dispatchers.IO) {
        dao.deleteCallLogsByIds(callIds)
    }

    suspend fun deleteCallLogsForNumber(phoneNumber: String) = withContext(Dispatchers.IO) {
        val normalized = PhoneNumberUtilsHelper.normalizeNumber(phoneNumber)
        dao.deleteCallLogsForNumber(normalized)
    }

    suspend fun clearAllCallHistory() = withContext(Dispatchers.IO) {
        dao.clearAllCallLogs()
    }

    suspend fun blockNumber(phoneNumber: String, label: String = "") = withContext(Dispatchers.IO) {
        val normalized = PhoneNumberUtilsHelper.normalizeNumber(phoneNumber)
        if (normalized.isEmpty()) return@withContext
        val matchedContact = dao.findContactByNumber(normalized, phoneNumber)
        val resolvedLabel = label.ifEmpty { matchedContact?.fullName ?: "" }

        dao.insertBlockedNumber(
            BlockedNumberEntity(
                phoneNumber = phoneNumber.trim(),
                normalizedNumber = normalized,
                label = resolvedLabel
            )
        )
        dao.updateContactBlockedByNumber(normalized, true)
        syncBlockedNumberWithSystemIfDefaultDialer(phoneNumber.trim(), block = true)
    }

    suspend fun unblockNumber(phoneNumber: String) = withContext(Dispatchers.IO) {
        val normalized = PhoneNumberUtilsHelper.normalizeNumber(phoneNumber)
        dao.unblockByNormalizedNumber(normalized)
        dao.updateContactBlockedByNumber(normalized, false)
        syncBlockedNumberWithSystemIfDefaultDialer(phoneNumber.trim(), block = false)
    }

    suspend fun isNumberBlocked(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        val normalized = PhoneNumberUtilsHelper.normalizeNumber(phoneNumber)
        if (normalized.isEmpty()) return@withContext false
        dao.isNumberBlocked(normalized)
    }

    /**
     * Synchronizes device contacts and call history from Android's ContactsContract and CallLog
     * when the user grants READ_CONTACTS and READ_CALL_LOG permissions.
     */
    suspend fun syncWithSystemProvidersIfPermitted() = withContext(Dispatchers.IO) {
        syncSystemContacts()
        syncSystemCallLogs()
    }

    private suspend fun syncSystemContacts() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        runCatching {
            val existingContacts = dao.getAllContactsList()
            val existingByNormalized = existingContacts.associateBy { it.normalizedNumber }

            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.STARRED
            )

            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val starredIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

                while (cursor.moveToNext()) {
                    val rawNumber = if (numIdx >= 0) cursor.getString(numIdx) ?: "" else ""
                    val normalized = PhoneNumberUtilsHelper.normalizeNumber(rawNumber)
                    if (normalized.isEmpty() || existingByNormalized.containsKey(normalized)) continue

                    val sysId = if (idIdx >= 0) cursor.getLong(idIdx) else null
                    val displayName = if (nameIdx >= 0) cursor.getString(nameIdx) ?: rawNumber else rawNumber
                    val photoUri = if (photoIdx >= 0) cursor.getString(photoIdx) else null
                    val isStarred = if (starredIdx >= 0) cursor.getInt(starredIdx) == 1 else false

                    val parts = displayName.trim().split(" ", limit = 2)
                    val first = parts.firstOrNull() ?: rawNumber
                    val last = if (parts.size > 1) parts[1] else ""
                    val isBlocked = dao.isNumberBlocked(normalized)

                    dao.insertContact(
                        ContactEntity(
                            firstName = first,
                            lastName = last,
                            phoneNumber = rawNumber,
                            normalizedNumber = normalized,
                            photoUri = photoUri,
                            avatarColorIndex = abs((displayName + normalized).hashCode()) % 8,
                            isFavorite = isStarred,
                            isBlocked = isBlocked,
                            systemContactId = sysId
                        )
                    )
                }
            }
        }
    }

    private suspend fun syncSystemCallLogs() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        runCatching {
            val existingLogs = dao.getAllCallLogsList()
            val existingSystemIds = existingLogs.mapNotNull { it.systemCallLogId }.toSet()

            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            )

            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)

                var count = 0
                while (cursor.moveToNext() && count < 200) {
                    count++
                    val sysId = if (idIdx >= 0) cursor.getLong(idIdx) else continue
                    if (existingSystemIds.contains(sysId)) continue

                    val number = if (numIdx >= 0) cursor.getString(numIdx) ?: "" else ""
                    if (number.isEmpty()) continue
                    val normalized = PhoneNumberUtilsHelper.normalizeNumber(number)
                    val cachedName = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val rawType = if (typeIdx >= 0) cursor.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                    val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                    val duration = if (durIdx >= 0) cursor.getLong(durIdx) else 0L

                    val recordType = when (rawType) {
                        CallLog.Calls.INCOMING_TYPE -> CallRecordType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> CallRecordType.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallRecordType.MISSED
                        CallLog.Calls.REJECTED_TYPE -> CallRecordType.REJECTED
                        CallLog.Calls.BLOCKED_TYPE -> CallRecordType.BLOCKED
                        else -> CallRecordType.INCOMING
                    }

                    val matchedContact = dao.findContactByNumber(normalized, number)
                    dao.insertCallLog(
                        CallLogEntity(
                            phoneNumber = number,
                            normalizedNumber = normalized,
                            contactName = matchedContact?.fullName ?: cachedName,
                            photoUri = matchedContact?.photoUri,
                            avatarColorIndex = matchedContact?.avatarColorIndex ?: (abs(normalized.hashCode()) % 8),
                            callType = recordType,
                            timestamp = date,
                            durationSeconds = duration,
                            simSlot = 1,
                            simCarrierName = "SIM 1",
                            systemCallLogId = sysId
                        )
                    )
                }
            }
        }
    }

    private fun writeContactToSystemIfPermitted(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        email: String,
        company: String,
        notes: String
    ): Long? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return runCatching {
            val ops = ArrayList<ContentProviderOperation>()
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )
            val fullName = listOf(firstName, lastName).filter { it.isNotEmpty() }.joinToString(" ")
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, fullName)
                    .build()
            )
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
                    .build()
            )
            if (email.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                        .build()
                )
            }
            if (company.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                        .build()
                )
            }
            if (notes.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, notes)
                        .build()
                )
            }
            val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            results.firstOrNull()?.uri?.lastPathSegment?.toLongOrNull()
        }.getOrNull()
    }

    private fun writeCallLogToSystemIfPermitted(callLog: CallLogEntity) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALL_LOG) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        runCatching {
            val sysType = when (callLog.callType) {
                CallRecordType.INCOMING -> CallLog.Calls.INCOMING_TYPE
                CallRecordType.OUTGOING -> CallLog.Calls.OUTGOING_TYPE
                CallRecordType.MISSED -> CallLog.Calls.MISSED_TYPE
                CallRecordType.REJECTED -> CallLog.Calls.REJECTED_TYPE
                CallRecordType.BLOCKED -> CallLog.Calls.BLOCKED_TYPE
            }
            val values = ContentValues().apply {
                put(CallLog.Calls.NUMBER, callLog.phoneNumber)
                put(CallLog.Calls.TYPE, sysType)
                put(CallLog.Calls.DATE, callLog.timestamp)
                put(CallLog.Calls.DURATION, callLog.durationSeconds)
                put(CallLog.Calls.NEW, if (callLog.callType == CallRecordType.MISSED) 1 else 0)
            }
            context.contentResolver.insert(CallLog.Calls.CONTENT_URI, values)
        }
    }

    private fun syncBlockedNumberWithSystemIfDefaultDialer(phoneNumber: String, block: Boolean) {
        runCatching {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val isDefaultDialer = telecomManager?.defaultDialerPackage == context.packageName
            if (!isDefaultDialer) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                BlockedNumberContract.canCurrentUserBlockNumbers(context)
            ) {
                if (block) {
                    val values = ContentValues().apply {
                        put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, phoneNumber)
                    }
                    context.contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values)
                } else {
                    BlockedNumberContract.unblock(context, phoneNumber)
                }
            }
        }
    }
}
