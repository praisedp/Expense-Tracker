package com.example.expencetracker.util

import android.content.Context
import android.net.Uri
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import com.example.expencetracker.data.BackupData
import com.example.expencetracker.data.CategoryBudget
import com.example.expencetracker.data.PrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Build



object BackupManager {
    private const val BACKUP_PREFIX = "backup_"
    private const val INTERNAL_BACKUP_FILENAME = "internal_backup.json"

    /**
     * Creates an internal backup in the app's private directory.
     * @return File path of the backup
     */
    fun createInternalBackup(context: Context): String {
        // Gather everything
        val data = BackupData(
            transactions     = PrefsManager.loadTransactions(),
            categories       = PrefsManager.loadCategories(),
            totalBudget      = PrefsManager.getTotalBudget(),
            categoryBudgets  = PrefsManager.loadCategoryBudgets(),
            currency         = PrefsManager.getCurrency()
        )

        // Serialize JSON
        val gson = Gson()
        val json = gson.toJson(data)

        // Save to internal storage
        val file = File(context.filesDir, INTERNAL_BACKUP_FILENAME)
        file.outputStream().use { it.write(json.toByteArray()) }
        
        // Return the absolute path
        return file.absolutePath
    }
    
    /**
     * Restores data from the internal backup if it exists.
     * @return true on success, false if backup doesn't exist or on failure
     */
    suspend fun restoreInternalBackup(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, INTERNAL_BACKUP_FILENAME)
                
                // Check if backup exists
                if (!file.exists()) {
                    return@withContext false
                }
                
                // Read the backup
                val json = file.readText()
                
                // Deserialize
                val data = Gson().fromJson(json, BackupData::class.java)
                
                // Overwrite prefs
                PrefsManager.saveTransactions(data.transactions)
                PrefsManager.saveCategories(data.categories)
                PrefsManager.setTotalBudget(data.totalBudget)
                PrefsManager.saveCategoryBudgets(data.categoryBudgets)
                PrefsManager.setCurrency(data.currency)
                
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    /**
     * Checks if an internal backup exists.
     * @return true if backup exists, false otherwise
     */
    fun internalBackupExists(context: Context): Boolean {
        val file = File(context.filesDir, INTERNAL_BACKUP_FILENAME)
        return file.exists()
    }

    /**
     * Exports data to Downloads folder for sharing/external backup.
     * This was previously named exportBackup.
     * @return String representing the path where the file was saved
     */
    fun exportToDownloads(context: Context): String {
        // Gather everything
        val data = BackupData(
            transactions     = PrefsManager.loadTransactions(),
            categories       = PrefsManager.loadCategories(),
            totalBudget      = PrefsManager.getTotalBudget(),
            categoryBudgets  = PrefsManager.loadCategoryBudgets(),
            currency         = PrefsManager.getCurrency()
        )

        // 1) Serialize JSON
        val gson = Gson()
        val json = gson.toJson(data)

        // 2) Timestamped filename
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "$BACKUP_PREFIX$timestamp.json"

        // 3) Attempt public‑Downloads via MediaStore (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                return uri.toString()
            }
        } else {
            // Legacy write to public Download folder (visible on older Android)
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileLegacy = File(downloadsDir, filename)
            fileLegacy.outputStream().use { it.write(json.toByteArray()) }
            return fileLegacy.absolutePath
        }

        // 4) Fallback: app‑specific external Documents (private but guaranteed)
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val file = File(dir, filename)
        file.outputStream().use { it.write(json.toByteArray()) }
        return file.absolutePath
    }

    /**
     * Imports data from the given JSON Uri, overwriting existing preferences.
     * @return true on success, false on any failure.
     */
    suspend fun importBackup(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Read the JSON text
                val json = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    .use { it?.readText() }
                    ?: return@withContext false

                // Deserialize
                val data = Gson().fromJson(json, BackupData::class.java)

                // Overwrite prefs
                PrefsManager.saveTransactions(data.transactions)
                PrefsManager.saveCategories(data.categories)
                PrefsManager.setTotalBudget(data.totalBudget)
                PrefsManager.saveCategoryBudgets(data.categoryBudgets)
                PrefsManager.setCurrency(data.currency)

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}