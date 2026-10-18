package nl.giejay.android.tv.immich.home

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.DividerPresenter
import androidx.leanback.widget.DividerRow
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.PageRow
import androidx.leanback.widget.PresenterSelector
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowHeaderPresenter
import androidx.leanback.widget.SectionRow
import kotlinx.coroutines.launch
import nl.giejay.android.tv.immich.ImmichApplication
import nl.giejay.android.tv.immich.R
import nl.giejay.android.tv.immich.album.AlbumFragment
import nl.giejay.android.tv.immich.home.nav.HomeNavViewModel
import nl.giejay.android.tv.immich.assets.AllAssetFragment
import nl.giejay.android.tv.immich.assets.FolderFragment
import nl.giejay.android.tv.immich.assets.RandomAssetsFragment
import nl.giejay.android.tv.immich.assets.RecentAssetsFragment
import nl.giejay.android.tv.immich.assets.SimilarTimeAssetsFragment
import nl.giejay.android.tv.immich.people.PeopleFragment
import nl.giejay.android.tv.immich.settings.SettingsFragment
import nl.giejay.android.tv.immich.shared.fragment.GridFragment
import nl.giejay.android.tv.immich.shared.prefs.HIDDEN_HOME_ITEMS
import nl.giejay.android.tv.immich.shared.prefs.PreferenceManager
import nl.giejay.android.tv.immich.timeline.TimelineFragment
import timber.log.Timber

class HomeFragment : BrowseSupportFragment() {
    private lateinit var mRowsAdapter: ArrayObjectAdapter
    private lateinit var rows: List<PageRow>
    val immichRowPresenter = ImmichRowPresenter()
    private val homeNavViewModel: HomeNavViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.i("Loaded Home")

        setupUi()
        loadData()

        mainFragmentRegistry.registerFragment(PageRow::class.java, PageRowFragmentFactory())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // The top tab bar drives page selection; mirror its selection onto the browse host.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeNavViewModel.selectedTabIndex.collect { index -> selectedPosition = index }
            }
        }
    }

    private fun setupUi() {
        headersState = HEADERS_DISABLED
        brandColor = resources.getColor(android.R.color.black)
        // First page is Timeline, which never shows a Browse title.
        title = ""
        showTitle(false)
//        setOnSearchClickedListener {
//            Toast.makeText(
//                activity, "Search!", Toast.LENGTH_SHORT
//            ).show()
//        }

        // Lines of code to be added
        val sHeaderPresenter: PresenterSelector = ClassPresenterSelector()
            .addClassPresenter(DividerRow::class.java, DividerPresenter())
            .addClassPresenter(
                SectionRow::class.java,
                RowHeaderPresenter()
            )
            .addClassPresenter(Row::class.java, immichRowPresenter)

        setHeaderPresenterSelector(sHeaderPresenter)
    }

    private fun loadData() {
        mRowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        adapter = mRowsAdapter
        rows = createRows()
        mRowsAdapter.addAll(0, rows)
        // Tab bar mirrors the same filtered list, so tab index == selectedPosition.
        homeNavViewModel.setSections(rows.map { it.headerItem.name })
    }

    private fun createRows(): List<PageRow> {
        return HEADERS.mapIndexed { index, header -> index to header }
            .filter { (index, header) ->
                index != EDIT_INDEX &&
                    !PreferenceManager.itemInStringSet(header.name, HIDDEN_HOME_ITEMS)
            }
            .map { (index, header) -> PageRow(HeaderItem(index.toLong(), header.name)) }
    }

    private class PageRowFragmentFactory : FragmentFactory<Fragment>() {
        override fun createFragment(rowObj: Any): Fragment {
            val row = rowObj as Row
            Timber.i("Going to show page: ${row.headerItem.name}")
            return HEADERS[row.headerItem.id.toInt()].fragment()
        }
    }

    companion object {
        private val HEADERS: List<Header> = listOf(
            Header(ImmichApplication.appContext!!.getString(R.string.timeline)) { TimelineFragment() },
            Header(ImmichApplication.appContext!!.getString(R.string.albums)) {
                AlbumFragment().apply {
                    arguments = bundleOf("selectionMode" to false)
                }
            },
            Header(ImmichApplication.appContext!!.getString(R.string.photos)) { AllAssetFragment() },
            Header(ImmichApplication.appContext!!.getString(R.string.random)) { RandomAssetsFragment() },
            Header(ImmichApplication.appContext!!.getString(R.string.people)) { PeopleFragment() },
            Header(ImmichApplication.appContext!!.getString(R.string.recent)) { RecentAssetsFragment() },
            Header(ImmichApplication.appContext!!.getString(R.string.seasonal)) { SimilarTimeAssetsFragment() },
            Header(ImmichApplication.appContext!!.getString(R.string.folders)) { FolderFragment() },
            Header(ImmichApplication.appContext!!.getString(R.string.edit)) { GridFragment(hideProgressBar = true) },
            Header(ImmichApplication.appContext!!.getString(R.string.settings)) { SettingsFragment() },
        )

        private val EDIT_INDEX: Int =
            HEADERS.indexOfFirst { it.name == ImmichApplication.appContext!!.getString(R.string.edit) }
    }
}

class Header(val name: String, var show: Boolean = false, val fragment: () -> Fragment)
