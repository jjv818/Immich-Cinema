package nl.giejay.android.tv.immich.shared.fragment

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.leanback.app.BackgroundManager
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import arrow.core.Either
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.giejay.android.tv.immich.ImmichApplication
import nl.giejay.android.tv.immich.R
import nl.giejay.android.tv.immich.api.ApiClient
import nl.giejay.android.tv.immich.api.ApiClientConfig
import nl.giejay.android.tv.immich.card.Card
import nl.giejay.android.tv.immich.card.CardPresenterSelector
import nl.giejay.android.tv.immich.home.HomeFragmentDirections
import nl.giejay.android.tv.immich.shared.prefs.API_KEY
import nl.giejay.android.tv.immich.shared.prefs.DEBUG_MODE
import nl.giejay.android.tv.immich.shared.prefs.DISABLE_SSL_VERIFICATION
import nl.giejay.android.tv.immich.shared.prefs.HOST_NAME
import nl.giejay.android.tv.immich.shared.prefs.LOAD_BACKGROUND_IMAGE
import nl.giejay.android.tv.immich.shared.prefs.PreferenceManager
import nl.giejay.android.tv.immich.shared.util.Debouncer
import nl.giejay.android.tv.immich.shared.viewmodel.KeyEventsViewModel
import timber.log.Timber
import java.util.concurrent.TimeUnit


abstract class VerticalCardGridFragment<ITEM> : GridFragment() {
    protected var assets: List<ITEM> = emptyList()
    protected val startPage = 1

    private lateinit var mMetrics: DisplayMetrics
    private var mBackgroundManager: BackgroundManager? = null
    private var backdrop: ImageView? = null

    protected lateinit var apiClient: ApiClient
    private lateinit var keyEvents: KeyEventsViewModel
    private val ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_SMALL
    protected val ioScope = CoroutineScope(Job() + Dispatchers.IO)
    private val mainScope = CoroutineScope(Job() + Dispatchers.Main)
    private val assetsStillToRender: MutableList<ITEM> = mutableListOf()
    protected var currentPage: Int = startPage
    protected var allPagesLoaded: Boolean = false
    private var currentLoadingJob: Job? = null
    protected val selectionMode: Boolean
        get() = arguments?.getBoolean("selectionMode", false) ?: false
    private var currentSelectedIndex: Int = 0

    abstract fun sortItems(items: List<ITEM>): List<ITEM>
    abstract suspend fun loadItems(
        apiClient: ApiClient,
        page: Int,
        pageCount: Int
    ): Either<String, List<ITEM>>

    abstract fun createCard(a: ITEM): Card
    abstract fun getBackgroundPicture(it: ITEM): String?
    open fun setTitle(response: List<ITEM>) {
        // default no title
    }

    abstract fun onItemSelected(card: Card, indexOf: Int)
    abstract fun onItemClicked(card: Card)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (PreferenceManager.isLoggedId()) {
            apiClient =
                ApiClient.getClient(
                    ApiClientConfig(
                        PreferenceManager.hostName,
                        PreferenceManager.get(API_KEY),
                        PreferenceManager.get(DISABLE_SSL_VERIFICATION),
                        PreferenceManager.get(DEBUG_MODE)
                    )
                )
        } else {
            Toast.makeText(
                ImmichApplication.appContext,
                "Invalid Immich server settings, redirecting to login screen.",
                Toast.LENGTH_SHORT
            ).show()
            findNavController().navigate(HomeFragmentDirections.actionGlobalSignInFragment())
            return
        }

        keyEvents = ViewModelProvider(requireActivity())[KeyEventsViewModel::class.java]
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                keyEvents.state.collect {
                    // open popup menu on the right side if its the last photo in the row and user presses right button
                    if (it?.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && ((adapter.size() == 0 && allPagesLoaded) || currentSelectedIndex > 0 && (currentSelectedIndex % COLUMNS == 3 || currentSelectedIndex + 1 == adapter.size()))) {
                        openPopUpMenu()
                    } else if (it?.keyCode == KeyEvent.KEYCODE_FORWARD || it?.keyCode == KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD || it?.keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                        updateManualPositionHandler(adapter.size() - 1)
                    } else if (it?.keyCode == KeyEvent.KEYCODE_MEDIA_REWIND || it?.keyCode == KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD) {
                        updateManualPositionHandler((currentSelectedIndex - FETCH_PAGE_COUNT).coerceAtLeast(0))
                    }
                }
            }
        }

        setupAdapter()
        setupBackgroundManager()

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            val card: Card = item as Card
            if (selectionMode) {
                card.selected = !card.selected
                adapter.notifyArrayItemRangeChanged(currentSelectedIndex, 1)
                onItemSelected(card, currentSelectedIndex)
            } else {
                onItemClicked(card)
            }
        }
        setOnItemViewSelectedListener { _, item, _, _ ->
            currentSelectedIndex = adapter.indexOf(item)
            item?.let {
                loadBackgroundDebounced((it as Card).backgroundUrl) {
                    loadBackgroundDebounced(it.thumbnailUrl) {
                        Timber.tag(javaClass.name)
                            .e("Could not load background url")
                    }
                }
            }
            with(this@VerticalCardGridFragment) {
                loadNextItemsIfNeeded(adapter.indexOf(item))
            }
        }
        // fetch initial items
        fetchInitialItems()
    }

    protected open fun fetchInitialItems() {
        ioScope.launch {
            loadData().fold(
                { itLeft -> showErrorMessage(itLeft) },
                { itRight ->
                    val assets = filterItems(itRight)
                    setupViews(assets)
                    if (assets.size < FETCH_COUNT) {
                        // immediately load next assets
                        currentLoadingJob = fetchNextItems()
                    }
                }
            )
        }
    }

    private fun loadNextItemsIfNeeded(selectedIndex: Int) {
        if (selectedIndex != -1 && (adapter.size() - selectedIndex < FETCH_NEXT_THRESHOLD)) {
            if (currentLoadingJob?.isActive != true && assetsStillToRender.isEmpty() && !allPagesLoaded) {
                // try a next page if its available
                currentLoadingJob = fetchNextItems()
            } else {
                mainScope.launch {
                    addAssetsPaginated()
                }
            }
        }
    }

    protected open fun resortItems() {
        assets = sortItems(assets)
        adapter.clear()
        adapter.addAll(0, assets.map { createCard(it) })
    }

    protected open fun clearState() {
        currentPage = startPage
        assets = emptyList()
        assetsStillToRender.clear()
        adapter.clear()
    }

    protected open fun openPopUpMenu() {
        // to implement by children
    }

    private fun fetchNextItems(): Job {
        return ioScope.launch {
            val nextAssets = loadMoreAssets()
            setDataOnMain(nextAssets)
        }
    }

    protected open fun filterItems(items: List<ITEM>): List<ITEM> = items

    protected open suspend fun loadMoreAssets(): List<ITEM> {
        if (allPagesLoaded) {
            return emptyList()
        }
        currentPage += 1
        return loadData().fold(
            { errorMessage ->
                showErrorMessage(errorMessage)
                emptyList()
            },
            { items ->
                Timber.i("Loading next items, ${items.size}")
                val filteredItems = filterItems(items)
                allPagesLoaded = allPagesLoaded(items)
                if (filteredItems.size < FETCH_COUNT) {
                    return filteredItems + loadMoreAssets()
                } else  {
                    return filteredItems
                }
            }
        )
    }

    protected open fun allPagesLoaded(items: List<ITEM>): Boolean =
        items.size < FETCH_PAGE_COUNT

    override fun onResume() {
        super.onResume()
        if (assets.isNotEmpty()) {
            progressBar?.visibility = View.GONE
        }
        // Returning from the viewer restores focus but not the artwork; re-apply the backdrop
        // for whatever card is selected now (ALBUM-004).
        view?.post { applyBackdropForSelection() }
    }

    private fun setupAdapter() {
        // Leanback's focus dimmer darkens every unfocused grid item to 40% of its rendered colour.
        // It is a dimming overlay rather than a view alpha, which is why every alpha probe read 1.0
        // while the artwork and titles measured a 2.6:1 contrast ratio (VIS-002). The cards supply
        // their own focus treatment (1.06 scale + hairline + glow), so the dimmer is disabled.
        val presenter = VerticalGridPresenter(ZOOM_FACTOR, false)
        presenter.numberOfColumns = COLUMNS
        gridPresenter = presenter
        val cardPresenter = CardPresenterSelector(requireContext())
        adapter = ArrayObjectAdapter(cardPresenter)
    }

    private fun setupBackgroundManager() {
        // The grid now owns its backdrop (grid_backdrop ImageView) so the cross-fade uses
        // motion_background_ms instead of BackgroundManager's fixed 500ms. Clear the shared
        // manager's drawable so a leftover Timeline photo cannot linger behind the grid.
        mBackgroundManager = BackgroundManager.getInstance(activity)
        if (!mBackgroundManager!!.isAttached) {
            mBackgroundManager!!.attach(requireActivity().window)
        }
        mBackgroundManager?.drawable = null
        mMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(mMetrics)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // onCreate runs before the view exists, so the backdrop is bound here.
        backdrop = view.findViewById(R.id.grid_backdrop)
    }

    /** Re-loads the backdrop for the currently selected card, if any. */
    private fun applyBackdropForSelection() {
        if (!PreferenceManager.get(LOAD_BACKGROUND_IMAGE)) {
            backdrop?.setImageDrawable(null)
            return
        }
        val index = currentSelectedIndex
        val card = if (index in 0 until adapter.size()) adapter.get(index) as? Card else null
        val url = card?.backgroundUrl ?: card?.thumbnailUrl ?: return
        loadBackground(url) { }
    }

    override fun onPause() {
        // Cancel pending hover-background loads so they don't paint onto Timeline later.
        Debouncer.cancel(BACKGROUND_DEBOUNCE_KEY)
        super.onPause()
    }

    override fun onDestroyView() {
        Debouncer.cancel(BACKGROUND_DEBOUNCE_KEY)
        super.onDestroyView()
        backdrop?.setImageDrawable(null)
        backdrop = null
        if (mBackgroundManager?.isAttached == true) {
            mBackgroundManager?.drawable = null
        }
    }

    private suspend fun setupViews(assets: List<ITEM>) =
        withContext(Dispatchers.Main) {
            progressBar?.visibility = View.GONE
            setTitle(assets)
            assets.firstOrNull()?.let { loadBackground(getBackgroundPicture(it)) {} }
            setDataOnMain(assets)
        }

    protected suspend fun setDataOnMain(assets: List<ITEM>) = withContext(Dispatchers.Main) {
        setData(assets)
    }

    protected open fun setData(assets: List<ITEM>) {
        val sortedItems = sortItems(assets.filter { !this@VerticalCardGridFragment.assets.contains(it) })
        this@VerticalCardGridFragment.assets += sortedItems
        assetsStillToRender.addAll(sortedItems)
        addAssetsPaginated()
    }

    protected open suspend fun loadData(): Either<String, List<ITEM>> {
        return loadItems(apiClient, currentPage, FETCH_PAGE_COUNT)
    }

    private fun loadBackgroundDebounced(backgroundUrl: String?, onLoadFailed: () -> Unit) {
        if (PreferenceManager.get(LOAD_BACKGROUND_IMAGE)) {
            Debouncer.debounce(
                BACKGROUND_DEBOUNCE_KEY,
                { loadBackground(backgroundUrl, onLoadFailed) },
                1,
                TimeUnit.SECONDS
            )
        }
    }

    private fun loadBackground(backgroundUrl: String?, onLoadFailed: () -> Unit) {
        if (!isAdded || !isResumed || !PreferenceManager.get(LOAD_BACKGROUND_IMAGE)) {
            return
        }
        if (backgroundUrl.isNullOrEmpty()) {
            Timber.i("Could not load background because background url is null")
            return
        }
        val iv = backdrop ?: return
        try {
            Glide.with(requireActivity())
                .load(backgroundUrl)
                .centerCrop()
                .override(mMetrics.widthPixels, mMetrics.heightPixels)
                .transition(
                    DrawableTransitionOptions.withCrossFade(
                        resources.getInteger(R.integer.motion_background_ms)
                    )
                )
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ) = false

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        // e.g. a video asset's "original"/"preview" cannot decode as a bitmap ??                        // keep the current backdrop and run the caller's fallback chain.
                        Timber.w("Backdrop load failed: %s", e?.message)
                        onLoadFailed()
                        return false
                    }
                })
                .into(iv)
        } catch (e: Exception) {
            Timber.e(e, "Could not fetch background")
            onLoadFailed()
        }
    }

    private fun addAssetsPaginated() {
        val assetsPaginated = assetsStillToRender.take(FETCH_COUNT)
        val cards = assetsPaginated.map { createCard(it) }
        adapter.addAll(adapter.size(), cards)
        assetsStillToRender.removeAll(assetsPaginated)
    }

    private suspend fun showErrorMessage(message: String) = withContext(Dispatchers.Main) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG)
                .show()
        }
        Timber.e(message)
    }

    companion object {
        const val COLUMNS = 4
        private const val FETCH_NEXT_THRESHOLD = COLUMNS * 6
        const val FETCH_COUNT = 50
        const val FETCH_PAGE_COUNT = FETCH_COUNT
        /** Shared with Timeline so it can cancel a pending Photos background paint. */
        const val BACKGROUND_DEBOUNCE_KEY = "background"
    }
}