package com.novaproject.novai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaproject.novai.ui.components.AuthTextField
import com.novaproject.novai.ui.components.NovAILogo
import com.novaproject.novai.ui.theme.*
import com.novaproject.novai.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    onPrivacyPolicy: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    LaunchedEffect(uiState.user) {
        if (uiState.user != null) onLoginSuccess()
    }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            delay(4000)
            viewModel.clearError()
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            containerColor = NovCard,
            title = {
                Text(
                    "Сброс пароля",
                    color = NovTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Введите email для сброса пароля:",
                        color = NovTextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    AuthTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = "Email",
                        leadingIcon = Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetPassword(resetEmail)
                        showForgotDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NovCyan)
                ) { Text("Отправить", color = NovDark) }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Отмена", color = NovTextSecondary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(NovDark, Color(0xFF0A0A18), NovDark))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 72.dp, bottom = 40.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NovAILogo(size = 80.dp)

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Добро пожаловать",
                color = NovTextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Войдите в аккаунт чтобы продолжить",
                color = NovTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 36.dp)
            )

            AnimatedVisibility(visible = uiState.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ErrorRed.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = uiState.error ?: "",
                        color = ErrorRed,
                        modifier = Modifier.padding(14.dp),
                        fontSize = 14.sp
                    )
                }
            }

            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                leadingIcon = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(Modifier.height(14.dp))

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Пароль",
                leadingIcon = Icons.Default.Lock,
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = NovTextSecondary
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.signIn(email, password)
                    }
                )
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { resetEmail = email; showForgotDialog = true }
                ) {
                    Text("Забыли пароль?", color = NovCyan, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.signIn(email, password) },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NovCyan,
                    disabledContainerColor = NovCyan.copy(alpha = 0.45f)
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        Modifier.size(22.dp),
                        color = NovDark,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        "Войти",
                        color = NovDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Нет аккаунта? ", color = NovTextSecondary, fontSize = 14.sp)
                Text(
                    text = "Зарегистрироваться",
                    color = NovCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Входя, вы соглашаетесь с ",
                    color = NovTextSecondary.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Text(
                    text = "Политикой конфиденциальности",
                    color = NovCyan.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onPrivacyPolicy() }
                )
            }
        }
    }
}
