package de.pquiel.workout.strava

import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Zugangstoken von Strava; [expiresAt] in Epoch-Sekunden. */
data class StravaTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val athleteName: String?,
)

/**
 * Fehler beim Aufruf der Strava-API. [retryable] heißt: später erneut versuchen lohnt sich
 * (kein Netz, Strava überlastet oder Ratenlimit erreicht).
 */
class StravaException(
    message: String,
    val status: Int = 0,
    val retryable: Boolean = false,
    cause: Throwable? = null,
) : Exception(message, cause) {
    val unauthorized: Boolean get() = status == 401
}

/** Minimaler Client für die benötigten Strava-Endpunkte, ohne zusätzliche Bibliotheken. */
class StravaClient(private val clientId: String, private val clientSecret: String) {

    fun exchangeCode(code: String): StravaTokens =
        tokens(post(TOKEN_URL, oauthParams("grant_type" to "authorization_code", "code" to code)), previous = null)

    fun refresh(current: StravaTokens): StravaTokens =
        tokens(
            post(TOKEN_URL, oauthParams("grant_type" to "refresh_token", "refresh_token" to current.refreshToken)),
            previous = current,
        )

    /** Legt die Aktivität an und gibt ihre Strava-ID zurück. */
    fun createActivity(accessToken: String, activity: StravaActivity): Long {
        val id = post("$API_URL/activities", activity.formParams(), accessToken).optLong("id", 0L)
        if (id == 0L) throw StravaException("Unerwartete Antwort von Strava")
        return id
    }

    fun deauthorize(accessToken: String) {
        post(DEAUTHORIZE_URL, mapOf("access_token" to accessToken))
    }

    private fun oauthParams(vararg extra: Pair<String, String>): Map<String, String> =
        linkedMapOf("client_id" to clientId, "client_secret" to clientSecret) + extra

    private fun tokens(json: JSONObject, previous: StravaTokens?): StravaTokens {
        val athlete = json.optJSONObject("athlete")
        val name = athlete?.let {
            listOf(it.optString("firstname"), it.optString("lastname")).filter(String::isNotBlank).joinToString(" ")
        }?.takeIf { it.isNotBlank() }
        return try {
            StravaTokens(
                accessToken = json.getString("access_token"),
                refreshToken = json.getString("refresh_token"),
                expiresAt = json.getLong("expires_at"),
                athleteName = name ?: previous?.athleteName,
            )
        } catch (e: JSONException) {
            throw StravaException("Unerwartete Antwort von Strava", cause = e)
        }
    }

    private fun post(url: String, params: Map<String, String>, accessToken: String? = null): JSONObject {
        val connection = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw StravaException("Keine Verbindung zu Strava", retryable = true, cause = e)
        }
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            accessToken?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            connection.outputStream.use { it.write(StravaAuth.formEncode(params).encodeToByteArray()) }

            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.use { it.readBytes().decodeToString() }
                .orEmpty()
            if (status in 200..299) return if (body.isBlank()) JSONObject() else JSONObject(body)
            throw StravaException(
                message = errorMessage(status, body),
                status = status,
                retryable = status == 429 || status >= 500,
            )
        } catch (e: IOException) {
            throw StravaException("Keine Verbindung zu Strava", retryable = true, cause = e)
        } catch (e: JSONException) {
            throw StravaException("Unerwartete Antwort von Strava", cause = e)
        } finally {
            connection.disconnect()
        }
    }

    private fun errorMessage(status: Int, body: String): String {
        val detail = runCatching {
            val json = JSONObject(body)
            val errors = json.optJSONArray("errors")
            val first = errors?.optJSONObject(0)
            listOfNotNull(
                json.optString("message").takeIf { it.isNotBlank() },
                first?.let { "${it.optString("field")} ${it.optString("code")}".trim() }?.takeIf { it.isNotBlank() },
            ).joinToString(": ")
        }.getOrNull().orEmpty()
        val base = when (status) {
            401 -> "Strava hat den Zugriff abgelehnt"
            403 -> "Keine Berechtigung bei Strava"
            429 -> "Strava-Ratenlimit erreicht"
            in 500..599 -> "Strava ist gerade nicht erreichbar"
            else -> "Strava-Fehler $status"
        }
        return if (detail.isEmpty()) base else "$base ($detail)"
    }

    private companion object {
        const val TOKEN_URL = "https://www.strava.com/oauth/token"
        const val DEAUTHORIZE_URL = "https://www.strava.com/oauth/deauthorize"
        const val API_URL = "https://www.strava.com/api/v3"
        const val TIMEOUT_MS = 20_000
    }
}
