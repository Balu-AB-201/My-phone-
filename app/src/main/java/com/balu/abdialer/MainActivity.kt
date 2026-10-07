package com.balu.abdialer

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.balu.abdialer.ui.DialerScreen
import com.balu.abdialer.ui.theme.ABDialerTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_DIALER_ROLE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ABDialerTheme {
                DialerScreen(modifier = Modifier.fillMaxSize())
            }
        }

        requestDefaultDialerRole()
    }

    private fun requestDefaultDialerRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val roleManager = getSystemService(RoleManager::class.java) ?: return
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) return
        if (roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) return

        runCatching {
            startActivityForResult(
                roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER),
                REQUEST_DIALER_ROLE
            )
        }
    }
}
