package com.balu.abdialer.ui

import android.telecom.Call
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.balu.abdialer.ActiveCallStore
import kotlinx.coroutines.delay

@Composable
fun InCallScreen(
    number: String,
    onEndCall: () -> Unit,
    incoming: Boolean = false
) {
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var muted by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf(false) }
    var keypad by remember { mutableStateOf(false) }
    val callState by ActiveCallStore.uiState.collectAsState()
    val connected = callState?.state == Call.STATE_ACTIVE

    LaunchedEffect(incoming) {
        if (!incoming) {
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val title = if (incoming) "Incoming call" else "Calling"

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
            Text(title, color = Color.White.copy(alpha = 0.62f), fontSize = 16.sp)
            Spacer(Modifier.size(10.dp))
            Text(number, color = Color.White, fontSize = 32.sp)
            Spacer(Modifier.size(8.dp))
            if (!incoming) {
                Text(
                    if (connected) String.format("%02d:%02d", minutes, seconds) else "Connecting…",
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.weight(1f))

            if (incoming) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .liquidGlass(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                                tint = Color(0xFF35D07F).copy(alpha = 0.45f)
                            )
                            .clickable(
                                indication = null,
                                interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
                                onClick = ActiveCallStore::answer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Call, "Answer", tint = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .liquidGlass(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                                tint = Color(0xFFE5484D).copy(alpha = 0.5f)
                            )
                            .clickable(
                                indication = null,
                                interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
                                onClick = {
                                    ActiveCallStore.reject()
                                    onEndCall()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CallEnd, "Reject", tint = Color.White)
                    }
                }
            } else {
                AnimatedVisibility(
                    visible = keypad,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    DtmfKeypad()
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
                        onClick = {
                            muted = !muted
                            ActiveCallStore.setMuted(muted)
                        }
                    )
                    CallControl(
                        active = speaker,
                        label = if (speaker) "Speaker on" else "Speaker",
                        icon = { Icon(Icons.Rounded.VolumeUp, null, tint = Color.White) },
                        onClick = {
                            speaker = !speaker
                            ActiveCallStore.setSpeaker(speaker)
                        }
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
                            onClick = {
                                ActiveCallStore.disconnect()
                                onEndCall()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CallEnd, "End call", tint = Color.White)
                }
            }

            Spacer(Modifier.size(26.dp))
        }
    }
}

@Composable
private val DTMF_DIGITS = listOf("1","2","3","4","5","6","7","8","9","*","0","#")

@Composable
private fun DtmfKeypad() {
    val digits = DTMF_DIGITS
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Keypad", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().size(250.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(digits) { digit ->
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .liquidGlass()
                        .pointerInput(digit) {
                            detectTapGestures(
                                onPress = {
                                    digit.firstOrNull()?.let { ActiveCallStore.playDtmfTone(it) }
                                    try {
                                        tryAwaitRelease()
                                    } finally {
                                        ActiveCallStore.stopDtmfTone()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(digit, color = Color.White, fontSize = 24.sp)
                }
            }
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
        ) { icon() }
        Spacer(Modifier.size(8.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}
