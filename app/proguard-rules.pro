# R8 rules for Linkpad release. Narrow keeps for max shrink without breaking HID.

# ── R8 full mode safety: Hilt generated factories ──
# Hilt generates *_Factory, *_MembersInjector, Hilt_* — kept by annotation processor metadata.
-keep,allowobfuscation @interface dagger.hilt.android.AndroidEntryPoint
-keep,allowobfuscation @interface dagger.hilt.android.HiltAndroidApp
-keep,allowobfuscation @interface dagger.hilt.android.lifecycle.HiltViewModel
-keep,allowobfuscation @interface dagger.hilt.InstallIn
-keep,allowobfuscation @interface javax.inject.Inject
-keep,allowobfuscation @interface javax.inject.Singleton

# Keep Hilt-generated entry points (full names referenced via reflection)
-keep class **_HiltModules { *; }
-keep class **_HiltModules$* { *; }
-keep class **_HiltComponents { *; }
-keep class **_HiltComponents$* { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep class dagger.hilt.internal.aggregatedroot.codegen.** { *; }
-keep class dagger.hilt.android.internal.managers.** { *; }

# Keep Hilt_* shadow classes — Activity/Application subclasses
-keep class com.btremote.app.Hilt_* { *; }
-keep class com.btremote.app.**.Hilt_* { *; }

# ── Kotlin metadata for Compose + reflection ──
-keep class kotlin.Metadata { *; }

# ── Bluetooth HID — must NOT obfuscate Callback overrides; framework calls by reflection ──
-keepclassmembers class * extends android.bluetooth.BluetoothHidDevice$Callback {
    public *;
}
-keepclassmembers class * extends android.bluetooth.BluetoothProfile$ServiceListener {
    public *;
}
-keep class com.btremote.app.bluetooth.HidDescriptors { *; }
-keep class com.btremote.app.bluetooth.HidDescriptors$* { *; }

# ── App ViewModels — Hilt instantiates via generated factory ──
-keep,allowobfuscation class com.btremote.app.**ViewModel
-keep,allowobfuscation class com.btremote.app.**ViewModel_HiltModules*

# ── DataStore proto generated — keep field readers ──
-keepclassmembers class androidx.datastore.preferences.protobuf.** { *; }

# ── Strip verbose logging in release ──
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ── Compose — R8 handles Compose intrinsics via @Composable metadata; only need this for previews ──
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ── Suppress noise warnings (no runtime effect) ──
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**
-dontwarn kotlinx.coroutines.debug.**
-dontwarn java.lang.invoke.StringConcatFactory
