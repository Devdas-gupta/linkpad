package com.btremote.app.bluetooth

object HidDescriptors {

    // Match commercial Bluetooth Keyboard & Mouse APK layout exactly.
    // ReportID 1 = Keyboard (hosts auto-bind first ReportID as primary HID role).
    // ReportID 2 = Mouse.
    // ReportID 3 = Consumer.
    const val REPORT_ID_KEYBOARD: Byte = 1
    const val REPORT_ID_MOUSE: Byte = 2
    const val REPORT_ID_CONSUMER: Byte = 3

    const val SDP_NAME = "Linkpad"
    const val SDP_DESCRIPTION = "Wireless Keyboard & Mouse"
    const val SDP_PROVIDER = "Linkpad"
    // 0xC0 = Combo (Keyboard + Pointing) per HID 1.1 SDP.
    const val SUBCLASS: Byte = 0xC0.toByte()
    const val COUNTRY: Byte = 0x21  // US

    /**
     * Combined HID descriptor matching the commercial APK byte-for-byte.
     *
     *   Report ID 1 = Boot Keyboard (8 bytes: modifier, reserved, k1..k6)
     *   Report ID 2 = Mouse (4 bytes: buttons[1], dx[1], dy[1], wheel[1])
     *   Report ID 3 = Consumer Control (2 bytes: little-endian usage)
     */
    val COMBINED_DESCRIPTOR: ByteArray = byteArrayOf(
        // ── BOOT KEYBOARD (Report ID 1) ──
        0x05, 0x01,                    // Usage Page (Generic Desktop)
        0x09, 0x06,                    // Usage (Keyboard)
        0xA1.toByte(), 0x01,           // Collection (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD,// Report ID (1)
        0x05, 0x07,                    //   Usage Page (Key Codes)
        0x19, 0xE0.toByte(),           //   Usage Min (LeftCtrl)
        0x29, 0xE7.toByte(),           //   Usage Max (RightGUI)
        0x15, 0x00,                    //   Logical Min (0)
        0x25, 0x01,                    //   Logical Max (1)
        0x75, 0x01,                    //   Report Size (1)
        0x95.toByte(), 0x08,           //   Report Count (8)
        0x81.toByte(), 0x02,           //   Input (Data, Var, Abs)
        0x95.toByte(), 0x01,           //   Report Count (1) reserved byte
        0x75, 0x08,                    //   Report Size (8)
        0x81.toByte(), 0x01,           //   Input (Const)
        0x95.toByte(), 0x05,           //   Report Count (5) LEDs
        0x75, 0x01,                    //   Report Size (1)
        0x05, 0x08,                    //   Usage Page (LEDs)
        0x19, 0x01,                    //   Usage Min (NumLock)
        0x29, 0x05,                    //   Usage Max (Kana)
        0x91.toByte(), 0x02,           //   Output (Data, Var, Abs)
        0x95.toByte(), 0x01,           //   Report Count (1) padding
        0x75, 0x03,                    //   Report Size (3)
        0x91.toByte(), 0x01,           //   Output (Const)
        0x95.toByte(), 0x06,           //   Report Count (6) keycodes
        0x75, 0x08,                    //   Report Size (8)
        0x15, 0x00,                    //   Logical Min (0)
        0x25, 0x65,                    //   Logical Max (101)
        0x05, 0x07,                    //   Usage Page (Key Codes)
        0x19, 0x00,                    //   Usage Min (0)
        0x29, 0x65,                    //   Usage Max (101)
        0x81.toByte(), 0x00,           //   Input (Data, Array)
        0xC0.toByte(),                 // End Collection

        // ── MOUSE (Report ID 2) — byte-exact match to commercial APK gy.a ──
        // Report layout: [buttons(1), dx(1), dy(1), wheel(1), acPan(1)] = 5 bytes
        0x05, 0x01,                    // Usage Page (Generic Desktop)
        0x09, 0x02,                    // Usage (Mouse)
        0xA1.toByte(), 0x01,           // Collection (Application)
        0x85.toByte(), REPORT_ID_MOUSE,//   Report ID (2)
        0x09, 0x01,                    //   Usage (Pointer)
        0xA1.toByte(), 0x00,           //   Collection (Physical)
        0x05, 0x09,                    //     Usage Page (Buttons)
        0x19, 0x01,                    //     Usage Min (1)
        0x29, 0x05,                    //     Usage Max (5)
        0x15, 0x00,                    //     Logical Min (0)
        0x25, 0x01,                    //     Logical Max (1)
        0x75, 0x01,                    //     Report Size (1)
        0x95.toByte(), 0x05,           //     Report Count (5)
        0x81.toByte(), 0x02,           //     Input (Data, Var, Abs)
        0x75, 0x03,                    //     Report Size (3) padding
        0x95.toByte(), 0x01,           //     Report Count (1)
        0x81.toByte(), 0x01,           //     Input (Const)
        0x05, 0x01,                    //     Usage Page (Generic Desktop)
        0x09, 0x30,                    //     Usage (X)
        0x09, 0x31,                    //     Usage (Y)
        0x09, 0x38,                    //     Usage (Wheel)
        0x15, 0x81.toByte(),           //     Logical Min (-127)
        0x25, 0x7F,                    //     Logical Max (127)
        0x75, 0x08,                    //     Report Size (8)
        0x95.toByte(), 0x03,           //     Report Count (3)
        0x81.toByte(), 0x06,           //     Input (Data, Var, Rel)
        0x05, 0x0C,                    //     Usage Page (Consumer) — for AC Pan
        0x0A, 0x38, 0x02,              //     Usage (AC Pan, 0x0238) horizontal scroll
        0x95.toByte(), 0x01,           //     Report Count (1)
        0x81.toByte(), 0x06,           //     Input (Data, Var, Rel)
        0xC0.toByte(),                 //   End Collection (Physical)
        0xC0.toByte(),                 // End Collection (Application)

        // ── CONSUMER (Report ID 3) — 12-bit usage + 4-bit pad, byte-exact match commercial APK ──
        0x05, 0x0C,                    // Usage Page (Consumer)
        0x09, 0x01,                    // Usage (Consumer Control)
        0xA1.toByte(), 0x01,           // Collection (Application)
        0x85.toByte(), REPORT_ID_CONSUMER,// Report ID (3)
        0x19, 0x00,                    //   Usage Min (0)
        0x2A, 0xFF.toByte(), 0x03,     //   Usage Max (0x3FF = 1023)
        0x75, 0x0C,                    //   Report Size (12)
        0x95.toByte(), 0x01,           //   Report Count (1)
        0x15, 0x00,                    //   Logical Min (0)
        0x26, 0xFF.toByte(), 0x03,     //   Logical Max (1023)
        0x81.toByte(), 0x00,           //   Input (Data, Array)
        0x75, 0x04,                    //   Report Size (4) padding
        0x95.toByte(), 0x01,           //   Report Count (1)
        0x81.toByte(), 0x01,           //   Input (Const)
        0xC0.toByte()                  // End Collection
    )
}

enum class MouseButtonMask(val mask: Int) {
    LEFT(0x01),
    RIGHT(0x02),
    MIDDLE(0x04),
    BACK(0x08),
    FORWARD(0x10)
}

enum class HidKeyCode(val code: Byte) {
    NONE(0x00),
    A(0x04), B(0x05), C(0x06), D(0x07), E(0x08), F(0x09), G(0x0A),
    H(0x0B), I(0x0C), J(0x0D), K(0x0E), L(0x0F), M(0x10), N(0x11),
    O(0x12), P(0x13), Q(0x14), R(0x15), S(0x16), T(0x17), U(0x18),
    V(0x19), W(0x1A), X(0x1B), Y(0x1C), Z(0x1D),
    NUM_1(0x1E), NUM_2(0x1F), NUM_3(0x20), NUM_4(0x21), NUM_5(0x22),
    NUM_6(0x23), NUM_7(0x24), NUM_8(0x25), NUM_9(0x26), NUM_0(0x27),
    ENTER(0x28), ESCAPE(0x29), BACKSPACE(0x2A), TAB(0x2B), SPACE(0x2C),
    MINUS(0x2D), EQUAL(0x2E), LEFT_BRACKET(0x2F), RIGHT_BRACKET(0x30),
    BACKSLASH(0x31), SEMICOLON(0x33), QUOTE(0x34), GRAVE(0x35),
    COMMA(0x36), PERIOD(0x37), SLASH(0x38), CAPS_LOCK(0x39),
    F1(0x3A), F2(0x3B), F3(0x3C), F4(0x3D), F5(0x3E), F6(0x3F),
    F7(0x40), F8(0x41), F9(0x42), F10(0x43), F11(0x44), F12(0x45),
    PRINT_SCREEN(0x46), SCROLL_LOCK(0x47), PAUSE(0x48),
    INSERT(0x49), HOME(0x4A), PAGE_UP(0x4B), DELETE(0x4C),
    END(0x4D), PAGE_DOWN(0x4E),
    RIGHT_ARROW(0x4F), LEFT_ARROW(0x50), DOWN_ARROW(0x51), UP_ARROW(0x52);

    companion object {
        fun fromChar(c: Char): Pair<HidKeyCode, Int>? {
            val mod = if (c.isUpperCase() || c in "!@#\$%^&*()_+{}|:\"<>?~") MODIFIER_LEFT_SHIFT else 0
            val key = when (c.lowercaseChar()) {
                in 'a'..'z' -> values()[A.ordinal + (c.lowercaseChar() - 'a')]
                '1' -> NUM_1; '2' -> NUM_2; '3' -> NUM_3; '4' -> NUM_4; '5' -> NUM_5
                '6' -> NUM_6; '7' -> NUM_7; '8' -> NUM_8; '9' -> NUM_9; '0' -> NUM_0
                ' ' -> SPACE; '\n' -> ENTER; '\t' -> TAB
                '-' -> MINUS; '=' -> EQUAL; '[' -> LEFT_BRACKET; ']' -> RIGHT_BRACKET
                '\\' -> BACKSLASH; ';' -> SEMICOLON; '\'' -> QUOTE; '`' -> GRAVE
                ',' -> COMMA; '.' -> PERIOD; '/' -> SLASH
                '!' -> NUM_1; '@' -> NUM_2; '#' -> NUM_3; '$' -> NUM_4; '%' -> NUM_5
                '^' -> NUM_6; '&' -> NUM_7; '*' -> NUM_8; '(' -> NUM_9; ')' -> NUM_0
                '_' -> MINUS; '+' -> EQUAL; '{' -> LEFT_BRACKET; '}' -> RIGHT_BRACKET
                '|' -> BACKSLASH; ':' -> SEMICOLON; '"' -> QUOTE; '~' -> GRAVE
                '<' -> COMMA; '>' -> PERIOD; '?' -> SLASH
                else -> return null
            }
            return key to mod
        }
    }
}

const val MODIFIER_LEFT_CTRL = 0x01
const val MODIFIER_LEFT_SHIFT = 0x02
const val MODIFIER_LEFT_ALT = 0x04
const val MODIFIER_LEFT_GUI = 0x08
const val MODIFIER_RIGHT_CTRL = 0x10
const val MODIFIER_RIGHT_SHIFT = 0x20
const val MODIFIER_RIGHT_ALT = 0x40
const val MODIFIER_RIGHT_GUI = 0x80

enum class ConsumerUsage(val code: Int) {
    PLAY_PAUSE(0xCD),
    NEXT_TRACK(0xB5),
    PREV_TRACK(0xB6),
    STOP(0xB7),
    FAST_FORWARD(0xB3),
    REWIND(0xB4),
    VOLUME_UP(0xE9),
    VOLUME_DOWN(0xEA),
    MUTE(0xE2),
    BRIGHTNESS_UP(0x6F),
    BRIGHTNESS_DOWN(0x70),
    AC_HOME(0x223),
    AC_BACK(0x224)
}
