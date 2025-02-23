-dontwarn com.google.errorprone.annotations.**

-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**
