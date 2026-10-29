# Publicar BlockCall en tiendas comunitarias

Todo es gratis. Las solicitudes a IzzyOnDroid y F-Droid se hacen desde **GitLab** con una cuenta propia (gratuita).

## 1. GitHub Releases + Obtainium (ya disponible)
Cada versión publica su APK en <https://github.com/cmonroyp/BlockCall/releases>.
Con [Obtainium](https://github.com/ImranR98/Obtainium) cualquiera recibe las actualizaciones automáticamente:
*Agregar app* → pegar `https://github.com/cmonroyp/BlockCall`.

## 2. IzzyOnDroid (días)
Usa el mismo APK y la misma firma que GitHub, así que es compatible con las descargas de Releases.

1. Crea una cuenta en <https://gitlab.com>.
2. Abre una solicitud en <https://gitlab.com/IzzyOnDroid/repo/-/issues/new> y pega:

```
Title: Inclusion request: BlockCall (io.github.cmonroyp.blockcall)

- Source code: https://github.com/cmonroyp/BlockCall
- License: GPL-3.0-only
- APK: attached to each GitHub release (https://github.com/cmonroyp/BlockCall/releases)
- Package name: io.github.cmonroyp.blockcall
- Description: Silently rejects incoming calls from numbers that aren't in your contacts
  (or only allows a custom list / nobody), with schedule, "allow if they insist",
  hidden-number blocking and a silent summary notification. Uses CallScreeningService,
  no background services, no INTERNET permission, no trackers, no proprietary libraries.
- Metadata: fastlane structure in fastlane/metadata/android (es-ES, en-US)
- Anti-features: none
```

## 3. F-Droid (semanas)
F-Droid compila la app desde el código y la firma con **su propia clave**: quien la instale desde F-Droid no puede actualizar con el APK de GitHub (ni al revés) sin desinstalar.

1. Con tu cuenta de GitLab, haz *fork* de <https://gitlab.com/fdroid/fdroiddata>.
2. Copia [`fdroid/io.github.cmonroyp.blockcall.yml`](fdroid/io.github.cmonroyp.blockcall.yml) a `metadata/` en tu fork.
3. Abre un *merge request* titulado `New app: BlockCall`. Los revisores de F-Droid pueden pedir ajustes.

## 4. Otras tiendas gratuitas (no exigen código abierto)
- **Uptodown** (muy usada en Latinoamérica): crea una cuenta de desarrollador y sube el APK de Releases.
- **APKPure**, **Aptoide**, **Samsung Galaxy Store**, **Huawei AppGallery**: cuenta gratuita, subes el APK y pasan una revisión.

## Antes de cada versión nueva
1. Sube `versionCode` y `versionName` en `app/build.gradle.kts`.
2. Agrega `fastlane/metadata/android/*/changelogs/<versionCode>.txt`.
3. `./gradlew :app:assembleRelease` y publica el APK con `gh release create vX.Y`.
4. La clave de firma (`keystore/`, `keystore.properties`) nunca va al repositorio: respáldala aparte.

## Verificación de desarrolladores de Google (2026–2027)
Google exigirá registrar el identificador de la app a nombre de un desarrollador verificado, también fuera de Play (en todo el mundo desde 2027). Para distribución amplia hace falta la verificación estándar; F-Droid se opone públicamente y la política aún puede cambiar.
