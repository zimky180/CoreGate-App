package com.zimky.coregateUS.ui

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Resolve a browser rather than an app claiming the destination URL. */
internal fun openInBrowser(context: Context, url: String): Boolean = runCatching {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        selector = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}.isSuccess
