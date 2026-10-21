package de.pquiel.workout.strava

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.pquiel.workout.HistoryStore

/** Lädt ein gespeichertes Training im Hintergrund zu Strava hoch und versucht es bei Netzproblemen erneut. */
class StravaUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val recordId = inputData.getLong(KEY_RECORD_ID, -1)
        val strava = Strava.get(applicationContext)
        // Inzwischen gelöschte Trainings werden nicht mehr hochgeladen.
        val record = HistoryStore(applicationContext.filesDir).load().firstOrNull { it.id == recordId }
            ?: return Result.success()
        return try {
            strava.upload(record)
            Result.success()
        } catch (e: StravaException) {
            val reason = e.message ?: "Unbekannter Fehler"
            if (e.retryable && runAttemptCount < MAX_ATTEMPTS) {
                strava.setUpload(recordId, StravaUpload.Retrying("$reason – wird erneut versucht"))
                Result.retry()
            } else {
                strava.setUpload(recordId, StravaUpload.Failed(reason))
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_RECORD_ID = "record_id"
        private const val MAX_ATTEMPTS = 10
    }
}
