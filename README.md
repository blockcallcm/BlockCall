# BlockCall

App Android (10+) que rechaza en silencio las llamadas entrantes que no vienen de tus contactos ni de tu lista de permitidos.

## Cómo funciona

- Usa la API oficial `CallScreeningService`. Android activa la app **solo** cuando entra una llamada, decide en milisegundos y la libera. No queda ningún proceso en segundo plano, así que no gasta batería y ningún limpiador de tareas la puede desactivar.
- Las llamadas bloqueadas no suenan ni vibran. Quedan en la pestaña **Bloqueadas** y, si "Avisarme de bloqueos" está activo, en una única notificación silenciosa que lleva la cuenta.
- Android siempre deja la llamada bloqueada en el historial del teléfono como "bloqueada"; BlockCall pide que no se anuncie como llamada perdida.
- Los números ocultos o privados se bloquean siempre que el filtro esté activo.
- **¿Quién puede llamarte?**: *Contactos* (contactos + Permitidos), *Mi lista* (solo Permitidos) o *Nadie* (se rechaza todo, incluso insistentes).
- **Permitir si insiste**: si el mismo número llama 2 veces en menos de 3 minutos, la segunda entra.
- Si falta el permiso de contactos o hay algún error, la llamada **se deja pasar**.

## Probar sin llamadas reales

El módulo `callsim` (solo para pruebas) inyecta llamadas entrantes falsas por ADB:

```bash
./gradlew :callsim:assembleDebug
adb install callsim/build/outputs/apk/debug/callsim-debug.apk
adb shell am broadcast -n com.blockcall.callsim/.TriggerReceiver -a register
adb shell telecom set-phone-account-enabled com.blockcall.callsim/.SimConnectionService sim 0
adb shell am broadcast -n com.blockcall.callsim/.TriggerReceiver -a call --es number +573001234567
adb logcat -s BlockCall
```

Desinstálalo al terminar (`adb uninstall com.blockcall.callsim`).

## Instalación en el celular

1. Descarga el APK de la sección **Releases** del repositorio, ábrelo en el celular y permite "instalar apps de origen desconocido" si te lo pide.
2. Abre BlockCall: un asistente te guía paso a paso.
   - **Filtro de llamadas**: en la lista marca **BlockCall** (no "Ninguna") y toca **Hacer predeterminada**. No reemplaza tu app de Teléfono.
   - **Contactos** y **notificaciones**: toca **Permitir**.
3. Al terminar queda activa: siempre, entran contactos y Permitidos, ocultos bloqueados, "Permitir si insiste" y aviso silencioso.

Para actualizar, instala el APK nuevo encima: conserva la configuración.

### Xiaomi / Redmi / POCO (MIUI / HyperOS)
En *Ajustes → Apps → BlockCall*, activa **Inicio automático**. Sin esto, MIUI puede impedir que el sistema active el filtro.

## Compilar

```bash
./gradlew assembleRelease
```

El APK queda en `app/build/outputs/apk/release/app-release.apk`.
Guarda `keystore/` y `keystore.properties`. Sin ellos no podrás instalar futuras versiones encima de la actual.
