package com.example.telecom

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.data.local.CallRecordType
import com.example.data.local.DialerDatabase
import com.example.data.repository.DialerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Official Android CallScreeningService implementation to screen and block incoming calls
 * matching numbers in the user's Blocked Numbers list.
 */
class DialerCallScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle
        val rawNumber = handle?.schemeSpecificPart ?: ""

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            callDetails.callDirection != Call.Details.DIRECTION_INCOMING
        ) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        serviceScope.launch {
            val db = DialerDatabase.getInstance(applicationContext)
            val repo = DialerRepository(applicationContext, db.dialerDao())
            val isBlocked = repo.isNumberBlocked(rawNumber)

            if (isBlocked) {
                val response = CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(true)
                    .build()
                respondToCall(callDetails, response)

                repo.recordCall(
                    phoneNumber = rawNumber,
                    callType = CallRecordType.BLOCKED,
                    durationSeconds = 0L,
                    simSlot = 1,
                    simCarrierName = "Blocked"
                )
            } else {
                val response = CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()
                respondToCall(callDetails, response)
            }
        }
    }
}
