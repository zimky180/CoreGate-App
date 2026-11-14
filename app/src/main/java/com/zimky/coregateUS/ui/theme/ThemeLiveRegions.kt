package com.zimky.coregateUS.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Rect

/** Regions that keep rendering live above the frozen theme crossfade. */
val LocalThemeLiveRegions = staticCompositionLocalOf<SnapshotStateMap<Any, Rect>?> { null }
