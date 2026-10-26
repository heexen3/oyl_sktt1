package cl.oyl.frutossecos.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.domain.logic.FieldError

@Composable
fun fieldErrorText(error: FieldError?): String? = when (error) {
    null -> null
    FieldError.REQUIRED -> stringResource(R.string.error_required)
    FieldError.INVALID_EMAIL -> stringResource(R.string.error_email)
    FieldError.INVALID_PHONE -> stringResource(R.string.error_phone)
    FieldError.PASSWORD_TOO_SHORT -> stringResource(R.string.error_password_short)
    FieldError.PASSWORD_WEAK -> stringResource(R.string.error_password_weak)
    FieldError.ALIAS_INVALID -> stringResource(R.string.error_alias)
    FieldError.INVALID_NUMBER -> stringResource(R.string.error_number)
    FieldError.NEGATIVE -> stringResource(R.string.error_negative)
}

@Composable
fun OylTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: FieldError? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    supporting: String? = null,
) {
    val errorText = fieldErrorText(error)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = (errorText ?: supporting)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, error: FieldError? = null) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val errorText = fieldErrorText(error)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = errorText?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = stringResource(if (visible) R.string.cd_hide_password else R.string.cd_show_password),
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}
