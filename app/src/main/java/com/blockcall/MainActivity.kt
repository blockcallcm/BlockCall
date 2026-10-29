package com.blockcall

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockcall.ui.AllowedScreen
import com.blockcall.ui.BlockCallTheme
import com.blockcall.ui.BlockedScreen
import com.blockcall.ui.HomeScreen
import com.blockcall.ui.OnboardingScreen
import com.blockcall.ui.isDarkTheme

data class Setup(val hasRole: Boolean, val hasContacts: Boolean, val hasNotifications: Boolean) {
    val ready get() = hasRole && hasContacts
}

class MainActivity : ComponentActivity() {

    private var setup by mutableStateOf(Setup(hasRole = false, hasContacts = false, hasNotifications = false))
    private var tab by mutableIntStateOf(0)
    private val store by lazy { Store.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tab = savedInstanceState?.getInt(EXTRA_TAB) ?: intent.getIntExtra(EXTRA_TAB, 0)
        refreshSetup()
        // Quien actualiza desde una versión anterior ya configurada no necesita el asistente.
        if (!store.settings.value.onboarded && setup.hasRole) store.setOnboarded()

        setContent {
            val settings by store.settings.collectAsStateWithLifecycle()
            val log by store.log.collectAsStateWithLifecycle()
            val unseen = log.count { it.time > settings.lastSeen }
            val dark = isDarkTheme(settings.theme)
            // Iconos de la barra de estado acordes al tema elegido, no solo al del sistema.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            LaunchedEffect(tab, settings.onboarded) { if (tab == TAB_BLOCKED && settings.onboarded) markBlockedSeen() }

            BlockCallTheme(dark) {
                if (!settings.onboarded) {
                    OnboardingScreen(store, setup, ::refreshSetup)
                    return@BlockCallTheme
                }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            TABS.forEachIndexed { i, (label, icon) ->
                                NavigationBarItem(
                                    selected = tab == i,
                                    onClick = { tab = i },
                                    icon = {
                                        BadgedBox(badge = {
                                            if (i == TAB_BLOCKED && unseen > 0 && tab != TAB_BLOCKED) Badge { Text("$unseen") }
                                        }) {
                                            Icon(icon, contentDescription = null)
                                        }
                                    },
                                    label = { Text(stringResource(label)) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    val modifier = Modifier.padding(padding)
                    when (tab) {
                        0 -> HomeScreen(store, setup, ::refreshSetup, modifier)
                        1 -> AllowedScreen(store, modifier)
                        else -> BlockedScreen(store, modifier)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        tab = intent.getIntExtra(EXTRA_TAB, tab)
    }

    override fun onResume() {
        super.onResume()
        refreshSetup()
        if (tab == TAB_BLOCKED) markBlockedSeen()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_TAB, tab)
    }

    private fun markBlockedSeen() {
        store.markSeen()
        BlockedNotifier.cancel(this)
    }

    private fun refreshSetup() {
        setup = Setup(
            hasRole = getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING),
            hasContacts = checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED,
            hasNotifications = BlockedNotifier.canNotify(this),
        )
    }

    companion object {
        const val EXTRA_TAB = "tab"
        const val TAB_BLOCKED = 2

        private val TABS = listOf(
            R.string.tab_home to Icons.Rounded.Shield,
            R.string.tab_allowed to Icons.Rounded.Group,
            R.string.tab_blocked to Icons.Rounded.Block,
        )
    }
}
