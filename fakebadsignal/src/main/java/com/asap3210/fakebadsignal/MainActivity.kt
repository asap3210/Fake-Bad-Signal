package com.asap3210.fakebadsignal

import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val PRIVACY_URL = "https://github.com/asap3210/Fake-Bad-Signal/blob/main/PRIVACY.md"

class MainActivity : ComponentActivity() {
    private var permsOk by mutableStateOf(false)
    private var roleOk by mutableStateOf(false)
    private var testEffect: CallEffect? = null

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permsOk = hasPerms()
        }
    private var dialog by mutableStateOf<String?>(null)
    private var pendingAfterMic: (() -> Unit)? = null
    private val micLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            if (ok) pendingAfterMic?.invoke()
            pendingAfterMic = null
        }
    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            roleOk = hasRole()
        }

    private fun neededPerms() = buildList {
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun hasPerms() = neededPerms().all {
        checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasMic() =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun openPrivacy() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
    }

    private fun hasRole() =
        getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permsOk = hasPerms()
        roleOk = hasRole()
        setContent { AppTheme { App() } }
    }

    override fun onDestroy() {
        testEffect?.stop()
        super.onDestroy()
    }

    // ---------- Tema ----------

    @Composable
    private fun AppTheme(content: @Composable () -> Unit) = MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFF59E0B), onPrimary = Color(0xFF2B1B5E),
            secondary = Color(0xFFA78BFA), onSecondary = Color(0xFF14102B),
            secondaryContainer = Color(0xFF4B3A8F), onSecondaryContainer = Color.White,
            background = Color(0xFF14102B), onBackground = Color(0xFFF4F1FF),
            surface = Color(0xFF14102B), onSurface = Color(0xFFF4F1FF),
            surfaceVariant = Color(0xFF221B4A), onSurfaceVariant = Color(0xFFC9C2EA),
            outline = Color(0xFF6B5FB0)
        ),
        content = {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                content()
            }
        }
    )

    @Composable
    private fun Panel(title: String?, content: @Composable ColumnScope.() -> Unit) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                content()
            }
        }
    }

    // ---------- Navegación ----------

    @Composable
    private fun App() {
        var screen by remember { mutableStateOf("home") }
        var contacts by remember { mutableStateOf(emptyList<Contact>()) }
        var selected by remember { mutableStateOf(Prefs.selected(this)) }
        BackHandler(enabled = screen != "home") { screen = "home" }
        LaunchedEffect(permsOk) { if (permsOk) contacts = Contacts.load(this@MainActivity) }

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
            when (screen) {
                "contacts" -> ContactsScreen(
                    contacts, selected,
                    onToggle = { key ->
                        selected = if (key in selected) selected - key else selected + key
                        Prefs.setSelected(this@MainActivity, selected)
                    },
                    onBack = { screen = "home" }
                )
                else -> HomeScreen(contacts, selected, onOpenContacts = { screen = "contacts" })
            }
        }

        // Divulgación destacada ANTES de pedir cada permiso sensible
        when (dialog) {
            "core" -> AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(stringResource(R.string.perm_title)) },
                text = { Text(stringResource(R.string.perm_core_body), modifier = Modifier.verticalScroll(rememberScrollState())) },
                confirmButton = {
                    TextButton(onClick = { dialog = null; permLauncher.launch(neededPerms().toTypedArray()) }) {
                        Text(stringResource(R.string.perm_continue))
                    }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.perm_not_now)) } }
            )
            "mic" -> AlertDialog(
                onDismissRequest = { dialog = null; pendingAfterMic = null },
                title = { Text(stringResource(R.string.perm_mic_title)) },
                text = { Text(stringResource(R.string.perm_mic_body)) },
                confirmButton = {
                    TextButton(onClick = { dialog = null; micLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                        Text(stringResource(R.string.perm_continue))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialog = null; pendingAfterMic = null }) {
                        Text(stringResource(R.string.perm_not_now))
                    }
                }
            )
        }
    }

    // ---------- Pantalla principal ----------

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun HomeScreen(contacts: List<Contact>, selected: Set<String>, onOpenContacts: () -> Unit) {
        var auto by remember { mutableStateOf(Prefs.auto(this)) }
        var noise by remember { mutableStateOf(Prefs.noise(this)) }
        var intensity by remember { mutableFloatStateOf(Prefs.intensity(this)) }
        var testing by remember { mutableStateOf(false) }
        var previewMsg by remember { mutableStateOf("") }
        var previewBusy by remember { mutableStateOf(false) }

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("👻", fontSize = 44.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (!permsOk || !roleOk) item {
                Panel(stringResource(R.string.setup_pending)) {
                    if (!permsOk) Button(
                        onClick = { dialog = "core" },
                        Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.setup_permissions)) }
                    if (!roleOk) Button(
                        onClick = {
                            val rm = getSystemService(RoleManager::class.java)
                            roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                        },
                        Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.setup_role)) }
                }
            }

            item {
                Panel(null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.auto_title), fontWeight = FontWeight.Bold)
                            Text(
                                if (auto) stringResource(R.string.auto_on)
                                else stringResource(R.string.auto_off),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = auto, onCheckedChange = { auto = it; Prefs.setAuto(this@MainActivity, it) })
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenContacts() }
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("👥", fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.contacts_card_title), fontWeight = FontWeight.Bold)
                            val names = contacts.filter { it.key in selected }.take(3)
                                .joinToString(", ") { it.name.substringBefore(" ") }
                            Text(
                                if (selected.isEmpty()) stringResource(R.string.contacts_none_yet)
                                else stringResource(R.string.selected_n, selected.size) + if (names.isNotEmpty()) ": $names" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item {
                Panel(stringResource(R.string.effect_title)) {
                    Text(stringResource(R.string.intensity_fmt, (intensity * 100).toInt()))
                    Slider(value = intensity, onValueChange = { intensity = it; Prefs.setIntensity(this@MainActivity, it) })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.cuts_occasional), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.cuts_heavy), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.static_title))
                            Text(stringResource(R.string.static_sub), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = noise, onCheckedChange = { noise = it; Prefs.setNoise(this@MainActivity, it) })
                    }
                }
            }

            item {
                Panel(stringResource(R.string.test_title)) {
                    Button(
                        enabled = !previewBusy,
                        onClick = {
                            val go = {
                                previewBusy = true
                                Preview.run(this@MainActivity, intensity, noise) { msg, done ->
                                    previewMsg = msg
                                    if (done) previewBusy = false
                                }
                            }
                            if (hasMic()) go() else { pendingAfterMic = go; dialog = "mic" }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.preview_btn)) }
                    if (previewMsg.isNotEmpty()) Text(previewMsg, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(
                        onClick = {
                            if (testing) { testEffect?.stop(); testEffect = null }
                            else testEffect = CallEffect(this@MainActivity).also { it.start() }
                            testing = !testing
                        },
                        Modifier.fillMaxWidth()
                    ) { Text(if (testing) stringResource(R.string.manual_stop) else stringResource(R.string.manual_start)) }
                }
            }

            item {
                TextButton(onClick = { openPrivacy() }, Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.privacy_policy))
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // ---------- Pantalla de contactos ----------

    @Composable
    private fun ContactsScreen(
        contacts: List<Contact>, selected: Set<String>,
        onToggle: (String) -> Unit, onBack: () -> Unit
    ) {
        var query by remember { mutableStateOf("") }
        var onlySelected by remember { mutableStateOf(false) }
        val shown = contacts.filter {
            (!onlySelected || it.key in selected) &&
                (query.isBlank() || it.name.contains(query, true) || it.number.contains(query))
        }
        val palette = listOf(0xFFF59E0B, 0xFFA78BFA, 0xFF34D399, 0xFFF472B6, 0xFF60A5FA, 0xFFFB7185)

        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back), fontSize = 16.sp) }
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.selected_n, selected.size), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Text(stringResource(R.string.contacts_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.contacts_subtitle), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                label = { Text(stringResource(R.string.search_hint)) }, singleLine = true,
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.padding(vertical = 8.dp)) {
                FilterChip(selected = onlySelected, onClick = { onlySelected = !onlySelected },
                    label = { Text(stringResource(R.string.only_selected)) })
            }
            if (contacts.isEmpty()) {
                Text(
                    if (permsOk) stringResource(R.string.no_contacts) else stringResource(R.string.no_permission),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(shown, key = { it.key }) { c ->
                    val on = c.key in selected
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (on) Color(0xFF3A2F7A) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onToggle(c.key) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(42.dp).clip(CircleShape)
                                .background(Color(palette[(c.name.hashCode() and 0x7fffffff) % palette.size])),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(c.name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?",
                                color = Color(0xFF14102B), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, fontWeight = FontWeight.Medium)
                            Text(c.number, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Checkbox(checked = on, onCheckedChange = null)
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
