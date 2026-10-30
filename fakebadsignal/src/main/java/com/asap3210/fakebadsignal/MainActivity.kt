package com.asap3210.fakebadsignal

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

class MainActivity : ComponentActivity() {
    private var permsOk by mutableStateOf(false)
    private var roleOk by mutableStateOf(false)
    private var testEffect: CallEffect? = null

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permsOk = hasPerms()
        }
    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            roleOk = hasRole()
        }

    private fun neededPerms() = buildList {
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun hasPerms() = neededPerms().all {
        checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
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
        content = content
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
                        Text("Fake Bad Signal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Que parezca que no hay cobertura", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (!permsOk || !roleOk) item {
                Panel("Configuración pendiente") {
                    if (!permsOk) Button(
                        onClick = { permLauncher.launch(neededPerms().toTypedArray()) },
                        Modifier.fillMaxWidth()
                    ) { Text("1. Conceder permisos") }
                    if (!roleOk) Button(
                        onClick = {
                            val rm = getSystemService(RoleManager::class.java)
                            roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                        },
                        Modifier.fillMaxWidth()
                    ) { Text("2. Permitir detectar con quién llamas") }
                }
            }

            item {
                Panel(null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Activación automática", fontWeight = FontWeight.Bold)
                            Text(
                                if (auto) "Se activa sola en llamadas con tus contactos"
                                else "Desactivada",
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
                            Text("Contactos afectados", fontWeight = FontWeight.Bold)
                            val names = contacts.filter { it.key in selected }.take(3)
                                .joinToString(", ") { it.name.substringBefore(" ") }
                            Text(
                                if (selected.isEmpty()) "Ninguno elegido todavía"
                                else "${selected.size} elegido(s)" + if (names.isNotEmpty()) ": $names" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item {
                Panel("Efecto en tu voz") {
                    Text("Intensidad: ${(intensity * 100).toInt()}%")
                    Slider(value = intensity, onValueChange = { intensity = it; Prefs.setIntensity(this@MainActivity, it) })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cortes ocasionales", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Casi sin cobertura", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Añadir estática por el altavoz")
                            Text("Solo se oye con la llamada en altavoz", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = noise, onCheckedChange = { noise = it; Prefs.setNoise(this@MainActivity, it) })
                    }
                }
            }

            item {
                Panel("Probar") {
                    Button(
                        enabled = permsOk && !previewBusy,
                        onClick = {
                            previewBusy = true
                            Preview.run(this@MainActivity, intensity, noise) { msg, done ->
                                previewMsg = msg
                                if (done) previewBusy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Escuchar cómo me oirán (8 s)") }
                    if (previewMsg.isNotEmpty()) Text(previewMsg, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(
                        onClick = {
                            if (testing) { testEffect?.stop(); testEffect = null }
                            else testEffect = CallEffect(this@MainActivity).also { it.start() }
                            testing = !testing
                        },
                        Modifier.fillMaxWidth()
                    ) { Text(if (testing) "Detener prueba manual" else "Activar ahora (durante una llamada)") }
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
                TextButton(onClick = onBack) { Text("‹ Volver", fontSize = 16.sp) }
                Spacer(Modifier.weight(1f))
                Text("${selected.size} elegido(s)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Text("Contactos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Con estas personas te oirán entrecortado", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                label = { Text("Buscar por nombre o número") }, singleLine = true,
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.padding(vertical = 8.dp)) {
                FilterChip(selected = onlySelected, onClick = { onlySelected = !onlySelected },
                    label = { Text("Solo elegidos") })
            }
            if (contacts.isEmpty()) {
                Text(
                    if (permsOk) "No se encontraron contactos con número." else "Falta el permiso de contactos (vuelve y concédelo).",
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
