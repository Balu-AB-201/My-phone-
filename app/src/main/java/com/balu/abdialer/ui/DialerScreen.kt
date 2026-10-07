package com.balu.abdialer.ui

import android.Manifest
import android.telecom.Call
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class DialKey(val number: String, val letters: String)

private val keys = listOf(
    DialKey("1", ""), DialKey("2", "ABC"), DialKey("3", "DEF"),
    DialKey("4", "GHI"), DialKey("5", "JKL"), DialKey("6", "MNO"),
    DialKey("7", "PQRS"), DialKey("8", "TUV"), DialKey("9", "WXYZ"),
    DialKey("*", ""), DialKey("0", "+"), DialKey("#", "")
)

@Composable
fun DialerScreen(modifier: Modifier = Modifier) {
    var number by rememberSaveable { mutableStateOf("") }
    var favoriteNumber by rememberSaveable { mutableStateOf("") }
    var activeCallNumber by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val telecomCall = ActiveCallStore.uiState.collectAsState().value
    val telephonyManager = remember {
        context.getSystemService(TelephonyManager::class.java)
    }

    DisposableEffect(telephonyManager) {
        fun clearIfIdle(state: Int) {
            if (state == TelephonyManager.CALL_STATE_IDLE) {
                activeCallNumber = null
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    clearIfIdle(state)
                }
            }
            runCatching {
                telephonyManager.registerTelephonyCallback(
                    context.mainExecutor,
                    callback
                )
            }
            onDispose {
                runCatching { telephonyManager.unregisterTelephonyCallback(callback) }
            }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Suppress("DEPRECATION")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    clearIfIdle(state)
                }
            }
            @Suppress("DEPRECATION")
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            onDispose {
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
            }
        }
    }

    val callLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val callGranted = permissions[Manifest.permission.CALL_PHONE] == true
        val phoneStateGranted = permissions[Manifest.permission.READ_PHONE_STATE] == true

        if (callGranted && phoneStateGranted && number.isNotBlank()) {
            activeCallNumber = number
            context.startActivity(
                Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(number)))
            )
        }
    }

    fun placeCall() {
        if (number.isBlank()) return

        val callGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        val phoneStateGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (callGranted && phoneStateGranted) {
            activeCallNumber = number
            context.startActivity(
                Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(number)))
            )
        } else {
            callLauncher.launch(
                arrayOf(
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_PHONE_STATE
                )
            )
        }
    }

    fun openContacts() {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("content://contacts/people/")
            })
        }
    }

    val callUi = telecomCall ?: activeCallNumber?.let {
        com.balu.abdialer.CallUiState(it, Call.STATE_DIALING)
    }

    callUi?.let { call ->
        InCallScreen(
            number = call.number,
            incoming = call.state == Call.STATE_RINGING,
            onEndCall = {
                ActiveCallStore.disconnect()
                activeCallNumber = null
            }
        )
        return
    }

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(
                    Color(0xFF11131F),
                    Color(0xFF242044),
                    Color(0xFF0A0B10)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AB Dialer",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = ::openContacts) {
                    Icon(
                        imageVector = Icons.Rounded.Contacts,
                        contentDescription = "Contacts",
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.size(96.dp))

            Text(
                text = number.ifEmpty { " " },
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(number) {
                        detectTapGestures(
                            onLongPress = {
                                if (number.isNotEmpty()) number = ""
                            }
                        )
                    }
            )

            Spacer(Modifier.size(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                keys.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { key ->
                            DialKeyButton(key = key, onClick = { number += key.number })
                        }
                    }
                }
            }

            Spacer(Modifier.size(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (number.isNotBlank()) favoriteNumber = number },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = "Favorites",
                        tint = if (favoriteNumber == number && number.isNotBlank()) Color(0xFFFFD54F)
                        else Color.White.copy(alpha = 0.82f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .liquidGlass(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                        )
                        .background(Color(0xFF35D07F).copy(alpha = 0.72f))
                        .clickable(
                            indication = null,
                            interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
                            onClick = ::placeCall
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Call,
                        contentDescription = "Call",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                IconButton(
                    onClick = { if (number.isNotEmpty()) number = number.dropLast(1) },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        Icons.Rounded.Backspace,
                        contentDescription = "Delete",
                        tint = Color.White.copy(alpha = 0.82f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialKeyButton(
    key: DialKey,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(82.dp)
            .liquidGlass()
            .clickable(
                indication = null,
                interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = key.number,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light
            )
            if (key.letters.isNotEmpty()) {
                Text(
                    text = key.letters,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}
