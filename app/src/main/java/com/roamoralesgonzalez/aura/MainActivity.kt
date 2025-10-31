package com.roamoralesgonzalez.aura

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.roamoralesgonzalez.aura.model.ConfiguracionAlerta
import com.roamoralesgonzalez.aura.service.SensorDataManager
import com.roamoralesgonzalez.aura.service.SensorMonitoringService
import com.roamoralesgonzalez.aura.services.FloatingBubbleService
import com.roamoralesgonzalez.aura.ui.screens.SettingsScreen
import com.roamoralesgonzalez.aura.ui.theme.AURATheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {
    private val _magneticStrength = MutableStateFlow(0f)
    val magneticStrength: StateFlow<Float> = _magneticStrength

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted.")
        } else {
            Log.d("MainActivity", "Notification permission denied.")
            // Optionally, show a rationale to the user.
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Settings.canDrawOverlays(this)) {
            startFloatingBubble()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        solicitarPermisoNotificaciones()
        setContent {
            AURATheme {
                MainContent(
                    activity = this,
                    onStartMonitoring = { startMonitoring() },
                    onStopMonitoring = { stopMonitoring() }
                )
            }
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission is already granted.
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Explain to the user why you need the permission.
                    // You could show a dialog here before requesting again.
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Directly ask for the permission.
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "magnetic_field_alert_channel"
            val name = "Alertas de Campo Magnético"
            val descriptionText = "Notificaciones para niveles altos de campo magnético"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun checkOverlayPermission() {
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

    fun startFloatingBubble() {
        val serviceIntent = Intent(this, FloatingBubbleService::class.java)
        startService(serviceIntent)
    }

    fun stopFloatingBubble() {
        val serviceIntent = Intent(this, FloatingBubbleService::class.java)
        stopService(serviceIntent)
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
    activity: MainActivity,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("main") }
    var config by remember { mutableStateOf(ConfiguracionAlerta()) }

    LaunchedEffect(config.mostrarBurbujaFlotante) {
        if (config.mostrarBurbujaFlotante) {
            activity.checkOverlayPermission()
        } else {
            activity.stopFloatingBubble()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            "main" -> MainScreen(
                config = config,
                onStartMonitoring = onStartMonitoring,
                onStopMonitoring = onStopMonitoring,
                onSettingsClick = { currentScreen = "settings" }
            )
            "settings" -> SettingsScreen(
                config = config,
                onConfigChange = { newConfig -> config = newConfig },
                onNavigateBack = { currentScreen = "main" }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    config: ConfiguracionAlerta,
    onStartMonitoring: () -> Unit = {},
    onStopMonitoring: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {

    val context = LocalContext.current
    var isMonitoring by remember { mutableStateOf(false) }
    var magneticStrength by remember { mutableStateOf(0f) }
    var warningLevel by remember { mutableStateOf(0) }
    var mostrarDialogoWifi by remember { mutableStateOf(false) }
    var mostrarDialogoBluetooth by remember { mutableStateOf(false) }
    var mostrarDialogoModoAvion by remember { mutableStateOf(false) }
    var highRiskTimestamp by remember { mutableStateOf<Long?>(null) }
    var alertsFired by remember { mutableStateOf(emptySet<Long>()) }

    // Observar los cambios del magnetómetro
    LaunchedEffect(Unit) {
        SensorDataManager.magneticStrength.collect { strength ->
            magneticStrength = strength
        }
    }

    // Actualizar el nivel de advertencia basado en la intensidad
    LaunchedEffect(magneticStrength, config) {
        warningLevel = when {
            magneticStrength > config.umbralNivel2 -> 2  // Nivel peligroso
            magneticStrength > config.umbralNivel1 -> 1  // Nivel de precaución
            else -> 0                     // Nivel seguro
        }
    }

    // Lógica para enviar una alerta cuando se está en nivel de peligro por 10, 30 y 60 segundos.
    LaunchedEffect(warningLevel, config) {
        if (warningLevel == 2) {
            if (highRiskTimestamp == null) {
                highRiskTimestamp = System.currentTimeMillis()
            }
            while (true) { // Se cancelará cuando warningLevel cambie
                delay(1000) // Revisar cada segundo
                if (highRiskTimestamp != null) {
                    val elapsedTime = System.currentTimeMillis() - highRiskTimestamp!!
                    val alertTimes = listOf(
                        config.tiempoNivel1,
                        config.tiempoNivel2,
                        config.tiempoNivel3
                    )

                    alertTimes.forEach { alertTime ->
                        if (elapsedTime >= alertTime && !alertsFired.contains(alertTime)) {
                            enviarAlerta(context, 2, alertTime)
                            alertsFired = alertsFired + alertTime
                        }
                    }
                }
            }
        } else {
            highRiskTimestamp = null // Resetear si no estamos en la zona de alto riesgo
            alertsFired = emptySet() // Resetear las alertas disparadas
        }
    }

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
                config = config,
                modifier = Modifier
                    .size(300.dp)  // Tamaño reducido del círculo
                    .padding(top = 32.dp)  // Espacio adicional arriba
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
    if (mostrarDialogoWifi) {
        ConfirmacionDialogo(
            titulo = "Desactivar Wi-Fi",
            mensaje = "¿Deseas desactivar el Wi-Fi?",
            onConfirmar = {
                mostrarDialogoWifi = false
                abrirConfiguracionWifi(context)
            },
            onCancelar = { mostrarDialogoWifi = false }
        )
    }

    if (mostrarDialogoBluetooth) {
        ConfirmacionDialogo(
            titulo = "Desactivar Bluetooth",
            mensaje = "¿Deseas desactivar el Bluetooth?",
            onConfirmar = {
                mostrarDialogoBluetooth = false
                desactivarBluetooth()
            },
            onCancelar = { mostrarDialogoBluetooth = false }
        )
    }

    if (mostrarDialogoModoAvion) {
        ConfirmacionDialogo(
            titulo = "Activar modo avión",
            mensaje = "¿Deseas activar el modo avión?",
            onConfirmar = {
                mostrarDialogoModoAvion = false
                abrirConfiguracionModoAvion(context)
            },
            onCancelar = { mostrarDialogoModoAvion = false }
        )
    }

}

@Composable
fun MagneticFieldIndicator(
    strength: Float,
    warningLevel: Int,
    config: ConfiguracionAlerta,
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
        val maxStrength = if (config.umbralNivel2 > 0) config.umbralNivel2 else 200f
        val sweepAngle = (animatedStrength / maxStrength) * 360f
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

fun desactivarWifi(context: Context) {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    wifiManager.isWifiEnabled = false
}

fun desactivarBluetooth() {
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    bluetoothAdapter?.disable()
}
fun abrirConfiguracionModoAvion(context: Context) {
    val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
    context.startActivity(intent)
}
fun abrirConfiguracionWifi(context: Context) {
    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
    context.startActivity(intent)
}

fun enviarAlerta(context: Context, nivel: Int, durationInMillis: Long) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // Si no tenemos permiso, simplemente no hacemos nada (o podríamos registrar un log).
            Log.d("enviarAlerta", "Permission to post notifications not granted.")
            return
        }
    }

    val notificationId = durationInMillis.toInt()
    val channelId = "magnetic_field_alert_channel"
    val durationInSeconds = durationInMillis / 1000

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("¡Alerta de Campo Magnético!")
        .setContentText("Nivel de campo magnético alto detectado por más de $durationInSeconds segundos.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    with(NotificationManagerCompat.from(context)) {
        notify(notificationId, builder.build())
    }
    println("Alerta nivel $nivel ($durationInSeconds s) activada con notificación")
}


//API 28 e inferior

fun aplicarAcciones(context: Context, config: ConfiguracionAlerta) {
    if (config.desactivarWifi) {
        desactivarWifi(context)
    }
    if (config.desactivarBluetooth) {
        desactivarBluetooth()
    }
    if (config.activarModoAvion) {
        abrirConfiguracionModoAvion(context)
    }
}
//API 29 y superiores
@Composable
fun ConfirmacionDialogo(
    titulo: String,
    mensaje: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}

fun aplicarAccionesConDialogos(
    config: ConfiguracionAlerta,
    mostrarWifi: () -> Unit,
    mostrarBluetooth: () -> Unit,
    mostrarModoAvion: () -> Unit
) {
    if (config.desactivarWifi) mostrarWifi()
    if (config.desactivarBluetooth) mostrarBluetooth()
    if (config.activarModoAvion) mostrarModoAvion()
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    AURATheme {
        MainScreen(config = ConfiguracionAlerta())
    }
}
