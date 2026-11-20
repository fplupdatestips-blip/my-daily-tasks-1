package app.dailytasks

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.dailytasks.core.Completion
import app.dailytasks.core.CompletionStatus
import app.dailytasks.core.RepeatEngine
import app.dailytasks.core.RepeatType
import app.dailytasks.core.Settings
import app.dailytasks.core.Task
import app.dailytasks.core.TodayItem
import app.dailytasks.core.completionId
import app.dailytasks.data.Repository
import app.dailytasks.data.SyncState
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DataState(
    val tasks: List<Task> = emptyList(),
    val completions: List<Completion> = emptyList(),
    val settings: Settings = Settings(),
    val sync: SyncState = SyncState.SYNCED,
)

sealed interface AuthUi {
    data object Loading : AuthUi
    data object NoFirebase : AuthUi
    data object SignedOut : AuthUi
    data class SignedIn(val user: FirebaseUser) : AuthUi
}

/** Theme and dynamic color are per device, so they live in SharedPreferences. */
data class DevicePrefs(val theme: String = "system", val dynamicColor: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    val repo: Repository? = Repository.create(app)
    private val prefs = app.getSharedPreferences("device", Context.MODE_PRIVATE)
    private val deviceId: String = prefs.getString("deviceId", null) ?: ("android-" + UUID.randomUUID().toString().take(8)).also {
        prefs.edit().putString("deviceId", it).apply()
    }

    private val _prefs = MutableStateFlow(DevicePrefs(prefs.getString("theme", "system") ?: "system", prefs.getBoolean("dynamicColor", false)))
    val devicePrefs: StateFlow<DevicePrefs> = _prefs.asStateFlow()

    fun setTheme(t: String) {
        prefs.edit().putString("theme", t).apply()
        _prefs.value = _prefs.value.copy(theme = t)
    }

    fun setDynamicColor(on: Boolean) {
        prefs.edit().putBoolean("dynamicColor", on).apply()
        _prefs.value = _prefs.value.copy(dynamicColor = on)
    }

    val auth: StateFlow<AuthUi> = (repo?.authState()?.map<FirebaseUser?, AuthUi> { u -> if (u == null) AuthUi.SignedOut else AuthUi.SignedIn(u) } ?: flowOf<AuthUi>(AuthUi.NoFirebase))
        .stateIn(viewModelScope, SharingStarted.Eagerly, if (repo == null) AuthUi.NoFirebase else AuthUi.Loading)

    private val online: Flow<Boolean> = callbackFlow {
        val cm = app.getSystemService(ConnectivityManager::class.java)
        fun current() = cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(current())
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(current()) }
        }
        cm.registerDefaultNetworkCallback(cb)
        awaitClose { cm.unregisterNetworkCallback(cb) }
    }

    val data: StateFlow<DataState> = auth.flatMapLatest { a ->
        val r = repo
        if (a !is AuthUi.SignedIn || r == null) {
            flowOf(DataState())
        } else {
            val uid = a.user.uid
            combine(r.tasks(uid), r.completions(uid), r.settings(uid), online) { t, c, s, on ->
                val sync = when {
                    !on -> SyncState.OFFLINE
                    t.pending || c.pending || t.fromCache -> SyncState.SYNCING
                    else -> SyncState.SYNCED
                }
                DataState(t.value, c.value, s, sync)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DataState())

    /** Ticks every 30 s so the list rolls over to a new day by itself. */
    val now: StateFlow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(30_000)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LocalDateTime.now())

    fun today(): LocalDate = RepeatEngine.logicalToday(now.value, data.value.settings.dayEndsAt)

    private val uid: String? get() = (auth.value as? AuthUi.SignedIn)?.user?.uid

    // ---- Writes ----

    fun saveTask(t: Task) { uid?.let { repo?.saveTask(it, t) } }
    fun deleteTask(id: String) { uid?.let { repo?.deleteTask(it, id) } }
    fun saveSettings(s: Settings) { uid?.let { repo?.saveSettings(it, s) } }
    fun signOut() = repo?.signOut()

    private fun saveCompletion(c: Completion) { uid?.let { repo?.saveCompletion(it, c) } }
    private fun removeCompletion(id: String) { uid?.let { repo?.removeCompletion(it, id) } }

    private fun completion(t: Task, date: LocalDate, status: CompletionStatus, subs: List<String>) =
        Completion(completionId(t.id, date), t.id, date, status, subs, System.currentTimeMillis(), deviceId)

    /** Remembers the current state so the change can be undone exactly. */
    private fun snapshot(task: Task, date: LocalDate): () -> Unit {
        val id = completionId(task.id, date)
        val prevC = data.value.completions.firstOrNull { it.id == id }
        val prevTask = data.value.tasks.firstOrNull { it.id == task.id }
        return {
            if (prevC != null) saveCompletion(prevC) else removeCompletion(id)
            if (prevTask != null && task.repeat.type == RepeatType.NONE) saveTask(prevTask)
        }
    }

    fun toggleDone(item: TodayItem, date: LocalDate): () -> Unit {
        val undo = snapshot(item.task, date)
        val t = item.task
        val oneOff = t.repeat.type == RepeatType.NONE
        if (item.done) {
            removeCompletion(completionId(t.id, date))
            if (oneOff && t.subtasks.isNotEmpty()) saveTask(t.copy(subtasks = t.subtasks.map { it.copy(done = false) }))
        } else {
            saveCompletion(completion(t, date, CompletionStatus.DONE, t.subtasks.map { it.id }))
            if (oneOff && t.subtasks.isNotEmpty()) saveTask(t.copy(subtasks = t.subtasks.map { it.copy(done = true) }))
        }
        return undo
    }

    fun skip(item: TodayItem, date: LocalDate): () -> Unit {
        val undo = snapshot(item.task, date)
        if (item.skipped) removeCompletion(completionId(item.task.id, date))
        else saveCompletion(completion(item.task, date, CompletionStatus.SKIPPED, item.subtasksDone))
        return undo
    }

    /** Ticking the last subtask completes the task. Returns (undo, completedTask). */
    fun toggleSubtask(item: TodayItem, subId: String, date: LocalDate): Pair<() -> Unit, Boolean> {
        val undo = snapshot(item.task, date)
        val t = item.task
        val set = item.subtasksDone.toMutableSet()
        if (!set.add(subId)) set.remove(subId)
        val all = t.subtasks.isNotEmpty() && t.subtasks.all { it.id in set }
        val ids = t.subtasks.filter { it.id in set }.map { it.id }
        if (t.repeat.type == RepeatType.NONE) {
            saveTask(t.copy(subtasks = t.subtasks.map { it.copy(done = it.id in set) }))
            if (all) saveCompletion(completion(t, date, CompletionStatus.DONE, ids))
            else if (item.done) removeCompletion(completionId(t.id, date))
        } else if (all) {
            saveCompletion(completion(t, date, CompletionStatus.DONE, ids))
        } else if (ids.isNotEmpty()) {
            saveCompletion(completion(t, date, CompletionStatus.PARTIAL, ids))
        } else {
            removeCompletion(completionId(t.id, date))
        }
        return undo to (all && !item.done)
    }

    /** Backfill from the calendar. */
    fun toggleDay(task: Task, date: LocalDate) {
        val id = completionId(task.id, date)
        val c = data.value.completions.firstOrNull { it.id == id }
        if (c?.status == CompletionStatus.DONE) removeCompletion(id)
        else saveCompletion(completion(task, date, CompletionStatus.DONE, task.subtasks.map { it.id }))
    }
}
