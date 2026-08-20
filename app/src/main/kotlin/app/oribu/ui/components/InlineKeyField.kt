package app.oribu.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Campo de credencial no molde do tonkatsu_box: valor mascarado por padrão, com toggle de
 * visibilidade, e só chama `onSave` em Enter ou no botão "Salvar" explícito — nunca ao perder
 * o foco, para não persistir um valor incompleto sem intenção do usuário.
 */
@Composable
fun InlineKeyField(
    label: String,
    value: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(value) { mutableStateOf(value) }
    var visible by remember { mutableStateOf(false) }
    val dirty = draft != value

    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (dirty) onSave(draft) }),
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { visible = !visible }) {
                    Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                }
                if (dirty) {
                    TextButton(onClick = { onSave(draft) }) { Text("Salvar") }
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}
