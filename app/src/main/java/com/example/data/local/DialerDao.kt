package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DialerDao {

    // --- CONTACTS ---
    @Query("SELECT * FROM contacts ORDER BY firstName COLLATE NOCASE ASC, lastName COLLATE NOCASE ASC")
    fun getAllContactsFlow(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY firstName COLLATE NOCASE ASC, lastName COLLATE NOCASE ASC")
    suspend fun getAllContactsList(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE isFavorite = 1 ORDER BY firstName COLLATE NOCASE ASC, lastName COLLATE NOCASE ASC")
    fun getFavoriteContactsFlow(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    fun getContactByIdFlow(id: Long): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE normalizedNumber = :normalizedNumber OR phoneNumber = :rawNumber LIMIT 1")
    suspend fun findContactByNumber(normalizedNumber: String, rawNumber: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :contactId")
    suspend fun deleteContactById(contactId: Long)

    @Query("UPDATE contacts SET isFavorite = :isFavorite WHERE id = :contactId")
    suspend fun setContactFavorite(contactId: Long, isFavorite: Boolean)

    @Query("UPDATE contacts SET isBlocked = :isBlocked WHERE normalizedNumber = :normalizedNumber")
    suspend fun updateContactBlockedByNumber(normalizedNumber: String, isBlocked: Boolean)

    // --- CALL LOGS ---
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogsFlow(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    suspend fun getAllCallLogsList(): List<CallLogEntity>

    @Query("SELECT * FROM call_logs WHERE normalizedNumber = :normalizedNumber ORDER BY timestamp DESC")
    fun getCallLogsForNumberFlow(normalizedNumber: String): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE id = :callId LIMIT 1")
    fun getCallLogByIdFlow(callId: Long): Flow<CallLogEntity?>

    @Query("SELECT * FROM call_logs WHERE id = :callId LIMIT 1")
    suspend fun getCallLogById(callId: Long): CallLogEntity?

    @Query("SELECT COUNT(*) FROM call_logs WHERE normalizedNumber = :normalizedNumber")
    suspend fun getCallCountForNumber(normalizedNumber: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLogEntity): Long

    @Query("DELETE FROM call_logs WHERE id = :callId")
    suspend fun deleteCallLogById(callId: Long)

    @Query("DELETE FROM call_logs WHERE id IN (:callIds)")
    suspend fun deleteCallLogsByIds(callIds: List<Long>)

    @Query("DELETE FROM call_logs WHERE normalizedNumber = :normalizedNumber")
    suspend fun deleteCallLogsForNumber(normalizedNumber: String)

    @Query("DELETE FROM call_logs")
    suspend fun clearAllCallLogs()

    @Query("UPDATE call_logs SET contactName = :contactName, photoUri = :photoUri WHERE normalizedNumber = :normalizedNumber")
    suspend fun updateCallLogsContactInfo(normalizedNumber: String, contactName: String?, photoUri: String?)

    // --- BLOCKED NUMBERS ---
    @Query("SELECT * FROM blocked_numbers ORDER BY createdAt DESC")
    fun getAllBlockedNumbersFlow(): Flow<List<BlockedNumberEntity>>

    @Query("SELECT * FROM blocked_numbers ORDER BY createdAt DESC")
    suspend fun getAllBlockedNumbersList(): List<BlockedNumberEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE normalizedNumber = :normalizedNumber)")
    suspend fun isNumberBlocked(normalizedNumber: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE normalizedNumber = :normalizedNumber)")
    fun isNumberBlockedFlow(normalizedNumber: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedNumber(blockedNumber: BlockedNumberEntity): Long

    @Query("DELETE FROM blocked_numbers WHERE normalizedNumber = :normalizedNumber")
    suspend fun unblockByNormalizedNumber(normalizedNumber: String)

    @Query("DELETE FROM blocked_numbers WHERE id = :id")
    suspend fun deleteBlockedNumberById(id: Long)
}
