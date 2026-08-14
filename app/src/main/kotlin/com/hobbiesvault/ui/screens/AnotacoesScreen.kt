package com.hobbiesvault.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hobbiesvault.data.db.DB
import kotlinx.coroutines.launch
import com.hobbiesvault.ui.navigation.consumeAnotacoesItem
import com.hobbiesvault.ui.navigation.returnAnotacoesResult

/**
 * Página própria de Anotações (em vez de modal): título = nome do conteúdo, Salvar
 * no canto esquerdo e X para fechar no canto direito, ambos na mesma linha do título.
 */
@Composable
fun AnotacoesScreen(navController: NavController) {
    val item = remember { navController.consumeAnotacoesItem() }
    var text by remember { mutableStateOf(item?.personalNotes ?: "") }
    val scope = rememberCoroutineScope()

    fun save() {
        scope.launch {
            if (item?.id != null) {
                DB.repo.update(item.copy(personalNotes = text.ifBlank { null }))
            }
            navController.returnAnotacoesResult(text)
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { save() }) { Text("Salvar") }
                Text(
                    item?.title ?: "Anotações",
                    style      = MaterialTheme.typography.titleMedium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }
        },
    ) { padding ->
        OutlinedTextField(
            value         = text,
            onValueChange = { text = it },
            placeholder   = { Text("Escreva uma anotação livre sobre este item...") },
            modifier      = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        )
    }
}
