package com.example.gemmacontrol.ui.main

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Waveform animation composable.
 *
 * On Android 13+ (API 33): renders Gallery's AGSL RuntimeShader waveform.
 * On Android 12 and below: renders 5 animated bars as fallback.
 *
 * Amplitude range: 0..65535 (Gallery's convertRmsDbToAmplitude scale).
 *
 * @param amplitude  Mic RMS amplitude in 0..65535 range from VoiceAssistantViewModel
 * @param bgColor    Background color the shader blends into (use surface/background color)
 * @param modifier   Applied to the root composable
 */
@Composable
fun WaveformAnimation(
    amplitude: Int,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        WaveformShader(amplitude = amplitude, bgColor = bgColor, modifier = modifier)
    } else {
        WaveformBars(amplitude = amplitude, modifier = modifier)
    }
}
