# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/davidbrodsky/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontobfuscate

-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault,*Annotation*

-dontwarn java.lang.reflect.Method
-dontwarn java.lang.invoke**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
-dontwarn java.nio.file.Files
-dontwarn java.nio.file.Path
-dontwarn java.nio.file.OpenOption
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class org.jivesoftware.smack.** { *; }
-keep class org.jivesoftware.smackx.** { *; }
-keep class info.guardianproject.libcore.** { *; }
-keep class info.guardianproject.iocipher.** { *; }
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.* { *; }

