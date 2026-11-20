package app.dailytasks.data

import android.content.Context
import app.dailytasks.core.Completion
import app.dailytasks.core.RepeatEngine
import app.dailytasks.core.Settings
import app.dailytasks.core.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Data lives at users/{uid}/tasks, users/{uid}/completions and
// users/{uid}/settings/main, the same paths the web app uses. Firestore keeps
// an on-disk cache on Android, so everything works offline and syncs later.

enum class SyncState { SYNCED, SYNCING, OFFLINE }

data class Snapshot<T>(val value: T, val pending: Boolean, val fromCache: Boolean)

class Repository private constructor(private val auth: FirebaseAuth, private val db: FirebaseFirestore) {

    companion object {
        /** How far back completions are loaded (enough for a year of history). */
        const val HISTORY_DAYS = 400L

        /** Null when google-services.json was missing at build time. */
        fun create(context: Context): Repository? {
            val app = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context) ?: return null
            return Repository(FirebaseAuth.getInstance(app), FirebaseFirestore.getInstance(app))
        }
    }

    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val l = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(l)
        awaitClose { auth.removeAuthStateListener(l) }
    }

    suspend fun signInWithGoogleIdToken(idToken: String) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    fun signOut() = auth.signOut()

    private fun base(uid: String) = db.collection("users").document(uid)

    fun tasks(uid: String): Flow<Snapshot<List<Task>>> = callbackFlow {
        val reg: ListenerRegistration = base(uid).collection("tasks")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val list = snap.documents
                    .filter { it.getBoolean("deleted") != true }
                    .map { taskFromDoc(it.id, it.data ?: emptyMap()) }
                trySend(Snapshot(list, snap.metadata.hasPendingWrites(), snap.metadata.isFromCache))
            }
        awaitClose { reg.remove() }
    }

    fun completions(uid: String): Flow<Snapshot<List<Completion>>> = callbackFlow {
        val since = RepeatEngine.logicalToday().minusDays(HISTORY_DAYS).toString()
        val reg = base(uid).collection("completions")
            .whereGreaterThanOrEqualTo("date", since)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val list = snap.documents.mapNotNull { completionFromDoc(it.id, it.data ?: emptyMap()) }
                trySend(Snapshot(list, snap.metadata.hasPendingWrites(), snap.metadata.isFromCache))
            }
        awaitClose { reg.remove() }
    }

    fun settings(uid: String): Flow<Settings> = callbackFlow {
        val reg = base(uid).collection("settings").document("main").addSnapshotListener { snap, _ ->
            trySend(settingsFromDoc(snap?.data))
        }
        awaitClose { reg.remove() }
    }

    // Writes go to the local cache at once and to the server when online;
    // we never wait for the server, the listeners show pending state.

    fun saveTask(uid: String, t: Task) {
        base(uid).collection("tasks").document(t.id)
            .set(taskToDoc(t) + mapOf("updatedAt" to FieldValue.serverTimestamp(), "deleted" to false))
    }

    /** Soft delete, so an offline device learns about it. */
    fun deleteTask(uid: String, id: String) {
        base(uid).collection("tasks").document(id)
            .set(mapOf("deleted" to true, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
    }

    fun saveCompletion(uid: String, c: Completion) {
        base(uid).collection("completions").document(c.id)
            .set(completionToDoc(c) + mapOf("updatedAt" to FieldValue.serverTimestamp()))
    }

    fun removeCompletion(uid: String, id: String) {
        base(uid).collection("completions").document(id).delete()
    }

    fun saveSettings(uid: String, s: Settings) {
        base(uid).collection("settings").document("main")
            .set(settingsToDoc(s) + mapOf("updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
    }
}
