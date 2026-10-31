package com.blockcall

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.StatusBarManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.text.format.DateFormat
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.util.Date

/** Botón de ajustes rápidos: no corre en segundo plano, solo mientras la cortina está abierta. */
object Tiles {
    /** Pide al sistema que redibuje el botón tras un cambio hecho desde la app. */
    fun refresh(context: Context) = TileService.requestListeningState(context, component(context))

    fun until(context: Context, time: Long): String = DateFormat.getTimeFormat(context).format(Date(time))

    private fun component(context: Context) = ComponentName(context, ProtectionTile::class.java)

    /**
     * Pide al sistema agregar el botón (Android 13+): sale un aviso y el usuario decide. Si ya estaba,
     * el sistema no muestra nada y se avisa aquí. Android no deja a las apps saber de antemano si el
     * botón está en la cortina, por eso la opción se ofrece siempre.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestAdd(context: Context) {
        context.getSystemService(StatusBarManager::class.java).requestAddTileService(
            component(context),
            context.getString(R.string.tile_label),
            Icon.createWithResource(context, R.drawable.ic_tile_shield),
            context.mainExecutor,
        ) { result ->
            if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                Toast.makeText(context, R.string.tile_already_added, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/**
 * Un solo botón para la cortina. Cada toque pasa al siguiente estado:
 * activa → pausa 30 min → 1 h → 2 h → activa. Una pausa con hora elegida en la app se cancela con
 * el primer toque. Apagar del todo solo se puede desde la app, para no olvidarla apagada.
 */
class ProtectionTile : TileService() {

    override fun onStartListening() = render()

    override fun onClick() {
        val store = Store.get(this)
        val s = store.settings.value
        if (!s.enabled) {
            // Sin el rol de filtro encender no serviría de nada: se abre la app para concederlo.
            if (getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                store.setEnabled(true)
            } else {
                openApp()
            }
        } else {
            val current = Store.PAUSE_OPTIONS.indexOf(s.pauseMinutes)
            val next = when {
                !s.isPaused() -> Store.PAUSE_OPTIONS.first()
                current < 0 -> null
                else -> Store.PAUSE_OPTIONS.getOrNull(current + 1)
            }
            if (next == null) store.resume() else store.pause(next)
        }
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val s = Store.get(this).settings.value
        val paused = s.isPaused()
        // En pausa sigue resaltado (es un estado elegido, no un apagado) y el aviso va en el nombre:
        // algunas cortinas (p. ej. OnePlus) no muestran el subtítulo.
        tile.state = if (s.enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, if (paused) R.drawable.ic_tile_pause else R.drawable.ic_tile_shield)
        tile.label = if (paused) {
            getString(R.string.tile_paused_until, Tiles.until(this, s.pausedUntil))
        } else {
            getString(R.string.tile_label)
        }
        tile.subtitle = getString(
            when {
                paused -> R.string.tile_paused_hint
                s.enabled -> R.string.tile_on
                else -> R.string.tile_off
            },
        )
        tile.stateDescription = tile.subtitle
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
