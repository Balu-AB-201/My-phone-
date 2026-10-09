package com.balu.abdialer.ui

import android.Manifest
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.Call
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.balu.abdialer.ActiveCallStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallMissed
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.balu.abdialer.ui.liquidGlassSweep

private data class DialKey(val number: String, val letters: String)

private data class RecentCall(
    val number: String,
    val type: Int,
    val timestamp: Long,
    val contactName: String?
)

private data class ContactEntry(
    val name: String,
    val number: String
)

private val keys = listOf(
    DialKey("1", ""), DialKey("2", "ABC"), DialKey("3", "DEF"),
    DialKey("4", "GHI"), DialKey("5", "JKL"), DialKey("6", "MNO"),
    DialKey("7", "PQRS"), DialKey("8", "TUV"), DialKey("9", "WXYZ"),
    DialKey("*", ""), DialKey("0", "+"), DialKey("#", "")
)

@Composable
fun DialerScreen(modifier: Modifier = Modifier) {
    var number by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val preferences = remember { context.getSharedPreferences("ab_dialer", android.content.Context.MODE_PRIVATE) }
    var favoriteNumber by rememberSaveable { mutableStateOf(preferences.getString("favorite_number", "") ?: "") }
    var activeCallNumber by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCallNumber by rememberSaveable { mutableStateOf<String?>(null) }
    var showRecents by rememberSaveable { mutableStateOf(false) }
    var showContacts by rememberSaveable { mutableStateOf(false) }
    var recentCalls by remember { mutableStateOf<List<RecentCall>>(emptyList()) }
    var contacts by remember { mutableStateOf<List<ContactEntry>>(emptyList()) }
    var contactSearch by rememberSaveable { mutableStateOf("") }
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

    val recentCallPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            recentCalls = loadRecentCalls(context)
            showRecents = true
        }
    }

    fun openRecents() {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            recentCalls = loadRecentCalls(context)
            showRecents = true
        } else {
            recentCallPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
        }
    }

    val callLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val callGranted = permissions[Manifest.permission.CALL_PHONE] == true
        val phoneStateGranted = permissions[Manifest.permission.READ_PHONE_STATE] == true

        val targetNumber = pendingCallNumber ?: number
        if (callGranted && phoneStateGranted && targetNumber.isNotBlank()) {
            activeCallNumber = targetNumber
            pendingCallNumber = null
            context.startActivity(
                Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(targetNumber)))
            )
        }
    }

    fun callRecentNumber(target: String) {
        if (target.isBlank()) return

        pendingCallNumber = target
        val callGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        val phoneStateGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (callGranted && phoneStateGranted) {
            activeCallNumber = target
            pendingCallNumber = null
            context.startActivity(
                Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(target)))
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

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contacts = loadContacts(context)
            contactSearch = ""
            showContacts = true
        }
    }

    fun openContacts() {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            contacts = loadContacts(context)
            showContacts = true
        } else {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = ::openRecents) {
                        Icon(Icons.Rounded.History, "Recent calls", tint = Color.White)
                    }
                    IconButton(onClick = ::openContacts) {
                    Icon(
                        imageVector = Icons.Rounded.Contacts,
                        contentDescription = "Contacts",
                        tint = Color.White
                    )
                    }
                }
            }

            Spacer(Modifier.size(96.dp))

            if (showContacts) {
                ContactsPanel(
                    contacts = contacts,
                    searchQuery = contactSearch,
                    onSearchQueryChange = { contactSearch = it },
                    favoriteNumber = favoriteNumber,
                    onToggleFavorite = { contact ->
                        val next = if (favoriteNumber == contact.number) "" else contact.number
                        favoriteNumber = next
                        preferences.edit().putString("favorite_number", next).apply()
                    },
                    onSelect = {
                        number = it.number
                        showContacts = false
                    },
                    onCall = { callRecentNumber(it.number) },
                    onClose = { showContacts = false }
                )
                Spacer(Modifier.size(16.dp))
            } else if (showRecents) {
                RecentCallsPanel(
                    calls = recentCalls,
                    onSelect = {
                        number = it.number
                        showRecents = false
                    },
                    onCall = { callRecentNumber(it.number) },
                    onClose = { showRecents = false }
                )
                Spacer(Modifier.size(16.dp))
            }

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

            if (number.isEmpty() && favoriteNumber.isNotBlank()) {
                Spacer(Modifier.size(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                            tint = Color.White.copy(alpha = 0.08f)
                        )
                        .clickable(
                            indication = null,
                            interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
                            onClick = { number = favoriteNumber }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .liquidGlass(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(19.dp),
                                tint = Color.White.copy(alpha = 0.10f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = "Favorite",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Favorite", color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp)
                        Text(
                            favoriteNumber,
                            color = Color.White,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        Icons.Rounded.Call,
                        contentDescription = "Call favorite",
                        tint = Color.White.copy(alpha = 0.86f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

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
                    onClick = {
                        if (number.isNotBlank()) {
                            val next = if (favoriteNumber == number) "" else number
                            favoriteNumber = next
                            preferences.edit().putString("favorite_number", next).apply()
                        }
                    },
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
                        .liquidGlassSweep()
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
                    onClick = {
                        if (number.isNotEmpty()) {
                            number = number.dropLast(1)
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .pointerInput(number) {
                            detectTapGestures(
                                onLongPress = {
                                    if (number.isNotEmpty()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        number = ""
                                    }
                                }
                            )
                        }
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
    onClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(82.dp)
            .liquidGlass()
            .pointerInput(key.number) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            },
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

private fun lookupContactName(
    context: android.content.Context,
    number: String
): String? {
    return runCatching {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)
                )?.takeIf { it.isNotBlank() }
            } else {
                null
            }
        }
    }.getOrNull()
}

private fun loadContacts(context: android.content.Context): List<ContactEntry> {
    val result = mutableListOf<ContactEntry>()
    runCatching {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC"
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex).orEmpty().trim()
                val number = cursor.getString(numberIndex).orEmpty().trim()
                if (name.isNotBlank() && number.isNotBlank()) {
                    result += ContactEntry(name, number)
                }
            }
        }
    }
    return result.distinctBy { it.name + "|" + it.number }
}

private fun loadRecentCalls(context: android.content.Context): List<RecentCall> {
    val result = mutableListOf<RecentCall>()
    runCatching {
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE),
            null,
            null,
            CallLog.Calls.DATE + " DESC"
        )?.use { cursor ->
            val numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE)
            while (cursor.moveToNext() && result.size < 8) {
                val number = cursor.getString(numberIndex).orEmpty()
                if (number.isNotBlank()) {
                    result += RecentCall(
                        number = number,
                        type = cursor.getInt(typeIndex),
                        timestamp = cursor.getLong(dateIndex),
                        contactName = lookupContactName(context, number)
                    )
                }
            }
        }
    }
    return result
}

@Composable
private fun RecentCallsPanel(
    calls: List<RecentCall>,
    onSelect: (RecentCall) -> Unit,
    onCall: (RecentCall) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("ab_dialer", android.content.Context.MODE_PRIVATE) }
    val favoriteNumber = remember { preferences.getString("favorite_number", "") ?: "" }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }

    val filteredCalls = calls.filter { call ->
        val matchesSearch = if (searchQuery.isBlank()) {
            true
        } else {
            val query = searchQuery.trim()
            call.number.contains(query, ignoreCase = true) ||
                (call.contactName?.contains(query, ignoreCase = true) == true)
        }
        val matchesFilter = when (selectedFilter) {
            "Missed" -> call.type == CallLog.Calls.MISSED_TYPE
            "Incoming" -> call.type == CallLog.Calls.INCOMING_TYPE
            "Outgoing" -> call.type == CallLog.Calls.OUTGOING_TYPE
            else -> true
        }
        matchesSearch && matchesFilter
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                tint = Color.White.copy(alpha = 0.10f)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent calls", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Close",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onClose)
            )
        }

        Spacer(Modifier.size(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    tint = Color.White.copy(alpha = 0.07f),
                    highlight = Color.White.copy(alpha = 0.18f),
                    borderAlpha = 0.22f
                )
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            if (searchQuery.isEmpty()) {
                Text("Search recent calls", color = Color.White.copy(alpha = 0.42f), fontSize = 14.sp)
            }
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.size(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Missed", "Incoming", "Outgoing").forEach { filter ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .liquidGlass(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            tint = if (selectedFilter == filter) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
                            highlight = if (selectedFilter == filter) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.14f),
                            borderAlpha = if (selectedFilter == filter) 0.34f else 0.18f
                        )
                        .clickable { selectedFilter = filter }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        filter,
                        color = Color.White.copy(alpha = if (selectedFilter == filter) 0.94f else 0.58f),
                        fontSize = 11.sp,
                        fontWeight = if (selectedFilter == filter) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.size(10.dp))

        if (filteredCalls.isEmpty()) {
            Text(
                if (calls.isEmpty()) "No recent calls" else "No matching calls",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 18.dp)
            )
        } else {
            filteredCalls.forEach { call ->
                val label = when (call.type) {
                    CallLog.Calls.MISSED_TYPE -> "Missed"
                    CallLog.Calls.INCOMING_TYPE -> "Incoming"
                    CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                    else -> "Call"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(call) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .liquidGlass(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                                tint = if (call.type == CallLog.Calls.MISSED_TYPE) Color(0xFFFF6B7A).copy(alpha = 0.13f) else Color.White.copy(alpha = 0.08f),
                                highlight = Color.White.copy(alpha = 0.20f),
                                borderAlpha = 0.24f
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val callIcon = when (call.type) {
                            CallLog.Calls.MISSED_TYPE -> Icons.Rounded.CallMissed
                            CallLog.Calls.INCOMING_TYPE -> Icons.Rounded.CallReceived
                            else -> Icons.Rounded.CallMade
                        }
                        Icon(callIcon, contentDescription = label, tint = if (call.type == CallLog.Calls.MISSED_TYPE) Color(0xFFFF8A8A) else Color.White.copy(alpha = 0.72f), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            call.contactName ?: call.number,
                            color = Color.White,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (call.contactName != null) {
                            Text(
                                call.number,
                                color = Color.White.copy(alpha = 0.48f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                label,
                                color = if (call.type == CallLog.Calls.MISSED_TYPE)
                                    Color(0xFFFF8A8A)
                                else Color.White.copy(alpha = 0.58f),
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                formatRecentCallTime(call.timestamp),
                                color = Color.White.copy(alpha = 0.38f),
                                fontSize = 11.sp
                            )
                            if (favoriteNumber.isNotBlank() && favoriteNumber == call.number) {
                                Spacer(Modifier.size(6.dp))
                                Icon(
                                    Icons.Rounded.Star,
                                    contentDescription = "Favorite",
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { onCall(call) },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Call,
                            contentDescription = "Call ${call.contactName ?: call.number}",
                            tint = Color.White.copy(alpha = 0.86f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatRecentCallTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val age = (now - timestamp).coerceAtLeast(0L)
    return when {
        age < 60_000L -> "Just now"
        age < 3_600_000L -> "${age / 60_000L}m ago"
        age < 86_400_000L -> "${age / 3_600_000L}h ago"
        age < 172_800_000L -> "Yesterday"
        else -> SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
private fun ContactsPanel(
    contacts: List<ContactEntry>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    favoriteNumber: String,
    onToggleFavorite: (ContactEntry) -> Unit,
    onSelect: (ContactEntry) -> Unit,
    onCall: (ContactEntry) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                tint = Color.White.copy(alpha = 0.10f)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Contacts", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Close",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onClose)
            )
        }
        Spacer(Modifier.size(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    tint = Color.White.copy(alpha = 0.07f),
                    highlight = Color.White.copy(alpha = 0.18f),
                    borderAlpha = 0.22f
                )
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            if (searchQuery.isEmpty()) {
                Text("Search contacts", color = Color.White.copy(alpha = 0.42f), fontSize = 14.sp)
            }
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.size(10.dp))

        val filteredContacts = if (searchQuery.isBlank()) {
            contacts
        } else {
            val query = searchQuery.trim()
            contacts.filter { it.name.contains(query, ignoreCase = true) || it.number.contains(query, ignoreCase = true) }
        }

        if (filteredContacts.isEmpty()) {
            Text(
                if (contacts.isEmpty()) "No contacts found" else "No matching contacts",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 18.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(filteredContacts, key = { it.name + "|" + it.number }) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(contact) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .liquidGlass(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(21.dp),
                                    tint = Color.White.copy(alpha = 0.08f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                contact.name.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                contact.name,
                                color = Color.White,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                contact.number,
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { onToggleFavorite(contact) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Star,
                                contentDescription = if (favoriteNumber == contact.number) "Remove favorite" else "Add favorite",
                                tint = if (favoriteNumber == contact.number) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.42f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { onCall(contact) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Call,
                                contentDescription = "Call ${contact.name}",
                                tint = Color.White.copy(alpha = 0.82f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
