# Third-party notices

- AndroidX / Jetpack Compose: https://android.googlesource.com/platform/frameworks/support/ (Apache-2.0).
- Miuix: https://github.com/compose-miuix-ui/miuix (Apache-2.0).
- AndroidLiquidGlass: https://github.com/Kyant0/AndroidLiquidGlass (Apache-2.0). Adapted liquid components retain source headers.
- Material Kolor: https://github.com/jordond/MaterialKolor (MIT).

Dependency versions are recorded in gradle/libs.versions.toml. Upstream copyrights and licenses apply to third-party code; this project's MIT license applies to original project code.

## Duck-Detector-Refactoring contributor wall

Copyright 2026 Duck Apps Contributor. Apache License 2.0.
Source: https://github.com/eltavine/Duck-Detector-Refactoring/tree/8b365ba9ba2f0e434189c6b8c0d52dbddc161392/feature/settings/ui/src/main/kotlin/com/eltavine/duckdetector/features/settings/ui/components

Adapted ContributorWallCanvas, ContributorWallGeometry, ContributorWallMotion and ContributorWallEdgeFade into `ui/contributors/`. Original copyright and license headers are retained. Changes: CoreGate package, Miuix theme tokens, fixed local contributor order, local avatars and QQ profile actions; removed the upstream pager integration. Contributor details and update prompt follow the reference Miuix layout using CoreGate components. License: `LICENSES/Apache-2.0-Duck-Detector.txt`.
## Telegram Android profile metaball
Source: https://github.com/DrKLO/Telegram/blob/master/TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileMetaballView.java
Upstream snapshot SHA-256: `f05972aac35e5d6618f417ffcb00d4ef211dcfc30305e8a97c1867c3aad1569f`.

`ui/TelegramMetaballPath.java` extracts the original `createMetaballPath`, `dist`, `angle` and `getVector` methods. The Java geometry is retained, with ZXing distance replaced by Math.hypot and an independent wrapper added. `ui/HomeAvatarOverlay.kt` adapts the upstream radius/alpha/blur and connector blend formulas to CoreGate's Compose scroll coordinates and Android display cutout; Telegram's ProfileActivity/ImageReceiver, story UI and background blur queue are not included. This adapter is also distributed under GPL-2.0.

License text: `app/src/main/assets/licenses/Telegram-GPL-2.0.txt`. MIT original code is GPL-compatible; distributing a combined application containing this GPL-derived code requires compliance with GPL-2.0 for the combined work, including corresponding source. Merely including this notice and license text is not sufficient.
