package de.pquiel.workout

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import de.pquiel.workout.model.HistoryFile
import de.pquiel.workout.model.WorkoutRecord
import java.io.FileNotFoundException

/**
 * Sicherungsdatei des Verlaufs außerhalb des App-Speichers, z. B. im Ordner „Download“ oder
 * „Documents“ des Handys. Die Datei wird über die Android-Dateiauswahl gewählt und bleibt bei
 * einer Deinstallation der App erhalten, sodass eine neue Version den Verlauf wieder einlesen kann.
 */
class HistoryBackup(context: Context) {
    private val resolver = context.contentResolver
    private val prefs = context.getSharedPreferences("history_backup", Context.MODE_PRIVATE)

    /** Die verbundene Datei, sofern die Zugriffsberechtigung noch besteht. */
    val uri: Uri?
        get() {
            val uri = prefs.getString(KEY_URI, null)?.let(Uri::parse) ?: return null
            val granted = resolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission }
            return if (granted) uri else null
        }

    /** Merkt sich [uri] dauerhaft (auch über Neustarts des Handys hinweg). */
    fun link(uri: Uri) {
        resolver.takePersistableUriPermission(uri, PERMISSIONS)
        val old = prefs.getString(KEY_URI, null)?.let(Uri::parse)
        if (old != null && old != uri) release(old)
        prefs.edit().putString(KEY_URI, uri.toString()).apply()
    }

    fun unlink() {
        prefs.getString(KEY_URI, null)?.let(Uri::parse)?.let(::release)
        prefs.edit().remove(KEY_URI).apply()
    }

    fun displayName(uri: Uri): String =
        try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (e: Exception) {
            null
        } ?: uri.lastPathSegment ?: HistoryFile.DEFAULT_NAME

    /** Liest die Trainings aus [uri]; null, wenn die Datei kein Verlauf der App ist. */
    fun read(uri: Uri): List<WorkoutRecord>? {
        val text = resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: throw FileNotFoundException(uri.toString())
        return HistoryFile.decode(text)
    }

    fun write(uri: Uri, records: List<WorkoutRecord>) {
        val bytes = HistoryFile.encode(records).encodeToByteArray()
        // "wt" kürzt die Datei vorher; nicht jeder Anbieter kennt den Modus.
        val out = try {
            resolver.openOutputStream(uri, "wt")
        } catch (e: IllegalArgumentException) {
            resolver.openOutputStream(uri, "w")
        } ?: throw FileNotFoundException(uri.toString())
        out.use { it.write(bytes) }
    }

    private fun release(uri: Uri) {
        try {
            resolver.releasePersistableUriPermission(uri, PERMISSIONS)
        } catch (e: SecurityException) {
            // Berechtigung bestand schon nicht mehr.
        }
    }

    companion object {
        private const val KEY_URI = "uri"
        private const val PERMISSIONS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}
