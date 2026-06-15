package com.novaproject.novai.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaproject.novai.R
import com.novaproject.novai.data.model.FREE_MODEL_LIST
import com.novaproject.novai.data.model.FreeModel
import com.novaproject.novai.data.model.Message
import com.novaproject.novai.data.model.friendlyModelName
import com.novaproject.novai.ui.components.MarkdownText
import com.novaproject.novai.ui.theme.*
import com.novaproject.novai.viewmodel.ChatViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    convId: String,
    username: String,
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.chatState.collectAsState()
    val convState by viewModel.convState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val accent = LocalAccentColor.current

    var input by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var showModelDropdown by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }

    val aiName = state.aiName
    val convTitle = convState.conversations.firstOrNull { it.id == convId }?.title ?: "Диалог"

    // Build full model list: free presets + all user-added custom models
    val allModelOptions = remember(state.customModels) {
        val custom = state.customModels
            .filter { id -> FREE_MODEL_LIST.none { it.modelId == id } }
            .map { id -> FreeModel(
                label = friendlyModelName(id),
                modelId = id,
                description = if (id.endsWith(":free")) "Своя модель · бесплатная" else "Своя модель · OpenRouter"
            )}
        FREE_MODEL_LIST + custom
    }

    val currentModelDisplay = allModelOptions.firstOrNull { it.modelId == state.currentModel }?.label
        ?: if (state.currentModel.isBlank()) "NovAI"
        else friendlyModelName(state.currentModel)

    LaunchedEffect(convId) { viewModel.openConversation(convId) }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            kotlinx.coroutines.delay(80)
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }
    DisposableEffect(Unit) { onDispose { viewModel.resetChat() } }

    Scaffold(
        containerColor = NovDark,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AiAvatarSmall(accent = accent)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    if (username.isNotBlank()) username else aiName,
                                    color = NovTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(accent.copy(0.12f))
                                        .clickable { showModelDropdown = true }
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(currentModelDisplay, color = accent, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = NovTextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (state.messages.isNotEmpty()) {
                                val text = viewModel.getShareText(convTitle)
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                                }, "Поделиться диалогом"))
                            }
                        }) {
                            Icon(Icons.Default.Share, "Поделиться", tint = NovTextSecondary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NovSurface)
                )

                // Model picker dropdown
                DropdownMenu(
                    expanded = showModelDropdown,
                    onDismissRequest = { showModelDropdown = false },
                    containerColor = NovCard
                ) {
                    Text(
                        "Выбор модели",
                        color = NovTextSecondary, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    allModelOptions.forEach { opt ->
                        val isCurrent = state.currentModel == opt.modelId
                        DropdownMenuItem(
                            text = {
                                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text(
                                        opt.label,
                                        color = if (isCurrent) accent else NovTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(opt.description, color = NovTextSecondary, fontSize = 11.sp)
                                }
                            },
                            leadingIcon = if (isCurrent) { Icon(Icons.Default.Check, null, tint = accent, modifier = Modifier.size(16.dp)) } else null,
                            onClick = { viewModel.switchModel(opt.modelId); showModelDropdown = false }
                        )
                        if (opt != allModelOptions.last()) HorizontalDivider(color = NovDivider.copy(0.5f), modifier = Modifier.padding(horizontal = 8.dp))
                    }
                    // Link to settings to add custom models
                    HorizontalDivider(color = NovDivider, modifier = Modifier.padding(horizontal = 8.dp))
                    DropdownMenuItem(
                        text = { Text("Добавить свою модель в настройках →", color = NovTextSecondary, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Settings, null, tint = NovTextSecondary, modifier = Modifier.size(15.dp)) },
                        onClick = { showModelDropdown = false }
                    )
                }

                AnimatedGradientBar(accent = accent)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            AnimatedVisibility(visible = !isOnline, enter = expandVertically(), exit = shrinkVertically()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF7C3000)).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.WifiOff, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                    Text("Нет подключения — сообщения не отправятся", color = Color(0xFFFFB74D), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.hasMoreMessages || state.isLoadingMore) {
                    item(key = "load_more") {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (state.isLoadingMore) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircularProgressIndicator(Modifier.size(16.dp), color = accent, strokeWidth = 2.dp)
                                    Text("Загрузка...", color = NovTextSecondary, fontSize = 12.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.loadMoreMessages() },
                                    border = BorderStroke(1.dp, accent.copy(0.4f)),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Загрузить более ранние", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Bug-5: AI context is capped at the last 20 messages (see ChatRepository.sendMessage).
                // Show an informational banner when the conversation exceeds that limit.
                if (state.messages.size > 20) {
                    item(key = "context_limit_banner") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(accent.copy(alpha = 0.08f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "ИИ видит только последние 20 сообщений из этого диалога",
                                color = accent,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                if (state.messages.isEmpty() && !state.hasMoreMessages) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AiAvatarSmall(accent = accent, size = 80.dp)
                                Spacer(Modifier.height(16.dp))
                                Text(if (username.isNotBlank()) "Привет, $username!" else "Привет!", color = NovTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Spacer(Modifier.height(6.dp))
                                Text("Задай вопрос или выбери шаблон ниже", color = NovTextSecondary, fontSize = 14.sp)
                                Spacer(Modifier.height(24.dp))
                                SuggestionChips(
                                    suggestions = listOf("Расскажи что-то интересное", "Помоги написать код", "Придумай идею для проекта", "Объясни сложную тему просто"),
                                    onSelect = { viewModel.sendMessage(it) }
                                )
                            }
                        }
                    }
                } else {
                    items(state.messages, key = { it.id }) { msg ->
                        val lastAiMsg = if (!state.isSending) state.messages.lastOrNull { it.role == "assistant" } else null
                        MessageBubble(
                            msg = msg, accent = accent,
                            onEdit = if (msg.role == "user") { editingMessage = msg; input = msg.content } else null,
                            onReaction = { isThumbsUp -> viewModel.addReaction(convId, msg.id, isThumbsUp) },
                            isLast = msg.id == lastAiMsg?.id,
                            onRegenerate = if (msg.id == lastAiMsg?.id) { viewModel.regenerateLastResponse() } else null,
                        )
                    }
                }

                if (state.isSending) {
                    item(key = "sending") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            if (state.streamingContent.isNotBlank()) {
                                StreamingBubble(content = state.streamingContent, accent = accent)
                            } else {
                                TypingIndicator(aiName = aiName, accent = accent)
                            }
                        }
                    }
                }
            }

            state.error?.let { err ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(err, color = ErrorRed, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearChatError() }) { Text("OK", color = NovTextSecondary) }
                    }
                }
            }

            if (editingMessage != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(accent.copy(0.08f)).padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Create, null, tint = accent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(editingMessage!!.content, color = NovTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = { editingMessage = null; input = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Отмена", tint = NovTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            AnimatedVisibility(visible = showTemplates, enter = expandVertically(), exit = shrinkVertically()) {
                PromptTemplatesPanel(onSelect = { template -> input = template.text; showTemplates = false }, onDismiss = { showTemplates = false })
            }

            Row(
                modifier = Modifier.fillMaxWidth().background(NovSurface)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .navigationBarsPadding().imePadding(),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    onClick = { showTemplates = !showTemplates },
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (showTemplates) accent.copy(0.15f) else NovCard)
                ) {
                    Icon(Icons.Default.AutoAwesome, "Шаблоны", tint = if (showTemplates) accent else NovTextSecondary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Написать сообщение...", color = NovTextSecondary, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent, unfocusedBorderColor = NovDivider,
                        focusedContainerColor = NovCard, unfocusedContainerColor = NovCard,
                        cursorColor = accent, focusedTextColor = NovTextPrimary, unfocusedTextColor = NovTextPrimary
                    )
                )
                Spacer(Modifier.width(8.dp))
                val canSend = input.isNotBlank() && !state.isSending && isOnline
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(
                        if (canSend) Brush.linearGradient(listOf(accent, NovPurple))
                        else Brush.linearGradient(listOf(NovCard, NovCard))
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (canSend) {
                                val em = editingMessage
                                if (em != null) { viewModel.editMessage(em.id, input); editingMessage = null }
                                else viewModel.sendMessage(input)
                                input = ""
                            }
                        },
                        enabled = canSend, modifier = Modifier.fillMaxSize()
                    ) {
                        if (state.isSending) CircularProgressIndicator(Modifier.size(20.dp), color = accent, strokeWidth = 2.dp)
                        else Icon(Icons.AutoMirrored.Filled.Send, "Отправить", tint = if (canSend) NovDark else NovTextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// ── Prompt Templates Panel ─────────────────────────────────────────────────────

@Composable
private fun PromptTemplatesPanel(onSelect: (PromptTemplate) -> Unit, onDismiss: () -> Unit) {
    val accent = LocalAccentColor.current
    var selectedCategory by remember { mutableStateOf(PROMPT_CATEGORIES.first()) }
    val scrollState = rememberScrollState()
    val templateScroll = rememberScrollState()

    Column(modifier = Modifier.fillMaxWidth().background(NovCard).padding(top = 12.dp, bottom = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Шаблоны промтов", color = NovTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "Закрыть", tint = NovTextSecondary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PROMPT_CATEGORIES.forEach { cat ->
                val isSelected = cat.name == selectedCategory.name
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) accent.copy(0.18f) else NovDark)
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(cat.name, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = if (isSelected) accent else NovTextSecondary)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(templateScroll).padding(horizontal = 12.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            selectedCategory.templates.forEach { template ->
                Card(
                    modifier = Modifier.width(140.dp).clickable { onSelect(template) },
                    colors = CardDefaults.cardColors(containerColor = NovSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(template.icon, fontSize = 24.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(template.title, color = NovTextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(3.dp))
                        Text(template.text.take(50).trim() + "…", color = NovTextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// ── AI Avatar — logo fills the full circle ─────────────────────────────────────

@Composable
fun AiAvatarSmall(
    accent: androidx.compose.ui.graphics.Color = LocalAccentColor.current,
    size: androidx.compose.ui.unit.Dp = 36.dp
) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape)
            .background(Brush.linearGradient(listOf(accent, NovPurple))),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.novai_logo),
            contentDescription = "NovAI",
            modifier = Modifier.fillMaxSize().clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun MsgAiAvatar(accent: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier.size(32.dp).clip(CircleShape)
            .background(Brush.linearGradient(listOf(accent, NovPurple))),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.novai_logo),
            contentDescription = "NovAI",
            modifier = Modifier.fillMaxSize().clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

// ── Streaming bubble ──────────────────────────────────────────────────────────

@Composable
private fun StreamingBubble(content: String, accent: androidx.compose.ui.graphics.Color) {
    var showCursor by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { while (true) { delay(500); showCursor = !showCursor } }
    Row(verticalAlignment = Alignment.Top) {
        MsgAiAvatar(accent = accent)
        Spacer(Modifier.width(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = NovCard), shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)) {
            MarkdownText(text = content + if (showCursor) "▍" else " ", modifier = Modifier.widthIn(max = 288.dp).padding(horizontal = 14.dp, vertical = 10.dp))
        }
    }
}

// ── Message bubble ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    msg: Message,
    isLast: Boolean = false,
    accent: androidx.compose.ui.graphics.Color = LocalAccentColor.current,
    onEdit: (() -> Unit)? = null,
    onReaction: ((Boolean) -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null
) {
    val isUser = msg.role == "user"
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
            if (!isUser) { MsgAiAvatar(accent = accent); Spacer(Modifier.width(8.dp)) }
            Box {
                Card(
                    modifier = Modifier.widthIn(max = 288.dp).combinedClickable(onClick = {}, onLongClick = { showMenu = true }),
                    shape = if (isUser) RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp) else RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isUser) accent.copy(0.12f) else NovCard),
                    border = if (isUser) BorderStroke(1.dp, accent.copy(0.3f)) else null
                ) {
                    if (isUser)
                        Text(msg.content, color = NovTextPrimary, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                    else
                        MarkdownText(text = msg.content, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = NovCard) {
                    DropdownMenuItem(
                        text = { Text("Скопировать", color = NovTextPrimary, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = accent, modifier = Modifier.size(18.dp)) },
                        onClick = { clipboardManager.setText(AnnotatedString(msg.content)); showMenu = false }
                    )
                    if (isUser && onEdit != null) {
                        DropdownMenuItem(
                            text = { Text("Редактировать", color = NovTextPrimary, fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Create, null, tint = accent, modifier = Modifier.size(18.dp)) },
                            onClick = { showMenu = false; onEdit() }
                        )
                    }
                }
            }
        }

        // ── Action row ──────────────────────────────────────────────────────────
        if (!isUser) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.padding(start = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Copy
                IconButton(onClick = { clipboardManager.setText(AnnotatedString(msg.content)) }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.ContentCopy, "Копировать",
                        tint = NovTextSecondary.copy(0.55f), modifier = Modifier.size(14.dp))
                }
                // Thumbs up — colour only, no count
                IconButton(onClick = { onReaction?.invoke(true) }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.ThumbUp, "Нравится",
                        tint = if (msg.thumbsUp > 0) accent else NovTextSecondary.copy(0.55f),
                        modifier = Modifier.size(14.dp))
                }
                // Thumbs down — colour only, no count
                IconButton(onClick = { onReaction?.invoke(false) }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.ThumbDown, "Не нравится",
                        tint = if (msg.thumbsDown > 0) ErrorRed else NovTextSecondary.copy(0.55f),
                        modifier = Modifier.size(14.dp))
                }
                // Regenerate — only under the last AI message
                if (isLast && onRegenerate != null) {
                    IconButton(onClick = onRegenerate, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Refresh, "Повторить",
                            tint = NovTextSecondary.copy(0.55f), modifier = Modifier.size(14.dp))
                    }
                }
            }
        } else {
            // Pencil edit button below user messages
            if (onEdit != null) {
                Spacer(Modifier.height(2.dp))
                Row(modifier = Modifier.padding(end = 4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, "Редактировать",
                            tint = NovTextSecondary.copy(0.45f), modifier = Modifier.size(13.dp))
                    }
                }
            }
        }
    }
}
    var dots by remember { mutableStateOf(1) }
    LaunchedEffect(Unit) { while (true) { delay(500); dots = if (dots >= 3) 1 else dots + 1 } }
    Row(verticalAlignment = Alignment.CenterVertically) {
        MsgAiAvatar(accent = accent)
        Spacer(Modifier.width(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = NovCard), shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)) {
            Text("$aiName печатает" + ".".repeat(dots), color = NovTextSecondary, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
    }
}

@Composable
private fun AnimatedGradientBar(accent: androidx.compose.ui.graphics.Color = LocalAccentColor.current) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradientBar")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Reverse),
        label = "offset"
    )
    Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
        Brush.linearGradient(listOf(NovPurple, accent, NovPurple, accent),
            start = Offset(animOffset, 0f), end = Offset(animOffset + 500f, 0f))
    ))
}

@Composable
private fun SuggestionChips(suggestions: List<String>, onSelect: (String) -> Unit) {
    val accent = LocalAccentColor.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        suggestions.forEach { suggestion ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(containerColor = NovCard),
                shape = RoundedCornerShape(16.dp),
                onClick = { onSelect(suggestion) }
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚡", fontSize = 14.sp, modifier = Modifier.padding(end = 10.dp))
                    Text(suggestion, color = NovTextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("↗", color = accent, fontSize = 14.sp)
                }
            }
        }
    }
}
