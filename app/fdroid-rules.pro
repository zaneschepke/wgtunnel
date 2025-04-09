-dontwarn com.google.errorprone.annotations.**

-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep all classes in the org.xbill.DNS package and subpackages
-keep class org.xbill.DNS.** { *; }
-dontwarn org.xbill.DNS.**

# Preserve JNA classes if used (e.g., for IPHlpAPI on Windows)
-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**

# Keep DNS resolver configuration classes that might be loaded dynamically
-keep class org.xbill.DNS.config.** { *; }
-dontwarn org.xbill.DNS.config.**

-keep class org.xbill.DNS.** { *; }

# Prevent optimization issues with native or reflection-based calls
-dontoptimize
-dontshrink
# Uncomment the above if errors persist, but use sparingly as they’re broad

# Suppress warnings about missing classes if not all features are used
-dontwarn java.lang.management.**
-dontwarn sun.nio.ch.**

-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn javax.lang.model.element.Modifier
-dontwarn org.joda.time.Instant
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder
-dontwarn org.slf4j.impl.StaticMarkerBinder
