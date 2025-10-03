package com.roamoralesgonzalez.aura.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SensorDataManager {
    private val _magneticStrength = MutableStateFlow(0f)
    val magneticStrength: StateFlow<Float> = _magneticStrength

    fun updateMagneticStrength(strength: Float) {
        _magneticStrength.value = strength
    }
}
