package com.novaproject.novai.ui.screens

  import android.Manifest
  import android.content.pm.PackageManager
  import android.os.Build
  import androidx.activity.compose.rememberLauncherForActivityResult
  import androidx.activity.result.contract.ActivityResultContracts
  import androidx.compose.foundation.Image
  import androidx.compose.foundation.background
  import androidx.compose.foundation.border
  import androidx.compose.foundation.layout.*
  import androidx.compose.foundation.pager.HorizontalPager
  import androidx.compose.foundation.pager.rememberPagerState
  import androidx.compose.foundation.rememberScrollState
  import androidx.compose.foundation.shape.CircleShape
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.foundation.verticalScroll
  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.filled.*
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.draw.clip
  import androidx.compose.ui.graphics.vector.ImageVector
  import androidx.compose.ui.layout.ContentScale
  import androidx.compose.ui.platform.LocalContext
  import androidx.compose.ui.res.painterResource
  import androidx.compose.ui.text.font.FontWeight
  import androidx.compose.ui.text.style.TextAlign
  import androidx.compose.ui.unit.dp
  import androidx.compose.ui.unit.sp
  import androidx.core.content.ContextCompat
  import com.novaproject.novai.R
  import com.novaproject.novai.ui.theme.*
  import kotlinx.coroutines.launch
  import kotlin.math.roundToInt

  @Composable
  fun OnboardingScreen(onComplete: () -> Unit) {
      val pagerState = rememberPagerState(pageCount = { 3 })
      val scope = rememberCoroutineScope()
      val context = LocalContext.current

      var temperature by remember { mutableStateOf(0.7f) }
      var maxTokens by remember { mutableStateOf(1024f) }
      var topP by remember { mutableStateOf(1.0f) }
      var frequencyPenalty by remember { mutableStateOf(0f) }
      var presencePenalty by remember { mutableStateOf(0f) }
      var sendHistory by remember { mutableStateOf(true) }

      var notifGranted by remember {
          mutableStateOf(
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                  ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
              else true
          )
      }

      val notifPermissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
      ) { granted -> notifGranted = granted }

      Box(modifier = Modifier.fillMaxSize().background(NovDark)) {
          HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
              when (page) {
                  0 -> OnboardingFeaturesPage()
                  1 -> OnboardingSettingsPage(
                      temperature = temperature,
                      maxTokens = maxTokens,
                      topP = topP,
                      frequencyPenalty = frequencyPenalty,
                      presencePenalty = presencePenalty,
                      sendHistory = sendHistory,
                      onTemperature = { temperature = it },
                      onMaxTokens = { maxTokens = it },
                      onTopP = { topP = it },
                      onFrequencyPenalty = { frequencyPenalty = it },
                      onPresencePenalty = { presencePenalty = it },
                      onSendHistory = { sendHistory = it }
                  )
                  2 -> OnboardingNotificationPage(
                      isGranted = notifGranted,
                      onRequestPermission = {
                          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                              notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                          }
                      }
                  )
              }
          }

          Column(
              modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 52.dp),
              horizontalAlignment = Alignment.CenterHorizontally
          ) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                  repeat(3) { index ->
                      val selected = pagerState.currentPage == index
                      Box(
                          modifier = Modifier
                              .clip(RoundedCornerShape(50))
                              .background(if (selected) NovCyan else NovTextSecondary.copy(0.3f))
                              .then(if (selected) Modifier.size(24.dp, 8.dp) else Modifier.size(8.dp))
                      )
                  }
              }

              Spacer(Modifier.height(28.dp))

              FloatingActionButton(
                  onClick = {
                      if (pagerState.currentPage < 2) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                      else onComplete()
                  },
                  containerColor = NovCyan, contentColor = NovDark, shape = CircleShape, modifier = Modifier.size(60.dp)
              ) {
                  Icon(
                      imageVector = if (pagerState.currentPage < 2) Icons.Default.ArrowForward else Icons.Default.Check,
                      contentDescription = null, modifier = Modifier.size(26.dp)
                  )
              }
          }
      }
  }

  @Composable
  private fun OnboardingFeaturesPage() {
      Column(
          modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 32.dp)
              .padding(top = 80.dp, bottom = 180.dp),
          horizontalAlignment = Alignment.CenterHorizontally
      ) {
          // App icon with cyan border
          Box(
              modifier = Modifier
                  .size(84.dp)
                  .clip(RoundedCornerShape(22.dp))
                  .border(2.dp, NovCyan, RoundedCornerShape(22.dp)),
              contentAlignment = Alignment.Center
          ) {
              Image(
                  painter = painterResource(R.drawable.novai_logo),
                  contentDescription = "NovAI",
                  modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                  contentScale = ContentScale.Crop
              )
          }

          Spacer(Modifier.height(18.dp))

          Text("NOVA PROJECT", color = NovTextSecondary.copy(0.65f), fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 3.sp)
          Spacer(Modifier.height(6.dp))
          Text("NovAI", color = NovTextPrimary, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
          Spacer(Modifier.height(4.dp))
          Text("by Xenor", color = NovTextSecondary.copy(0.6f), fontSize = 13.sp)
          Spacer(Modifier.height(40.dp))

          val features = listOf(
              Triple(Icons.Default.SmartToy, "ИИ-ассистент", "На базе передовых языковых моделей"),
              Triple(Icons.Default.Chat, "Умные диалоги", "История чатов хранится в облаке"),
              Triple(Icons.Default.Tune, "Гибкие настройки", "Температура, стиль и поведение ИИ"),
              Triple(Icons.Default.Cloud, "Синхронизация", "История чатов и настройки в облаке"),
          )

          features.forEach { (icon, title, subtitle) ->
              OnboardingFeatureRow(icon, title, subtitle)
              Spacer(Modifier.height(14.dp))
          }
      }
  }

  @Composable
  private fun OnboardingFeatureRow(icon: ImageVector, title: String, subtitle: String) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
          Box(
              modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(NovCyan.copy(0.12f)),
              contentAlignment = Alignment.Center
          ) { Icon(icon, contentDescription = null, tint = NovCyan, modifier = Modifier.size(24.dp)) }
          Spacer(Modifier.width(16.dp))
          Column {
              Text(title, color = NovTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
              Text(subtitle, color = NovTextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
          }
      }
  }

  @Composable
  private fun OnboardingSettingsPage(
      temperature: Float, maxTokens: Float, topP: Float,
      frequencyPenalty: Float, presencePenalty: Float, sendHistory: Boolean,
      onTemperature: (Float) -> Unit, onMaxTokens: (Float) -> Unit,
      onTopP: (Float) -> Unit, onFrequencyPenalty: (Float) -> Unit,
      onPresencePenalty: (Float) -> Unit, onSendHistory: (Boolean) -> Unit
  ) {
      Column(
          modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 28.dp)
              .padding(top = 72.dp, bottom = 180.dp),
          horizontalAlignment = Alignment.CenterHorizontally
      ) {
          Box(
              modifier = Modifier.size(72.dp).clip(CircleShape).background(NovPurple.copy(0.15f)),
              contentAlignment = Alignment.Center
          ) { Icon(Icons.Default.Tune, contentDescription = null, tint = NovPurple, modifier = Modifier.size(36.dp)) }

          Spacer(Modifier.height(20.dp))

          Text("Настройте ИИ", color = NovTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
          Spacer(Modifier.height(8.dp))
          Text(
              "Задайте параметры по вкусу — изменить можно в любой момент в настройках",
              color = NovTextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
          )

          Spacer(Modifier.height(32.dp))

          OnboardingSlider("Температура", "Креативность и непредсказуемость ответов", temperature, String.format("%.1f", temperature), 0f..2f, onTemperature)
          Spacer(Modifier.height(22.dp))
          OnboardingSlider("Макс. токенов", "Максимальная длина одного ответа", maxTokens, maxTokens.roundToInt().toString(), 256f..4096f, onMaxTokens)
          Spacer(Modifier.height(22.dp))
          OnboardingSlider("Top-P", "Разнообразие используемого словаря", topP, String.format("%.2f", topP), 0f..1f, onTopP)
          Spacer(Modifier.height(22.dp))
          OnboardingSlider("Frequency Penalty", "Штраф за повтор одних слов", frequencyPenalty, String.format("%.1f", frequencyPenalty), 0f..2f, onFrequencyPenalty)
          Spacer(Modifier.height(22.dp))
          OnboardingSlider("Presence Penalty", "Поощрение новых тем в разговоре", presencePenalty, String.format("%.1f", presencePenalty), 0f..2f, onPresencePenalty)

          Spacer(Modifier.height(22.dp))

          // ── История диалога toggle ───────────────────────────────────────────
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
          ) {
              Column(modifier = Modifier.weight(1f)) {
                  Text("История диалога", color = NovTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                  Text("Отправлять контекст чата вместе с запросом", color = NovTextSecondary, fontSize = 11.sp)
              }
              Switch(
                  checked = sendHistory,
                  onCheckedChange = onSendHistory,
                  colors = SwitchDefaults.colors(
                      checkedThumbColor = NovDark,
                      checkedTrackColor = NovCyan,
                      uncheckedThumbColor = NovTextSecondary,
                      uncheckedTrackColor = NovTextSecondary.copy(0.3f)
                  )
              )
          }

          Spacer(Modifier.height(20.dp))

          Text(
              "Можно пропустить — настроить позже в разделе Настройки",
              color = NovTextSecondary.copy(0.45f), fontSize = 12.sp, textAlign = TextAlign.Center
          )
      }
  }

  @Composable
  private fun OnboardingNotificationPage(isGranted: Boolean, onRequestPermission: () -> Unit) {
      Column(
          modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 32.dp)
              .padding(top = 100.dp, bottom = 180.dp),
          horizontalAlignment = Alignment.CenterHorizontally
      ) {
          Box(
              modifier = Modifier
                  .size(90.dp)
                  .clip(CircleShape)
                  .background(NovCyan.copy(0.13f))
                  .border(2.dp, NovCyan, CircleShape),
              contentAlignment = Alignment.Center
          ) {
              Icon(Icons.Default.Notifications, contentDescription = null, tint = NovCyan, modifier = Modifier.size(44.dp))
          }

          Spacer(Modifier.height(30.dp))

          Text("Уведомления", color = NovTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)

          Spacer(Modifier.height(14.dp))

          Text(
              if (isGranted)
                  "Отлично! Вы будете получать уведомления когда ИИ ответит, пока приложение свёрнуто."
              else
                  "Разрешите уведомления, чтобы узнавать об ответах ИИ, даже когда приложение закрыто.",
              color = NovTextSecondary, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp
          )

          Spacer(Modifier.height(48.dp))

          if (isGranted) {
              Box(
                  modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(16.dp))
                      .background(NovCyan.copy(0.12f))
                      .padding(vertical = 20.dp),
                  contentAlignment = Alignment.Center
              ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                      Icon(Icons.Default.Check, null, tint = NovCyan, modifier = Modifier.size(22.dp))
                      Text("Уведомления разрешены", color = NovCyan, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                  }
              }
          } else {
              Button(
                  onClick = onRequestPermission,
                  modifier = Modifier.fillMaxWidth().height(54.dp),
                  shape = RoundedCornerShape(14.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = NovCyan, contentColor = NovDark)
              ) {
                  Icon(Icons.Default.Notifications, null, modifier = Modifier.size(20.dp))
                  Spacer(Modifier.width(8.dp))
                  Text("Разрешить уведомления", fontWeight = FontWeight.Bold, fontSize = 15.sp)
              }

              Spacer(Modifier.height(16.dp))

              Text(
                  "Можно разрешить позже в системных настройках",
                  color = NovTextSecondary.copy(0.5f), fontSize = 12.sp, textAlign = TextAlign.Center
              )
          }
      }
  }

  @Composable
  private fun OnboardingSlider(
      label: String, description: String, value: Float, displayValue: String,
      valueRange: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit
  ) {
      Column(modifier = Modifier.fillMaxWidth()) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
          ) {
              Column(modifier = Modifier.weight(1f)) {
                  Text(label, color = NovTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                  Text(description, color = NovTextSecondary, fontSize = 11.sp)
              }
              Spacer(Modifier.width(8.dp))
              Text(displayValue, color = NovCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
          }
          Slider(
              value = value, onValueChange = onValueChange, valueRange = valueRange,
              colors = SliderDefaults.colors(thumbColor = NovCyan, activeTrackColor = NovCyan, inactiveTrackColor = NovCyan.copy(0.18f)),
              modifier = Modifier.fillMaxWidth()
          )
      }
  }
  