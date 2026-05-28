package com.example.gemmacontrol.ui.main

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * AGSL shader source — ported from Google AI Edge Gallery's AudioAnimation.kt.
 * Colors tuned to Material3 primary/secondary palette instead of Gallery's fixed palette.
 *
 * Gallery source:
 * gallery/Android/src/.../ui/common/AudioAnimation.kt
 */
private const val WAVEFORM_SHADER = """
uniform float2 iResolution;
uniform vec4 bgColor;
uniform float iTime;
uniform float amplitude;
uniform float pOffset;

vec3 mix4(vec3 c1, vec3 c2, vec3 c3, vec3 c4, vec2 uv) {
  float t1 = sin(iTime / 1.6);
  float t2 = sin(iTime / 1.8);
  return mix(
    mix(c1, c2, smoothstep(0.0 + t1*0.1, 0.24 + t1*0.1, uv.y)),
    mix(c3, c4, smoothstep(-0.16 - t2*0.1, 0.24 - t2*0.1, uv.y)),
    smoothstep(0.0, 0.7 + t1*0.1, uv.x));
}

float hash(float i) {
  return -1. + 2. * fract(sin(i * 127.1) * 43758.1453123);
}

float perlin1d(float d) {
  float i = floor(d);
  float f = d - i;
  float y = f*f*f*(6.*f*f - 15.*f + 10.);
  float r = mix(hash(i)*f, hash(i+1.)*(f-1.), y);
  return r * 0.5 + 0.5;
}

half4 main(float2 fragCoord) {
  float2 uv = fragCoord / iResolution.xy;
  uv.y = 1.0 - uv.y;

  if (amplitude == 0.) {
    uv.y += sin(uv.x * 4.0 + -iTime * 1.2) * 0.036;
  } else {
    uv.y -= perlin1d(pOffset + uv.x * 3.) * amplitude / 2.0;
  }

  // GemmaControl brand colors: deep indigo / violet / teal / soft blue
  vec3 col = mix4(
    vec3(0.42, 0.35, 0.95),  // indigo
    vec3(0.65, 0.42, 0.95),  // violet
    vec3(0.27, 0.75, 0.78),  // teal
    vec3(0.38, 0.58, 0.93),  // soft blue
    uv);

  float fade = smoothstep(0.24, 0.34, uv.y);
  vec4 final = mix(vec4(col, 1.0), bgColor, fade);
  return vec4(half3(final.xyz) * (1.0 + amplitude * 0.2), final.a);
}
"""

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

/** AGSL shader waveform — API 33+ (Gallery AudioAnimation.kt port). */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun WaveformShader(
    amplitude: Int,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    val shader = remember { RuntimeShader(WAVEFORM_SHADER) }
    val shaderBrush = remember { ShaderBrush(shader) }

    var iTime by remember { mutableFloatStateOf(0f) }
    var curPOffset by remember { mutableFloatStateOf(0f) }
    var prevNorm by remember { mutableDoubleStateOf(0.0) }

    // Gallery: pow(0.5) makes quiet audio levels visible in animation
    val normalised = (amplitude / 32767.0).pow(0.5)
    var animatedAmplitude by remember { mutableFloatStateOf(normalised.toFloat()) }

    // Gallery: 100ms tween to new amplitude (smooth transitions)
    LaunchedEffect(amplitude) {
        val anim = Animatable(animatedAmplitude)
        anim.animateTo(
            targetValue = normalised.toFloat(),
            animationSpec = tween(durationMillis = 100)
        ) { animatedAmplitude = value }
    }

    // Gallery: update iTime every frame for smooth animation
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { ms -> iTime = ms / 1000f }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Gallery: random perlin offset when audio drops from high to low — prevents repetition
        if (normalised < 0.2 && prevNorm >= 0.2) {
            curPOffset = Random.nextFloat() * 1000f
        }
        prevNorm = normalised

        shader.setFloatUniform("iTime", iTime)
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("bgColor", bgColor.red, bgColor.green, bgColor.blue, bgColor.alpha)
        shader.setFloatUniform("amplitude", animatedAmplitude)
        shader.setFloatUniform("pOffset", curPOffset)

        drawRect(brush = shaderBrush)
    }
}

/**
 * 5-bar fallback waveform for API < 33.
 * Bar heights driven by amplitude with spring animation (Material feel).
 */
@Composable
fun WaveformBars(
    amplitude: Int,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val bars = 5

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(bars) { i ->
            // Each bar has a phase offset so they look like a wave
            val phase = sin(i * 1.1f + System.currentTimeMillis() / 300f)
            val targetHeight = if (amplitude > 0) {
                val factor = (phase + 1f) / 2f  // 0..1
                (6 + (amplitude / 65535f) * 44f * factor).dp
            } else {
                (4 + i * 2).dp  // gentle idle ramp
            }
            val height by animateDpAsState(
                targetValue = targetHeight,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "bar$i"
            )
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(height)
                    .clip(RoundedCornerShape(3.dp))
                    .background(primary)
            )
        }
    }
}
