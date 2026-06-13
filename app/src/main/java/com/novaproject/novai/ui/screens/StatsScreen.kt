package com.novaproject.novai.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Token
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.novaproject.novai.data.model.DayActivity
import com.novaproject.novai.data.model.ModelTokenStats
import com.novaproject.novai.data.model.friendlyModelName
import com.novaproject.novai.ui.theme.*
import com.novaproject.novai.viewmodel.StatsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val viewModel: StatsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = NovDark,
        topBar = {
            TopAppBar(
                title = { Text("Статистика", color = NovTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = NovTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Обновить", tint = NovCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NovSurface)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NovCyan, strokeWidth = 2.dp)
            }
        } else if (state.error != null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error ?: "", color = ErrorRed, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.load() }, colors = ButtonDefaults.buttonColors(containerColor = NovCyan)) {
                        Text("Повторить", color = NovDark)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ChatBubble,
                        label = "Диалоги",
                        value = state.stats.totalConversations.toString(),
                        gradient = listOf(NovCyan.copy(0.15f), NovCyan.copy(0.05f)),
                        iconTint = NovCyan
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Today,
                        label = "Сегодня",
                        value = state.stats.activeToday.toString(),
                        gradient = listOf(NovPurple.copy(0.15f), NovPurple.copy(0.05f)),
                        iconTint = NovPurple
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CalendarToday,
                        label = "7 дней",
                        value = state.stats.activeThisWeek.toString(),
                        gradient = listOf(SuccessGreen.copy(0.15f), SuccessGreen.copy(0.05f)),
                        iconTint = SuccessGreen
                    )
                }

                val totalTokens = state.stats.tokenStatsByModel.sumOf { it.totalTokens }
                if (state.stats.totalMessages > 0 || totalTokens > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.ChatBubble,
                            label = "Сообщений",
                            value = state.stats.totalMessages.toString(),
                            gradient = listOf(NovCyan.copy(0.1f), NovPurple.copy(0.1f)),
                            iconTint = NovCyan
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Token,
                            label = "Токенов",
                            value = formatTokenCount(totalTokens),
                            gradient = listOf(NovPurple.copy(0.15f), NovCyan.copy(0.05f)),
                            iconTint = NovPurple
                        )
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = NovCard), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Активность за 7 дней", color = NovTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            "Диалоги с активностью по дням",
                            color = NovTextSecondary, fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                        )
                        ActivityBarChart(
                            daily = state.stats.dailyActivity,
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        )
                    }
                }

                val avg = if (state.stats.dailyActivity.isNotEmpty())
                    state.stats.dailyActivity.sumOf { it.count }.toFloat() / 7f else 0f

                Card(colors = CardDefaults.cardColors(containerColor = NovCard), shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(NovCyan.copy(0.2f), NovPurple.copy(0.2f)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = NovCyan, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Среднее в день", color = NovTextSecondary, fontSize = 12.sp)
                            Text("%.1f диалогов".format(avg), color = NovTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Text("за 7 дней", color = NovTextSecondary, fontSize = 12.sp)
                    }
                }

                if (state.stats.tokenStatsByModel.isNotEmpty()) {
                    Text(
                        "ИСПОЛЬЗОВАНИЕ ТОКЕНОВ ПО МОДЕЛЯМ",
                        color = NovTextSecondary, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                    Card(colors = CardDefaults.cardColors(containerColor = NovCard), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val maxTokens = state.stats.tokenStatsByModel.maxOfOrNull { it.totalTokens }?.coerceAtLeast(1) ?: 1
                            state.stats.tokenStatsByModel.forEach { ms ->
                                ModelTokenRow(ms = ms, maxTokens = maxTokens)
                                if (ms != state.stats.tokenStatsByModel.last()) {
                                    HorizontalDivider(color = NovDivider)
                                }
                            }
                        }
                    }
                }

                if (state.stats.totalConversations == 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NovCyan.copy(0.08f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "Начните первый диалог с NovAI, чтобы здесь появилась статистика!",
                            color = NovCyan, fontSize = 13.sp,
                            modifier = Modifier.padding(14.dp), lineHeight = 20.sp
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun formatTokenCount(tokens: Long): String = when {
    tokens >= 1_000_000 -> "%.1fM".format(tokens / 1_000_000.0)
    tokens >= 1_000 -> "%.1fK".format(tokens / 1_000.0)
    else -> tokens.toString()
}

@Composable
private fun ModelTokenRow(ms: ModelTokenStats, maxTokens: Long) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(ms.totalTokens) {
        progress.snapTo(0f)
        progress.animateTo(ms.totalTokens.toFloat() / maxTokens.toFloat(), animationSpec = tween(700))
    }
    // Use friendly model name (e.g. nex-agi/nex-n2-pro:free → NovAI)
    val displayName = friendlyModelName(ms.model)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                displayName, color = NovTextPrimary, fontSize = 13.sp,
                fontWeight = FontWeight.Medium, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(formatTokenCount(ms.totalTokens) + " токенов", color = NovCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                .background(NovDivider)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.value)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(listOf(NovCyan, NovPurple)))
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${ms.messages} сообщ.", color = NovTextSecondary, fontSize = 10.sp)
            Text("▲${formatTokenCount(ms.promptTokens)} ▼${formatTokenCount(ms.completionTokens)}", color = NovTextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    gradient: List<Color>,
    iconTint: Color
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = NovCard), shape = RoundedCornerShape(16.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(gradient)).padding(14.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(6.dp))
                Text(value, color = NovTextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                Text(label, color = NovTextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ActivityBarChart(daily: List<DayActivity>, modifier: Modifier = Modifier) {
    val maxCount = daily.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val animProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(daily) {
        scope.launch {
            animProgress.snapTo(0f)
            animProgress.animateTo(1f, animationSpec = tween(800))
        }
    }

    val barColor = NovCyan
    val barColorDim = NovCyan.copy(alpha = 0.35f)
    val labelColor = NovTextSecondary

    Column(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .drawBehind {
                    drawActivityBars(daily, maxCount, animProgress.value, barColor, barColorDim)
                }
        )
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            daily.forEach { day ->
                Text(
                    text = day.label, color = labelColor, fontSize = 9.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun DrawScope.drawActivityBars(
    daily: List<DayActivity>,
    maxCount: Int,
    progress: Float,
    barColor: Color,
    barColorDim: Color
) {
    val n = daily.size
    if (n == 0) return
    val slotWidth = size.width / n
    val barW = slotWidth * 0.55f
    val maxBarH = size.height - 4.dp.toPx()
    val today = n - 1
    daily.forEachIndexed { i, day ->
        val barH = if (maxCount > 0) (day.count.toFloat() / maxCount) * maxBarH * progress else 0f
        val left = i * slotWidth + (slotWidth - barW) / 2f
        val top = size.height - barH
        val color = if (i == today) barColor else barColorDim
        if (barH > 0) {
            drawRoundRect(color = color, topLeft = Offset(left, top), size = Size(barW, barH), cornerRadius = CornerRadius(6.dp.toPx()))
        } else {
            drawRoundRect(color = barColorDim.copy(alpha = 0.15f), topLeft = Offset(left, size.height - 4.dp.toPx()), size = Size(barW, 4.dp.toPx()), cornerRadius = CornerRadius(2.dp.toPx()))
        }
    }
}
