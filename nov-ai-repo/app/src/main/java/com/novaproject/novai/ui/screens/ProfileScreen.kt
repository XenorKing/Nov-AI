package com.novaproject.novai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.novaproject.novai.ui.theme.*
import com.novaproject.novai.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onPrivacyPolicy: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    var showNickDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }
    var showPromoDialog by remember { mutableStateOf(false) }
    var promoInput by remember { mutableStateOf("") }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) { delay(3000); viewModel.clearSuccess() }
    }
    LaunchedEffect(state.error) {
        if (state.error != null) { delay(4000); viewModel.clearError() }
    }

    if (showNickDialog) {
        ChangeNickDialog(
            currentName = state.user?.displayName ?: "",
            onDismiss = { showNickDialog = false },
            onConfirm = { newName -> viewModel.updateDisplayName(newName); showNickDialog = false }
        )
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { current, new, confirm -> viewModel.updatePassword(current, new, confirm); showPasswordDialog = false }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = NovCard,
            title = { Text("Выйти из аккаунта?", color = NovTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Ваши диалоги сохранены в облаке.", color = NovTextSecondary) },
            confirmButton = {
                Button(onClick = { showSignOutDialog = false; viewModel.signOut(); onSignOut() }, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) {
                    Text("Выйти")
                }
            },
            dismissButton = { TextButton(onClick = { showSignOutDialog = false }) { Text("Отмена", color = NovTextSecondary) } }
        )
    }

    // Delete account dialog with confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; deleteConfirmText = "" },
            containerColor = NovCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Удалить аккаунт?", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Это действие необратимо. Все ваши диалоги, сообщения и настройки будут удалены навсегда.",
                        color = NovTextSecondary, fontSize = 14.sp, lineHeight = 20.sp
                    )
                    Text("Введите УДАЛИТЬ для подтверждения:", color = NovTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        singleLine = true,
                        placeholder = { Text("УДАЛИТЬ", color = NovTextSecondary.copy(0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ErrorRed, unfocusedBorderColor = NovDivider,
                            focusedContainerColor = NovDark, unfocusedContainerColor = NovDark,
                            cursorColor = ErrorRed, focusedTextColor = NovTextPrimary, unfocusedTextColor = NovTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        deleteConfirmText = ""
                        viewModel.signOut()
                        onDeleteAccount()
                    },
                    enabled = deleteConfirmText == "УДАЛИТЬ",
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, disabledContainerColor = ErrorRed.copy(0.3f))
                ) { Text("Удалить навсегда", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; deleteConfirmText = "" }) {
                    Text("Отмена", color = NovTextSecondary)
                }
            }
        )
    }

    if (showPromoDialog) {
        AlertDialog(
            onDismissRequest = { showPromoDialog = false; promoInput = "" },
            containerColor = NovCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CardGiftcard, null, tint = NovCyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Активировать промокод", color = NovTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Введите промокод, чтобы получить Premium доступ.", color = NovTextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                    OutlinedTextField(
                        value = promoInput,
                        onValueChange = { promoInput = it.uppercase() },
                        placeholder = { Text("NOVAI-XXXXX", color = NovTextSecondary.copy(0.5f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NovCyan, unfocusedBorderColor = NovDivider,
                            focusedContainerColor = NovDark, unfocusedContainerColor = NovDark,
                            cursorColor = NovCyan, focusedTextColor = NovTextPrimary, unfocusedTextColor = NovTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.redeemPromoCode(promoInput); showPromoDialog = false; promoInput = "" },
                    enabled = promoInput.isNotBlank() && !state.promoLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = NovCyan, disabledContainerColor = NovCyan.copy(0.3f))
                ) {
                    if (state.promoLoading) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = NovDark, strokeWidth = 2.dp)
                    } else {
                        Text("Активировать", color = NovDark, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromoDialog = false; promoInput = "" }) {
                    Text("Отмена", color = NovTextSecondary)
                }
            }
        )
    }

    Scaffold(
        containerColor = NovDark,
        topBar = {
            TopAppBar(
                title = { Text("Профиль", color = NovTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = NovTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NovSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar section
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NovCyan, NovPurple))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (state.user?.displayName?.firstOrNull()?.uppercaseChar() ?: 'U').toString(),
                            color = NovDark, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(state.user?.displayName ?: "Пользователь", color = NovTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(state.user?.email ?: "", color = NovTextSecondary, fontSize = 14.sp)
                }
            }

            AnimatedVisibility(visible = state.successMessage != null) {
                Card(colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(0.15f)), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(state.successMessage ?: "", color = SuccessGreen, fontSize = 14.sp)
                    }
                }
            }

            AnimatedVisibility(visible = state.error != null) {
                Card(colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(0.15f)), shape = RoundedCornerShape(12.dp)) {
                    Text(state.error ?: "", color = ErrorRed, modifier = Modifier.padding(14.dp), fontSize = 14.sp)
                }
            }

            ProfileSection(title = "АККАУНТ") {
                ProfileItem(icon = Icons.Default.Edit, iconTint = NovCyan, title = "Сменить никнейм", subtitle = state.user?.displayName ?: "", onClick = { showNickDialog = true })
                HorizontalDivider(color = NovDivider, modifier = Modifier.padding(horizontal = 16.dp))
                ProfileItem(icon = Icons.Default.Lock, iconTint = NovPurple, title = "Сменить пароль", subtitle = "••••••••", onClick = { showPasswordDialog = true })
            }

            // Premium section
            val isPremium = state.premiumExpiresAt > System.currentTimeMillis()
            val premiumLabel = if (isPremium) {
                val days = ((state.premiumExpiresAt - System.currentTimeMillis()) / 86_400_000L).toInt()
                val hours = ((state.premiumExpiresAt - System.currentTimeMillis()) / 3_600_000L).toInt()
                if (days > 0) "Активен ещё $days дн." else "Активен ещё $hours ч."
            } else "Нет подписки"

            ProfileSection(title = "PREMIUM") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isPremium) NovCyan.copy(0.08f) else NovCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                    .background(if (isPremium) NovCyan.copy(0.2f) else NovDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isPremium) Icons.Default.Star else Icons.Default.StarBorder,
                                    null,
                                    tint = if (isPremium) NovCyan else NovTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Premium", color = if (isPremium) NovCyan else NovTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(premiumLabel, color = if (isPremium) NovCyan.copy(0.8f) else NovTextSecondary, fontSize = 12.sp)
                            }
                        }
                        TextButton(
                            onClick = { showPromoDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = NovCyan)
                        ) {
                            Icon(Icons.Default.CardGiftcard, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Промокод", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            ProfileSection(title = "О ПРИЛОЖЕНИИ") {
                ProfileInfoItem(label = "Версия", value = "1.0.0", icon = Icons.Default.Label, iconTint = NovPurple)
                HorizontalDivider(color = NovDivider, modifier = Modifier.padding(horizontal = 16.dp))
                ProfileInfoItem(label = "Компания", value = "Nova Project", icon = Icons.Default.Apps, iconTint = NovCyan)
                HorizontalDivider(color = NovDivider, modifier = Modifier.padding(horizontal = 16.dp))
                ProfileInfoItem(label = "Разработчик", value = "Xenor", icon = Icons.Default.Code, iconTint = NovCyan)
            }

            ProfileSection(title = "ОБРАТНАЯ СВЯЗЬ") {
                ProfileItem(
                    icon = Icons.Default.Email,
                    iconTint = Color(0xFFFF5722),
                    title = "Email", subtitle = "novaprojecthelp@mail.ru", showArrow = true,
                    onClick = { try { uriHandler.openUri("mailto:novaprojecthelp@mail.ru") } catch (_: Exception) {} }
                )
                HorizontalDivider(color = NovDivider, modifier = Modifier.padding(horizontal = 16.dp))
                ProfileItem(
                    icon = Icons.Default.Send,
                    iconTint = Color(0xFF29B6F6),
                    title = "Telegram", subtitle = "@NovaProjectNews", showArrow = true,
                    onClick = { try { uriHandler.openUri("https://t.me/NovaProjectNews") } catch (_: Exception) {} }
                )
                HorizontalDivider(color = NovDivider, modifier = Modifier.padding(horizontal = 16.dp))
                ProfileItem(
                    icon = Icons.Default.Language,
                    iconTint = Color(0xFF2979FF),
                    title = "ВКонтакте", subtitle = "Nova Project VK", showArrow = true,
                    onClick = { try { uriHandler.openUri("https://vk.ru/club238958808") } catch (_: Exception) {} }
                )
            }

            ProfileSection(title = "ИНФОРМАЦИЯ") {
                ProfileItem(icon = Icons.Default.Shield, iconTint = NovCyan, title = "Политика конфиденциальности", showArrow = false, onClick = onPrivacyPolicy)
            }

            // Sign out
            Card(
                colors = CardDefaults.cardColors(containerColor = NovCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showSignOutDialog = true }.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(ErrorRed.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Logout, null, tint = ErrorRed, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Text("Выйти из аккаунта", color = ErrorRed, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Delete account — dangerous action
            Card(
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(0.06f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showDeleteDialog = true }.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(ErrorRed.copy(0.2f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.DeleteForever, null, tint = ErrorRed, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Удалить аккаунт", color = ErrorRed, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Удалит все данные навсегда", color = ErrorRed.copy(0.6f), fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, color = NovTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = NovCard), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column { content() }
        }
    }
}

@Composable
private fun ProfileInfoItem(label: String, value: String, icon: ImageVector, iconTint: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(iconTint.copy(0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = NovTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(value, color = NovTextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun ProfileItem(icon: ImageVector, iconTint: Color, title: String, subtitle: String = "", showArrow: Boolean = false, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(iconTint.copy(0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = NovTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle.isNotBlank()) Text(subtitle, color = NovTextSecondary, fontSize = 13.sp, maxLines = 1)
        }
        Icon(if (showArrow) Icons.Default.OpenInNew else Icons.Default.ChevronRight, null, tint = NovTextSecondary.copy(0.5f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ChangeNickDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = NovCard,
        title = { Text("Сменить никнейм", color = NovTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Новый никнейм", color = NovTextSecondary) }, singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NovCyan, unfocusedBorderColor = NovDivider, focusedContainerColor = NovCardAlt, unfocusedContainerColor = NovCardAlt, cursorColor = NovCyan, focusedTextColor = NovTextPrimary, unfocusedTextColor = NovTextPrimary)
            )
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onConfirm(name) }, colors = ButtonDefaults.buttonColors(containerColor = NovCyan)) { Text("Сохранить", color = NovDark, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = NovTextSecondary) } }
    )
}

@Composable
private fun ChangePasswordDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = NovCard,
        title = { Text("Сменить пароль", color = NovTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PasswordField(current, { current = it }, "Текущий пароль", showCurrent, { showCurrent = !showCurrent })
                PasswordField(new, { new = it }, "Новый пароль", showNew, { showNew = !showNew })
                PasswordField(confirm, { confirm = it }, "Подтвердите пароль", showConfirm, { showConfirm = !showConfirm }, confirm.isNotBlank() && new != confirm)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(current, new, confirm) }, colors = ButtonDefaults.buttonColors(containerColor = NovCyan)) { Text("Изменить", color = NovDark, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = NovTextSecondary) } }
    )
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String, visible: Boolean, onToggle: () -> Unit, isError: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label, color = NovTextSecondary) }, singleLine = true,
        isError = isError,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggle) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = NovTextSecondary) }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NovCyan, unfocusedBorderColor = NovDivider, errorBorderColor = ErrorRed, focusedContainerColor = NovCardAlt, unfocusedContainerColor = NovCardAlt, cursorColor = NovCyan, focusedTextColor = NovTextPrimary, unfocusedTextColor = NovTextPrimary)
    )
}
