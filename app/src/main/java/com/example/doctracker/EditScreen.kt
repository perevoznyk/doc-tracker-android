package com.example.doctracker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.doctracker.data.Document
import com.example.doctracker.data.PhotoStore
import com.example.doctracker.data.ReminderOption
import com.example.doctracker.data.pretty
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(vm: MainViewModel, id: Long, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val cats by vm.categories.collectAsState()
    val existing by produceState<Document?>(null, id) { value = if (id == 0L) null else vm.get(id) }

    var initialized by rememberSaveable { mutableStateOf(id == 0L) }
    var name by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var from by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var until by rememberSaveable { mutableLongStateOf(LocalDate.now().plusYears(1).toEpochDay()) }
    var reminders by rememberSaveable { mutableStateOf("MONTH_1;ON_DAY") }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraFile by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(existing) {
        val e = existing
        if (e != null && !initialized) {
            name = e.name; categoryId = e.categoryId
            from = e.validFrom.toEpochDay(); until = e.validUntil.toEpochDay()
            reminders = e.reminders; photo = e.photoPath
            initialized = true
        }
    }

    /** Replaces the current photo, deleting it if it was added during this editing session. */
    fun setPhoto(new: String?) {
        if (photo != null && photo != existing?.photoPath) PhotoStore.delete(photo)
        photo = new
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { setPhoto(PhotoStore.import(ctx, it)) }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val p = cameraFile
        if (ok && p != null) setPhoto(p) else PhotoStore.delete(p)
    }

    val selected = ReminderOption.parse(reminders)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id == 0L) "New document" else "Edit document") },
                navigationIcon = {
                    IconButton(onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                name, { name = it }, label = { Text("Document name") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded, { expanded = it }) {
                OutlinedTextField(
                    value = cats.firstOrNull { it.id == categoryId }?.name ?: "None",
                    onValueChange = {}, readOnly = true, label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded, { expanded = false }) {
                    DropdownMenuItem(text = { Text("None") }, onClick = { categoryId = null; expanded = false })
                    cats.forEach { c ->
                        DropdownMenuItem(text = { Text(c.name) }, onClick = { categoryId = c.id; expanded = false })
                    }
                }
            }

            DateField("Valid from", from) { from = it }
            DateField("Valid until", until) { until = it }
            if (until < from) Text("“Valid until” must not be before “Valid from”.",
                color = MaterialTheme.colorScheme.error)

            Text("Reminders", style = MaterialTheme.typography.titleMedium)
            ReminderOption.entries.forEach { o ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(o in selected, {
                        reminders = ReminderOption.encode(if (o in selected) selected - o else selected + o)
                    })
                    Text(o.label)
                }
            }

            Text("Photo (optional)", style = MaterialTheme.typography.titleMedium)
            photo?.let {
                AsyncImage(File(it), "Document photo", Modifier.fillMaxWidth().heightIn(max = 240.dp),
                    contentScale = ContentScale.Fit)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Gallery") }
                OutlinedButton({
                    val f = PhotoStore.newFile(ctx)
                    cameraFile = f.absolutePath
                    camera.launch(FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f))
                }) { Text("Camera") }
                if (photo != null) TextButton({ setPhoto(null) }) { Text("Remove") }
            }

            Button(
                enabled = name.isNotBlank() && until >= from && initialized,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val oldPhoto = existing?.photoPath
                    if (oldPhoto != null && oldPhoto != photo) PhotoStore.delete(oldPhoto)
                    vm.save(
                        Document(
                            id = id, name = name.trim(), categoryId = categoryId,
                            validFrom = LocalDate.ofEpochDay(from), validUntil = LocalDate.ofEpochDay(until),
                            reminders = reminders, photoPath = photo
                        ), onDone
                    )
                }
            ) { Text("Save") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, epochDay: Long, onChange: (Long) -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedButton({ show = true }, Modifier.fillMaxWidth()) {
        Text("$label: ${LocalDate.ofEpochDay(epochDay).pretty()}")
    }
    if (show) {
        // DatePicker works with UTC-midnight milliseconds
        val state = rememberDatePickerState(initialSelectedDateMillis = epochDay * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton({
                    state.selectedDateMillis?.let { onChange(Math.floorDiv(it, 86_400_000L)) }
                    show = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton({ show = false }) { Text("Cancel") } }
        ) { DatePicker(state) }
    }
}
