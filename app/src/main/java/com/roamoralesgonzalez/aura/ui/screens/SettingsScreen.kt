package com.roamoralesgonzalez.aura.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roamoralesgonzalez.aura.R
import com.roamoralesgonzalez.aura.model.ConfiguracionAlerta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: ConfiguracionAlerta,
    onConfigChange: (ConfiguracionAlerta) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val githubUrl = "https://github.com/cam-mor/expoFisica-AURA/tree/master"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Section for thresholds
            Text("Umbrales de Sensibilidad (µT)", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            SettingSlider(
                label = "Nivel Medio (Amarillo)",
                value = config.umbralNivel1,
                onValueChange = { onConfigChange(config.copy(umbralNivel1 = it)) },
                valueRange = 1f..100f,
                steps = 98,
                valueLabel = { "%.0f µT".format(it) }
            )

            SettingSlider(
                label = "Nivel Alto (Rojo)",
                value = config.umbralNivel2,
                onValueChange = { onConfigChange(config.copy(umbralNivel2 = it)) },
                valueRange = 101f..500f,
                steps = 398,
                valueLabel = { "%.0f µT".format(it) }
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Section for alert times
            Text("Tiempos de Alerta (segundos)", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            SettingSlider(
                label = "Alerta 1",
                value = config.tiempoNivel1 / 1000f,
                onValueChange = { onConfigChange(config.copy(tiempoNivel1 = it.toLong() * 1000)) },
                valueRange = 5f..20f,
                steps = 14,
                valueLabel = { "%.0f s".format(it) }
            )

            SettingSlider(
                label = "Alerta 2",
                value = config.tiempoNivel2 / 1000f,
                onValueChange = { onConfigChange(config.copy(tiempoNivel2 = it.toLong() * 1000)) },
                valueRange = 21f..45f,
                steps = 23,
                valueLabel = { "%.0f s".format(it) }
            )

            SettingSlider(
                label = "Alerta 3",
                value = config.tiempoNivel3 / 1000f,
                onValueChange = { onConfigChange(config.copy(tiempoNivel3 = it.toLong() * 1000)) },
                valueRange = 46f..90f,
                steps = 43,
                valueLabel = { "%.0f s".format(it) }
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Section for general settings
            Text("Configuración General", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Burbuja Flotante", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = config.mostrarBurbujaFlotante,
                    onCheckedChange = { onConfigChange(config.copy(mostrarBurbujaFlotante = it)) }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // About section
            Text("Acerca de", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { uriHandler.openUri(githubUrl) }
            ) {
                Text(
                    text = "Repositorio en GitHub",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: (Float) -> String
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(valueLabel(value), style = MaterialTheme.typography.bodyLarge)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}
