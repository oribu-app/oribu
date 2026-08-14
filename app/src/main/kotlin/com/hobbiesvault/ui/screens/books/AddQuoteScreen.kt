package com.hobbiesvault.ui.screens.books

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
import com.hobbiesvault.model.MediaItem
import kotlinx.coroutines.launch

/**
 * Página própria para adicionar citação (em vez de modal): título = nome do
 * livro, Salvar no canto esquerdo e X para fechar no canto direito, mesma
 * linha do título — mesmo padrão da AnotacoesScreen.
 */
@Composable
fun AddQuoteScreen(navController: NavController, book: MediaItem) {
    var quote by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun save() {
        val id = book.id ?: return
        if (quote.isBlank()) return
        scope.launch {
            DB.repo.addBookQuote(id, quote.trim(), comment.trim().ifBlank { null })
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
                TextButton(onClick = { save() }, enabled = quote.isNotBlank()) { Text("Salvar") }
                Text(
                    book.title,
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value         = quote,
                onValueChange = { quote = it },
                placeholder   = { Text("Citação") },
                minLines      = 4,
                maxLines      = 10,
                modifier      = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value         = comment,
                onValueChange = { comment = it },
                placeholder   = { Text("Comentário (opcional)") },
                modifier      = Modifier.fillMaxWidth(),
            )
        }
    }
}
