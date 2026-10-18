package nl.giejay.android.tv.immich.home.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tab bar and page fragments coordinate through this activity-scoped ViewModel; this guards
 * the focus contract (isTabBarFocused / contentFocusRequest) and tab selection.
 */
class HomeNavViewModelTest {

    @Test
    fun `selectTab updates selected index`() {
        val vm = HomeNavViewModel()
        assertEquals(0, vm.selectedTabIndex.value)
        vm.selectTab(3)
        assertEquals(3, vm.selectedTabIndex.value)
    }

    @Test
    fun `setSections publishes titles in order`() {
        val vm = HomeNavViewModel()
        vm.setSections(listOf("Timeline", "Albums", "Settings"))
        assertEquals(listOf("Timeline", "Albums", "Settings"), vm.sections.value)
    }

    @Test
    fun `requestContentFocus increments monotonically`() {
        val vm = HomeNavViewModel()
        val before = vm.contentFocusRequest.value
        vm.requestContentFocus()
        vm.requestContentFocus()
        assertEquals(before + 2, vm.contentFocusRequest.value)
    }

    @Test
    fun `setTabBarFocused toggles focus state`() {
        val vm = HomeNavViewModel()
        assertFalse(vm.isTabBarFocused.value)
        vm.setTabBarFocused(true)
        assertTrue(vm.isTabBarFocused.value)
        vm.setTabBarFocused(false)
        assertFalse(vm.isTabBarFocused.value)
    }
}
