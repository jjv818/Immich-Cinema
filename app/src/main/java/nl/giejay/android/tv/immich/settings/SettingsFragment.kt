package nl.giejay.android.tv.immich.settings

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.ItemBridgeAdapter
import androidx.leanback.widget.VerticalGridView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import nl.giejay.android.tv.immich.R
import nl.giejay.android.tv.immich.home.HomeFragmentDirections
import nl.giejay.android.tv.immich.home.nav.HomeNavViewModel
import nl.giejay.android.tv.immich.shared.prefs.DebugPrefScreen
import nl.giejay.android.tv.immich.shared.prefs.HomeScreenChannelsPrefScreen
import nl.giejay.android.tv.immich.shared.prefs.ScreensaverPrefScreen
import nl.giejay.android.tv.immich.shared.prefs.SectionsPrefScreen
import nl.giejay.android.tv.immich.shared.prefs.ViewPrefScreen

/**
 * Group header above a settings hub row group (iOS-style grouped list).
 */
class SettingsHubHeader(val title: String)

/**
 * Settings hub (SET-001/SET-002): a single-column vertical list of text-first rows with
 * trailing chevrons, grouped by section — replaces the old one-row horizontal card rail
 * (RowsSupportFragment). Rows, focus treatment and chevrons come from the shared settings
 * surface; this fragment only owns the grid, the tab-bar interaction and the row data.
 */
class SettingsFragment : Fragment(), BrowseSupportFragment.MainFragmentAdapterProvider {

    private val mMainFragmentAdapter =
        BrowseSupportFragment.MainFragmentAdapter<SettingsFragment>(this)
    private var gridView: VerticalGridView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.settings_hub_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val grid = view.findViewById<VerticalGridView>(R.id.settings_hub_grid).apply {
            // Aligned columns keep the rows full-width under the tab bar window.
            setWindowAlignment(VerticalGridView.WINDOW_ALIGN_BOTH_EDGE)
            setNumColumns(1)
        }
        gridView = grid

        // SET-004 backdrop: one random library asset behind the row groups; flat surface
        // (the scrim alone) when the library has nothing or background images are off.
        val backdrop = view.findViewById<ImageView>(R.id.settings_hub_backdrop)
        viewLifecycleOwner.lifecycleScope.launch {
            val url = SettingsBackdropLoader.fetchBackdropUrl()
            if (isAdded) {
                SettingsBackdropLoader.loadBackdrop(backdrop, url)
            }
        }

        // Settings rows never animate in (no staggered entries); layout is stable.
        grid.itemAnimator = null

        // The settings hub row adapter is built eagerly; the page is a plain vertical list,
        // so signal readiness immediately (same contract GridFragment/TimelineFragment keep).
        mMainFragmentAdapter.fragmentHost?.notifyViewCreated(mMainFragmentAdapter)

        // UP from the top row hands focus to the top tab bar (which lives in the activity
        // layout, outside this fragment's focus hierarchy). Inner rows move within the list.
        grid.setOnKeyInterceptListener { event ->
            if (event.action == KeyEvent.ACTION_DOWN &&
                event.keyCode == KeyEvent.KEYCODE_DPAD_UP &&
                grid.hasFocus()
            ) {
                val pos = grid.getChildAdapterPosition(grid.focusedChild)
                if (pos == RecyclerView.NO_POSITION || pos == 0) {
                    requireActivity().findViewById<View>(R.id.home_tab_bar)?.requestFocus()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }

        // After a tab is selected, hand focus into the freshly-shown list content.
        val navViewModel = ViewModelProvider(requireActivity()).get(HomeNavViewModel::class.java)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                navViewModel.contentFocusRequest.collect { request ->
                    if (request > 0) {
                        grid.postDelayed({ grid.requestFocus() }, 400)
                    }
                }
            }
        }

        // Collapse the floating tab bar once the list is scrolled away from the top, expand
        // at the top. VerticalGridView's GridLayoutManager doesn't implement scroll-offset
        // methods, so read the real scroll from the first child (same as GridFragment).
        grid.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val scrolled = recyclerView.childCount > 0 &&
                    recyclerView.getChildAt(0).top < recyclerView.paddingTop
                navViewModel.setTabBarExpanded(!scrolled)
            }
        })

        val selector = ClassPresenterSelector()
            .addClassPresenter(SettingsHubHeader::class.java, SettingsHubHeaderPresenter())
            .addClassPresenter(SettingsCard::class.java, SettingsHubRowPresenter())
        val adapter = ArrayObjectAdapter(selector)
        buildRows().forEach { (header, cards) ->
            adapter.add(header)
            adapter.addAll(adapter.size(), cards)
        }
        grid.adapter = ItemBridgeAdapter(adapter)

        // The adapter is ready synchronously; tell the PageRow host now that the view is up.
        view.post { mMainFragmentAdapter.fragmentHost?.notifyDataReady(mMainFragmentAdapter) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        gridView = null
    }

    override fun getMainFragmentAdapter(): BrowseSupportFragment.MainFragmentAdapter<*> {
        return mMainFragmentAdapter
    }

    /**
     * The grouped hub data. Row order matches the v1 hub: Server, View Settings,
     * Screensaver, Sections, Home screen channels, Debug.
     */
    private fun buildRows(): List<Pair<SettingsHubHeader, List<SettingsCard>>> = listOf(
        SettingsHubHeader(getString(R.string.settings_group_connection)) to listOf(
            SettingsCard(
                getString(R.string.server),
                null,
                "server",
                null,
                null
            ) {
                findNavController().navigate(HomeFragmentDirections.actionGlobalSignInFragment())
            }
        ),
        SettingsHubHeader(getString(R.string.settings_group_playback)) to listOf(
            SettingsCard(
                getString(R.string.view_settings),
                null,
                "view_settings",
                null,
                null
            ) {
                findNavController().navigate(
                    HomeFragmentDirections.actionGlobalToSettingsDialog(ViewPrefScreen.key)
                )
            },
            SettingsCard(
                getString(R.string.screensaver),
                null,
                "screensaver",
                null,
                null
            ) {
                findNavController().navigate(
                    HomeFragmentDirections.actionGlobalToSettingsDialog(ScreensaverPrefScreen.key)
                )
            }
        ),
        SettingsHubHeader(getString(R.string.settings_group_library)) to listOf(
            SettingsCard(
                getString(R.string.sections),
                null,
                "sections",
                null,
                null
            ) {
                findNavController().navigate(
                    HomeFragmentDirections.actionGlobalToSettingsDialog(SectionsPrefScreen.key)
                )
            },
            SettingsCard(
                getString(R.string.home_screen_channels),
                null,
                "home_screen_channels",
                null,
                null
            ) {
                findNavController().navigate(
                    HomeFragmentDirections.actionGlobalToSettingsDialog(HomeScreenChannelsPrefScreen.key)
                )
            }
        ),
        SettingsHubHeader(getString(R.string.settings_group_advanced)) to listOf(
            SettingsCard(
                getString(R.string.debug),
                null,
                "debug",
                null,
                null
            ) {
                findNavController().navigate(
                    HomeFragmentDirections.actionGlobalToSettingsDialog(DebugPrefScreen.key)
                )
            }
        ),
    )
}
