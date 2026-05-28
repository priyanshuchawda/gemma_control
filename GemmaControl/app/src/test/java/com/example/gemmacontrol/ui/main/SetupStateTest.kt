package com.example.gemmacontrol.ui.main

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SetupStateTest {

    @Test
    fun testSetupUiState_NonXiaomi() {
        val state = SetupUiState(
            isXiaomiLikeDevice = false,
            notificationAccessEnabled = true,
            batteryOptimizationIgnored = false,
            xiaomiAutostartAcknowledged = false
        )
        assertTrue(state.minimumAccessGranted)
        assertTrue(state.reliabilitySetupComplete)
    }

    @Test
    fun testSetupUiState_Xiaomi_Incomplete() {
        val state = SetupUiState(
            isXiaomiLikeDevice = true,
            notificationAccessEnabled = true,
            batteryOptimizationIgnored = false,
            xiaomiAutostartAcknowledged = false
        )
        assertTrue(state.minimumAccessGranted)
        assertFalse(state.reliabilitySetupComplete)
    }

    @Test
    fun testSetupUiState_Xiaomi_Complete() {
        val state = SetupUiState(
            isXiaomiLikeDevice = true,
            notificationAccessEnabled = true,
            batteryOptimizationIgnored = true,
            xiaomiAutostartAcknowledged = true
        )
        assertTrue(state.minimumAccessGranted)
        assertTrue(state.reliabilitySetupComplete)
    }

    @Test
    fun testPolicyRegression_BatteryOptimizationsPermission() {
        // Manifest should NOT contain REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        // Check both in the current directory and parent paths to handle run configurations
        val paths = listOf("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml")
        val manifestFile = paths.map { File(it) }.firstOrNull { it.exists() }
        
        if (manifestFile != null) {
            val content = manifestFile.readText()
            assertFalse(
                "Manifest must not request battery optimization exemption directly",
                content.contains("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")
            )
        }
    }

    @Test
    fun testPolicyRegression_BatteryOptimizationsIntent() {
        // Kotlin source code must NOT contain ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        val paths = listOf("src/main/java", "app/src/main/java")
        val sourceDir = paths.map { File(it) }.firstOrNull { it.exists() }
        
        if (sourceDir != null) {
            val matches = sourceDir.walkBottomUp()
                .filter { it.isFile && it.extension == "kt" }
                .any { file ->
                    file.readText().contains("ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")
                }
            assertFalse(
                "Source code must not use ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS directly",
                matches
            )
        }
    }
}
