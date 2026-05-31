package com.example.gemmacontrol.ui.main

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import kotlin.math.pow
import kotlin.random.Random

private val WaveformAmplitudeTweenMillis = PulseDurationMillis / 10
private const val WaveformAmplitudeScale = 32767.0
private const val QuietAmplitudeThreshold = 0.2
private val WaveformPerlinOffsetRange = PulseDurationMillis.toFloat()

/** AGSL shader waveform - API 33+ (Gallery AudioAnimation.kt port). */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
internal fun WaveformShader(
    amplitude: Int,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    val resources = rememberWaveformShaderResources()
    val normalisedAmplitude = normalisedWaveformAmplitude(amplitude)
    val frame = WaveformShaderFrame(
        timeSeconds = rememberShaderTimeSeconds(),
        amplitude = rememberAnimatedWaveformAmplitude(normalisedAmplitude),
        perlinOffset = rememberPerlinOffset(normalisedAmplitude),
        backgroundColor = bgColor
    )

    WaveformShaderCanvas(
        resources = resources,
        frame = frame,
        modifier = modifier
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun rememberWaveformShaderResources(): WaveformShaderResources {
    val shader = remember { RuntimeShader(WAVEFORM_SHADER_SOURCE) }
    val shaderBrush = remember { ShaderBrush(shader) }
    return WaveformShaderResources(shader = shader, brush = shaderBrush)
}

@Composable
private fun rememberAnimatedWaveformAmplitude(normalisedAmplitude: Double): Float {
    var animatedAmplitude by remember { mutableFloatStateOf(normalisedAmplitude.toFloat()) }

    LaunchedEffect(normalisedAmplitude) {
        val anim = Animatable(animatedAmplitude)
        anim.animateTo(
            targetValue = normalisedAmplitude.toFloat(),
            animationSpec = tween(durationMillis = WaveformAmplitudeTweenMillis)
        ) { animatedAmplitude = value }
    }

    return animatedAmplitude
}

@Composable
private fun rememberShaderTimeSeconds(): Float {
    var shaderTimeSeconds by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { frameMillis ->
                shaderTimeSeconds = frameMillis / PulseDurationMillis.toFloat()
            }
        }
    }

    return shaderTimeSeconds
}

@Composable
private fun rememberPerlinOffset(normalisedAmplitude: Double): Float {
    var perlinOffset by remember { mutableFloatStateOf(0f) }
    var previousAmplitude by remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(normalisedAmplitude) {
        if (normalisedAmplitude < QuietAmplitudeThreshold && previousAmplitude >= QuietAmplitudeThreshold) {
            perlinOffset = Random.nextFloat() * WaveformPerlinOffsetRange
        }
        previousAmplitude = normalisedAmplitude
    }

    return perlinOffset
}

private fun normalisedWaveformAmplitude(amplitude: Int): Double =
    (amplitude / WaveformAmplitudeScale).pow(0.5)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun WaveformShaderCanvas(
    resources: WaveformShaderResources,
    frame: WaveformShaderFrame,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        resources.shader.setFloatUniform("iTime", frame.timeSeconds)
        resources.shader.setFloatUniform("iResolution", size.width, size.height)
        resources.shader.setFloatUniform(
            "bgColor",
            frame.backgroundColor.red,
            frame.backgroundColor.green,
            frame.backgroundColor.blue,
            frame.backgroundColor.alpha
        )
        resources.shader.setFloatUniform("amplitude", frame.amplitude)
        resources.shader.setFloatUniform("pOffset", frame.perlinOffset)

        drawRect(brush = resources.brush)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private data class WaveformShaderResources(
    val shader: RuntimeShader,
    val brush: ShaderBrush
)

private data class WaveformShaderFrame(
    val timeSeconds: Float,
    val amplitude: Float,
    val perlinOffset: Float,
    val backgroundColor: Color
)
