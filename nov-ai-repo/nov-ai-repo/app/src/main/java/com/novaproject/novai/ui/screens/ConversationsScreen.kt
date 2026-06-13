package com.novaproject.novai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaproject.novai.R
import com.novaproject.novai.data.model.Conversation
import com.novaproject.novai.data.model.MessageSearchResult
import com.novaproject.novai.ui.theme.*
import com.novaproject.novai.viewmodel.ChatViewModel
import java.util.Calendar

enum class ChatFilter(val label: String) {
    ALL("Все"), TODAY("Сегодня"), YESTERDAY("Вчера"), WEEK("7 дней")
}

private data class DateGroup(val label: String, val conversations: List<Conversation>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    viewModel: ChatViewModel,
    onOpenChat: (String) -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onStats: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val state by viewModel.convState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    var selectedFilter by remember { mutableStateOf(ChatFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    val accent = LocalAccentColor.current

    val firebaseUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }
    val userName = firebaseUser?.displayName?.takeIf { it.isNotBlank() } ?: "Пользователь"
    val userInitial = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "П"

    // Propagate search query to ViewModel for full-text search
    LaunchedEffect(searchQuery) { viewModel.searchMessages(searchQuery) }
    LaunchedEffect(state.error) {
        if (state.error != null) { kotlinx.coroutines.delay(5000); viewModel.clearConvError() }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "headerGradient")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = -1200f, targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "gradientX"
    )

    // Conversation-level filter (only used when not doing full-text search)
    val isFullTextSearch = searchQuery.length >= 2
    val filtered = remember(state.conversations, selectedFilter, searchQuery) {
        if (isFullTextSearch) state.conversations // search results shown separately
        else {
            val q = searchQuery.trim()
            state.conversations.let { list ->
                if (q.isNotEmpty())
                    list.filter { it.title.contains(q, true) || it.preview.contains(q, true) }
                else list
            }
        }
    }
    val pinned = remember(filtered) { filtered.filter { it.isPinned } }
    val unpinned = remember(filtered) { filtered.filter { !it.isPinned } }
    val grouped = remember(unpinned, selectedFilter) { buildGroups(unpinned, selectedFilter) }

    Box(modifier = Modifier.fillMaxSize().background(NovDark)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().background(NovSurface).statusBarsPadding()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomStart)
                        .background(Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, accent, NovPurple, accent, Color.Transparent),
                            startX = gradientOffset, endX = gradientOffset + 2400f
                        ))
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onProfile) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(accent.copy(0.25f), NovPurple.copy(0.35f)))),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Person, "Профиль", tint = accent, modifier = Modifier.size(20.dp)) }
                    }
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(accent, NovPurple))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(userInitial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(userName, color = NovTextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text("NovAI · ИИ-ассистент", color = NovTextSecondary, fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onStats) {
                        Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(0.08f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.BarChart, "Статистика", tint = accent, modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(onClick = onSettings) {
                        Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(NovCard), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Settings, "Настройки", tint = NovTextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // ── Offline banner ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = !isOnline,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFF7C3000))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.WifiOff, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                    Text("Нет подключения к интернету", color = Color(0xFFFFB74D), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            // ── Search bar ────────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск по диалогам и сообщениям...", color = NovTextSecondary, fontSize = 13.sp) },
                    leadingIcon = {
                        if (searchState.isSearching)
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accent, strokeWidth = 2.dp)
                        else
                            Icon(Icons.Default.Search, null, tint = NovTextSecondary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; viewModel.clearSearch() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = NovTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent, unfocusedBorderColor = NovDivider,
                        focusedContainerColor = NovCard, unfocusedContainerColor = NovCard,
                        cursorColor = accent, focusedTextColor = NovTextPrimary, unfocusedTextColor = NovTextPrimary
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                )
            }

            // ── Error banner ──────────────────────────────────────────────────
            AnimatedVisibility(visible = state.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(0.12f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(state.error ?: "", color = ErrorRed, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    }
                }
            }

            // ── Date filters (hide when full-text search is active) ──────────
            if (!isFullTextSearch) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChatFilter.values().forEach { filter ->
                        val selected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) Brush.horizontalGradient(listOf(accent.copy(0.25f), NovPurple.copy(0.2f))) else Brush.horizontalGradient(listOf(NovCard, NovCard)))
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(filter.label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) accent else NovTextSecondary)
                        }
                    }
                }
            }

            // ── Full-text search results ──────────────────────────────────────
            if (isFullTextSearch) {
                val results = searchState.messageResults
                if (results.isEmpty() && !searchState.isSearching) {
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, null, tint = NovTextSecondary.copy(0.3f), modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Ничего не найдено в сообщениях", color = NovTextSecondary, fontSize = 15.sp)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
                        if (results.isNotEmpty()) {
                            item {
                                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Search, null, tint = accent, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("НАЙДЕНО В СООБЩЕНИЯХ (${results.size})", color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            items(results, key = { "${it.convId}_${it.message.id}" }) { result ->
                                MessageSearchResultItem(result = result, query = searchQuery, accent = accent, onClick = { onOpenChat(result.convId) })
                            }
                        }
                    }
                }
            } else {
                // ── Standard conversation list ────────────────────────────────
                val hasContent = pinned.isNotEmpty() || grouped.isNotEmpty()
                if (!hasContent) {
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(90.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(accent.copy(0.08f), NovPurple.copy(0.08f)))),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.ChatBubbleOutline, null, tint = NovTextSecondary.copy(0.4f), modifier = Modifier.size(40.dp)) }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                if (searchQuery.isNotEmpty()) "Ничего не найдено"
                                else if (selectedFilter == ChatFilter.ALL) "Нет диалогов" else "Нет диалогов за этот период",
                                color = NovTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (searchQuery.isNotEmpty()) "Попробуйте другой запрос" else "Нажмите + чтобы начать",
                                color = NovTextSecondary, fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
                        if (pinned.isNotEmpty()) {
                            item(key = "pinned_header") {
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PushPin, null, tint = accent, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("ЗАКРЕПЛЁННЫЕ", color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            items(pinned, key = { "pinned_${it.id}" }) { conv ->
                                ConversationItem(conv = conv, accent = accent,
                                    onClick = { onOpenChat(conv.id) },
                                    onDelete = { viewModel.deleteConversation(conv.id) },
                                    onRename = { viewModel.renameConversation(conv.id, it) },
                                    onTogglePin = { viewModel.pinConversation(conv.id, !conv.isPinned) })
                            }
                            item(key = "pinned_divider") {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = NovDivider)
                            }
                        }
                        grouped.forEach { group ->
                            item(key = "hdr_${group.label}") { DateSectionHeader(group.label) }
                            items(group.conversations, key = { it.id }) { conv ->
                                ConversationItem(conv = conv, accent = accent,
                                    onClick = { onOpenChat(conv.id) },
                                    onDelete = { viewModel.deleteConversation(conv.id) },
                                    onRename = { viewModel.renameConversation(conv.id, it) },
                                    onTogglePin = { viewModel.pinConversation(conv.id, !conv.isPinned) })
                            }
                        }
                    }
                }
            }
        }

        // ── FAB ──────────────────────────────────────────────────────────────
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            if (state.isLoading) {
                Box(modifier = Modifier.size(60.dp).shadow(12.dp, CircleShape).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accent, NovPurple))), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(26.dp), color = NovDark, strokeWidth = 2.5.dp)
                }
            } else {
                Box(modifier = Modifier.size(60.dp).shadow(12.dp, CircleShape).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accent, NovPurple)))
                    .clickable { viewModel.createNewConversation { id -> onOpenChat(id) } },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Add, "Новый диалог", tint = NovDark, modifier = Modifier.size(28.dp)) }
            }
        }
    }
}

@Composable
private fun MessageSearchResultItem(
    result: MessageSearchResult,
    query: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = NovCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (result.message.role == "user") Icons.Default.Person else Icons.Default.SmartToy,
                    null, tint = if (result.message.role == "user") NovPurple else accent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(result.convTitle, color = NovTextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, null, tint = NovTextSecondary, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.height(6.dp))
            HighlightedText(text = result.message.content, query = query, accent = accent)
        }
    }
}

@Composable
private fun HighlightedText(text: String, query: String, accent: androidx.compose.ui.graphics.Color) {
    val lower = text.lowercase()
    val queryLower = query.lowercase()
    val idx = lower.indexOf(queryLower)
    if (idx < 0) {
        Text(text.take(120), color = NovTextSecondary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        return
    }
    val start = maxOf(0, idx - 30)
    val end = minOf(text.length, idx + query.length + 60)
    val snippet = (if (start > 0) "…" else "") + text.substring(start, end) + (if (end < text.length) "…" else "")
    val snipLower = snippet.lowercase()
    val qIdx = snipLower.indexOf(queryLower)

    val annotated = buildAnnotatedString {
        if (qIdx > 0) append(snippet.take(qIdx))
        withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold, background = accent.copy(0.15f))) {
            append(snippet.substring(qIdx, qIdx + query.length))
        }
        if (qIdx + query.length < snippet.length) append(snippet.substring(qIdx + query.length))
    }
    Text(annotated, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun DateSectionHeader(label: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = NovDivider)
        Box(modifier = Modifier.padding(horizontal = 10.dp).clip(RoundedCornerShape(20.dp)).background(NovCard).padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text(label, color = NovTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(modifier = Modifier.weight(1f), color = NovDivider)
    }
}

private fun buildGroups(conversations: List<Conversation>, filter: ChatFilter): List<DateGroup> {
    val now = Calendar.getInstance()
    val yCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val wCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    fun cal(c: Conversation) = Calendar.getInstance().apply { time = c.updatedAt.toDate() }
    fun isToday(c: Calendar) = c.get(Calendar.YEAR) == now.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    fun isYest(c: Calendar) = c.get(Calendar.YEAR) == yCal.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == yCal.get(Calendar.DAY_OF_YEAR)
    fun isWeek(c: Calendar) = c.timeInMillis >= wCal.timeInMillis
    return when (filter) {
        ChatFilter.TODAY -> conversations.filter { isToday(cal(it)) }.let { if (it.isEmpty()) emptyList() else listOf(DateGroup("Сегодня", it)) }
        ChatFilter.YESTERDAY -> conversations.filter { isYest(cal(it)) }.let { if (it.isEmpty()) emptyList() else listOf(DateGroup("Вчера", it)) }
        ChatFilter.WEEK -> buildList {
            val t = conversations.filter { isToday(cal(it)) }
            val y = conversations.filter { isYest(cal(it)) }
            val w = conversations.filter { val c = cal(it); isWeek(c) && !isToday(c) && !isYest(c) }
            if (t.isNotEmpty()) add(DateGroup("Сегодня", t)); if (y.isNotEmpty()) add(DateGroup("Вчера", y)); if (w.isNotEmpty()) add(DateGroup("Последние 7 дней", w))
        }
        ChatFilter.ALL -> buildList {
            val t = mutableListOf<Conversation>(); val y = mutableListOf<Conversation>()
            val w = mutableListOf<Conversation>(); val o = mutableListOf<Conversation>()
            conversations.forEach { conv -> val c = cal(conv); when { isToday(c) -> t; isYest(c) -> y; isWeek(c) -> w; else -> o }.add(conv) }
            if (t.isNotEmpty()) add(DateGroup("Сегодня", t)); if (y.isNotEmpty()) add(DateGroup("Вчера", y))
            if (w.isNotEmpty()) add(DateGroup("Последние 7 дней", w)); if (o.isNotEmpty()) add(DateGroup("Ранее", o))
        }
    }
}

private fun formatRelativeTime(timestamp: com.google.firebase.Timestamp): String {
    val diff = System.currentTimeMillis() - timestamp.toDate().time
    return when {
        diff < 60_000L -> "только что"; diff < 3_600_000L -> "${diff / 60_000L} мин"
        diff < 86_400_000L -> "${diff / 3_600_000L} ч назад"; diff < 2 * 86_400_000L -> "вчера"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000L} дн назад"
        else -> { val sdf = java.text.SimpleDateFormat("d MMM", java.util.Locale("ru")); sdf.format(timestamp.toDate()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationItem(
    conv: Conversation,
    accent: androidx.compose.ui.graphics.Color = LocalAccentColor.current,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit = {},
    onTogglePin: () -> Unit = {}
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember(conv.title) { mutableStateOf(conv.title) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { v -> if (v == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false }
    )

    // Outer Box so action buttons can float ABOVE the SwipeToDismissBox (no gesture conflict)
    Box(modifier = Modifier.fillMaxWidth()) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp)).background(ErrorRed), contentAlignment = Alignment.CenterEnd) {
                    Icon(Icons.Default.Delete, "Удалить", tint = Color.White, modifier = Modifier.padding(end = 20.dp))
                }
            }
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = NovCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.weight(1f).clickable { onClick() }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(accent, NovPurple))), contentAlignment = Alignment.Center) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(R.drawable.novai_logo),
                                contentDescription = "NovAI",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (conv.isPinned) { Icon(Icons.Default.PushPin, null, tint = accent, modifier = Modifier.size(11.dp)); Spacer(Modifier.width(3.dp)) }
                                Text(conv.title, color = NovTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (conv.preview.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(conv.preview, color = NovTextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    // Reserve space so content doesn't slide under floating buttons
                    Spacer(Modifier.width(78.dp))
                }
            }
        }

        // Action buttons layered ABOVE the SwipeToDismissBox — clicks are never intercepted by the swipe gesture
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(formatRelativeTime(conv.updatedAt), color = NovTextSecondary, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Row {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).clickable { onTogglePin() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PushPin,
                        contentDescription = if (conv.isPinned) "Открепить" else "Закрепить",
                        tint = if (conv.isPinned) accent else NovTextSecondary.copy(0.45f),
                        modifier = Modifier.size(14.dp))
                }
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).clickable { renameText = conv.title; showRenameDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, "Переименовать", tint = NovTextSecondary.copy(0.4f), modifier = Modifier.size(14.dp))
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = NovCard,
            title = { Text("Переименовать диалог", color = NovTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = renameText, onValueChange = { renameText = it }, singleLine = true,
                    placeholder = { Text("Название диалога", color = NovTextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent, unfocusedBorderColor = NovDivider,
                        focusedContainerColor = NovDark, unfocusedContainerColor = NovDark,
                        cursorColor = accent, focusedTextColor = NovTextPrimary, unfocusedTextColor = NovTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { if (renameText.isNotBlank()) { onRename(renameText); showRenameDialog = false } }) {
                    Text("Сохранить", color = accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Отмена", color = NovTextSecondary) } }
        )
    }
}
