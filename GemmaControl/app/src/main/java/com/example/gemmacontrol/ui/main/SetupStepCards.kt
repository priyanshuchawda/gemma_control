package com.example.gemmacontrol.ui.main

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SetupStepCardState(
    val step: Int,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val buttonLabel: String
)

@Composable
fun ProgressBadge(done: Int, total: Int) {
    val color = when (done) {
        total -> AccentGreen
        0 -> AccentRed
        else -> AccentOrange
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
    ) {
        Text(
            "$done / $total steps configured",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun SetupStepCard(
    state: SetupStepCardState,
    onAction: () -> Unit
) {
    val accentColor = if (state.isGranted) AccentGreen else AccentBlue
    val borderColor = if (state.isGranted) AccentGreen.copy(alpha = 0.4f) else CardBorder

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SetupStepCardHeader(state = state, accentColor = accentColor)
            Text(state.description, fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
            SetupStepCardAction(
                isGranted = state.isGranted,
                buttonLabel = state.buttonLabel,
                accentColor = accentColor,
                onAction = onAction
            )
        }
    }
}

@Composable
private fun SetupStepCardHeader(
    state: SetupStepCardState,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(accentColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(state.icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Step ${state.step}",
                fontSize = 11.sp,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Text(state.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        if (state.isGranted) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Granted", tint = AccentGreen, modifier = Modifier.size(24.dp))
        } else {
            Icon(Icons.Default.Warning, contentDescription = "Required", tint = AccentOrange, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SetupStepCardAction(
    isGranted: Boolean,
    buttonLabel: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onAction: () -> Unit
) {
    if (!isGranted) {
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(buttonLabel, fontWeight = FontWeight.SemiBold)
        }
    } else {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = AccentGreen.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "✓  Configured",
                fontSize = 13.sp,
                color = AccentGreen,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 10.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MiuiAutostartCard(
    acknowledged: Boolean,
    onAcknowledge: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val accentColor = if (acknowledged) AccentGreen else AccentOrange
    val borderColor = if (acknowledged) AccentGreen.copy(alpha = 0.4f) else AccentOrange.copy(alpha = 0.4f)

    val miuiAutostartIntent = remember {
        val intent = android.content.Intent()
        intent.setClassName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )
        intent
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("MI", color = accentColor, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Step 3 · XIAOMI / MIUI",
                        fontSize = 11.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text("Enable Autostart", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                if (acknowledged) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Confirmed manually", tint = AccentGreen, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Warning, contentDescription = "Unacknowledged", tint = AccentOrange, modifier = Modifier.size(24.dp))
                }
            }

            Text(
                "MIUI aggressively kills background services. You must whitelist GemmaControl in Security Settings → Autostart, otherwise notification capture will fail in the background.",
                fontSize = 13.sp,
                color = TextMuted,
                lineHeight = 18.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "1. Open  Security  (or  Phone Manager)  app",
                    "2. Tap  Manage apps  →  Permissions",
                    "3. Tap  Autostart",
                    "4. Find  GemmaControl  and toggle it  ON",
                    "5. Also disable Battery Saver for GemmaControl"
                ).forEach { step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("›", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(step, fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
                    }
                }
            }

            Button(
                onClick = {
                    try {
                        context.startActivity(miuiAutostartIntent)
                    } catch (e: Exception) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Autostart Settings", fontWeight = FontWeight.SemiBold)
            }

            HorizontalDivider(color = CardBorder)

            Text(
                text = "Autostart status cannot be verified automatically. Confirm only after enabling it manually in Xiaomi settings.",
                fontSize = 12.sp,
                color = TextMuted,
                lineHeight = 16.sp
            )

            if (!acknowledged) {
                Button(
                    onClick = { onAcknowledge(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I enabled Autostart for GemmaControl", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentGreen.copy(alpha = 0.1f))
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Confirmed manually",
                        fontSize = 13.sp,
                        color = AccentGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { onAcknowledge(false) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(20.dp)
                    ) {
                        Text("Undo", color = AccentRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
