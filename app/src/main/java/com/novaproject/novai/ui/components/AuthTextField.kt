package com.novaproject.novai.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.novaproject.novai.ui.theme.*

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        isError = isError,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NovCyan,
            unfocusedBorderColor = NovDivider,
            errorBorderColor = ErrorRed,
            focusedLabelColor = NovCyan,
            unfocusedLabelColor = NovTextSecondary,
            cursorColor = NovCyan,
            focusedContainerColor = NovCard,
            unfocusedContainerColor = NovCard,
            focusedTextColor = NovTextPrimary,
            unfocusedTextColor = NovTextPrimary,
            focusedLeadingIconColor = NovCyan,
            unfocusedLeadingIconColor = NovTextSecondary
        ),
        modifier = modifier.fillMaxWidth()
    )
}
