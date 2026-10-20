package nl.giejay.android.tv.immich.album

import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import arrow.core.Either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.giejay.android.tv.immich.R
import nl.giejay.android.tv.immich.api.ApiClient
import nl.giejay.android.tv.immich.api.ApiClientConfig
import nl.giejay.android.tv.immich.api.model.Asset
import nl.giejay.android.tv.immich.assets.GenericAssetFragment
import nl.giejay.android.tv.immich.card.Card
import nl.giejay.android.tv.immich.home.HomeFragmentDirections
import nl.giejay.android.tv.immich.shared.prefs.ContentType
import nl.giejay.android.tv.immich.shared.prefs.EnumByTitlePref
import nl.giejay.android.tv.immich.shared.prefs.FILTER_CONTENT_TYPE_FOR_SPECIFIC_ALBUM
import nl.giejay.android.tv.immich.shared.prefs.PHOTOS_SORTING_FOR_SPECIFIC_ALBUM
import nl.giejay.android.tv.immich.shared.prefs.PhotosOrder


class AlbumDetailsFragment : GenericAssetFragment() {
    lateinit var albumId: String
    lateinit var albumName: String

    /** Cached so the title is only rebuilt once; null until the album metadata arrives. */
    private var albumAssetCount: Int? = null

    override fun getFilterKey(): EnumByTitlePref<ContentType> {
        albumId = AlbumDetailsFragmentArgs.fromBundle(requireArguments()).albumId
        albumName = AlbumDetailsFragmentArgs.fromBundle(requireArguments()).albumName
        return FILTER_CONTENT_TYPE_FOR_SPECIFIC_ALBUM(albumId, albumName)
    }

    override fun getSortingKey(): EnumByTitlePref<PhotosOrder> {
        albumId = AlbumDetailsFragmentArgs.fromBundle(requireArguments()).albumId
        albumName = AlbumDetailsFragmentArgs.fromBundle(requireArguments()).albumName
        return PHOTOS_SORTING_FOR_SPECIFIC_ALBUM(albumId, albumName)
    }

    override suspend fun loadItems(apiClient: ApiClient, page: Int, pageCount: Int): Either<String, List<Asset>> {
        return apiClient.listAssets(
            page = page,
            pageCount = pageCount,
            order = if (currentSort == PhotosOrder.NEWEST_OLDEST) "desc" else "asc",
            contentType = currentFilter,
            albumIds = listOf(albumId)
        ).map { it.assets.map { a -> a.copy(albumName = albumName) } }
    }

    override fun onItemSelected(card: Card, indexOf: Int) {
        // no use case yet
    }

    override fun openPopUpMenu() {
        findNavController().navigate(
            HomeFragmentDirections.actionGlobalToSettingsDialog("album_details", albumId, albumName)
        )
    }

    override fun setTitle(response: List<Asset>) {
        val count = albumAssetCount
        title = if (count != null) albumTitle(albumName, count) else albumName
        if (count != null) return
        // The paged search response does not carry the album's total, so fetch it once and
        // rebuild the contextual title with the count (ALBUM-002).
        viewLifecycleOwner.lifecycleScope.launch {
            val fetched = withContext(Dispatchers.IO) {
                ApiClient.getClient(ApiClientConfig.fromPrefs())
                    .getAlbum(albumId)
                    .fold({ null }, { it.assetCount })
            }
            if (fetched != null && isAdded) {
                albumAssetCount = fetched
                title = albumTitle(albumName, fetched)
            }
        }
    }

    private fun albumTitle(name: String, count: Int): String =
        resources.getQuantityString(R.plurals.album_title_count, count, name, count)
}
