package com.novaproject.novai.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaproject.novai.R
import com.novaproject.novai.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigate: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    var navigated by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(900),
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(2200)
        if (!navigated) {
            navigated = true
            onNavigate()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NovDark)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
        ) {
            Spacer(Modifier.weight(1f))

            // Logo with solid NovCyan border (matches NovAI text color)
            Box(
                modifier = Modifier
                    .size(126.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(NovCyan),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.novai_logo),
                    contentDescription = "NovAI",
                    modifier = Modifier
                        .size(119.dp)
                        .clip(RoundedCornerShape(27.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "NOVA PROJECT",
                color = NovTextSecondary.copy(alpha = 0.65f),
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "NovAI",
                color = NovCyan,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 38.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "by Xenor",
                color = NovTextSecondary.copy(alpha = 0.55f),
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = "© 2026 Nova Project",
                color = NovTextSecondary.copy(alpha = 0.3f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}
