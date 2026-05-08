# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Room database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Sunmi AIDL (antigo - não usado mais, mas mantido por compatibilidade)
-keep class woyou.aidlservice.jiuiv5.** { *; }
-keep interface woyou.aidlservice.jiuiv5.** { *; }

# Keep Sunmi Printer SDK (PrinterSdk - novo)
-keep class com.sunmi.printerx.** { *; }
-keep interface com.sunmi.printerx.** { *; }
-dontwarn com.sunmi.printerx.**

# Keep data classes
-keep @androidx.room.Entity class *
-keep class com.seucaixa.caixacombo.data.model.** { *; }

# Keep ViewModels
-keep class com.seucaixa.caixacombo.ui.viewmodel.** { *; }

# Gson rules - preserve generic signatures
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.seucaixa.caixacombo.data.model.** { *; }
-keepclassmembers class com.seucaixa.caixacombo.data.model.** { *; }
-dontwarn com.google.gson.**

# Keep TypeToken and generic types
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.internal.bind.** { *; }

# Optimize
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify

# R8 full mode optimizations
-keep class * { <init>(...); }

# Remove logs em release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Keep data classes with @Keep annotation if needed
-keep @androidx.annotation.Keep class * { *; }

# Fix R8 missing classes from google.crypto.tink (referenced transitively)
-dontwarn com.google.api.client.http.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.joda.time.**
-dontwarn com.google.crypto.tink.util.KeysDownloader
