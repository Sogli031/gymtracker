# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-keep class com.gymtracker.app.data.model.** { *; }
-keep class com.gymtracker.app.data.repository.** { *; }
-keep class com.gymtracker.app.data.backup.** { *; }
-keep class com.gymtracker.app.util.** { *; }

# Google Play Services Auth
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Google API Client
-keep class com.google.api.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.** { *; }
-keep class * extends com.google.api.client.json.GenericJson { *; }
-dontwarn com.google.api.**

# Google HTTP Client
-keep class com.google.http.** { *; }
-dontwarn com.google.http.**

# Gson (used by Google API)
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Apache HTTP Client (transitive dependency from Google API)
-dontwarn org.apache.http.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**

# Room (entiteti, DAO, konverteri, baza)
-keep class com.gymtracker.app.data.local.** { *; }
