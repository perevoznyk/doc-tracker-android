package com.example.doctracker

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.doctracker.data.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(vm: MainViewModel, onBack: () -> Unit) {
    val cats by vm.categories.collectAsState()
    var dialog by remember { mutableStateOf<Category?>(null) } // id == 0 → new category

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton({ dialog = Category(name = "") }) { Icon(Icons.Default.Add, "Add category") }
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(cats, key = { it.id }) { c ->
                ListItem(
                    headlineContent = { Text(c.name) },
                    trailingContent = {
                        Row {
                            IconButton({ dialog = c }) { Icon(Icons.Default.Edit, "Rename") }
                            IconButton({ vm.deleteCategory(c) }) { Icon(Icons.Default.Delete, "Delete") }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    dialog?.let { d ->
        var text by remember(d) { mutableStateOf(d.name) }
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(if (d.id == 0L) "New category" else "Rename category") },
            text = { OutlinedTextField(text, { text = it }, singleLine = true, label = { Text("Name") }) },
            confirmButton = {
                TextButton(enabled = text.isNotBlank(), onClick = {
                    vm.saveCategory(d.copy(name = text.trim())); dialog = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton({ dialog = null }) { Text("Cancel") } }
        )
    }
}
