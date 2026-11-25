package com.example.doctracker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.doctracker.data.Document
import com.example.doctracker.data.pretty
import com.example.doctracker.data.validity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    vm: MainViewModel,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onCategories: () -> Unit
) {
    val docs by vm.documents.collectAsState()
    val cats by vm.categories.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var filter by rememberSaveable { mutableStateOf<Long?>(null) }
    var menu by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) scope.launch {
            val csv = vm.exportCsv()
            val ok = runCatching {
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                }
            }.isSuccess
            snack.showSnackbar(if (ok) "Exported ${docs.size} documents" else "Export failed")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            val count = runCatching {
                val text = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }.orEmpty()
                }
                vm.importCsv(text)
            }.getOrNull()
            snack.showSnackbar(if (count == null) "Import failed" else "Imported $count documents")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Documents") },
                actions = {
                    IconButton(onCategories) { Icon(Icons.AutoMirrored.Filled.Label, "Categories") }
                    Box {
                        IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, "More") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Export to CSV") }, onClick = {
                                menu = false; exportLauncher.launch("documents.csv")
                            })
                            DropdownMenuItem(text = { Text("Import from CSV") }, onClick = {
                                menu = false; importLauncher.launch(arrayOf("*/*"))
                            })
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onAdd) { Icon(Icons.Default.Add, "Add document") }
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (cats.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { FilterChip(filter == null, { filter = null }, { Text("All") }) }
                    items(cats, key = { it.id }) { c ->
                        FilterChip(filter == c.id, { filter = c.id }, { Text(c.name) })
                    }
                }
            }
            val shown = docs.filter { filter == null || it.categoryId == filter }
            if (shown.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No documents yet. Tap + to add one.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shown, key = { it.id }) { d ->
                        DocumentCard(d, cats.firstOrNull { it.id == d.categoryId }?.name) { onOpen(d.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentCard(doc: Document, categoryName: String?, onClick: () -> Unit) {
    val v = doc.validity()
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(doc.name, style = MaterialTheme.typography.titleMedium)
            if (categoryName != null)
                Text(categoryName, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text("Valid until ${doc.validUntil.pretty()}", style = MaterialTheme.typography.bodyMedium)
            Text(
                v.text, style = MaterialTheme.typography.bodyMedium,
                color = if (v.expired) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { v.percent / 100f }, modifier = Modifier.fillMaxWidth(),
                color = if (v.expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}
