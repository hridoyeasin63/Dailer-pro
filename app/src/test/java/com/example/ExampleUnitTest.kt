package com.example

import com.example.data.local.CallLogEntity
import com.example.data.local.CallRecordType
import com.example.data.local.ContactEntity
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.ContactNameFormat
import com.example.data.preferences.ContactSortOrder
import com.example.data.preferences.DialerSettings
import com.example.data.repository.PhoneNumberUtilsHelper
import com.example.telecom.ActiveCallInfo
import com.example.telecom.AudioRouteState
import com.example.telecom.CallStatus
import com.example.telecom.SimAccountInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `validate Bangladesh and international phone numbers`() {
        // Valid Bangladesh local & +880 numbers
        assertTrue(PhoneNumberUtilsHelper.isValidPhoneNumber("01712345678"))
        assertTrue(PhoneNumberUtilsHelper.isValidPhoneNumber("+8801712345678"))
        assertTrue(PhoneNumberUtilsHelper.isValidPhoneNumber("01819-554433"))
        assertTrue(PhoneNumberUtilsHelper.isValidPhoneNumber("+1 (415) 555-2671"))
        assertTrue(PhoneNumberUtilsHelper.isValidPhoneNumber("*121#"))

        // Invalid phone numbers
        assertFalse(PhoneNumberUtilsHelper.isValidPhoneNumber(""))
        assertFalse(PhoneNumberUtilsHelper.isValidPhoneNumber("12"))
        assertFalse(PhoneNumberUtilsHelper.isValidPhoneNumber("01112345678")) // invalid BD operator prefix 011
        assertFalse(PhoneNumberUtilsHelper.isValidPhoneNumber("abc1234567"))
    }

    @Test
    fun `normalize Bangladesh +880 and local numbers to same key`() {
        val normLocal = PhoneNumberUtilsHelper.normalizeNumber("01712-345678")
        val normIntl = PhoneNumberUtilsHelper.normalizeNumber("+880 1712 345678")
        val normNoPlus = PhoneNumberUtilsHelper.normalizeNumber("8801712345678")

        assertEquals("01712345678", normLocal)
        assertEquals(normLocal, normIntl)
        assertEquals(normLocal, normNoPlus)
    }

    @Test
    fun `T9 name conversion and formatting work accurately`() {
        assertEquals("56460363", PhoneNumberUtilsHelper.nameToT9Digits("John Doe"))
        assertEquals("01712-345678", PhoneNumberUtilsHelper.formatForDisplay("01712345678"))
        assertEquals("+880 1712-345678", PhoneNumberUtilsHelper.formatForDisplay("+8801712345678"))
    }

    @Test
    fun `ContactEntity displays name, initials, and supports photoUri`() {
        val contactWithPhoto = ContactEntity(
            id = 1L,
            firstName = "Arifa",
            lastName = "Sultana",
            phoneNumber = "+8801855667788",
            normalizedNumber = "01855667788",
            email = "arifa@example.com",
            photoUri = "content://media/external/images/media/42",
            isFavorite = true
        )
        assertEquals("Arifa Sultana", contactWithPhoto.fullName)
        assertEquals("Sultana, Arifa", contactWithPhoto.displayName(lastNameFirst = true))
        assertEquals("Arifa Sultana", contactWithPhoto.displayName(lastNameFirst = false))
        assertEquals("AS", contactWithPhoto.initials)
        assertNotNull(contactWithPhoto.photoUri)
        assertTrue(contactWithPhoto.isFavorite)
    }

    @Test
    fun `ActiveCallInfo caller ID fallback, duration formatting, and fullscreen photo`() {
        val photoUri = "content://media/external/images/media/42"
        val unknownCaller = ActiveCallInfo(
            phoneNumber = "01911223344",
            callerName = null,
            photoUri = null,
            status = CallStatus.RINGING,
            isIncoming = true,
            durationSeconds = 42L
        )
        assertEquals("Unknown Number", unknownCaller.displayCallerTitle)
        assertEquals("00:42", unknownCaller.formattedDuration)
        assertNull(unknownCaller.photoUri)

        val knownCaller = unknownCaller.copy(
            callerName = "Rahim Uddin",
            photoUri = photoUri,
            audioRoute = AudioRouteState.SPEAKER,
            durationSeconds = 151L
        )
        assertEquals("Rahim Uddin", knownCaller.displayCallerTitle)
        assertEquals("02:31", knownCaller.formattedDuration)
        assertEquals(photoUri, knownCaller.photoUri)
        assertTrue(knownCaller.isSpeakerOn)
        assertFalse(knownCaller.isBluetoothOn)
    }

    @Test
    fun `SimAccountInfo and DialerSettings preferences logic`() {
        val sim1 = SimAccountInfo(slotIndex = 1, subscriptionId = 1, displayName = "Grameenphone", carrierName = "GP")
        val sim2 = SimAccountInfo(slotIndex = 2, subscriptionId = 2, displayName = "Robi", carrierName = "Robi Axiata")

        assertEquals(1, sim1.slotIndex)
        assertEquals(2, sim2.slotIndex)

        val settings = DialerSettings(
            themeMode = AppThemeMode.DARK,
            defaultSimSlot = 2,
            rememberSimPreference = true,
            contactSortOrder = ContactSortOrder.LAST_NAME,
            contactNameFormat = ContactNameFormat.LAST_NAME_FIRST
        )
        assertEquals(AppThemeMode.DARK, settings.themeMode)
        assertEquals(2, settings.defaultSimSlot)
        assertTrue(settings.rememberSimPreference)
        assertEquals(ContactSortOrder.LAST_NAME, settings.contactSortOrder)
    }

    @Test
    fun `CallLogEntity supports all call types and SIM carrier info`() {
        val log = CallLogEntity(
            phoneNumber = "01711223344",
            normalizedNumber = "01711223344",
            contactName = "Tanvir Ahmed",
            photoUri = "content://photos/1",
            callType = CallRecordType.INCOMING,
            durationSeconds = 120L,
            simSlot = 1,
            simCarrierName = "SIM 1"
        )
        assertEquals(CallRecordType.INCOMING, log.callType)
        assertEquals(120L, log.durationSeconds)
        assertEquals(1, log.simSlot)
    }
}
