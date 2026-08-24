# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Barcode scanner library — young/pre-1.0, keep it intact rather than
# risk R8 stripping something it needs reflectively.
-keep class org.ncgroup.kscan.** { *; }

# Room-generated database code.
-keep class * extends androidx.room3.RoomDatabase
-keep @androidx.room3.Entity class * { *; }
