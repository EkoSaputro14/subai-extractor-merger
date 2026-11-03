package com.example.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.domain.BatchOutputFileWriter
import com.example.domain.LocalDirectoryBatchOutputWriter
import com.example.domain.SavedBatchOutputFile
import com.example.model.BatchQueueState
import com.example.model.BatchTerminalOutcome
import com.example.model.FileConflictPolicy
import java.io.File
import java.util.UUID

/**
 * Payload of a batch completion notification sent to the phone.
 */
data class BatchNotificationPayload(
    val notificationId: Int,
    val outcome: BatchTerminalOutcome,
    val title: String,
    val message: String,
    val summaryDetail: String,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Helper for creating and posting native Android system tray notifications when the
 * Batch Subtitle Translation Queue finishes (All Succeeded, Finished with Issues, or Cancelled).
 */
class BatchNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "batch_subtitle_translation_channel"
        const val CHANNEL_NAME = "Batch Subtitle Translation"
        const val NOTIFICATION_ID_BATCH_FINISH = 40210

        /**
         * Builds the exact notification payload for the terminal state of [state].
         * Returns `null` if the queue is still running/queued or if notifications are disabled.
         */
        fun buildNotificationPayload(state: BatchQueueState): BatchNotificationPayload? {
            if (!state.notificationsEnabled) return null
            val outcome = state.terminalOutcome
            if (outcome == BatchTerminalOutcome.IN_PROGRESS_OR_IDLE) return null

            val total = state.totalFiles
            val completed = state.completedCount
            val failedOrAttention = state.needsAttentionCount + state.failedCount
            val cancelled = state.cancelledCount

            return when (outcome) {
                BatchTerminalOutcome.ALL_SUCCEEDED -> BatchNotificationPayload(
                    notificationId = NOTIFICATION_ID_BATCH_FINISH,
                    outcome = outcome,
                    title = "Batch Translation Completed",
                    message = "Semua file subtitle berhasil diterjemahkan dan lolos validasi.",
                    summaryDetail = "Total: $total file | Berhasil: $completed | Folder: ${state.outputFolderDisplayName}"
                )
                BatchTerminalOutcome.FINISHED_WITH_ISSUES -> BatchNotificationPayload(
                    notificationId = NOTIFICATION_ID_BATCH_FINISH,
                    outcome = outcome,
                    title = "Batch Translation Finished with Issues",
                    message = "Antrean telah selesai diproses, tetapi beberapa file memerlukan perhatian.",
                    summaryDetail = "Total: $total file | Berhasil: $completed | Perlu Perhatian: $failedOrAttention | Folder: ${state.outputFolderDisplayName}"
                )
                BatchTerminalOutcome.CANCELLED -> BatchNotificationPayload(
                    notificationId = NOTIFICATION_ID_BATCH_FINISH,
                    outcome = outcome,
                    title = "Batch Translation Cancelled",
                    message = "Proses batch dibatalkan sebelum seluruh file selesai diterjemahkan.",
                    summaryDetail = "Total: $total file | Selesai: $completed | Dibatalkan: $cancelled | Folder: ${state.outputFolderDisplayName}"
                )
                BatchTerminalOutcome.IN_PROGRESS_OR_IDLE -> null
            }
        }
    }

    init {
        ensureNotificationChannel()
    }

    fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi saat antrean Batch Subtitle Translation selesai diproses."
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Dispatches a native Android notification when the batch reaches a terminal state.
     * Returns the [BatchNotificationPayload] if sent (or prepared when notifications are enabled).
     */
    fun notifyBatchTerminalState(state: BatchQueueState): BatchNotificationPayload? {
        val payload = buildNotificationPayload(state) ?: return null
        ensureNotificationChannel()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return payload

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_tab", 4) // Open Batch Queue tab
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            payload.notificationId,
            intent,
            pendingIntentFlags
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(payload.title)
            .setContentText(payload.message)
            .setSubText("${state.completedCount}/${state.totalFiles} selesai")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${payload.message}\n${payload.summaryDetail}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            manager.notify(payload.notificationId, notification)
        }
        return payload
    }
}

/**
 * Hybrid [BatchOutputFileWriter] that supports both:
 * 1. Android Storage Access Framework (SAF) Tree URIs (`content://...`) chosen via `OpenDocumentTree`
 *    using standard `DocumentsContract` APIs.
 * 2. Standard filesystem directories (`File`) such as `<externalFilesDir>/Translated_Subtitles`.
 */
class AndroidBatchOutputWriter(
    private val context: Context,
    private val folderUriOrPath: String
) : BatchOutputFileWriter {

    private val isContentUri: Boolean
        get() = folderUriOrPath.startsWith("content://")

    val resolvedLocalDir: File
        get() = if (folderUriOrPath.isNotBlank() && !isContentUri) {
            File(folderUriOrPath)
        } else {
            val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
            File(baseDir, "Translated_Subtitles")
        }

    override fun isOutputFolderWritable(): Pair<Boolean, String?> {
        if (!isContentUri) {
            return LocalDirectoryBatchOutputWriter(resolvedLocalDir).isOutputFolderWritable()
        }
        return try {
            val treeUri = Uri.parse(folderUriOrPath)
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val dirUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            val probeName = ".batch_probe_${System.nanoTime()}.tmp"
            val createdProbe = DocumentsContract.createDocument(
                context.contentResolver,
                dirUri,
                "application/octet-stream",
                probeName
            )
            if (createdProbe == null) {
                false to "Folder SAF tidak memiliki izin tulis. Silakan pilih ulang folder output."
            } else {
                runCatching { DocumentsContract.deleteDocument(context.contentResolver, createdProbe) }
                true to null
            }
        } catch (e: Exception) {
            false to "Folder output tidak dapat diakses: ${e.message}"
        }
    }

    override fun writeValidatedSubtitleAtomically(
        desiredFileName: String,
        validatedContent: String,
        conflictPolicy: FileConflictPolicy
    ): Result<SavedBatchOutputFile> {
        if (!isContentUri) {
            return LocalDirectoryBatchOutputWriter(resolvedLocalDir)
                .writeValidatedSubtitleAtomically(desiredFileName, validatedContent, conflictPolicy)
        }
        return runCatching {
            require(validatedContent.isNotBlank()) { "Konten terjemahan kosong." }
            val treeUri = Uri.parse(folderUriOrPath)
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)

            val sanitizedName = LocalDirectoryBatchOutputWriter.sanitizeFileName(desiredFileName)
            val existingChildren = listExistingChildNamesToUris(treeUri, treeDocId)
            val existingUri = existingChildren[sanitizedName]

            val finalFileName = if (existingUri == null) {
                sanitizedName
            } else {
                when (conflictPolicy) {
                    FileConflictPolicy.SKIP_IF_EXISTS -> {
                        return@runCatching SavedBatchOutputFile(
                            finalFileName = sanitizedName,
                            finalFilePath = existingUri.toString(),
                            wasSkippedDueToConflict = true
                        )
                    }
                    FileConflictPolicy.OVERWRITE -> sanitizedName
                    FileConflictPolicy.RENAME_WITH_SUFFIX -> {
                        val dotIndex = sanitizedName.lastIndexOf('.')
                        val baseName = if (dotIndex > 0) sanitizedName.substring(0, dotIndex) else sanitizedName
                        val ext = if (dotIndex > 0) sanitizedName.substring(dotIndex) else ""
                        var counter = 1
                        var candidate = "$baseName ($counter)$ext"
                        while (existingChildren.containsKey(candidate)) {
                            counter++
                            candidate = "$baseName ($counter)$ext"
                        }
                        candidate
                    }
                }
            }

            // Step 1: Write to temporary SAF document first
            val tempName = ".${finalFileName}.${UUID.randomUUID()}.tmp"
            val tempDocUri = DocumentsContract.createDocument(
                context.contentResolver,
                parentDocumentUri,
                "application/octet-stream",
                tempName
            ) ?: throw IllegalStateException("Gagal membuat file temporer di folder SAF.")

            try {
                context.contentResolver.openOutputStream(tempDocUri, "wt")?.use { out ->
                    out.write(validatedContent.toByteArray(Charsets.UTF_8))
                    out.flush()
                } ?: throw IllegalStateException("Gagal membuka stream file temporer SAF.")

                // Step 2: Verify temporary file is readable and non-empty
                val verifyText = context.contentResolver.openInputStream(tempDocUri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""
                if (verifyText.isBlank()) {
                    throw IllegalStateException("Verifikasi file temporer SAF gagal (kosong).")
                }

                // Step 3: Remove existing target only if OVERWRITE was explicitly confirmed and temp is verified
                if (conflictPolicy == FileConflictPolicy.OVERWRITE && existingUri != null) {
                    runCatching { DocumentsContract.deleteDocument(context.contentResolver, existingUri) }
                }

                // Step 4: Rename temporary document to final filename (or create final + delete temp)
                val renamedUri = runCatching {
                    DocumentsContract.renameDocument(context.contentResolver, tempDocUri, finalFileName)
                }.getOrNull()

                val finalUri = if (renamedUri != null) {
                    renamedUri
                } else {
                    val createdFinalUri = DocumentsContract.createDocument(
                        context.contentResolver,
                        parentDocumentUri,
                        "text/plain",
                        finalFileName
                    ) ?: throw IllegalStateException("Gagal membuat file akhir '$finalFileName'.")
                    context.contentResolver.openOutputStream(createdFinalUri, "wt")?.use { out ->
                        out.write(validatedContent.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                    runCatching { DocumentsContract.deleteDocument(context.contentResolver, tempDocUri) }
                    createdFinalUri
                }

                SavedBatchOutputFile(
                    finalFileName = finalFileName,
                    finalFilePath = finalUri.toString(),
                    wasSkippedDueToConflict = false
                )
            } catch (e: Exception) {
                runCatching { DocumentsContract.deleteDocument(context.contentResolver, tempDocUri) }
                throw e
            }
        }
    }

    override fun existsAndNonEmpty(outputFilePath: String?): Boolean {
        if (outputFilePath.isNullOrBlank()) return false
        return if (outputFilePath.startsWith("content://")) {
            try {
                val uri = Uri.parse(outputFilePath)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.read() != -1
                } ?: false
            } catch (_: Exception) {
                false
            }
        } else {
            LocalDirectoryBatchOutputWriter(resolvedLocalDir).existsAndNonEmpty(outputFilePath)
        }
    }

    override fun cleanupTempFiles() {
        if (!isContentUri) {
            LocalDirectoryBatchOutputWriter(resolvedLocalDir).cleanupTempFiles()
            return
        }
        runCatching {
            val treeUri = Uri.parse(folderUriOrPath)
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val children = listExistingChildNamesToUris(treeUri, treeDocId)
            children.forEach { (name, uri) ->
                if (name.startsWith(".") && name.endsWith(".tmp")) {
                    runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
                }
            }
        }
    }

    private fun listExistingChildNamesToUris(treeUri: Uri, treeDocId: String): Map<String, Uri> {
        val result = mutableMapOf<String, Uri>()
        runCatching {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            )
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    if (idCol >= 0 && nameCol >= 0) {
                        val childId = cursor.getString(idCol)
                        val name = cursor.getString(nameCol)
                        if (!childId.isNullOrBlank() && !name.isNullOrBlank()) {
                            result[name] = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                        }
                    }
                }
            }
        }
        return result
    }
}
