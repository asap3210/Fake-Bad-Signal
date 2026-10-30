package com.asap3210.backgroundsounds

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("backgroundsounds", Context.MODE_PRIVATE) }

    private val notifLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF2DD4BF), onPrimary = Color(0xFF08262B),
                    secondaryContainer = Color(0xFF1C5560), onSecondaryContainer = Color.White,
                    background = Color(0xFF0B1F24), onBackground = Color(0xFFEAFBFA),
                    surface = Color(0xFF0B1F24), onSurface = Color(0xFFEAFBFA),
                    surfaceVariant = Color(0xFF12343B), onSurfaceVariant = Color(0xFFA9CFD0)
                )
            ) { Screen() }
        }
    }

    private fun send(action: String, kind: Ambient? = null, vol: Float = 0.6f, minutes: Int = 0) {
        val i = Intent(this, SoundService::class.java).setAction(action)
            .putExtra(SoundService.EXTRA_VOLUME, vol)
            .putExtra(SoundService.EXTRA_MINUTES, minutes)
        if (kind != null) i.putExtra(SoundService.EXTRA_KIND, kind.name)
        if (action == SoundService.ACTION_PLAY) ContextCompat.startForegroundService(this, i)
        else startService(i)
    }

    @Composable
    private fun Screen() {
        var volume by remember { mutableFloatStateOf(prefs.getFloat("vol", 0.6f)) }
        var minutes by remember { mutableIntStateOf(prefs.getInt("min", 0)) }
        val playing = PlayerState.playing

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🎧", fontSize = 44.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Background Sounds", style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold)
                            Text("Sonidos de ambiente en bucle", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                if (playing != null) "Sonando: ${playing.emoji} ${playing.label}" else "Elige un sonido",
                                fontWeight = FontWeight.Bold
                            )
                            Text("Volumen: ${(volume * 100).toInt()}%")
                            Slider(value = volume, onValueChange = {
                                volume = it
                                prefs.edit().putFloat("vol", it).apply()
                                if (playing != null) send(SoundService.ACTION_VOLUME, vol = it)
                            })
                            Text("Apagar automáticamente", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(0 to "No", 15 to "15 min", 30 to "30 min", 60 to "1 h").forEach { (m, label) ->
                                    FilterChip(selected = minutes == m, onClick = {
                                        minutes = m
                                        prefs.edit().putInt("min", m).apply()
                                        if (playing != null) send(SoundService.ACTION_PLAY, playing, volume, m)
                                    }, label = { Text(label) })
                                }
                            }
                        }
                    }
                }

                items(Ambient.values().filter { it != Ambient.NONE }) { a ->
                    val on = playing == a
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .background(if (on) Color(0xFF1C6B73) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                if (on) send(SoundService.ACTION_STOP)
                                else send(SoundService.ACTION_PLAY, a, volume, minutes)
                            }
                            .padding(vertical = 14.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(a.emoji, fontSize = 30.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(a.label, fontSize = 13.sp, textAlign = TextAlign.Center,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { send(SoundService.ACTION_STOP) },
                            enabled = playing != null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("■ Detener") }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
