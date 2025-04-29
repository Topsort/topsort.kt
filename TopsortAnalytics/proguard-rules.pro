# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep JodaTime and JodaConvert classes
-keep class org.joda.time.** { *; }
-keep class org.joda.convert.** { *; }
-dontwarn org.joda.convert.**
-dontwarn org.joda.time.**

# Keep StringConcatFactory which is required for Java 9+ string concatenation
-keep class java.lang.invoke.StringConcatFactory { *; }
-keepclassmembers class * {
    java.lang.String toString();
}
-dontwarn java.lang.invoke.StringConcatFactory

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

# Keep public API
-keep public class com.topsort.analytics.Analytics {
    public *;
}

# Keep public model classes
-keep public class com.topsort.analytics.model.** {
    public *;
}

# Keep public banner classes
-keep public class com.topsort.analytics.banners.** {
    public *;
}

# Keep model classes that will be serialized/deserialized
-keepclassmembers class com.topsort.analytics.model.** {
    <fields>;
}

# Keep enums
-keepclassmembers enum com.topsort.analytics.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve core library classes
-keep class com.topsort.analytics.core.** {
    public *;
}

# WorkManager rules
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# Preserve annotations
-keepattributes *Annotation*

# Preserve the special static methods that are required in all enumeration classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name
-renamesourcefileattribute SourceFile