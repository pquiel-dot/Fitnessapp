package de.pquiel.workout.strava

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import de.pquiel.workout.model.WorkoutRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Stand des Strava-Uploads eines Trainings. */
sealed interface StravaUpload {
    data object Pending : StravaUpload
    data class Retrying(val reason: String) : StravaUpload
    data class Done(val activityId: Long) : StravaUpload
    data class Failed(val reason: String) : StravaUpload
}

data class StravaState(
    val clientId: String = "",
    val clientSecret: String = "",
    val connected: Boolean = false,
    val athleteName: String? = null,
    val autoUpload: Boolean = true,
    val busy: Boolean = false,
    /** Letzte Meldung zur Verbindung, z. B. ein Fehler beim Anmelden. */
    val message: String? = null,
    /** Upload-Stand je Training ([WorkoutRecord.id]). */
    val uploads: Map<Long, StravaUpload> = emptyMap(),
) {
    val autoUploadActive: Boolean get() = connected && autoUpload
}

/**
 * Verbindung zu Strava: Anmeldung per OAuth, Token-Verwaltung und Hochladen von Trainings.
 *
 * Client-ID und Client-Secret stammen aus der eigenen API-Anwendung des Nutzers
 * (strava.com/settings/api), damit keine Zugangsdaten im Quelltext oder im APK stehen.
 * Uploads laufen über WorkManager und werden ohne Internet automatisch nachgeholt.
 */
class Strava private constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences("strava", Context.MODE_PRIVATE)
    private val tokenLock = Mutex()

    private val _state = MutableStateFlow(
        StravaState(
            clientId = prefs.getString(KEY_CLIENT_ID, "").orEmpty(),
            clientSecret = prefs.getString(KEY_CLIENT_SECRET, "").orEmpty(),
            connected = prefs.getString(KEY_REFRESH_TOKEN, null) != null,
            athleteName = prefs.getString(KEY_ATHLETE, null),
            autoUpload = prefs.getBoolean(KEY_AUTO_UPLOAD, true),
            uploads = loadUploaded().mapValues { StravaUpload.Done(it.value) },
        )
    )
    val state: StateFlow<StravaState> = _state.asStateFlow()

    /** Speichert die Zugangsdaten und liefert die Adresse der Strava-Anmeldeseite. */
    fun startAuthorization(clientId: String, clientSecret: String): String {
        val id = clientId.trim()
        val secret = clientSecret.trim()
        val oauthState = UUID.randomUUID().toString()
        prefs.edit()
            .putString(KEY_CLIENT_ID, id)
            .putString(KEY_CLIENT_SECRET, secret)
            .putString(KEY_OAUTH_STATE, oauthState)
            .apply()
        _state.update { it.copy(clientId = id, clientSecret = secret, message = null) }
        return StravaAuth.authorizeUrl(id, oauthState)
    }

    fun isRedirect(uri: Uri?): Boolean = uri?.toString()?.startsWith(StravaAuth.REDIRECT_URI) == true

    /** Verarbeitet die Rückleitung von Strava nach der Anmeldung. */
    suspend fun completeAuthorization(uri: Uri) {
        val expected = prefs.getString(KEY_OAUTH_STATE, null)
        val error = uri.getQueryParameter("error")
        val code = uri.getQueryParameter("code")
        val message = when {
            error != null -> "Anmeldung bei Strava abgebrochen"
            expected == null || uri.getQueryParameter("state") != expected -> "Ungültige Antwort von Strava – bitte erneut verbinden"
            code == null -> "Strava hat keinen Anmeldecode geliefert"
            !StravaAuth.canWrite(uri.getQueryParameter("scope")) ->
                "Bitte bei Strava erlauben, dass Aktivitäten hochgeladen werden dürfen"
            else -> null
        }
        prefs.edit().remove(KEY_OAUTH_STATE).apply()
        if (message != null) {
            _state.update { it.copy(message = message) }
            return
        }
        _state.update { it.copy(busy = true, message = null) }
        try {
            val tokens = withContext(Dispatchers.IO) { client().exchangeCode(code!!) }
            saveTokens(tokens)
            _state.update { it.copy(connected = true, athleteName = tokens.athleteName, busy = false) }
        } catch (e: StravaException) {
            _state.update { it.copy(busy = false, message = "Verbindung fehlgeschlagen: ${e.message}") }
        }
    }

    suspend fun disconnect() {
        _state.update { it.copy(busy = true) }
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        // Die Abmeldung bei Strava ist Kür; lokal wird die Verbindung in jedem Fall getrennt.
        if (token != null) withContext(Dispatchers.IO) { runCatching { client().deauthorize(token) } }
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_ATHLETE)
            .apply()
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        _state.update { s ->
            s.copy(
                connected = false,
                athleteName = null,
                busy = false,
                message = null,
                uploads = s.uploads.filterValues { it is StravaUpload.Done },
            )
        }
    }

    fun setAutoUpload(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_UPLOAD, enabled).apply()
        _state.update { it.copy(autoUpload = enabled) }
    }

    /** Plant den Upload eines Trainings; er startet, sobald eine Internetverbindung besteht. */
    fun enqueueUpload(record: WorkoutRecord) {
        if (!_state.value.connected || _state.value.uploads[record.id] is StravaUpload.Done) return
        setUpload(record.id, StravaUpload.Pending)
        val request = OneTimeWorkRequestBuilder<StravaUploadWorker>()
            .setInputData(workDataOf(StravaUploadWorker.KEY_RECORD_ID to record.id))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(WORK_TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(record.id), ExistingWorkPolicy.KEEP, request)
    }

    /** Vergisst den Upload-Stand eines gelöschten Trainings; die Strava-Aktivität bleibt bestehen. */
    fun forget(recordId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(recordId))
        saveUploaded(loadUploaded() - recordId)
        _state.update { it.copy(uploads = it.uploads - recordId) }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    /** Lädt das Training hoch und gibt die ID der neuen Strava-Aktivität zurück. */
    internal suspend fun upload(record: WorkoutRecord): Long = withContext(Dispatchers.IO) {
        loadUploaded()[record.id]?.let { return@withContext it }
        val activity = StravaActivity.of(record)
        val id = try {
            client().createActivity(validAccessToken(forceRefresh = false), activity)
        } catch (e: StravaException) {
            // Ein abgelaufenes oder widerrufenes Token einmal erneuern, dann aufgeben.
            if (!e.unauthorized) throw e
            client().createActivity(validAccessToken(forceRefresh = true), activity)
        }
        saveUploaded(loadUploaded() + (record.id to id))
        setUpload(record.id, StravaUpload.Done(id))
        id
    }

    internal fun setUpload(recordId: Long, upload: StravaUpload) {
        _state.update { it.copy(uploads = it.uploads + (recordId to upload)) }
    }

    private suspend fun validAccessToken(forceRefresh: Boolean): String {
        tokenLock.withLock {
            val current = currentTokens() ?: throw StravaException("Nicht mit Strava verbunden", status = 401)
            if (!forceRefresh && current.expiresAt > System.currentTimeMillis() / 1000 + REFRESH_MARGIN_S) {
                return current.accessToken
            }
            val refreshed = try {
                client().refresh(current)
            } catch (e: StravaException) {
                if (e.retryable) throw e
                throw StravaException("Strava-Anmeldung abgelaufen – bitte neu verbinden", status = 401, cause = e)
            }
            saveTokens(refreshed)
            return refreshed.accessToken
        }
    }

    private fun client(): StravaClient {
        val s = _state.value
        return StravaClient(s.clientId, s.clientSecret)
    }

    private fun currentTokens(): StravaTokens? {
        val access = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refresh = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        return StravaTokens(access, refresh, prefs.getLong(KEY_EXPIRES_AT, 0), prefs.getString(KEY_ATHLETE, null))
    }

    private fun saveTokens(tokens: StravaTokens) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, tokens.accessToken)
            .putString(KEY_REFRESH_TOKEN, tokens.refreshToken)
            .putLong(KEY_EXPIRES_AT, tokens.expiresAt)
            .putString(KEY_ATHLETE, tokens.athleteName)
            .apply()
    }

    /** Hochgeladene Trainings als `recordId:activityId`. */
    @Synchronized
    private fun loadUploaded(): Map<Long, Long> =
        prefs.getStringSet(KEY_UPLOADED, emptySet()).orEmpty().mapNotNull { entry ->
            val (record, activity) = entry.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
            (record.toLongOrNull() ?: return@mapNotNull null) to (activity.toLongOrNull() ?: return@mapNotNull null)
        }.toMap()

    @Synchronized
    private fun saveUploaded(uploaded: Map<Long, Long>) {
        prefs.edit().putStringSet(KEY_UPLOADED, uploaded.map { "${it.key}:${it.value}" }.toSet()).commit()
    }

    companion object {
        private const val WORK_TAG = "strava-upload"
        private const val REFRESH_MARGIN_S = 300
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_CLIENT_SECRET = "client_secret"
        private const val KEY_OAUTH_STATE = "oauth_state"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_ATHLETE = "athlete"
        private const val KEY_AUTO_UPLOAD = "auto_upload"
        private const val KEY_UPLOADED = "uploaded"

        @Volatile
        private var instance: Strava? = null

        fun get(context: Context): Strava =
            instance ?: synchronized(this) {
                instance ?: Strava(context.applicationContext).also { instance = it }
            }

        private fun workName(recordId: Long) = "strava-upload-$recordId"
    }
}
