package com.balu.abdialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun InCallScreen(
    number: String,
    onEndCall: () -> Unit
) {
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var muted by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf(false) }
    var keypad by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0B10))
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.size(74.dp))
            Text("Calling", color = Color.White.copy(alpha = 0.62f), fontSize = 16.sp)
            Spacer(Modifier.size(10.dp))
            Text(number, color = Color.White, fontSize = 32.sp)
            Spacer(Modifier.size(8.dp))
            Text(
                String.format("%02d:%02d", minutes, seconds),
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 16.sp
            )

            Spacer(Modifier.weight(1f))

            if (keypad) {
                Text(
                    "Keypad",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 18.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallControl(
                    active = muted,
                    label = if (muted) "Unmute" else "Mute",
                    icon = { Icon(Icons.Rounded.MicOff, null, tint = Color.White) },
                    onClick = { muted = !muted }
                )
                CallControl(
                    active = speaker,
                    label = "Speaker",
                    icon = { Icon(Icons.Rounded.VolumeUp, null, tint = Color.White) },
                    onClick = { speaker = !speaker }
                )
                CallControl(
                    active = keypad,
                    label = "Keypad",
                    icon = { Icon(Icons.Rounded.Dialpad, null, tint = Color.White) },
                    onClick = { keypad = !keypad }
                )
            }

            Spacer(Modifier.size(34.dp))

            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .liquidGlass(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                        tint = Color(0xFFE5484D).copy(alpha = 0.5f),
                        highlight = Color.White.copy(alpha = 0.38f)
                    )
                    .clickable(
                        indication = null,
                        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
                        onClick = onEndCall
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.CallEnd, "End call", tint = Color.White)
            }

            Spacer(Modifier.size(26.dp))
        }
    }
}

@Composable
private fun CallControl(
    active: Boolean,
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .liquidGlass(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    tint = if (active) Color(0xFF7B61FF).copy(alpha = 0.4f)
                    else Color.White.copy(alpha = 0.10f)
                )
                .clickable(
                    indication = null,
                    interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(Modifier.size(8.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}
