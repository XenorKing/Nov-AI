package com.novaproject.novai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.novaproject.novai.R
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import com.novaproject.novai.data.model.FREE_MODEL_LIST
import com.novaproject.novai.data.model.friendlyModelName
import com.novaproject.novai.ui.theme.*
import com.novaproject.novai.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

private val ACCENT_COLORS = listOf(
    Triple("cyan", "Бирюза", NovCyan),
    Triple("purple", "Фиолет", NovPurple),
    Triple("orange", "Оранж", NovOrange)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPrivacyPolicy: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val settings = state.settings
    var tokenVisible by remember { mutableStateOf(false) }
    var newModelInput by remember { mutableStateOf("") }
    val accent = LocalAccentColor.current
    val context = LocalContext.current
      var notifGranted by remember {
          mutableStateOf(
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                  ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
              else true
          )
      }
      
    // Warning when any paid custom model is added but token is missing
    val hasPaidModel = settings.customModels.any { !it.endsWith(":free") }
    val needsTokenWarning = hasPaidModel && settings.openRouterToken.isBlank()

    // Build model list for per-model prompt editor: free presets + all custom models
    val allModelsForPrompt = remember(settings.customModels) {
        val custom = settings.customModels
            .filter { id -> FREE_MODEL_LIST.none { it.modelId == id } }
            .map { id -> com.novaproject.novai.data.model.FreeModel(
                label = friendlyModelName(id),
                modelId = id,
                description = "Своя модель"
            )}
        FREE_MODEL_LIST + custom
    }

    // Which model's prompt we're currently editing in settings
    val promptModelId = state.promptEditModelId
    val currentPromptText = settings.modelPrompts[promptModelId] ?: ""

    LaunchedEffect(state.error) {
        if (state.error != null) { kotlinx.coroutines.delay(4000); viewModel.clearError() }
    }

    Scaffold(
        containerColor = NovDark,
        topBar = {
            TopAppBar(
                title = { Text("Настройки ИИ-ассистента", color = NovTextPrimary, fontWeight = FontWeight.Bold) },
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

            AnimatedVisibility(visible = state.error != null) {
                Card(colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(0.15f)), shape = RoundedCornerShape(12.dp)) {
                    Text(state.error ?: "", color = ErrorRed, modifier = Modifier.padding(14.dp), fontSize = 14.sp)
                }
            }

            // ── Section: AI character ────────────────────────────────────────
            Text("ПЕРСОНАЖ ИИ", color = NovTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))

            Card(colors = CardDefaults.cardColors(containerColor = NovCard), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // AI name
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Имя ИИ", color = NovTextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        OutlinedTextField(
                            value = settings.aiName,
                            onValueChange = { viewModel.updateAiName(it) },
                            placeholder = { Text("NovAI", color = NovTextSecondary, fontSize = 13.sp) },
                            singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            colors = outlinedFieldColors(accent),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                        )
                    }

                    HorizontalDivider(color = NovDivider)

                    // Accent color
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, null, tint = accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Акцент-цвет", color = NovTextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ACCENT_COLORS.forEach { (key, label, color) ->
                                val isSelected = settings.accentColor == key
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(color)
                                            .border(if (isSelected) 3.dp else 0.dp, Color.White, CircleShape)
                                            .clickable { viewModel.updateAccentColor(key) }
                                    )
                                    Text(label, color = if (isSelected) NovTextPrimary else NovTextSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                        Text("Изменения применяются сразу после сохранения", color = NovTextSecondary, fontSize = 10.sp)
                    }
                }
            }

            // ── Section: custom model / OpenRouter ───────────────────────────
            Text("СВОЯ МОДЕЛЬ / OPENROUTER", color = NovTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))

            Card(colors = CardDefaults.cardColors(containerColor = NovCard), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(accent.copy(0.12f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SmartToy, null, tint = accent, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Свои модели OpenRouter", color = NovTextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("Добавленные модели появятся в чате в списке выбора", color = NovTextSecondary, fontSize = 10.sp)
                        }
                    }

                    // List of added custom models
                    if (settings.customModels.isEmpty()) {
                        Text("Нет добавленных моделей. Добавьте ниже.", color = NovTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            settings.customModels.forEach { modelId ->
                                val isFree = modelId.endsWith(":free")
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NovDark)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                                        .background(if (isFree) SuccessGreen else Color(0xFFFFB74D)))
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(friendlyModelName(modelId), color = NovTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text(modelId, color = NovTextSecondary, fontSize = 10.sp, maxLines = 1)
                                    }
                                    Text(
                                        if (isFree) "free" else "paid",
                                        color = if (isFree) SuccessGreen else Color(0xFFFFB74D),
                                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    IconButton(onClick = { viewModel.removeCustomModel(modelId) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, "Удалить", tint = NovTextSecondary, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Input to add a new model
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newModelInput,
                            onValueChange = { newModelInput = it },
                            placeholder = { Text("meta-llama/llama-3.1-8b-instruct:free", color = NovTextSecondary, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedFieldColors(accent),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            label = { Text("ID модели", color = NovTextSecondary, fontSize = 11.sp) }
                        )
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (newModelInput.isNotBlank()) accent else NovDivider)
                                .clickable {
                                    if (newModelInput.isNotBlank()) {
                                        viewModel.addCustomModel(newModelInput)
                                        newModelInput = ""
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, "Добавить", tint = if (newModelInput.isNotBlank()) NovDark else NovTextSecondary, modifier = Modifier.size(22.dp))
                        }
                    }

                    // Warning: paid model without token
                    AnimatedVisibility(visible = needsTokenWarning) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF4A2800)), shape = RoundedCornerShape(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Платная модель — нужен токен", color = Color(0xFFFFB74D), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(
                                        "«${settings.customModel}» — не бесплатная. Укажите OpenRouter токен, иначе запросы не пройдут. Бесплатные модели оканчиваются на :free",
                                        color = Color(0xFFFFB74D).copy(0.85f), fontSize = 11.sp, lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = NovDivider)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(NovPurple.copy(0.12f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Key, null, tint = NovPurple, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Токен OpenRouter", color = NovTextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("openrouter.ai → Keys → Create Key", color = NovTextSecondary, fontSize = 10.sp)
                        }
                    }
                    OutlinedTextField(
                        value = settings.openRouterToken,
                        onValueChange = { viewModel.updateOpenRouterToken(it) },
                        placeholder = { Text("sk-or-...", color = NovTextSecondary, fontSize = 12.sp) },
                        singleLine = true,
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = NovTextSecondary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors(NovPurple),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                    )

                    if (settings.openRouterToken.isNotBlank()) {
                        Text("✓ Запросы идут напрямую через OpenRouter + стриминг ответов", color = SuccessGreen, fontSize = 11.sp)
                    } else {
                        Text("Без токена используется стандартный NovAI прокси", color = NovTextSecondary, fontSize = 11.sp)
                    }

                    // Inline save button when paid model + token are filled
                    AnimatedVisibility(visible = hasPaidModel && settings.openRouterToken.isNotBlank()) {
                        Button(
                            onClick = { viewModel.save() },
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent, disabledContainerColor = accent.copy(0.5f))
                        ) {
                            when {
                                state.isSaving -> CircularProgressIndicator(Modifier.size(18.dp), color = NovDark, strokeWidth = 2.dp)
                                state.saved -> Text("Сохранено ✓", color = NovDark, fontWeight = FontWeight.Bold)
                                else -> Text("Сохранить", color = NovDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── Section: per-model system prompts ────────────────────────────
            Text("ПРОМТ ДЛЯ КАЖДОЙ МОДЕЛИ", color = NovTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))

            Card(colors = CardDefaults.cardColors(containerColor = NovCard), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "Выберите модель и задайте для неё свой системный промт. Пустое поле — использовать промт NovAI по умолчанию.",
                        color = NovTextSecondary, fontSize = 12.sp, lineHeight = 18.sp
                    )

                    // Model selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allModelsForPrompt.forEach { model ->
                            val isSelected = promptModelId == model.modelId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) accent.copy(0.2f) else NovDark)
                                    .border(1.dp, if (isSelected) accent else NovDivider, RoundedCornerShape(20.dp))
                                    .clickable { viewModel.setPromptEditModel(model.modelId) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (settings.modelPrompts[model.modelId]?.isNotBlank() == true) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(accent))
                                    }
                                    Text(
                                        model.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) accent else NovTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Prompt text field for selected model
                    val selectedModelLabel = allModelsForPrompt.firstOrNull { it.modelId == promptModelId }?.label ?: "NovAI"
                    OutlinedTextField(
                        value = currentPromptText,
                        onValueChange = { viewModel.updateModelPrompt(promptModelId, it) },
                        placeholder = { Text("Системный промт для $selectedModelLabel…", color = NovTextSecondary, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8,
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors(accent),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                    )

                    if (currentPromptText.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${currentPromptText.length} символов", color = NovTextSecondary, fontSize = 10.sp)
                            TextButton(onClick = { viewModel.updateModelPrompt(promptModelId, "") }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("Очистить", color = ErrorRed, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // ── Section: generation params ────────────────────────────────────
            Text("ПАРАМЕТРЫ ГЕНЕРАЦИИ", color = NovTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))

            Card(colors = CardDefaults.cardColors(containerColor = NovCard), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    SliderParam("Температура", "Точность vs Творчество", settings.temperature, "%.1f".format(settings.temperature), 0f, 2f, 19, "Точнее", "Творчески", accent) { viewModel.updateTemperature(it) }
                    HorizontalDivider(color = NovDivider)
                    SliderParam("Макс. токенов", "Длина ответа ИИ", settings.maxTokens.toFloat(), "${settings.maxTokens}", 128f, 4096f, 30, "128", "4096", accent) { viewModel.updateMaxTokens(it.roundToInt()) }
                    HorizontalDivider(color = NovDivider)
                    SliderParam("Top-P", "Ограничение вероятности токенов", settings.topP, "%.2f".format(settings.topP), 0f, 1f, 19, "0.0", "1.0", accent) { viewModel.updateTopP(it) }
                    HorizontalDivider(color = NovDivider)
                    SliderParam("Штраф за повторы", "Снижает повторение одних и тех же слов", settings.frequencyPenalty, "%.1f".format(settings.frequencyPenalty), 0f, 2f, 19, "0.0", "2.0", accent) { viewModel.updateFrequencyPenalty(it) }
                    HorizontalDivider(color = NovDivider)
                    SliderParam("Штраф за присутствие", "Стимулирует говорить о новых темах", settings.presencePenalty, "%.1f".format(settings.presencePenalty), 0f, 2f, 19, "0.0", "2.0", accent) { viewModel.updatePresencePenalty(it) }
                    HorizontalDivider(color = NovDivider)
                    // History toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Хранение истории диалога", color = NovTextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("Экономия токенов модели", color = NovTextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.sendHistory,
                            onCheckedChange = { viewModel.updateSendHistory(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NovDark,
                                checkedTrackColor = accent,
                                uncheckedThumbColor = NovTextSecondary,
                                uncheckedTrackColor = NovDivider
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Save button — always enabled
            Button(
                onClick = { viewModel.save() },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, disabledContainerColor = accent.copy(0.5f))
            ) {
                when {
                    state.isSaving -> CircularProgressIndicator(Modifier.size(20.dp), color = NovDark, strokeWidth = 2.dp)
                    state.saved -> Text("Сохранено ✓", color = NovDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    else -> Text("Сохранить", color = NovDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // ── Notification settings ──────────────────────────────────────────────
              Card(
                  onClick = {
                      val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                          Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                              putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                          }
                      } else {
                          Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                              data = android.net.Uri.parse("package:" + context.packageName)
                          }
                      }
                      context.startActivity(intent)
                      // re-check after returning
                      notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                          ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                      else true
                  },
                  colors = CardDefaults.cardColors(containerColor = NovCard),
                  shape = RoundedCornerShape(14.dp)
              ) {
                  Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                      Box(
                          modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                              .background(if (notifGranted) accent.copy(0.12f) else NovTextSecondary.copy(0.12f)),
                          contentAlignment = Alignment.Center
                      ) {
                          Icon(
                              if (notifGranted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                              null,
                              tint = if (notifGranted) accent else NovTextSecondary,
                              modifier = Modifier.size(18.dp)
                          )
                      }
                      Spacer(Modifier.width(12.dp))
                      Column {
                          Text("Уведомления", color = NovTextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                          Text(
                              if (notifGranted) "Включены — нажмите, чтобы изменить" else "Выключены — нажмите, чтобы разрешить",
                              color = if (notifGranted) NovTextSecondary else accent,
                              fontSize = 11.sp
                          )
                      }
                      Spacer(Modifier.weight(1f))
                      Text("›", color = NovTextSecondary, fontSize = 20.sp)
                  }
              }

                          Card(onClick = onPrivacyPolicy, colors = CardDefaults.cardColors(containerColor = NovCard), shape = RoundedCornerShape(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(0.12f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Policy, null, tint = accent, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Политика конфиденциальности", color = NovTextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Как мы обрабатываем ваши данные", color = NovTextSecondary, fontSize = 11.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("›", color = NovTextSecondary, fontSize = 20.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun outlinedFieldColors(accent: androidx.compose.ui.graphics.Color) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = accent, unfocusedBorderColor = NovDivider,
        focusedContainerColor = NovDark, unfocusedContainerColor = NovDark,
        cursorColor = accent, focusedTextColor = NovTextPrimary, unfocusedTextColor = NovTextPrimary
    )

@Composable
private fun SliderParam(
    label: String, description: String,
    value: Float, displayValue: String,
    min: Float, max: Float, steps: Int,
    minLabel: String, maxLabel: String,
    accent: androidx.compose.ui.graphics.Color = LocalAccentColor.current,
    onChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(label, color = NovTextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(description, color = NovTextSecondary, fontSize = 11.sp)
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(accent.copy(0.15f)).padding(horizontal = 12.dp, vertical = 5.dp)) {
                Text(displayValue, color = accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Slider(value = value, onValueChange = onChange, valueRange = min..max, steps = steps,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = NovDivider))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(minLabel, color = NovTextSecondary, fontSize = 10.sp)
            Text(maxLabel, color = NovTextSecondary, fontSize = 10.sp)
        }
    }
}
