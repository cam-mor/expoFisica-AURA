package com.roamoralesgonzalez.aura.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SensorMonitoringService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var notificationManager: NotificationManager
    private var magneticSensor: Sensor? = null
    private var proximitySensor: Sensor? = null

    private val _magneticFieldStrength = MutableStateFlow(0f)
    private val _isNearDevice = MutableStateFlow(false)

    companion object {
        private const val CHANNEL_ID = "magnetic_warning_channel"
        private const val NOTIFICATION_ID = 1
        private const val MAGNETIC_THRESHOLD_MICROTESLA = 500f // 0.5 mT = 500 µT
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        createNotificationChannel()

        // Registrar los sensores
        magneticSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Advertencias de Campo Magnético"
            val descriptionText = "Canal para advertencias de campo magnético peligroso"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val strength = calculateMagneticFieldStrength(event.values)
                _magneticFieldStrength.value = strength
                SensorDataManager.updateMagneticStrength(strength)
                checkMagneticFieldStrength(strength)
            }
            Sensor.TYPE_PROXIMITY -> {
                _isNearDevice.value = (event.values[0] < (proximitySensor?.maximumRange ?: 5f))
                checkProximityWarning()
            }
        }
    }

    private fun calculateMagneticFieldStrength(values: FloatArray): Float {
        return kotlin.math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
    }

    private fun checkMagneticFieldStrength(strength: Float) {
        if (strength > MAGNETIC_THRESHOLD_MICROTESLA) {
            showMagneticWarning(strength)
        } else {
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    private fun showMagneticWarning(strength: Float) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("¡Advertencia! Campo Magnético Detectado")
            .setContentText("Campo magnético de ${String.format("%.1f", strength)} µT detectado. Nivel peligroso para marcapasos.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun checkProximityWarning() {
        // Lógica para verificar la proximidad y mostrar advertencias si es necesario
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}
