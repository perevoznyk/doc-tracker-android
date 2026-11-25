package com.example.doctracker.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM documents ORDER BY validUntil") fun documents(): Flow<List<Document>>
    @Query("SELECT * FROM documents") suspend fun allDocuments(): List<Document>
    @Query("SELECT * FROM documents WHERE id = :id") fun document(id: Long): Flow<Document?>
    @Query("SELECT * FROM documents WHERE id = :id") suspend fun getDocument(id: Long): Document?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(d: Document): Long
    @Delete suspend fun delete(d: Document)

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE") fun categories(): Flow<List<Category>>
    @Query("SELECT * FROM categories") suspend fun categoriesOnce(): List<Category>
    @Query("SELECT * FROM categories WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun categoryByName(name: String): Category?
    @Insert suspend fun insertCategory(c: Category): Long
    @Update suspend fun updateCategory(c: Category)
    @Delete suspend fun deleteCategory(c: Category)
}

@Database(entities = [Document::class, Category::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "documents.db"
            ).build().also { instance = it }
        }
    }
}
