package com.novaproject.novai.data.repository

  import android.app.Application
  import android.app.NotificationManager
  import android.app.PendingIntent
  import android.content.Context
  import android.content.Intent
  import androidx.core.app.NotificationCompat
  import com.google.firebase.Timestamp
  import com.google.firebase.auth.FirebaseAuth
  import com.google.firebase.firestore.FirebaseFirestore
  import com.google.firebase.firestore.ListenerRegistration
  import com.google.firebase.firestore.Query
  import com.google.firebase.firestore.SetOptions
  import com.google.gson.Gson
  import com.novaproject.novai.MainActivity
  import com.novaproject.novai.NovAIApp
  import com.novaproject.novai.data.model.AISettings
  import com.novaproject.novai.data.model.ChatRequest
  import com.novaproject.novai.data.model.ChatResponse
  import com.novaproject.novai.data.model.Conversation
  import com.novaproject.novai.data.model.DayActivity
  import com.novaproject.novai.data.model.Message
  import com.novaproject.novai.data.model.MessageSearchResult
  import com.novaproject.novai.data.model.ModelTokenStats
  import com.novaproject.novai.data.model.UserStats
  import com.novaproject.novai.util.AnalyticsHelper
  import com.novaproject.novai.util.AppStateManager
  import com.novaproject.novai.util.TokenSecureStore
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.channels.awaitClose
  import kotlinx.coroutines.delay
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.callbackFlow
  import kotlinx.coroutines.flow.emptyFlow
  import kotlinx.coroutines.tasks.await
  import kotlinx.coroutines.withContext
  import okhttp3.MediaType.Companion.toMediaType
  import okhttp3.OkHttpClient
  import okhttp3.Request
  import okhttp3.RequestBody.Companion.toRequestBody
  import java.io.IOException
  import java.util.UUID
  import java.util.concurrent.atomic.AtomicInteger
  import javax.inject.Inject
  import javax.inject.Singleton

  private const val DEFAULT_MODEL_ID  = "nex-agi/nex-n2-pro:free"
  private const val CACHE_MAX_SIZE    = 50
  private const val WORKER_URL        = "https://novai-proxy.xenortvin.workers.dev/chat"
  private const val OPENROUTER_URL    = "https://openrouter.ai/api/v1/chat/completions"
  const val MESSAGES_PAGE_SIZE        = 50

  @Singleton
  class ChatRepository @Inject constructor(
      private val app: Application,
      private val auth: FirebaseAuth,
      private val firestore: FirebaseFirestore,
      private val httpClient: OkHttpClient,
      private val gson: Gson,
      private val tokenStore: TokenSecureStore,
      private val analytics: AnalyticsHelper
  ) {
      private val uid get() = auth.currentUser?.uid

      private val notifIdCounter = AtomicInteger(2000)

      private val responseCache: MutableMap<String, String> =
          object : LinkedHashMap<String, String>(64, 0.75f, true) {
              override fun removeEldestEntry(e: MutableMap.MutableEntry<String, String>) = size > CACHE_MAX_SIZE
          }

      private fun systemPrompt(override: String = ""): String {
          val nick = auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "пользователь"
          val base = "Ты — NovAI, умный ИИ-ассистент от Nova Project. " +
                  "Ты не ChatGPT, не GPT-4 и не продукт OpenAI. Ты — NovAI, созданный командой Nova Project. " +
                  "Всегда обращайся к пользователю по нику: $nick. " +
                  "Отвечай на русском языке, если пользователь пишет по-русски."
          return if (override.isNotBlank()) "$base\n\n$override" else base
      }

      private fun convsRef(userId: String) =
          firestore.collection("users").document(userId).collection("conversations")

      private fun msgsRef(userId: String, convId: String) =
          convsRef(userId).document(convId).collection("messages")

      private fun settingsRef(userId: String) =
          firestore.collection("users").document(userId).collection("settings").document("default")

      private fun statsRef(userId: String) =
          firestore.collection("users").document(userId).collection("stats").document("tokens")

      // ── Real-time streams ──────────────────────────────────────────────────────

      fun conversationsFlow(): Flow<List<Conversation>> {
          val userId = uid ?: return emptyFlow()
          return callbackFlow {
              val sub = convsRef(userId)
                  .orderBy("updatedAt", Query.Direction.DESCENDING)
                  .addSnapshotListener { snap, err ->
                      if (err != null) { close(err); return@addSnapshotListener }
                      trySend(snap?.documents?.mapNotNull { it.toObject(Conversation::class.java) } ?: emptyList())
                  }
              awaitClose { sub.remove() }
          }
      }

      fun messagesFlow(convId: String, pageSize: Int = MESSAGES_PAGE_SIZE): Flow<List<Message>> {
          val userId = uid ?: return emptyFlow()
          return callbackFlow {
              val sub = msgsRef(userId, convId)
                  .orderBy("timestamp")
                  .limitToLast(pageSize.toLong())
                  .addSnapshotListener { snap, err ->
                      if (err != null) { close(err); return@addSnapshotListener }
                      trySend(snap?.documents?.mapNotNull { it.toObject(Message::class.java) } ?: emptyList())
                  }
              awaitClose { sub.remove() }
          }
      }

      suspend fun loadOlderMessages(convId: String, beforeTimestamp: Timestamp, pageSize: Int = MESSAGES_PAGE_SIZE): List<Message> {
          val userId = uid ?: return emptyList()
          return try {
              msgsRef(userId, convId)
                  .orderBy("timestamp").endBefore(beforeTimestamp).limitToLast(pageSize.toLong())
                  .get().await().documents.mapNotNull { it.toObject(Message::class.java) }
          } catch (_: Exception) { emptyList() }
      }

      fun settingsFlow(): Flow<AISettings> = callbackFlow {
          var firestoreListener: ListenerRegistration? = null

          fun subscribe(userId: String) {
              firestoreListener?.remove()
              firestoreListener = settingsRef(userId).addSnapshotListener { snap, err ->
                  if (err != null) return@addSnapshotListener
                  try {
                      val base = snap?.toObject(AISettings::class.java) ?: AISettings()

                      // One-time migration: recover legacy token stored in Firestore before this fix.
                      val currentToken = try { tokenStore.getToken() } catch (_: Exception) { "" }
                      if (currentToken.isEmpty()) {
                          val legacyToken = snap?.getString("openRouterToken").orEmpty()
                          if (legacyToken.isNotEmpty()) {
                              try { tokenStore.saveToken(legacyToken) } catch (_: Exception) {}
                              snap?.reference?.update("openRouterToken",
                                  com.google.firebase.firestore.FieldValue.delete())
                          }
                      }

                      val token = try { tokenStore.getToken() } catch (_: Exception) { "" }
                      trySend(base.copy(
                          openRouterToken = token,
                          customModels = (base.customModels as? List<String>).orEmpty(),
                          modelPrompts = (base.modelPrompts as? Map<String, String>).orEmpty()
                      ))
                  } catch (e: Exception) {
                      android.util.Log.e("ChatRepository", "settingsFlow listener error", e)
                      trySend(AISettings())
                  }
              }
          }

          val authListener = FirebaseAuth.AuthStateListener { fb ->
              val id = fb.currentUser?.uid
              if (id != null) subscribe(id)
              else { firestoreListener?.remove(); firestoreListener = null; trySend(AISettings()) }
          }
          auth.addAuthStateListener(authListener)
          uid?.let { subscribe(it) }
          awaitClose { firestoreListener?.remove(); auth.removeAuthStateListener(authListener) }
      }

      // ── Full-text message search ───────────────────────────────────────────────

      suspend fun searchMessages(query: String, maxResults: Int = 60): List<MessageSearchResult> {
          val userId = uid ?: return emptyList()
          if (query.isBlank() || query.length < 2) return emptyList()

          return withContext(Dispatchers.IO) {
              try {
                  val convDocs = convsRef(userId)
                      .orderBy("updatedAt", Query.Direction.DESCENDING).limit(100).get().await()
                  val results = mutableListOf<MessageSearchResult>()
                  for (convDoc in convDocs.documents) {
                      if (results.size >= maxResults) break
                      val conv = convDoc.toObject(Conversation::class.java) ?: continue
                      val msgDocs = msgsRef(userId, conv.id).orderBy("timestamp", Query.Direction.DESCENDING).get().await()
                      for (msgDoc in msgDocs.documents) {
                          val msg = msgDoc.toObject(Message::class.java) ?: continue
                          if (msg.content.contains(query, ignoreCase = true)) {
                              results.add(MessageSearchResult(conv.id, conv.title, msg))
                              if (results.size >= maxResults) break
                          }
                      }
                  }
                  results
              } catch (_: Exception) { emptyList() }
          }
      }

      // ── Conversations CRUD ─────────────────────────────────────────────────────

      suspend fun createConversation(): String {
          val userId = uid ?: error("Не авторизован")
          val id = UUID.randomUUID().toString()
          convsRef(userId).document(id).set(
              Conversation(id = id, createdAt = Timestamp.now(), updatedAt = Timestamp.now())
          ).await()
          analytics.logConversationCreated()
          return id
      }

      suspend fun deleteConversation(convId: String) {
          val userId = uid ?: error("Не авторизован")
          msgsRef(userId, convId).get().await().documents.forEach { it.reference.delete().await() }
          convsRef(userId).document(convId).delete().await()
      }

      suspend fun renameConversation(convId: String, newTitle: String) {
          val userId = uid ?: error("Не авторизован")
          val t = newTitle.trim()
          if (t.isNotBlank()) convsRef(userId).document(convId).update("title", t).await()
      }

      suspend fun pinConversation(convId: String, isPinned: Boolean) {
          val userId = uid ?: error("Не авторизован")
          convsRef(userId).document(convId)
              .set(mapOf("isPinned" to isPinned), SetOptions.merge())
              .await()
      }

      // ── Sending messages ───────────────────────────────────────────────────────

      suspend fun sendMessage(
          convId: String,
          userText: String,
          history: List<Message>,
          settings: AISettings,
          onChunk: ((String) -> Unit)? = null
      ): String {
          val userId = uid ?: error("Не авторизован")

          val userMsg = Message(id = UUID.randomUUID().toString(), role = "user",
              content = userText, timestamp = Timestamp.now())
          msgsRef(userId, convId).document(userMsg.id).set(userMsg).await()

          val apiMessages = mutableListOf<Map<String, String>>()
          val isStandardModel = settings.customModel.isBlank()
          val modelId = if (settings.customModel.isNotBlank()) settings.customModel else DEFAULT_MODEL_ID

          analytics.logMessageSent(modelId)

          val perModelPrompt = settings.modelPrompts[modelId]?.takeIf { it.isNotBlank() }
              ?: settings.modelPrompts[""]?.takeIf { it.isNotBlank() && isStandardModel }
          val systemContent: String? = when {
              isStandardModel -> systemPrompt(perModelPrompt ?: settings.systemPromptOverride)
              perModelPrompt != null -> perModelPrompt
              settings.systemPromptOverride.isNotBlank() -> settings.systemPromptOverride
              else -> null
          }
          if (systemContent != null) apiMessages.add(mapOf("role" to "system", "content" to systemContent))

          if (settings.sendHistory) {
              history.takeLast(20).forEach { apiMessages.add(mapOf("role" to it.role, "content" to it.content)) }
          }
          apiMessages.add(mapOf("role" to "user", "content" to userText))

          val useStreaming = onChunk != null

          val chatRequest = ChatRequest(
              model = modelId, messages = apiMessages,
              temperature = settings.temperature.toDouble(),
              max_tokens = settings.maxTokens,
              top_p = settings.topP.toDouble(),
              frequency_penalty = settings.frequencyPenalty.toDouble(),
              presence_penalty = settings.presencePenalty.toDouble(),
              stream = useStreaming
          )

          val requestJson = gson.toJson(chatRequest)
          if (!useStreaming) {
              synchronized(responseCache) { responseCache[requestJson] }?.let {
                  return saveAndReturn(userId, convId, userText, history, it, modelId)
              }
          }

          val reqBody = requestJson.toRequestBody("application/json".toMediaType())
          val httpRequest = if (settings.openRouterToken.isNotBlank()) {
              Request.Builder().url(OPENROUTER_URL).post(reqBody)
                  .addHeader("Authorization", "Bearer ${settings.openRouterToken}")
                  .addHeader("HTTP-Referer", "https://novai.app")
                  .addHeader("X-Title", "NovAI")
                  .apply { if (useStreaming) addHeader("Accept", "text/event-stream") }
                  .build()
          } else {
              Request.Builder().url(WORKER_URL).post(reqBody)
                  .apply { if (useStreaming) addHeader("Accept", "text/event-stream") }
                  .build()
          }

          val reply: String = withContext(Dispatchers.IO) {
              try {
                  var retries = 0; var result: String? = null
                  while (result == null) {
                      val response = httpClient.newCall(httpRequest).execute()
                      val code = response.code
                      when {
                          code == 429 && retries < 1 -> { response.close(); retries++; delay(3000L) }
                          code == 429 -> { response.close(); throw Exception("Превышен лимит запросов. Попробуйте чуть позже.") }
                          code !in 200..299 -> {
                              val body = response.body?.string() ?: ""; response.close()
                              val detail = runCatching {
                                  val parsed = com.google.gson.JsonParser.parseString(body).asJsonObject
                                  val errorEl = parsed.get("error")
                                  when {
                                      errorEl == null || errorEl.isJsonNull ->
                                          parsed.get("message")?.takeIf { !it.isJsonNull }?.asString
                                      errorEl.isJsonPrimitive -> errorEl.asString
                                      errorEl.isJsonObject ->
                                          errorEl.asJsonObject.get("message")
                                              ?.takeIf { !it.isJsonNull }?.asString
                                      else -> null
                                  }
                              }.getOrNull()
                              throw Exception(detail ?: "Ошибка сервера ($code)")
                          }
                          useStreaming -> {
                              val full = StringBuilder()
                              response.body?.charStream()?.use { reader ->
                                  reader.forEachLine { line ->
                                      if (!line.startsWith("data:")) return@forEachLine
                                      val data = line.removePrefix("data:").trim()
                                      if (data == "[DONE]") return@forEachLine
                                      try {
                                          val obj = com.google.gson.JsonParser.parseString(data).asJsonObject
                                          val chunk = obj.getAsJsonArray("choices")
                                              ?.firstOrNull()?.asJsonObject
                                              ?.getAsJsonObject("delta")
                                              ?.get("content")?.takeIf { !it.isJsonNull }?.asString
                                              ?: return@forEachLine
                                          full.append(chunk)
                                          onChunk?.invoke(chunk)
                                      } catch (_: Exception) {}
                                  }
                              }
                              val text = full.toString().trim()
                              result = text.ifBlank { throw Exception("Пустой ответ от сервера") }
                          }
                          else -> {
                              val body = response.body?.string() ?: ""; response.close()
                              if (body.isBlank()) throw Exception("Пустой ответ от сервера")
                              result = body
                          }
                      }
                  }

                  val responseText = result!!
                  if (!useStreaming) {
                      try {
                          val obj = com.google.gson.JsonParser.parseString(responseText).asJsonObject
                          val usage = obj.get("usage")?.takeIf { !it.isJsonNull }?.asJsonObject
                          if (usage != null) {
                              val pt = runCatching { usage.get("prompt_tokens")?.asLong ?: 0L }.getOrDefault(0L)
                              val ct = runCatching { usage.get("completion_tokens")?.asLong ?: 0L }.getOrDefault(0L)
                              val tt = runCatching { usage.get("total_tokens")?.asLong ?: 0L }.getOrDefault(0L)
                              if (tt > 0) saveTokenUsage(userId, modelId, pt, ct, tt)
                          }
                      } catch (_: Exception) {}

                      val extracted: String = run {
                          // 1. Standard OpenAI format; fall back to "reasoning" for reasoning models
                          runCatching {
                              gson.fromJson(responseText, ChatResponse::class.java)
                                  ?.choices?.firstOrNull()?.message?.let { msg ->
                                      msg.get("content")
                                          ?.takeIf { !it.isJsonNull }?.asString
                                          ?.takeIf { it.isNotBlank() }
                                          ?: msg.get("reasoning")
                                              ?.takeIf { !it.isJsonNull }?.asString
                                              ?.trim()?.takeIf { it.isNotBlank() }
                                  }
                          }.getOrNull()
                              ?: try {
                                  val obj = runCatching {
                                      com.google.gson.JsonParser.parseString(responseText).asJsonObject
                                  }.getOrElse {
                                      throw Exception("Ошибка получения ответа. Попробуйте ещё раз.")
                                  }
                                  val errNode = obj.get("error")
                                  if (errNode != null && !errNode.isJsonNull) {
                                      val msg = if (errNode.isJsonPrimitive) errNode.asString
                                          else runCatching { errNode.asJsonObject.get("message")?.asString }.getOrNull()
                                      throw Exception(msg?.takeIf { it.isNotBlank() } ?: "Ошибка API сервера")
                                  }
                                  listOf("content", "text", "reply", "response", "message")
                                      .firstNotNullOfOrNull { k ->
                                          obj.get(k)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString
                                      }
                              } catch (e: Exception) { throw e }
                              ?: throw Exception("Ошибка получения ответа. Попробуйте ещё раз.")
                      }
                      synchronized(responseCache) { responseCache[requestJson] = extracted }
                      extracted
                  } else {
                      val estCompletion = (responseText.length / 4).toLong().coerceAtLeast(1L)
                      val estPrompt = (apiMessages.sumOf { (it["content"] ?: "").length } / 4).toLong()
                      val estTotal = estPrompt + estCompletion
                      try { saveTokenUsage(userId, modelId, estPrompt, estCompletion, estTotal) } catch (_: Exception) {}
                      responseText
                  }
              } catch (e: IOException) {
                  throw Exception("Нет соединения с сервером. Проверьте интернет.")
              }
          }

          return saveAndReturn(userId, convId, userText, history, reply, modelId)
      }

      suspend fun editMessage(
          convId: String, fromMessageId: String, newText: String,
          historyBefore: List<Message>, settings: AISettings, onChunk: ((String) -> Unit)? = null
      ): String {
          val userId = uid ?: error("Не авторизован")
          val allMsgs = msgsRef(userId, convId).orderBy("timestamp").get().await()
          var deleting = false
          for (doc in allMsgs.documents) {
              if (doc.id == fromMessageId) deleting = true
              if (deleting) doc.reference.delete().await()
          }
          return sendMessage(convId, newText, historyBefore, settings, onChunk)
      }

      // ── Settings ───────────────────────────────────────────────────────────────

      suspend fun getSettings(): AISettings {
          val userId = uid ?: return AISettings()
          return try {
              val base = settingsRef(userId).get().await().toObject(AISettings::class.java) ?: AISettings()
              val token = try { tokenStore.getToken() } catch (_: Exception) { "" }
              base.copy(
                  openRouterToken = token,
                  customModels = (base.customModels as? List<String>).orEmpty(),
                  modelPrompts = (base.modelPrompts as? Map<String, String>).orEmpty()
              )
          }
          catch (_: Exception) { AISettings() }
      }

      suspend fun saveSettings(settings: AISettings) {
          val userId = uid ?: error("Не авторизован")
          tokenStore.saveToken(settings.openRouterToken)
          settingsRef(userId).set(settings).await()
      }

      // ── Reactions / Sharing ────────────────────────────────────────────────────

      suspend fun toggleReaction(convId: String, msgId: String, isThumbsUp: Boolean) {
          val userId = uid ?: error("Не авторизован")
          val ref = msgsRef(userId, convId).document(msgId)
          val snap = ref.get().await()
          val currentUp = (snap.getLong("thumbsUp") ?: 0L).toInt()
          val currentDown = (snap.getLong("thumbsDown") ?: 0L).toInt()
          val updates = mutableMapOf<String, Any>()
          if (isThumbsUp) {
              updates["thumbsUp"] = if (currentUp > 0) 0L else 1L
              if (currentDown > 0) updates["thumbsDown"] = 0L
          } else {
              updates["thumbsDown"] = if (currentDown > 0) 0L else 1L
              if (currentUp > 0) updates["thumbsUp"] = 0L
          }
          ref.update(updates).await()
      }

      suspend fun getConversationText(convId: String, title: String): String {
          val userId = uid ?: error("Не авторизован")
          val docs = msgsRef(userId, convId).orderBy("timestamp").get().await()
          return buildString {
              appendLine("=== $title ==="); appendLine("Диалог из NovAI · Nova Project"); appendLine()
              docs.documents.forEach { doc ->
                  val m = doc.toObject(Message::class.java) ?: return@forEach
                  appendLine("[${if (m.role == "user") "Я" else "NovAI"}]: ${m.content}"); appendLine()
              }
          }
      }

      // ── Stats / Account ────────────────────────────────────────────────────────

      suspend fun getUserStats(): UserStats {
          val userId = uid ?: return UserStats()
          val convs = convsRef(userId).get().await()
          val now = System.currentTimeMillis(); val dayMs = 86_400_000L
          var activeToday = 0; var activeThisWeek = 0; val dailyCounts = IntArray(7)
          for (doc in convs.documents) {
              val ts = doc.getTimestamp("updatedAt") ?: continue
              val daysAgo = ((now - ts.toDate().time) / dayMs).toInt()
              if (daysAgo == 0) activeToday++
              if (daysAgo < 7) { activeThisWeek++; if (6 - daysAgo in 0..6) dailyCounts[6 - daysAgo]++ }
          }
          val sdf = java.text.SimpleDateFormat("d MMM", java.util.Locale("ru"))
          val daily = (0..6).map { i ->
              val daysAgo = 6 - i
              val label = when (daysAgo) {
                  0 -> "Сег"; 1 -> "Вч"
                  else -> { val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -daysAgo) }; sdf.format(cal.time) }
              }
              DayActivity(label = label, count = dailyCounts[i])
          }

          fun toLong(v: Any?) = when (v) { is Long -> v; is Int -> v.toLong(); is Number -> v.toLong(); else -> 0L }
          val (totalMessages, tokenStats) = try {
              val data = statsRef(userId).get().await().data ?: emptyMap()
              @Suppress("UNCHECKED_CAST")
              val models = (data["models"] as? Map<*, *>) ?: emptyMap<String, Any>()
              Pair(
                  toLong(data["totalMessages"]),
                  models.entries.mapNotNull { (k, v) ->
                      @Suppress("UNCHECKED_CAST")
                      val mv = v as? Map<String, Any> ?: return@mapNotNull null
                      ModelTokenStats(k.toString(), toLong(mv["promptTokens"]), toLong(mv["completionTokens"]), toLong(mv["totalTokens"]), toLong(mv["messages"]))
                  }.sortedByDescending { it.totalTokens }
              )
          } catch (_: Exception) { Pair(0L, emptyList()) }

          return UserStats(convs.size(), activeToday, activeThisWeek, daily, totalMessages, tokenStats)
      }

      suspend fun deleteAccount() {
          val userId = uid ?: error("Не авторизован")
          val convDocs = convsRef(userId).get().await()
          for (c in convDocs.documents) {
              msgsRef(userId, c.id).get().await().documents.forEach { it.reference.delete().await() }
              c.reference.delete().await()
          }
          runCatching { settingsRef(userId).delete().await() }
          runCatching { statsRef(userId).delete().await() }
          analytics.logAccountDeleted()
          auth.currentUser?.delete()?.await()
      }

      fun clearCache() = synchronized(responseCache) { responseCache.clear() }

      // ── Private helpers ────────────────────────────────────────────────────────

      private suspend fun saveAndReturn(
          userId: String, convId: String, userText: String,
          history: List<Message>, reply: String, modelId: String = ""
      ): String {
          val aiMsg = Message(id = UUID.randomUUID().toString(), role = "assistant", content = reply, timestamp = Timestamp.now())
          msgsRef(userId, convId).document(aiMsg.id).set(aiMsg).await()
          val update = mutableMapOf<String, Any>("updatedAt" to Timestamp.now(), "preview" to userText.take(60))
          if (history.isEmpty()) update["title"] = userText.take(40)
          convsRef(userId).document(convId).update(update).await()

          analytics.logAiResponse(modelId)

          // Show a local notification when the app is in the background so the user
          // knows the AI has finished responding — tap to open NovAI.
          if (!AppStateManager.isAppInForeground) {
              showAiResponseNotification(reply)
          }

          return reply
      }

      /**
       * Shows a local (non-FCM) notification informing the user that the AI
       * has replied. Only shown when the app is in the background.
       */
      private fun showAiResponseNotification(replyText: String) {
          val intent = Intent(app, MainActivity::class.java).apply {
              addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
          }
          val pendingIntent = PendingIntent.getActivity(
              app, 0, intent,
              PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
          )
          val preview = replyText.take(120).let { if (replyText.length > 120) "$it…" else it }
          val notification = NotificationCompat.Builder(app, NovAIApp.CHANNEL_ID)
              .setSmallIcon(com.novaproject.novai.R.mipmap.ic_launcher)
              .setContentTitle("NovAI ответил")
              .setContentText(preview)
              .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
              .setAutoCancel(true)
              .setPriority(NotificationCompat.PRIORITY_HIGH)
              .setContentIntent(pendingIntent)
              .build()

          val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
          manager.notify(notifIdCounter.getAndIncrement(), notification)
      }

      private suspend fun saveTokenUsage(userId: String, model: String, promptTokens: Long, completionTokens: Long, totalTokens: Long) {
          try {
              firestore.runTransaction { tx ->
                  val ref = statsRef(userId); val snap = tx.get(ref); val data = snap.data ?: emptyMap()
                  fun toLong(v: Any?) = when (v) { is Long -> v; is Int -> v.toLong(); is Number -> v.toLong(); else -> 0L }
                  @Suppress("UNCHECKED_CAST")
                  val models = HashMap((data["models"] as? Map<*, *> ?: emptyMap<String, Any>()) as Map<String, Any>)
                  @Suppress("UNCHECKED_CAST")
                  val ex = (models[model] as? Map<String, Any>) ?: emptyMap()
                  models[model] = mapOf(
                      "promptTokens" to (toLong(ex["promptTokens"]) + promptTokens),
                      "completionTokens" to (toLong(ex["completionTokens"]) + completionTokens),
                      "totalTokens" to (toLong(ex["totalTokens"]) + totalTokens),
                      "messages" to (toLong(ex["messages"]) + 1L)
                  )
                  tx.set(ref, mapOf("models" to models, "totalMessages" to (toLong(data["totalMessages"]) + 1L)))
              }.await()
          } catch (_: Exception) {}
      }
  }
  