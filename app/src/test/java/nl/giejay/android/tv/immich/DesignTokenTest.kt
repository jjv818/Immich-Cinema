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
            R.color.focus_glow, R.color.accent_brand, R.color.tab_scrim,
            R.color.tab_text_active, R.color.tab_text_idle, R.color.scrubber_badge,
            R.color.scrim_bottom_start, R.color.scrim_bottom_end,
            R.color.scrim_top_start, R.color.scrim_top_end,
            R.color.card_placeholder, R.color.playback_header,
            R.color.playback_item_regular, R.color.playback_item_favorite,
            R.color.popup_background
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
            R.dimen.artwork_card_width, R.dimen.artwork_card_height,
            R.dimen.focus_hairline_width, R.dimen.focus_glow_width, R.dimen.focus_hairline_inset,
            R.dimen.timeline_row_height, R.dimen.timeline_row_gap,
            R.dimen.card_scrim_height, R.dimen.card_text_padding_horizontal,
            R.dimen.card_text_padding_bottom, R.dimen.video_badge_margin,
            R.dimen.video_badge_size,
            R.dimen.day_header_padding_vertical, R.dimen.scrubber_padding_v,
            R.dimen.scrubber_label_end_pad, R.dimen.scrubber_dot_end_pad,
            R.dimen.scrubber_passive_line_width, R.dimen.scrubber_badge_pad_h,
            R.dimen.scrubber_badge_pad_v, R.dimen.scrubber_badge_radius,
            R.dimen.scrubber_accent_width
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
            R.drawable.focus_hairline_cell, R.drawable.focus_hairline_selector,
            R.drawable.tab_bar_background,
            R.drawable.tab_pill, R.drawable.settings_row_background
        )
        for (drawable in drawables) {
            resources.getDrawable(drawable, null)
        }
    }
}
