package com.example.gemmacontrol.ui.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * 5-bar fallback waveform for API < 33.
 * Bar heights are driven by amplitude with spring animation.
 */
@Composable
internal fun WaveformBars(
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
        repeat(bars) { index ->
            val height by animateDpAsState(
                targetValue = waveformBarTargetHeight(
                    amplitude = amplitude,
                    index = index
                ),
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "bar$index"
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

private fun waveformBarTargetHeight(
    amplitude: Int,
    index: Int
) = if (amplitude > 0) {
    val phase = sin(index * 1.1f + System.currentTimeMillis() / 300f)
    val factor = (phase + 1f) / 2f
    (6 + (amplitude / 65535f) * 44f * factor).dp
} else {
    (4 + index * 2).dp
}
