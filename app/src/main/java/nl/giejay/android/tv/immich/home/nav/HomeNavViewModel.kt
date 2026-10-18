package nl.giejay.android.tv.immich.home.nav

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Activity-scoped state shared by the top tab bar (owned by [nl.giejay.android.tv.immich.MainActivity])
 * and the `HomeFragment`/`TimelineFragment` pages it navigates.
 *
 * This is the "NavFocusContract" from AGENTS.md §6.2, realised as a shared ViewModel:
 * [isTabBarFocused] replaces the `BrowseSupportFragment` "headers showing" signal that
 * `TimelineFragment`'s leave-off logic used to depend on, and [selectedTabIndex] replaces the
 * header rail's selected position.
 */
class HomeNavViewModel : ViewModel() {

    private val _sections = MutableStateFlow<List<String>>(emptyList())
    /** Visible section titles in tab order (tab index == `BrowseSupportFragment.selectedPosition`). */
    val sections: StateFlow<List<String>> = _sections

    private val _selectedTabIndex = MutableStateFlow(0)
    /** Selected tab position; `HomeFragment` mirrors it onto `selectedPosition`. */
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex

    private val _isTabBarFocused = MutableStateFlow(false)
    /** True while the tab bar (nav chrome) owns focus. Replaces `isBrowseShowingHeaders()`. */
    val isTabBarFocused: StateFlow<Boolean> = _isTabBarFocused

    private val _contentFocusRequest = MutableStateFlow(0)
    /** Incremented each time a tab is selected; page fragments request content focus in response. */
    val contentFocusRequest: StateFlow<Int> = _contentFocusRequest

    fun setSections(titles: List<String>) {
        _sections.value = titles
    }

    fun selectTab(index: Int) {
        _selectedTabIndex.value = index
    }

    fun setTabBarFocused(focused: Boolean) {
        _isTabBarFocused.value = focused
    }

    /** Signals the currently-shown page to take focus (after a tab selection). */
    fun requestContentFocus() {
        _contentFocusRequest.value += 1
    }
}
