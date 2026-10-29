# Publicar BlockCall en tiendas comunitarias

Todo es gratis. F-Droid recibe solicitudes en **GitLab** e IzzyOnDroid en **Codeberg**, cada uno con una cuenta propia (gratuita).

Estado: solicitud a F-Droid enviada → <https://gitlab.com/fdroid/rfp/-/issues/4699>

## 1. GitHub Releases + Obtainium (ya disponible)
Cada versión publica su APK en <https://github.com/blockcallcm/BlockCall/releases>.
Con [Obtainium](https://github.com/ImranR98/Obtainium) cualquiera recibe las actualizaciones automáticamente:
*Agregar app* → pegar `https://github.com/blockcallcm/BlockCall`.

## 2. IzzyOnDroid (días)
Usa el mismo APK y la misma firma que GitHub, así que es compatible con las descargas de Releases.

Su antiguo repositorio de GitLab está archivado: ahora las solicitudes van a Codeberg.

1. Crea una cuenta en <https://codeberg.org>.
2. Revisa la política de inclusión (<https://izzyondroid.org/docs/general/AppInclusionPolicy/>) y abre la solicitud en el repositorio de mantenimiento de IzzyOnDroid en Codeberg, pegando:

```
Title: Inclusion request: BlockCall (io.github.blockcallcm.blockcall)

- Source code: https://github.com/blockcallcm/BlockCall
- License: GPL-3.0-only
- APK: attached to each GitHub release (https://github.com/blockcallcm/BlockCall/releases)
- Package name: io.github.blockcallcm.blockcall
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
2. Copia [`fdroid/io.github.blockcallcm.blockcall.yml`](fdroid/io.github.blockcallcm.blockcall.yml) a `metadata/` en tu fork.
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
