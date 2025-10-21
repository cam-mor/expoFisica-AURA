package com.roamoralesgonzalez.aura

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.roamoralesgonzalez.aura.ui.theme.AURATheme
import androidx.compose.animation.core.*
import com.roamoralesgonzalez.aura.service.SensorDataManager
import com.roamoralesgonzalez.aura.service.SensorMonitoringService
import com.roamoralesgonzalez.aura.services.FloatingBubbleService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.roamoralesgonzalez.aura.ui.screens.SettingsScreen
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Arrangement.Center
import com.roamoralesgonzalez.aura.model.ConfiguracionAlerta
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val _magneticStrength = MutableStateFlow(0f)
    val magneticStrength: StateFlow<Float> = _magneticStrength

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Settings.canDrawOverlays(this)) {
            startFloatingBubble()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkOverlayPermission()
        setContent {
            AURATheme {
                MainContent(
                    onStartMonitoring = { startMonitoring() },
                    onStopMonitoring = { stopMonitoring() }
                )
            }
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            startFloatingBubble()
        }
    }

    private fun startFloatingBubble() {
        val serviceIntent = Intent(this, FloatingBubbleService::class.java)
        startService(serviceIntent)
    }

    private fun startMonitoring() {
        val serviceIntent = Intent(this, SensorMonitoringService::class.java)
        startService(serviceIntent)
    }

    private fun stopMonitoring() {
        val serviceIntent = Intent(this, SensorMonitoringService::class.java)
        stopService(serviceIntent)
    }
}

@Composable
private fun MainContent(
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("main") }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            "main" -> MainScreen(
                onStartMonitoring = onStartMonitoring,
                onStopMonitoring = onStopMonitoring,
                onSettingsClick = { currentScreen = "settings" }
            )
            "settings" -> SettingsScreen(
                onNavigateBack = { currentScreen = "main" }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onStartMonitoring: () -> Unit = {},
    onStopMonitoring: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    var isMonitoring by remember { mutableStateOf(false) }
    var magneticStrength by remember { mutableStateOf(0f) }
    var warningLevel by remember { mutableStateOf(0) }
    val config = remember { mutableStateOf(ConfiguracionAlerta()) }


    // Observar los cambios del magnetómetro
    LaunchedEffect(Unit) {
        SensorDataManager.magneticStrength.collect { strength ->
            magneticStrength = strength
        }
    }

    // Actualizar el nivel de advertencia basado en la intensidad
    LaunchedEffect(magneticStrength) {
        warningLevel = when {
            magneticStrength > 200f -> 2  // Nivel peligroso
            magneticStrength > 50f -> 1  // Nivel de precaución
            else -> 0                     // Nivel seguro
        }
    }
    var tiempoContacto by remember { mutableStateOf(0L) }
    var alertaNivel by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AURA",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = FontFamily.Serif
                        ),
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Configuración",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF201B43)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Indicador visual
            MagneticFieldIndicator(
                strength = magneticStrength,
                warningLevel = warningLevel,
                modifier = Modifier
                    .size(300.dp)  // Tamaño reducido del círculo
                    .padding(top = 32.dp)  // Espacio adicional arriba
            )
    LaunchedEffect(isMonitoring, warningLevel) {
        if (isMonitoring && warningLevel > 0) {
            while (isMonitoring && warningLevel > 0) {
                delay(1000L)
                tiempoContacto += 1000L

                when {
                    tiempoContacto >= config.value.tiempoNivel3 && alertaNivel < 3 -> {
                        alertaNivel = 3
                        enviarAlerta(3)
                        aplicarAcciones(config.value)
                    }
                    tiempoContacto >= config.value.tiempoNivel2 && alertaNivel < 2 -> {
                        alertaNivel = 2
                        enviarAlerta(2)
                    }
                    tiempoContacto >= config.value.tiempoNivel1 && alertaNivel < 1 -> {
                        alertaNivel = 1
                        enviarAlerta(1)
                    }
                }
            }
        } else {
            tiempoContacto = 0L
            alertaNivel = 0
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Título
        Text(
            text = "AURA",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        // Indicador visual
        MagneticFieldIndicator(
            strength = magneticStrength,
            warningLevel = warningLevel,
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
        )

            // Estado actual
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)  // Ajuste del padding vertical
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Estado del sensor:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isMonitoring) "Monitorizando" else "Detenido",
                        color = if (isMonitoring) Color.Green else Color.Red
                    )
                    Text(
                        text = "Intensidad: %.2f µT".format(magneticStrength),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Nivel de advertencia: $warningLevel",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Botón centrado
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp),  // Espacio adicional abajo
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        isMonitoring = !isMonitoring
                        if (isMonitoring) {
                            onStartMonitoring()
                        } else {
                            onStopMonitoring()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMonitoring) Color.Red else Color.Green
                    )
                ) {
                    Text(if (isMonitoring) "Detener" else "Iniciar")
                }
            }
        }
    }


}

@Composable
fun MagneticFieldIndicator(
    strength: Float,
    warningLevel: Int,
    modifier: Modifier = Modifier
) {
    val animatedStrength by animateFloatAsState(
        targetValue = strength,
        animationSpec = tween(durationMillis = 500)
    )

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val radius = minOf(canvasWidth, canvasHeight) / 2

        // Dibujar círculo base
        drawCircle(
            color = Color.LightGray.copy(alpha = 0.3f),
            radius = radius,
            center = Offset(canvasWidth / 2, canvasHeight / 2)
        )

        // Dibujar indicador de intensidad
        val sweepAngle = (animatedStrength / 100f) * 360f
        drawArc(
            color = when (warningLevel) {
                0 -> Color.Green
                1 -> Color.Yellow
                2 -> Color.Red
                else -> Color.Red
            },
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = true,
            size = Size(radius * 2, radius * 2),
            topLeft = Offset(
                (canvasWidth - radius * 2) / 2,
                (canvasHeight - radius * 2) / 2
            )
        )
    }
}

fun enviarAlerta(nivel: Int) {
    println("Alerta nivel $nivel activada")
    // Aquí puedes usar NotificationManager para mostrar una notificación real
}

fun aplicarAcciones(config: ConfiguracionAlerta) {
    if (config.desactivarWifi) {
        // Desactivar Wi-Fi con WifiManager
    }
    if (config.desactivarBluetooth) {
        // Desactivar Bluetooth con BluetoothAdapter
    }
    if (config.activarModoAvion) {
        // Activar modo avión (requiere permisos especiales)
    }
}
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    AURATheme {
        MainScreen()
    }
}