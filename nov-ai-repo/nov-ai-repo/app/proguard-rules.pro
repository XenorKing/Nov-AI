# ── App data models (Firestore POJO serialization) ────────────────────────────
-keep class com.novaproject.novai.data.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ── OkHttp / Okio ─────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Gson ──────────────────────────────────────────────────────────────────────
-keep class com.google.gson.** { *; }

# ── Firebase Firestore ────────────────────────────────────────────────────────
# Keep the @Exclude annotation so Firestore's reflection-based serializer
# can read it at runtime (required for openRouterToken exclusion).
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keep @interface com.google.firebase.firestore.Exclude
-keep @interface com.google.firebase.firestore.DocumentId
-keep @interface com.google.firebase.firestore.PropertyName
-keep @interface com.google.firebase.firestore.ServerTimestamp

# ── Jetpack Security / EncryptedSharedPreferences ────────────────────────────
# security-crypto uses reflection internally; keep all necessary classes.
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ── Kotlin coroutines ─────────────────────────────────────────────────────────
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
