package nl.giejay.android.tv.immich

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Guards the design system: every token resolves at runtime. A typo in a token name fails at
 * compile time (the R.* reference), and a broken resource value fails here, in CI, not on the TV.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DesignTokenTest {

    private val resources = RuntimeEnvironment.getApplication().resources

    @Test
    fun `colors resolve`() {
        val colors = intArrayOf(
            R.color.surface_base, R.color.surface_raised, R.color.text_primary,
            R.color.text_secondary, R.color.text_tertiary, R.color.focus_hairline,
            R.color.accent_brand, R.color.tab_scrim
        )
        for (color in colors) {
            resources.getColor(color)
        }
    }

    @Test
    fun `dimens resolve`() {
        val dimens = intArrayOf(
            R.dimen.text_title, R.dimen.text_heading, R.dimen.text_body, R.dimen.text_meta,
            R.dimen.space_1, R.dimen.space_2, R.dimen.space_3, R.dimen.space_4, R.dimen.space_5,
            R.dimen.radius_card, R.dimen.radius_rail, R.dimen.radius_cell,
            R.dimen.tab_bar_height, R.dimen.tab_bar_top_margin,
            R.dimen.artwork_card_width, R.dimen.artwork_card_height
        )
        for (dimen in dimens) {
            resources.getDimension(dimen)
        }
    }

    @Test
    fun `integers resolve`() {
        val integers = intArrayOf(R.integer.motion_ui_ms, R.integer.motion_background_ms)
        for (integer in integers) {
            resources.getInteger(integer)
        }
    }

    @Test
    fun `drawables resolve`() {
        val drawables = intArrayOf(
            R.drawable.scrim_bottom, R.drawable.scrim_top, R.drawable.focus_hairline,
            R.drawable.focus_hairline_selector, R.drawable.tab_bar_background,
            R.drawable.tab_pill, R.drawable.settings_row_background
        )
        for (drawable in drawables) {
            resources.getDrawable(drawable, null)
        }
    }
}
