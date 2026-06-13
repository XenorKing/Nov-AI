package com.novaproject.novai.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.novaproject.novai.data.model.Conversation
import com.novaproject.novai.ui.screens.ChatScreen
import com.novaproject.novai.ui.screens.ProfileScreen
import com.novaproject.novai.ui.screens.SettingsScreen
import com.novaproject.novai.ui.theme.*
import com.novaproject.novai.viewmodel.AuthViewModel
import com.novaproject.novai.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private object Routes {
    const val AUTH   = "auth"
    const val HOME   = "home"
    const val CHAT   = "chat/{convId}"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"

    fun chat(convId: String) = "chat/$convId"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authVm: AuthViewModel = hiltViewModel()
    val chatVm: ChatViewModel = hiltViewModel()
    val authState by authVm.uiState.collectAsState()

    NavHost(navController = navController, startDestination = Routes.AUTH) {
        composable(Routes.AUTH) {
            AuthScreen(
                viewModel = authVm,
                onAuthenticated = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            LaunchedEffect(authState.user) {
                if (authState.user == null) {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            }
            HomeScreen(
                viewModel = chatVm,
                username = authState.user?.displayName ?: "",
                onOpenChat = { convId -> navController.navigate(Routes.chat(convId)) },
                onProfile = { navController.navigate(Routes.PROFILE) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("convId") { type = NavType.StringType })
        ) { back ->
            val convId = back.arguments?.getString("convId") ?: return@composable
            val user = authState.user?.displayName ?: ""
            ChatScreen(
                convId = convId,
                username = user,
                viewModel = chatVm,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onDeleteAccount = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// ── Home screen ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    viewModel: ChatViewModel,
    username: String,
    onOpenChat: (String) -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit
) {
    val convState by viewModel.convState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()

    var showSearch by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf<Conversation?>(null) }

    val pinnedConvs = convState.conversations.filter { it.isPinned }
    val unpinnedConvs = convState.conversations.filter { !it.isPinned }

    showDeleteDialog?.let { convId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = NovCard,
            title = { Text("Удалить чат?", color = NovTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Сообщения будут удалены безвозвратно.", color = NovTextSecondary) },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteConversation(convId); showDeleteDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Отмена", color = NovTextSecondary) }
            }
        )
    }

    showRenameDialog?.let { conv ->
        RenameDialog(
            current = conv.title,
            onDismiss = { showRenameDialog = null },
            onConfirm = { newTitle -> viewModel.renameConversation(conv.id, newTitle); showRenameDialog = null }
        )
    }

    Scaffold(
        containerColor = NovDark,
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchState.query,
                            onValueChange = viewModel::searchMessages,
                            placeholder = { Text("Поиск по всем чатам...", color = NovTextSecondary, fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LocalAccentColor.current,
                                unfocusedBorderColor = NovDivider,
                                focusedContainerColor = NovCard,
                                unfocusedContainerColor = NovCard,
                                cursorColor = LocalAccentColor.current,
                                focusedTextColor = NovTextPrimary,
                                unfocusedTextColor = NovTextPrimary
                            )
                        )
                    } else {
                        Text("NovAI", color = NovTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onProfile) {
                        Icon(Icons.Default.AccountCircle, "Профиль", tint = NovTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) viewModel.clearSearch()
                    }) {
                        Icon(
                            if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            "Поиск",
                            tint = if (showSearch) LocalAccentColor.current else NovTextSecondary
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "Настройки", tint = NovTextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NovSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.createNewConversation { convId -> onOpenChat(convId) }
                },
                containerColor = LocalAccentColor.current,
                contentColor = NovDark,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, "Новый чат")
            }
        }
    ) { padding ->
        if (showSearch && searchState.query.length >= 2) {
            SearchResultsList(
                searchState = searchState,
                onOpenChat = { convId -> showSearch = false; viewModel.clearSearch(); onOpenChat(convId) },
                modifier = Modifier.padding(padding)
            )
        } else {
            ConversationList(
                pinned = pinnedConvs,
                unpinned = unpinnedConvs,
                isLoading = convState.isLoading,
                error = convState.error,
                onOpen = onOpenChat,
                onPin = { conv -> viewModel.pinConversation(conv.id, !conv.isPinned) },
                onDelete = { conv -> showDeleteDialog = conv.id },
                onRename = { conv -> showRenameDialog = conv },
                onDismissError = { viewModel.clearConvError() },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

// ── Conversation list with swipe-to-pin ───────────────────────────────────────

@Composable
private fun ConversationList(
    pinned: List<Conversation>,
    unpinned: List<Conversation>,
    isLoading: Boolean,
    error: String?,
    onOpen: (String) -> Unit,
    onPin: (Conversation) -> Unit,
    onDelete: (Conversation) -> Unit,
    onRename: (Conversation) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccentColor.current
    val allEmpty = pinned.isEmpty() && unpinned.isEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        error?.let {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(it, color = ErrorRed, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismissError) { Text("OK", color = NovTextSecondary) }
                    }
                }
            }
        }

        if (isLoading && allEmpty) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            }
        } else if (allEmpty) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ChatBubbleOutline, null, tint = NovTextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Нет диалогов", color = NovTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Нажмите + чтобы начать новый чат", color = NovTextSecondary, fontSize = 14.sp)
                    }
                }
            }
        } else {
            if (pinned.isNotEmpty()) {
                item {
                    SectionHeader(text = "ЗАКРЕПЛЁННЫЕ", icon = Icons.Default.PushPin, tint = accent)
                }
                items(pinned, key = { it.id }) { conv ->
                    SwipeableConversationItem(
                        conv = conv,
                        onOpen = { onOpen(conv.id) },
                        onPin = { onPin(conv) },
                        onDelete = { onDelete(conv) },
                        onRename = { onRename(conv) }
                    )
                }
                item { HorizontalDivider(color = NovDivider, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) }
            }

            if (unpinned.isNotEmpty()) {
                item {
                    SectionHeader(text = "ДИАЛОГИ", icon = Icons.Default.Chat, tint = NovTextSecondary)
                }
                items(unpinned, key = { it.id }) { conv ->
                    SwipeableConversationItem(
                        conv = conv,
                        onOpen = { onOpen(conv.id) },
                        onPin = { onPin(conv) },
                        onDelete = { onDelete(conv) },
                        onRename = { onRename(conv) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        Text(text, color = NovTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Swipeable conversation item ────────────────────────────────────────────────
// Swipe RIGHT  → pin / unpin
// Swipe LEFT   → delete (shows red background)

private const val SWIPE_THRESHOLD = 120f

@Composable
private fun SwipeableConversationItem(
    conv: Conversation,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    val accent = LocalAccentColor.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var showMenu by remember { mutableStateOf(false) }

    val pinColor   = if (conv.isPinned) NovTextSecondary else accent
    val pinIcon    = if (conv.isPinned) PushPin else Icons.Default.PushPin
    val pinLabel   = if (conv.isPinned) "Открепить" else "Закрепить"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        // Background layers
        val o = offsetX.value
        if (o > 0f) {
            // Pin action background (right swipe)
            val alpha = (o / SWIPE_THRESHOLD).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha * 0.25f)),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.padding(start = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(pinIcon, pinLabel, tint = pinColor.copy(alpha), modifier = Modifier.size(22.dp))
                    if (alpha > 0.5f) {
                        Text(pinLabel, color = pinColor.copy(alpha), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else if (o < 0f) {
            // Delete action background (left swipe)
            val alpha = (-o / SWIPE_THRESHOLD).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ErrorRed.copy(alpha * 0.25f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier.padding(end = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (alpha > 0.5f) {
                        Text("Удалить", color = ErrorRed.copy(alpha), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Icon(Icons.Default.Delete, "Удалить", tint = ErrorRed.copy(alpha), modifier = Modifier.size(22.dp))
                }
            }
        }

        // Foreground — conversation card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(conv.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offsetX.value >= SWIPE_THRESHOLD -> {
                                        // Triggered: pin/unpin
                                        onPin()
                                        offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
                                    }
                                    offsetX.value <= -SWIPE_THRESHOLD -> {
                                        // Triggered: delete
                                        onDelete()
                                        offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
                                    }
                                    else -> {
                                        offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy)) }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val target = (offsetX.value + dragAmount).coerceIn(-SWIPE_THRESHOLD * 1.3f, SWIPE_THRESHOLD * 1.3f)
                                offsetX.snapTo(target)
                            }
                        }
                    )
                }
        ) {
            ConversationItem(
                conv = conv,
                accent = accent,
                onClick = onOpen,
                onLongClick = { showMenu = true }
            )

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = NovCard
            ) {
                DropdownMenuItem(
                    text = { Text(pinLabel, color = NovTextPrimary, fontSize = 14.sp) },
                    leadingIcon = { Icon(pinIcon, null, tint = accent, modifier = Modifier.size(18.dp)) },
                    onClick = { onPin(); showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Переименовать", color = NovTextPrimary, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = accent, modifier = Modifier.size(18.dp)) },
                    onClick = { onRename(); showMenu = false }
                )
                HorizontalDivider(color = NovDivider)
                DropdownMenuItem(
                    text = { Text("Удалить", color = ErrorRed, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = ErrorRed, modifier = Modifier.size(18.dp)) },
                    onClick = { onDelete(); showMenu = false }
                )
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conv: Conversation,
    accent: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val date = remember(conv.updatedAt) {
        val cal = Calendar.getInstance()
        val ts = conv.updatedAt.toDate()
        cal.time = ts
        val now = Calendar.getInstance()
        when {
            cal.get(Calendar.DATE) == now.get(Calendar.DATE) ->
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(ts)
            cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR) ->
                SimpleDateFormat("EEE", Locale("ru")).format(ts)
            else ->
                SimpleDateFormat("dd MMM", Locale("ru")).format(ts)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = NovSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (conv.isPinned) Icons.Default.PushPin else Icons.Default.Chat,
                    null,
                    tint = if (conv.isPinned) accent else NovTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    conv.title,
                    color = NovTextPrimary,
                    fontWeight = if (conv.isPinned) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (conv.preview.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        conv.preview,
                        color = NovTextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(date, color = NovTextSecondary, fontSize = 12.sp)
        }
    }
}

// ── Search results ─────────────────────────────────────────────────────────────

@Composable
private fun SearchResultsList(
    searchState: com.novaproject.novai.viewmodel.SearchUiState,
    onOpenChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccentColor.current
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        if (searchState.isSearching) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            }
        } else if (searchState.messageResults.isEmpty()) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, tint = NovTextSecondary, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Ничего не найдено", color = NovTextSecondary, fontSize = 14.sp)
                    }
                }
            }
        } else {
            item {
                Text(
                    "Найдено: ${searchState.messageResults.size}",
                    color = accent, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
            items(searchState.messageResults, key = { it.message.id }) { result ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clickable { onOpenChat(result.convId) },
                    colors = CardDefaults.cardColors(containerColor = NovCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(result.convTitle, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "[${if (result.message.role == "user") "Я" else "NovAI"}]: ${result.message.content}",
                            color = NovTextPrimary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ── Rename dialog ──────────────────────────────────────────────────────────────

@Composable
private fun RenameDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val accent = LocalAccentColor.current
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NovCard,
        title = { Text("Переименовать", color = NovTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent, unfocusedBorderColor = NovDivider,
                    focusedContainerColor = NovDark, unfocusedContainerColor = NovDark,
                    cursorColor = accent, focusedTextColor = NovTextPrimary, unfocusedTextColor = NovTextPrimary
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) { Text("Сохранить", color = NovDark, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = NovTextSecondary) } }
    )
}

// ── Auth screen ────────────────────────────────────────────────────────────────

@Composable
private fun AuthScreen(viewModel: AuthViewModel, onAuthenticated: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var showForgot by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }

    LaunchedEffect(state.user) {
        if (state.user != null) onAuthenticated()
    }

    if (showForgot) {
        AlertDialog(
            onDismissRequest = { showForgot = false },
            containerColor = NovCard,
            title = { Text("Восстановление пароля", color = NovTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = forgotEmail,
                    onValueChange = { forgotEmail = it },
                    label = { Text("Email", color = NovTextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovCyan, unfocusedBorderColor = NovDivider,
                        focusedContainerColor = NovDark, unfocusedContainerColor = NovDark,
                        cursorColor = NovCyan, focusedTextColor = NovTextPrimary, unfocusedTextColor = NovTextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetPassword(forgotEmail); showForgot = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NovCyan)
                ) { Text("Отправить", color = NovDark, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showForgot = false }) { Text("Отмена", color = NovTextSecondary) } }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(NovDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("NovAI", color = NovTextPrimary, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text("Умный ИИ-ассистент", color = NovTextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(36.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = NovCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TabButton("Войти", isLogin) { isLogin = true; viewModel.clearError() }
                        Spacer(Modifier.width(8.dp))
                        TabButton("Регистрация", !isLogin) { isLogin = false; viewModel.clearError() }
                    }

                    if (!isLogin) {
                        AuthField(value = name, onValue = { name = it }, label = "Никнейм")
                    }
                    AuthField(value = email, onValue = { email = it }, label = "Email", keyboardType = KeyboardType.Email)
                    AuthField(
                        value = password, onValue = { password = it }, label = "Пароль",
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = NovTextSecondary)
                            }
                        }
                    )
                    if (!isLogin) {
                        AuthField(
                            value = confirmPassword, onValue = { confirmPassword = it }, label = "Повтори пароль",
                            visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showConfirm = !showConfirm }) {
                                    Icon(if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = NovTextSecondary)
                                }
                            }
                        )
                    }

                    state.error?.let {
                        Text(it, color = ErrorRed, fontSize = 13.sp)
                    }
                    state.successMessage?.let {
                        Text(it, color = SuccessGreen, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (isLogin) viewModel.signIn(email, password)
                            else viewModel.register(email, password, name, confirmPassword)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = NovCyan, disabledContainerColor = NovCyan.copy(0.4f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = NovDark, strokeWidth = 2.dp)
                        } else {
                            Text(if (isLogin) "Войти" else "Создать аккаунт", color = NovDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    if (isLogin) {
                        TextButton(
                            onClick = { showForgot = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Забыли пароль?", color = NovTextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
            .background(if (selected) NovCyan.copy(0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) NovCyan else NovTextSecondary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
    }
}

@Composable
private fun AuthField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label, color = NovTextSecondary) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NovCyan, unfocusedBorderColor = NovDivider,
            focusedContainerColor = NovDark, unfocusedContainerColor = NovDark,
            cursorColor = NovCyan, focusedTextColor = NovTextPrimary, unfocusedTextColor = NovTextPrimary
        )
    )
}
