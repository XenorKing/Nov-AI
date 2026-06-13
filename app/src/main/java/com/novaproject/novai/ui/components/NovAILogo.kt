package com.novaproject.novai.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaproject.novai.R
import com.novaproject.novai.ui.theme.*

@Composable
fun NovAILogo(size: Dp = 72.dp, showText: Boolean = true) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.novai_logo),
            contentDescription = "NovAI",
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.28f)),
            contentScale = ContentScale.Crop
        )
        if (showText) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "NovAI",
                color = NovTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.32f).sp
            )
        }
    }
}
