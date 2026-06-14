package com.novaproject.novai.data.repository

  import android.util.Log
  import com.google.firebase.auth.EmailAuthProvider
  import com.google.firebase.auth.FirebaseAuth
  import com.google.firebase.auth.FirebaseUser
  import com.google.firebase.auth.userProfileChangeRequest
  import com.google.firebase.firestore.FieldValue
  import com.google.firebase.firestore.FirebaseFirestore
  import com.google.firebase.firestore.SetOptions
  import com.google.firebase.messaging.FirebaseMessaging
  import com.novaproject.novai.util.AnalyticsHelper
  import kotlinx.coroutines.channels.awaitClose
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.callbackFlow
  import kotlinx.coroutines.tasks.await
  import javax.inject.Inject
  import javax.inject.Singleton

  @Singleton
  class AuthRepository @Inject constructor(
      private val auth: FirebaseAuth,
      private val firestore: FirebaseFirestore,
      private val analytics: AnalyticsHelper
  ) {
      val currentUser: FirebaseUser? get() = auth.currentUser

      val authState: Flow<FirebaseUser?> = callbackFlow {
          val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
          auth.addAuthStateListener(listener)
          awaitClose { auth.removeAuthStateListener(listener) }
      }

      suspend fun signIn(email: String, password: String): Result<FirebaseUser> = runCatching {
          val user = auth.signInWithEmailAndPassword(email, password).await().user!!
          analytics.logLogin()
          saveFcmToken(user.uid)
          user
      }

      suspend fun register(email: String, password: String, name: String): Result<FirebaseUser> = runCatching {
          val result = auth.createUserWithEmailAndPassword(email, password).await()
          val user = result.user!!
          val profileUpdates = userProfileChangeRequest { displayName = name }
          user.updateProfile(profileUpdates).await()
          analytics.logSignUp()
          saveFcmToken(user.uid)
          user
      }

      suspend fun resetPassword(email: String): Result<Unit> = runCatching {
          auth.sendPasswordResetEmail(email).await()
      }

      suspend fun updateDisplayName(newName: String): Result<Unit> = runCatching {
          val user = auth.currentUser ?: error("Not authenticated")
          val updates = userProfileChangeRequest { displayName = newName }
          user.updateProfile(updates).await()
      }

      suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> = runCatching {
          val user = auth.currentUser ?: error("Not authenticated")
          val email = user.email ?: error("No email")
          val credential = EmailAuthProvider.getCredential(email, currentPassword)
          user.reauthenticate(credential).await()
          user.updatePassword(newPassword).await()
      }

      fun signOut() = auth.signOut()

      /**
       * Fetches the current FCM token and saves it to Firestore so the user can be
       * targeted individually from Cloud Functions or the Admin SDK.
       * The token refreshes automatically via NovAIMessagingService.onNewToken().
       */
      private suspend fun saveFcmToken(uid: String) {
          try {
              val token = FirebaseMessaging.getInstance().token.await()
              firestore.collection("users").document(uid)
                  .set(
                      mapOf(
                          "fcmToken" to token,
                          "fcmUpdatedAt" to FieldValue.serverTimestamp()
                      ),
                      SetOptions.merge()
                  )
                  .await()
          } catch (e: Exception) {
              Log.w("AuthRepository", "Failed to save FCM token", e)
          }
      }
  }
  