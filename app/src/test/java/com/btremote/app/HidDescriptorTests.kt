package com.btremote.app

import com.btremote.app.bluetooth.HidDescriptors
import com.btremote.app.bluetooth.HidKeyCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HidDescriptorTests {

    @Test
    fun descriptorIsNonEmpty() {
        assertTrue(HidDescriptors.COMBINED_DESCRIPTOR.isNotEmpty())
    }

    @Test
    fun reportIdsAreUnique() {
        val mouse = HidDescriptors.REPORT_ID_MOUSE.toInt()
        val keyboard = HidDescriptors.REPORT_ID_KEYBOARD.toInt()
        val consumer = HidDescriptors.REPORT_ID_CONSUMER.toInt()
        assertTrue(setOf(mouse, keyboard, consumer).size == 3)
    }

    @Test
    fun fromCharResolvesLowercaseLetters() {
        val (key, modifier) = HidKeyCode.fromChar('a')!!
        assertEquals(HidKeyCode.A, key)
        assertEquals(0, modifier)
    }

    @Test
    fun fromCharResolvesUppercaseLetters() {
        val (key, modifier) = HidKeyCode.fromChar('A')!!
        assertEquals(HidKeyCode.A, key)
        assertNotNull(modifier)
        assertTrue(modifier != 0)
    }
}
