/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package nl.giejay.android.tv.immich.shared.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.leanback.app.BrandedSupportFragment
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.transition.TransitionHelper
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.BrowseFrameLayout
import androidx.leanback.widget.OnChildLaidOutListener
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.VerticalGridPresenter
import androidx.leanback.widget.VerticalGridView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import nl.giejay.android.tv.immich.R
import nl.giejay.android.tv.immich.home.nav.HomeNavViewModel
import nl.giejay.android.tv.immich.shared.grid.GridPaging
import timber.log.Timber

/**
 * A fragment for rendering items in a vertical grids.
 */
open class GridFragment(val hideProgressBar: Boolean = false) : BrandedSupportFragment(), BrowseSupportFragment.MainFragmentAdapterProvider {
    private var manuallySelectedPosition: Int = -1
    private var mAdapter: ArrayObjectAdapter = ArrayObjectAdapter()
    private var mGridPresenter: VerticalGridPresenter? = null
    private var mGridViewHolder: VerticalGridPresenter.ViewHolder? = null
    private var mOnItemViewSelectedListener: OnItemViewSelectedListener? = null
    private var mOnItemViewClickedListener: OnItemViewClickedListener? = null
    private var mSceneAfterEntranceTransition: Any? = null
    var progressBar: ProgressBar? = null
    private var mSelectedPosition = -1
    private var navViewModel: HomeNavViewModel? = null
    private val mMainFragmentAdapter: BrowseSupportFragment.MainFragmentAdapter<Fragment> =
        object : BrowseSupportFragment.MainFragmentAdapter<Fragment>(this) {
            override fun setEntranceTransitionState(state: Boolean) {
                this@GridFragment.setEntranceTransitionState(state)
            }
        }
    var gridPresenter: VerticalGridPresenter?
        /**
         * Returns the grid presenter.
         */
        get() = mGridPresenter
        /**
         * Sets the grid presenter.
         */
        set(gridPresenter) {
            requireNotNull(gridPresenter) { "Grid presenter may not be null" }
            mGridPresenter = gridPresenter
            mGridPresenter!!.onItemViewSelectedListener = mViewSelectedListener
            if (mOnItemViewClickedListener != null) {
                mGridPresenter!!.onItemViewClickedListener = mOnItemViewClickedListener
            }
        }
    var adapter: ArrayObjectAdapter
        /**
         * Returns the object adapter.
         */
        get() = mAdapter
        /**
         * Sets the object adapter for the fragment.
         */
        set(adapter) {
            mAdapter = adapter
            updateAdapter()
        }
    private val mViewSelectedListener: OnItemViewSelectedListener =
        OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            val position: Int = mGridViewHolder!!.gridView.selectedPosition
            gridOnItemSelected(position)
            if (mOnItemViewSelectedListener != null) {
                mOnItemViewSelectedListener!!.onItemSelected(
                    itemViewHolder, item,
                    rowViewHolder, row
                )
            }
        }
    private val mChildLaidOutListener: OnChildLaidOutListener =
        OnChildLaidOutListener { _, _, position, _ ->
            if (position == 0) {
                showOrHideTitle()
            }
        }

    /**
     * Sets an item selection listener.
     */
    fun setOnItemViewSelectedListener(listener: OnItemViewSelectedListener?) {
        mOnItemViewSelectedListener = listener
    }

    private fun gridOnItemSelected(position: Int) {
        mSelectedPosition = position
        showOrHideTitle()
    }

    private fun showOrHideTitle() {
        // Section-page headings are dropped (AGENTS.md §5.4): the top tab bar already marks the
        // active section. Detail pages (album/person/folder) keep their contextual heading, which
        // they set via setTitle(); those show, empty section titles do not.
        val hasContextualTitle = !title.isNullOrBlank()
        showTitle(hasContextualTitle)
        mMainFragmentAdapter.fragmentHost?.showTitleView(hasContextualTitle)
    }

    var onItemViewClickedListener: OnItemViewClickedListener?
        /**
         * Returns the item clicked listener.
         */
        get() = mOnItemViewClickedListener
        /**
         * Sets an item clicked listener.
         */
        set(listener) {
            mOnItemViewClickedListener = listener
            if (mGridPresenter != null) {
                mGridPresenter!!.onItemViewClickedListener = mOnItemViewClickedListener
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.i("Loading ${this.javaClass.simpleName}")
        return inflater.inflate(R.layout.grid_fragment, container, false)
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val gridDock: ViewGroup = view.findViewById(R.id.browse_grid_dock)
        progressBar = gridDock.findViewById(R.id.browse_progressbar)
        if (hideProgressBar) {
            progressBar?.visibility = View.GONE
        }
        installTitleView(LayoutInflater.from(requireContext()), gridDock, savedInstanceState)
        mGridViewHolder = mGridPresenter?.onCreateViewHolder(gridDock)
        mGridViewHolder?.view?.apply { gridDock.addView(this) }
        mGridViewHolder?.gridView?.setOnChildLaidOutListener(mChildLaidOutListener)

        // GRID-007: Leanback's BaseGridView has no page navigation, so PAGE_UP/PAGE_DOWN (and the
        // channel up/down variants) did nothing on the asset grids. Intercept them here and jump a
        // screenful using the pure GridPaging helper. Only ACTION_DOWN is consumed so the matching
        // ACTION_UP still reaches the system.
        mGridViewHolder?.gridView?.setOnKeyInterceptListener { event -> handleGridPageKey(event) }

        // UP from the top grid row hands focus to the top tab bar (which lives in the activity
        // layout, outside this fragment's focus hierarchy).
        val frame = view.findViewById<BrowseFrameLayout>(R.id.grid_frame)
        frame.onFocusSearchListener = BrowseFrameLayout.OnFocusSearchListener { focused, direction ->
            if (direction == View.FOCUS_UP && focused != null) {
                // selectedPosition is the focused card's adapter index (getChildAdapterPosition
                // returns -1 through Leanback's ItemBridgeAdapter wrapper).
                val pos = mGridViewHolder?.gridView?.selectedPosition ?: -1
                val columns = mGridPresenter?.numberOfColumns ?: 1
                if (pos in 0 until columns) {
                    requireActivity().findViewById<View>(R.id.home_tab_bar)?.requestFocus()
                    return@OnFocusSearchListener focused
                }
            }
            null
        }

        // After a tab is selected, hand focus into the freshly-shown grid content.
        val navViewModel = ViewModelProvider(requireActivity()).get(HomeNavViewModel::class.java)
        this.navViewModel = navViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                navViewModel.contentFocusRequest.collect { request ->
                    if (request > 0) {
                        mGridViewHolder?.gridView?.postDelayed({
                            mGridViewHolder?.gridView?.requestFocus()
                        }, 400)
                    }
                }
            }
        }

        // Collapse the floating tab bar once the grid is scrolled away from the top, expand at the
        // top. VerticalGridView's GridLayoutManager does not implement the scroll-offset methods
        // (canScrollVertically(-1) is always false), so read the real scroll from the first child.
        mGridViewHolder?.gridView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val scrolled = recyclerView.childCount > 0 &&
                    recyclerView.getChildAt(0).top < recyclerView.paddingTop
                navViewModel.setTabBarExpanded(!scrolled)
            }
        })

        mSceneAfterEntranceTransition =
            TransitionHelper.createScene(gridDock) { setEntranceTransitionState(true) }
        mainFragmentAdapter.fragmentHost?.notifyViewCreated(mMainFragmentAdapter)
        updateAdapter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.i("${this.javaClass.simpleName} got destroyed")
        mGridViewHolder = null
    }

    /**
     * GRID-007: handles the remote's page keys on the asset grids. Returns true when the key was
     * consumed by the grid (so the default DPAD handling never sees it).
     */
    private fun handleGridPageKey(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            return false
        }
        val forward = when (event.keyCode) {
            KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> true
            KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_CHANNEL_UP -> false
            else -> return false
        }
        val gridView = mGridViewHolder?.gridView ?: return false
        val itemCount = gridView.adapter?.itemCount ?: 0
        if (itemCount <= 0) {
            return false
        }
        val selected = gridView.selectedPosition
        val target = GridPaging.pageTargetPosition(
            selected = selected,
            columns = gridColumns(gridView),
            rowsPerPage = visibleRowCount(gridView),
            itemCount = itemCount,
            forward = forward
        )
        if (target != selected) {
            gridView.setSelectedPositionSmooth(target)
        }
        if (!gridView.hasFocus()) {
            gridView.requestFocus()
        }
        return true
    }

    /**
     * Columns are read from the layout rather than hard-coded: [VerticalGridPresenter] installs a
     * [GridLayoutManager] whose span count is the real column count. Falls back to the presenter's
     * configured column count, then to 1, if the grid has not been laid out yet.
     */
    private fun gridColumns(gridView: VerticalGridView): Int {
        val spanCount = (gridView.layoutManager as? GridLayoutManager)?.spanCount ?: 0
        if (spanCount > 0) {
            return spanCount
        }
        val presenterColumns = mGridPresenter?.numberOfColumns ?: 0
        return presenterColumns.coerceAtLeast(1)
    }

    /**
     * Rows per page = fully visible rows in the grid viewport. Derived from the first laid-out
     * child's height so it adapts to the current card size and window height.
     */
    private fun visibleRowCount(gridView: VerticalGridView): Int {
        val childHeight = gridView.getChildAt(0)?.height ?: 0
        if (childHeight <= 0) {
            return 1
        }
        val viewport = gridView.height - gridView.paddingTop - gridView.paddingBottom
        if (viewport <= 0) {
            return 1
        }
        return (viewport / childHeight).coerceAtLeast(1)
    }

    override fun getMainFragmentAdapter(): BrowseSupportFragment.MainFragmentAdapter<*> {
        return mMainFragmentAdapter
    }

    /**
     * Sets the selected item position.
     */
    private fun setSelectedPosition(position: Int) {
        mSelectedPosition = position
        if (mGridViewHolder != null && mGridViewHolder!!.gridView.adapter != null) {
            mGridViewHolder!!.gridView.setSelectedPositionSmooth(position)
        }
    }

    private fun updateAdapter() {
        if (mGridViewHolder != null) {
            mGridPresenter?.onBindViewHolder(mGridViewHolder!!, mAdapter)
            if (mSelectedPosition != -1) {
                updateSelectionPosition(mSelectedPosition)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (manuallySelectedPosition != -1) {
            updateManualPositionHandler(manuallySelectedPosition)
        }
    }

    protected open fun updateManualPositionHandler(selectedPosition: Int) {
        Handler(Looper.getMainLooper()!!).postDelayed({
            setSelectedPosition(selectedPosition)
            manuallySelectedPosition = -1
        }, 100)
    }

    fun manualUpdatePosition(position: Int) {
        manuallySelectedPosition = position
    }

    private fun updateSelectionPosition(selectionPosition: Int) {
        mGridViewHolder?.gridView?.selectedPosition = selectionPosition
    }

    fun setEntranceTransitionState(afterTransition: Boolean) {
        mGridPresenter?.setEntranceTransitionState(mGridViewHolder!!, afterTransition)
    }
}