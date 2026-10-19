package nl.giejay.android.tv.immich.home.nav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the AGENTS.md §6.2 nav-focus contract, realised as [HomeNavViewModel.isTabBarFocused].
 *
 * `TimelineFragment` maps `isNavChromeExpanded()` to this value and, on enter, captures the
 * resume state + begins the focus lock; on exit it restores the leave-off selection. That mapping
 * must fire **exactly once per real transition** — a duplicate `true` (e.g. tab-to-tab movement
 * that briefly re-reports focus) must not re-capture, or leave-off gets clobbered.
 */
class NavFocusContractTest {

    @Test
    fun `isNavChromeExpanded mirrors the tab-bar focus state`() {
        val vm = HomeNavViewModel()
        assertFalse("collapsed at rest", vm.isTabBarFocused.value)
        vm.setTabBarFocused(true)
        assertTrue("expanded while the bar owns focus", vm.isTabBarFocused.value)
        vm.setTabBarFocused(false)
        assertFalse("collapsed again when focus returns to content", vm.isTabBarFocused.value)
    }

    @Test
    fun `enter and exit fire exactly once per transition`() = runBlocking {
        val vm = HomeNavViewModel()
        // Mirror TimelineFragment's listener: enter -> capture+lock, exit -> restore.
        var enters = 0
        var exits = 0
        val job = launch(Dispatchers.Unconfined) {
            vm.isTabBarFocused.drop(1).collect { expanded ->
                if (expanded) enters++ else exits++
            }
        }

        vm.setTabBarFocused(true)
        vm.setTabBarFocused(true)   // duplicate: must not re-capture
        vm.setTabBarFocused(false)
        vm.setTabBarFocused(false)  // duplicate: must not re-restore

        job.cancel()

        assertEquals("one capture/lock per enter", 1, enters)
        assertEquals("one restore per exit", 1, exits)
    }

    @Test
    fun `tab activation requests content focus`() {
        val vm = HomeNavViewModel()
        val before = vm.contentFocusRequest.value
        vm.selectTab(2)
        vm.requestContentFocus()
        assertEquals(2, vm.selectedTabIndex.value)
        assertEquals("activation hands focus into the page", before + 1, vm.contentFocusRequest.value)
    }
}
