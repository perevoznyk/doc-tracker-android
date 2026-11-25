package com.example.doctracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.doctracker.data.ReminderOption
import com.example.doctracker.data.pretty
import com.example.doctracker.data.validity
import java.io.File

private val Blue = Color(0xFF1565C0)
private val CardBg = Color(0xFFFAFAFA)

@Composable
fun DetailScreen(vm: MainViewModel, id: Long, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val flow = remember(id) { vm.document(id) }
    val doc by flow.collectAsState(initial = null)
    val cats by vm.categories.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }

    val d = doc ?: return
    val v = d.validity()
    val accent = if (v.expired) MaterialTheme.colorScheme.error else Blue
    val reminders = ReminderOption.parse(d.reminders)

    Column(
        Modifier.fillMaxSize().background(Color(0xFFE6E6E6)).verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(Modifier.fillMaxWidth().height(200.dp).background(Color(0xFF2F73C4))) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp)) {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Spacer(Modifier.weight(1f))
                IconButton({ onEdit(d.id) }) { Icon(Icons.Default.Edit, "Edit", tint = Color.White) }
                IconButton({ confirmDelete = true }) { Icon(Icons.Default.Delete, "Delete", tint = Color.White) }
            }
            Text(
                d.name, color = Color.White, fontSize = 36.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            )
        }

        // Validity card
        InfoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Label("Valid from"); Value(d.validFrom.pretty())
                    Spacer(Modifier.height(16.dp))
                    Label("Valid until"); Value(d.validUntil.pretty())
                }
                ArcProgress(v.percent, accent)
            }
            Spacer(Modifier.height(16.dp))
            Text(v.text, fontSize = 18.sp, color = if (v.expired) accent else Color.Gray)
        }

        // Reminder card
        InfoCard {
            Label("Reminder")
            if (reminders.isEmpty()) Value("No reminders")
            else ReminderOption.entries.filter { it in reminders }.forEach { Value("•  ${it.label}") }
        }

        // Category card
        cats.firstOrNull { it.id == d.categoryId }?.let { c ->
            InfoCard { Label("Category"); Value(c.name) }
        }

        // Photo card
        d.photoPath?.let { path ->
            InfoCard {
                Label("Photo")
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = File(path), contentDescription = "Document photo",
                    modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth
                )
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete document?") },
            text = { Text("“${d.name}” will be removed.") },
            confirmButton = {
                TextButton({ confirmDelete = false; vm.delete(d); onBack() }) { Text("Delete") }
            },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = androidx.compose.ui.graphics.RectangleShape,
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable private fun Label(t: String) = Text(t, fontSize = 24.sp, fontWeight = FontWeight.Bold)
@Composable private fun Value(t: String) =
    Text(t, fontSize = 18.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))

@Composable
private fun ArcProgress(percent: Int, color: Color) {
    Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            drawArc(Color.LightGray, 135f, 270f, false, style = stroke)
            if (percent > 0) drawArc(color, 135f, 270f * percent / 100f, false, style = stroke)
        }
        Text("$percent%", fontSize = 28.sp, color = Color.DarkGray)
    }
}
