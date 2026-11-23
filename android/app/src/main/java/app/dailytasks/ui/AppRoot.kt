package app.dailytasks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dailytasks.AppViewModel
import app.dailytasks.AuthUi
import app.dailytasks.core.RepeatEngine
import app.dailytasks.core.Task
import app.dailytasks.ui.screens.CalendarScreen
import app.dailytasks.ui.screens.DetailScreen
import app.dailytasks.ui.screens.EditorScreen
import app.dailytasks.ui.screens.ProgressScreen
import app.dailytasks.ui.screens.SettingsScreen
import app.dailytasks.ui.screens.TasksScreen
import app.dailytasks.ui.screens.TodayScreen
import app.dailytasks.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

/** What is open on top of the tabs. */
sealed interface Overlay {
    data class Edit(val task: Task?, val preset: Task? = null) : Overlay
    data class Detail(val taskId: String) : Overlay
    data object Settings : Overlay
}

@Composable
fun AppRoot(vm: AppViewModel, getIdToken: suspend () -> String) {
    val auth by vm.auth.collectAsStateWithLifecycle()
    val c = LocalAppColors.current
    Box(Modifier.fillMaxSize().background(c.bg)) {
        when (val a = auth) {
            AuthUi.Loading -> Unit
            AuthUi.NoFirebase -> Message(
                "Firebase isn't set up yet",
                "This build was made without google-services.json, so it can't sign in or sync. Add the file from the Firebase console and build again.",
            )
            AuthUi.SignedOut -> SignInScreen(vm, getIdToken)
            is AuthUi.SignedIn -> MainShell(vm)
        }
    }
}

@Composable
private fun Message(title: String, body: String) {
    val c = LocalAppColors.current
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = c.text, textAlign = TextAlign.Center)
        Text(body, color = c.text2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun SignInScreen(vm: AppViewModel, getIdToken: suspend () -> String) {
    val c = LocalAppColors.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).background(c.primary, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
            Icon(appIcon("check", 3f), contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(36.dp))
        }
        Text("Daily Tasks", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = c.text, modifier = Modifier.padding(top = 20.dp))
        Text("Your daily tasks, the same on your phone and on the web.", color = c.text2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 28.dp))
        Button(
            onClick = {
                busy = true
                error = null
                scope.launch {
                    try {
                        val token = getIdToken()
                        vm.repo?.signInWithGoogleIdToken(token)
                    } catch (e: Exception) {
                        error = e.message ?: "Sign-in failed"
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy,
            colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Continue with Google", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
        error?.let { Text(it, color = c.missed, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp)) }
    }
}

private data class TabDef(val key: String, val label: String, val icon: String)

private val TABS = listOf(TabDef("today", "Today", "today"), TabDef("calendar", "Calendar", "calendar"), TabDef("tasks", "Tasks", "list"), TabDef("progress", "Progress", "chart"))

@Composable
private fun MainShell(vm: AppViewModel) {
    val c = LocalAppColors.current
    val data by vm.data.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val today = RepeatEngine.logicalToday(now, data.settings.dayEndsAt)
    var tab by rememberSaveable { mutableStateOf("today") }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    var date by remember { mutableStateOf(today) }
    var lastToday by remember { mutableStateOf(today) }
    if (lastToday != today) {
        // The day rolled over while the app was open: follow it.
        if (date == lastToday) date = today
        lastToday = today
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showUndo: (String, () -> Unit) -> Unit = { text, undo ->
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(text, actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) undo()
        }
    }

    BackHandler(enabled = overlay != null || tab != "today") {
        if (overlay != null) overlay = null else tab = "today"
    }

    when (val o = overlay) {
        is Overlay.Edit -> {
            EditorScreen(
                task = o.task,
                preset = o.preset,
                today = today,
                nextSortOrder = (data.tasks.maxOfOrNull { it.sortOrder } ?: 0) + 1,
                onCancel = { overlay = o.task?.let { Overlay.Detail(it.id) } },
                onSave = {
                    vm.saveTask(it)
                    overlay = null
                },
            )
            return
        }
        is Overlay.Detail -> {
            val task = data.allTasks.firstOrNull { it.id == o.taskId }
            if (task != null) {
                DetailScreen(vm, task, today, onBack = { overlay = null }, onEdit = { overlay = Overlay.Edit(task) })
                return
            }
        }
        Overlay.Settings -> {
            SettingsScreen(vm, onBack = { overlay = null })
            return
        }
        null -> Unit
    }

    Scaffold(
        containerColor = c.bg,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (tab == "today") {
                FloatingActionButton(
                    onClick = { overlay = Overlay.Edit(null) },
                    containerColor = c.primary,
                    contentColor = c.onPrimary,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.size(64.dp),
                ) { Icon(appIcon("plus", 2.2f), contentDescription = "Add task", modifier = Modifier.size(26.dp)) }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = c.surface2) {
                TABS.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t.key,
                        onClick = { tab = t.key },
                        icon = { Icon(appIcon(t.icon), contentDescription = null) },
                        label = { Text(t.label, fontWeight = if (tab == t.key) FontWeight.SemiBold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = c.onPrimaryContainer,
                            indicatorColor = c.primaryContainer,
                            selectedTextColor = c.text,
                            unselectedIconColor = c.text2,
                            unselectedTextColor = c.text2,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            val openSettings = { overlay = Overlay.Settings }
            when (tab) {
                "today" -> TodayScreen(
                    vm = vm,
                    data = data,
                    today = today,
                    date = date,
                    setDate = { date = it },
                    onOpen = { overlay = Overlay.Detail(it.id) },
                    onEdit = { t, preset -> overlay = Overlay.Edit(t, preset) },
                    onUndo = showUndo,
                    onSettings = openSettings,
                )
                "calendar" -> CalendarScreen(
                    data = data,
                    today = today,
                    onOpen = { overlay = Overlay.Detail(it.id) },
                    onOpenDay = {
                        date = it
                        tab = "today"
                    },
                    onSettings = openSettings,
                )
                "tasks" -> TasksScreen(vm, data, today, onOpen = { overlay = Overlay.Detail(it.id) }, onSettings = openSettings)
                "progress" -> ProgressScreen(data, today, onSettings = openSettings)
            }
        }
    }
}
