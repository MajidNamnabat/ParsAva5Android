# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html
-dontwarn  com.khanenoor.parsavatts.**
-optimizationpasses 3
-overloadaggressively
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-flattenpackagehierarchy 'ob'
-repackageclasses 'obs'
-allowaccessmodification
-useuniqueclassmembernames
-adaptclassstrings
-renamesourcefileattribute ""

-dontwarn org.xmlpull.v1.**
-dontwarn org.kxml2.io.**
-dontwarn android.content.res.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn javax.xml.stream.Location
-dontwarn javax.xml.stream.XMLEventReader
-dontwarn javax.xml.stream.XMLInputFactory
-dontwarn javax.xml.stream.events.Attribute
-dontwarn javax.xml.stream.events.Characters
-dontwarn javax.xml.stream.events.StartElement
-dontwarn javax.xml.stream.events.XMLEvent
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

-keep class org.xmlpull.** { *; }
-keepclassmembers class org.xmlpull.** { *; }

-keep public class com.khanenoor.parsavatts.ttsService.DualTtsService { *; }
# extends android.speech.tts.TextToSpeechService
#-keep public class * extends com.khanenoor.parsavatts.engine.FaTts
#-keep public class * extends com.khanenoor.parsavatts.engine.EnTts
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

# note that <methods> means any method
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
#-keepclasseswithmembers,allowshrinking,allowoptimization class com.khanenoor.parsavatts.** {
#    native <methods>;
#}
#for callback methods
-keep class com.khanenoor.parsavatts.engine.FaTts {
    int nativeSynthCallback(byte[], int);
    int nativeSynthCallbackV2(java.nio.ByteBuffer, int);

}
-keep class com.khanenoor.parsavatts.engine.EnTts {
    int nativeSpeakEngCallback(java.lang.String);
}
# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod
# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep annotation default values (e.g., retrofit2.http.Field.encoded).
-keepattributes AnnotationDefault

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**
# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep inherited services.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# R8 full mode strips generic signatures from return types if not kept.
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# With R8 full mode generic signatures are stripped for classes that are not kept.
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Keep Guava / Google concurrent futures internals intact
-keep class com.google.common.util.concurrent.** { *; }
-keepclassmembers class com.google.common.util.concurrent.** {
    volatile java.lang.Object *;
}

-keep class **SafeAtomicHelper** { *; }
-keep class **AbstractFuture** { *; }
-keep class java.util.concurrent.atomic.AtomicReferenceFieldUpdater { *; }
