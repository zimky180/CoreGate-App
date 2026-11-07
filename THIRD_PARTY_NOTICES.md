# Third-party notices

- AndroidX / Jetpack Compose: https://android.googlesource.com/platform/frameworks/support/ (Apache-2.0).
- Miuix: https://github.com/compose-miuix-ui/miuix (Apache-2.0).
- AndroidLiquidGlass: https://github.com/Kyant0/AndroidLiquidGlass (Apache-2.0). Adapted liquid components retain source headers.
- Material Kolor: https://github.com/jordond/MaterialKolor (MIT).

Dependency versions are recorded in gradle/libs.versions.toml. Upstream copyrights and licenses apply to third-party code; this project's MIT license applies to original project code.

## Telegram for Android — GPL v2

`ui/TelegramMetaball.kt` ports `ProfileMetaballView.createMetaballPath` from
https://github.com/DrKLO/Telegram/blob/master/TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileMetaballView.java .
Copyright remains with the upstream Telegram contributors. The port preserves the
connection angles and Bezier handles; it replaces Android PointF with Compose
Offset and clamps acos inputs for numerical stability. This legacy utility is
retained as source only; the current HomeProfileHeader uses a regular right-aligned
avatar and no longer calls the metaball port.

The upstream GNU GPL v2 text is included in LICENSES/Telegram-GPL-2.0.txt.
The MIT grant for original code does not relicense this adaptation. Distribution
of an APK containing this GPL code must comply with applicable GPL requirements
for the combined work, including corresponding source and license notices;
the combined APK must not be described as MIT-only. Other dependencies retain
their licenses, whose compatibility must also be considered before distribution.

Upstream source snapshot (Git tree): `3de37c8cfb039452be7398909461da6288babf15`.