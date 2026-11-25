package com.example.doctracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.doctracker.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()

    val documents = dao.documents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = dao.categories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun document(id: Long) = dao.document(id)
    suspend fun get(id: Long) = dao.getDocument(id)

    fun save(doc: Document, onDone: () -> Unit) = viewModelScope.launch {
        dao.upsert(doc); onDone()
    }

    fun delete(doc: Document) = viewModelScope.launch {
        dao.delete(doc); PhotoStore.delete(doc.photoPath)
    }

    fun saveCategory(c: Category) = viewModelScope.launch {
        // Names are unique; ignore duplicates silently.
        runCatching { if (c.id == 0L) dao.insertCategory(c) else dao.updateCategory(c) }
    }

    fun deleteCategory(c: Category) = viewModelScope.launch { dao.deleteCategory(c) }

    // ---------- CSV ----------

    suspend fun exportCsv(): String {
        val cats = dao.categoriesOnce().associateBy { it.id }
        val rows = listOf(Csv.HEADER) + dao.allDocuments().map {
            listOf(
                it.name,
                cats[it.categoryId]?.name.orEmpty(),
                it.validFrom.toString(),
                it.validUntil.toString(),
                it.reminders
            )
        }
        return "\uFEFF" + Csv.write(rows) // BOM so Excel detects UTF-8
    }

    /** Returns the number of imported documents. Exact duplicates (name + both dates) are skipped. */
    suspend fun importCsv(text: String): Int {
        val rows = Csv.parse(text.removePrefix("\uFEFF"))
        if (rows.size < 2) return 0
        val seen = dao.allDocuments()
            .map { Triple(it.name.lowercase(), it.validFrom, it.validUntil) }.toMutableSet()
        var imported = 0
        for (r in rows.drop(1)) {
            if (r.size < 4) continue
            val name = r[0].trim()
            if (name.isEmpty()) continue
            val from = runCatching { LocalDate.parse(r[2].trim()) }.getOrNull() ?: continue
            val until = runCatching { LocalDate.parse(r[3].trim()) }.getOrNull() ?: continue
            if (!seen.add(Triple(name.lowercase(), from, until))) continue

            val catName = r[1].trim()
            val catId = if (catName.isEmpty()) null else {
                (dao.categoryByName(catName)
                    ?: Category(name = catName).let { it.copy(id = dao.insertCategory(it)) }).id
            }
            val reminders = ReminderOption.encode(ReminderOption.parse(r.getOrElse(4) { "" }))
            dao.upsert(Document(name = name, categoryId = catId, validFrom = from,
                validUntil = until, reminders = reminders))
            imported++
        }
        return imported
    }
}
