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
Offset and clamps acos inputs for numerical stability. `ui/TelegramAvatarRenderer.kt`
also adapts the upstream onDraw clipping, RenderNode blur, mirrored image capture,
alpha and gradient stages. `HomeAvatarOverlay` supplies local bitmap loading and
scroll geometry, keeping the initial avatar on the right. Telegram ImageReceiver,
story integration and the pre-API-31 software blur queue are replaced by static
local images and the API-33+ hardware rendering path. This is an adapted rendering
pipeline, not an unchanged copy of the entire Telegram profile screen.

The upstream GNU GPL v2 text is included in LICENSES/Telegram-GPL-2.0.txt.
The MIT grant for original code does not relicense this adaptation. Distribution
of an APK containing this GPL code must comply with applicable GPL requirements
for the combined work, including corresponding source and license notices;
the combined APK must not be described as MIT-only. Other dependencies retain
their licenses, whose compatibility must also be considered before distribution.

Upstream source snapshot (Git tree): `3de37c8cfb039452be7398909461da6288babf15`.

## Duck-Detector-Refactoring contributor wall

Copyright 2026 Duck Apps Contributor. Apache License 2.0.
Source: https://github.com/eltavine/Duck-Detector-Refactoring/tree/8b365ba9ba2f0e434189c6b8c0d52dbddc161392/feature/settings/ui/src/main/kotlin/com/eltavine/duckdetector/features/settings/ui/components

Adapted ContributorWallCanvas, ContributorWallGeometry, ContributorWallMotion and ContributorWallEdgeFade into `ui/contributors/`. Original copyright and license headers are retained. Changes: CoreGate package, Miuix theme tokens, fixed local contributor order, placeholder avatars and QQ profile actions; removed the upstream pager integration. Contributor details and update prompt follow the reference Miuix layout using CoreGate components. License: `LICENSES/Apache-2.0-Duck-Detector.txt`.