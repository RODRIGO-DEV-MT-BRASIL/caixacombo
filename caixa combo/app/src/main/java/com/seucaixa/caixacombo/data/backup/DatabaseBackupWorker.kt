package com.seucaixa.caixacombo.data.backup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val dbName = "caixa_combo_database"
                val dbFile = applicationContext.getDatabasePath(dbName)

                if (!dbFile.exists()) {
                    Log.w("DatabaseBackupWorker", "DB não encontrado: ${dbFile.absolutePath}")
                    return@withContext Result.success()
                }

                val backupDir = File(applicationContext.filesDir, "backups")
                if (!backupDir.exists()) backupDir.mkdirs()

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale("pt-BR")).format(Date())
                val backupBase = File(backupDir, "db_backup_$timestamp")

                copyIfExists(dbFile, File(backupDir, "${backupBase.name}.db"))
                copyIfExists(File(dbFile.absolutePath + "-wal"), File(backupDir, "${backupBase.name}.db-wal"))
                copyIfExists(File(dbFile.absolutePath + "-shm"), File(backupDir, "${backupBase.name}.db-shm"))

                keepOnlyLastNBackups(backupDir, 20)

                Log.d("DatabaseBackupWorker", "Backup criado em: ${backupDir.absolutePath}")
                Result.success()
            } catch (e: Exception) {
                Log.e("DatabaseBackupWorker", "Erro ao criar backup: ${e.message}", e)
                Result.retry()
            }
        }
    }

    private fun copyIfExists(from: File, to: File) {
        if (!from.exists()) return
        from.inputStream().use { input ->
            to.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun keepOnlyLastNBackups(dir: File, keep: Int) {
        val files = dir.listFiles()?.toList().orEmpty()
            .filter { it.name.startsWith("db_backup_") }
            .sortedByDescending { it.lastModified() }

        if (files.size <= keep) return

        files.drop(keep).forEach { runCatching { it.delete() } }
    }
}
