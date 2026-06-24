package com.btremote.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "HidReportSender"

@Singleton
class HidReportSender @Inject constructor() {

    @Volatile
    private var hidDevice: BluetoothHidDevice? = null

    @Volatile
    private var connectedDevice: BluetoothDevice? = null

    @Volatile
    private var mouseButtons: Int = 0

    @Volatile
    private var lastReportSentTime: Long = 0L

    private val accumX = AtomicInteger(0)
    private val accumY = AtomicInteger(0)
    private val accumScroll = AtomicInteger(0)
    private val accumHScroll = AtomicInteger(0)
    private val mouseDirty = AtomicBoolean(false)
    private val coalesceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var coalesceJob: Job? = null
    private val mouseRateMs = 8L  // ~125 Hz — matches commercial HID device polling rate

    private val kbMutex = Mutex()
    private val kbSlots = IntArray(6)
    @Volatile private var kbModifiers: Int = 0

    fun attach(hidDevice: BluetoothHidDevice?, device: BluetoothDevice?) {
        this.hidDevice = hidDevice
        this.connectedDevice = device
        if (hidDevice != null && device != null) {
            startCoalesceLoop()
            coalesceScope.launch {
                delay(150)
                sendMouseReport(0, 0, 0, 0)
                delay(50)
                sendBlankKeyboardReport()
                delay(50)
                sendConsumerBlank()
            }
        } else {
            stopCoalesceLoop()
            mouseButtons = 0
            kbSlots.fill(0)
            kbModifiers = 0
            accumX.set(0); accumY.set(0); accumScroll.set(0); accumHScroll.set(0)
            mouseDirty.set(false)
        }
    }

    fun isReady(): Boolean = hidDevice != null && connectedDevice != null

    private fun startCoalesceLoop() {
        if (coalesceJob?.isActive == true) return
        coalesceJob = coalesceScope.launch {
            lastReportSentTime = System.currentTimeMillis()
            while (true) {
                if (mouseDirty.compareAndSet(true, false)) {
                    val rawX = accumX.get()
                    val rawY = accumY.get()
                    val rawScroll = accumScroll.get()
                    val rawHScroll = accumHScroll.get()

                    val dx = rawX.coerceIn(-127, 127)
                    val dy = rawY.coerceIn(-127, 127)
                    val sc = rawScroll.coerceIn(-127, 127)
                    val hs = rawHScroll.coerceIn(-127, 127)

                    accumX.addAndGet(-dx)
                    accumY.addAndGet(-dy)
                    accumScroll.addAndGet(-sc)
                    accumHScroll.addAndGet(-hs)

                    if (accumX.get() != 0 || accumY.get() != 0 || accumScroll.get() != 0 || accumHScroll.get() != 0) {
                        mouseDirty.set(true)
                    }

                    sendMouseReport(mouseButtons, dx, dy, sc, hs)
                } else {
                    val now = System.currentTimeMillis()
                    if (now - lastReportSentTime >= 500) {
                        // Send keep-alive empty mouse report to prevent host (especially Windows) from dropping into sniff mode
                        sendMouseReport(mouseButtons, 0, 0, 0, 0)
                    }
                }
                delay(mouseRateMs)
            }
        }
    }

    private fun stopCoalesceLoop() {
        coalesceJob?.cancel()
        coalesceJob = null
    }

    fun queueMouseMove(dx: Int, dy: Int) {
        if (!isReady()) return
        accumX.addAndGet(dx)
        accumY.addAndGet(dy)
        mouseDirty.set(true)
    }

    fun queueScroll(delta: Int) {
        if (!isReady()) return
        accumScroll.addAndGet(delta)
        mouseDirty.set(true)
    }

    fun queueHScroll(delta: Int) {
        if (!isReady()) return
        accumHScroll.addAndGet(delta)
        mouseDirty.set(true)
    }

    @SuppressLint("MissingPermission")
    suspend fun sendMouseClick(buttonMask: Int, down: Boolean) {
        mouseButtons = if (down) (mouseButtons or buttonMask) else (mouseButtons and buttonMask.inv())
        flushMouseImmediate()
    }

    @SuppressLint("MissingPermission")
    suspend fun tapMouseClick(buttonMask: Int, holdMs: Long = 50L) {
        mouseButtons = mouseButtons or buttonMask
        flushMouseImmediate()
        delay(holdMs)
        mouseButtons = mouseButtons and buttonMask.inv()
        flushMouseImmediate()
    }

    private fun flushMouseImmediate() {
        val dx = accumX.getAndSet(0)
        val dy = accumY.getAndSet(0)
        val sc = accumScroll.getAndSet(0)
        val hs = accumHScroll.getAndSet(0)
        mouseDirty.set(false)
        sendMouseReport(mouseButtons, dx, dy, sc, hs)
    }

    @SuppressLint("MissingPermission")
    private fun sendMouseReport(buttons: Int, dx: Int, dy: Int, scroll: Int, hScroll: Int = 0): Boolean {
        val device = connectedDevice ?: return false
        val hid = hidDevice ?: return false
        val cx = dx.coerceIn(-127, 127)
        val cy = dy.coerceIn(-127, 127)
        val cs = scroll.coerceIn(-127, 127)
        val ch = hScroll.coerceIn(-127, 127)
        val payload = ByteArray(5)
        payload[0] = (buttons and 0x1F).toByte()
        payload[1] = cx.toByte()
        payload[2] = cy.toByte()
        payload[3] = cs.toByte()
        payload[4] = ch.toByte()
        return safeSend { hid.sendReport(device, HidDescriptors.REPORT_ID_MOUSE.toInt(), payload) }
    }

    suspend fun keyDown(keyCode: HidKeyCode, modifierMask: Int = 0): Int = kbMutex.withLock {
        kbModifiers = kbModifiers or modifierMask
        if (keyCode != HidKeyCode.NONE) {
            val existing = kbSlots.indexOf(keyCode.code.toInt() and 0xFF)
            if (existing < 0) {
                val free = kbSlots.indexOf(0)
                if (free >= 0) kbSlots[free] = keyCode.code.toInt() and 0xFF
                else {
                    sendKeyboardState()
                    return@withLock -1
                }
            }
        }
        sendKeyboardState()
        kbSlots.indexOf(keyCode.code.toInt() and 0xFF)
    }

    suspend fun keyUp(keyCode: HidKeyCode, modifierMask: Int = 0): Unit = kbMutex.withLock {
        kbModifiers = kbModifiers and modifierMask.inv()
        val idx = kbSlots.indexOf(keyCode.code.toInt() and 0xFF)
        if (idx >= 0) kbSlots[idx] = 0
        sendKeyboardState()
    }

    suspend fun pressAndRelease(keyCode: HidKeyCode, modifiers: Int = 0, holdMs: Long = 10L) {
        try {
            keyDown(keyCode, modifiers)
            delay(holdMs)
        } finally {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                keyUp(keyCode, modifiers)
            }
        }
    }

    suspend fun typeChar(c: Char, holdMs: Long = 10L, gapMs: Long = 10L) {
        val (key, modifier) = HidKeyCode.fromChar(c) ?: return
        pressAndRelease(key, modifier, holdMs)
        delay(gapMs)
    }

    suspend fun sendString(text: String) {
        for (c in text) typeChar(c)
    }

    @SuppressLint("MissingPermission")
    private fun sendKeyboardState(): Boolean {
        val device = connectedDevice ?: return false
        val hid = hidDevice ?: return false
        val payload = ByteArray(8)
        payload[0] = (kbModifiers and 0xFF).toByte()
        payload[1] = 0x00
        for (i in 0 until 6) payload[2 + i] = (kbSlots[i] and 0xFF).toByte()
        return safeSend { hid.sendReport(device, HidDescriptors.REPORT_ID_KEYBOARD.toInt(), payload) }
    }

    @SuppressLint("MissingPermission")
    private fun sendBlankKeyboardReport(): Boolean {
        val device = connectedDevice ?: return false
        val hid = hidDevice ?: return false
        return safeSend { hid.sendReport(device, HidDescriptors.REPORT_ID_KEYBOARD.toInt(), ByteArray(8)) }
    }

    @SuppressLint("MissingPermission")
    suspend fun sendConsumerKey(usage: ConsumerUsage, holdMs: Long = 50L) {
        val device = connectedDevice ?: return
        val hid = hidDevice ?: return
        val down = ByteArray(2)
        down[0] = (usage.code and 0xFF).toByte()
        down[1] = ((usage.code shr 8) and 0xFF).toByte()
        safeSend { hid.sendReport(device, HidDescriptors.REPORT_ID_CONSUMER.toInt(), down) }
        delay(holdMs)
        sendConsumerBlank()
    }

    @SuppressLint("MissingPermission")
    private fun sendConsumerBlank(): Boolean {
        val device = connectedDevice ?: return false
        val hid = hidDevice ?: return false
        return safeSend { hid.sendReport(device, HidDescriptors.REPORT_ID_CONSUMER.toInt(), ByteArray(2)) }
    }

    private inline fun safeSend(crossinline block: () -> Boolean): Boolean = try {
        val success = block()
        if (success) {
            lastReportSentTime = System.currentTimeMillis()
        }
        success
    } catch (t: Throwable) {
        Log.w(TAG, "sendReport: ${t.message}")
        false
    }

    private fun clamp8(value: Int): Byte = min(127, max(-127, value)).toByte()

    fun resetState() {
        mouseButtons = 0
        accumX.set(0); accumY.set(0); accumScroll.set(0); accumHScroll.set(0)
        mouseDirty.set(false)
        kbSlots.fill(0)
        kbModifiers = 0
    }
}
