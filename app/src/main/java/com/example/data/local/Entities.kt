package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CallRecordType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED,
    BLOCKED
}

@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["normalizedNumber"]),
        Index(value = ["firstName", "lastName"])
    ]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val firstName: String,
    val lastName: String = "",
    val phoneNumber: String,
    val normalizedNumber: String,
    val email: String = "",
    val company: String = "",
    val notes: String = "",
    val photoUri: String? = null,
    val avatarColorIndex: Int = 0,
    val isFavorite: Boolean = false,
    val isBlocked: Boolean = false,
    val preferredSimSlot: Int? = null, // 1 or 2, null for default
    val systemContactId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val fullName: String
        get() = listOf(firstName.trim(), lastName.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .ifEmpty { phoneNumber }

    fun displayName(lastNameFirst: Boolean = false): String {
        val f = firstName.trim()
        val l = lastName.trim()
        return when {
            f.isNotEmpty() && l.isNotEmpty() -> if (lastNameFirst) "$l, $f" else "$f $l"
            f.isNotEmpty() -> f
            l.isNotEmpty() -> l
            else -> phoneNumber
        }
    }

    val initials: String
        get() {
            val f = firstName.trim().firstOrNull()?.uppercaseChar()
            val l = lastName.trim().firstOrNull()?.uppercaseChar()
            return when {
                f != null && l != null -> "$f$l"
                f != null -> "$f"
                l != null -> "$l"
                else -> "#"
            }
        }
}

@Entity(
    tableName = "call_logs",
    indices = [
        Index(value = ["normalizedNumber"]),
        Index(value = ["timestamp"])
    ]
)
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val phoneNumber: String,
    val normalizedNumber: String,
    val contactName: String? = null,
    val photoUri: String? = null,
    val avatarColorIndex: Int = 0,
    val callType: CallRecordType,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0L,
    val simSlot: Int = 1,
    val simCarrierName: String = "SIM 1",
    val systemCallLogId: Long? = null
)

@Entity(
    tableName = "blocked_numbers",
    indices = [Index(value = ["normalizedNumber"], unique = true)]
)
data class BlockedNumberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val phoneNumber: String,
    val normalizedNumber: String,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
