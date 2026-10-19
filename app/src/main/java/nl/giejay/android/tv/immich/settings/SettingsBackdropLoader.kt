package nl.giejay.android.tv.immich.settings

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import nl.giejay.android.tv.immich.api.ApiClient
import nl.giejay.android.tv.immich.api.ApiClientConfig
import nl.giejay.android.tv.immich.api.util.ApiUtil
import nl.giejay.android.tv.immich.shared.prefs.ContentType
import nl.giejay.android.tv.immich.shared.prefs.LOAD_BACKGROUND_IMAGE
import nl.giejay.android.tv.immich.shared.prefs.PreferenceManager

/**
 * Background artwork for the settings surface (SET-004). Settings rows carry no artwork of
 * their own, so the surface borrows one: a single random library asset — the same
 * `ApiUtil.getFileUrl(.., "IMAGE")` source the grid pages feed into BackgroundManager —
 * fetched once per process and reused, so a drill-in pane opened from the hub shows the
 * same image and the two surfaces read as one.
 *
 * Loading honours LOAD_BACKGROUND_IMAGE exactly like the grid pages. On any failure the
 * surface falls back to the flat `surface_base` + scrim stack it already renders; the
 * surface stays correct without artwork. Presentation layer only: `listAssets(random)`
 * is the existing /search/random path used by the Random tab, unmodified.
 */
object SettingsBackdropLoader {

    @Volatile
    private var backdropUrl: String? = null

    /** One-shot random fetch; cached for the session so hub and panes stay consistent. */
    suspend fun fetchBackdropUrl(): String? {
        backdropUrl?.let { return it }
        if (!PreferenceManager.get(LOAD_BACKGROUND_IMAGE)) return null
        val url = try {
            ApiClient.getClient(ApiClientConfig.fromPrefs())
                .listAssets(page = 0, pageCount = 1, random = true, contentType = ContentType.ALL)
                .fold(
                    { null },
                    { res -> res.assets.firstOrNull()?.let { ApiUtil.getFileUrl(it.id, "IMAGE") } }
                )
        } catch (e: Exception) {
            null
        }
        if (url != null) {
            backdropUrl = url
        }
        return url
    }

    /** Cross-fade the backdrop in (250ms background cross-fade duration token). */
    fun loadBackdrop(view: ImageView, url: String?) {
        if (url.isNullOrEmpty()) return
        Glide.with(view.context)
            .load(url)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade(250))
            .into(view)
    }
}
