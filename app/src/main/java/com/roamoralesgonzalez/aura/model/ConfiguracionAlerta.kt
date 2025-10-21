package com.roamoralesgonzalez.aura.model



data class ConfiguracionAlerta(
    val tiempoNivel1: Long = 10_000L, // 10 segundos
    val tiempoNivel2: Long = 30_000L, // 30 segundos
    val tiempoNivel3: Long = 60_000L, // 60 segundos
    val desactivarWifi: Boolean = false,
    val desactivarBluetooth: Boolean = false,
    val activarModoAvion: Boolean = false
)