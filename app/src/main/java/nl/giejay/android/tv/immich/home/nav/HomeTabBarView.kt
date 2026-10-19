package nl.giejay.android.tv.immich.home.nav

import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.ItemBridgeAdapter
import androidx.leanback.widget.Presenter
import nl.giejay.android.tv.immich.R

/**
 * Top navigation bar: a single-row horizontal strip of section tabs.
 *
 * Styled as a floating capsule (translucent dark [R.drawable.tab_bar_background]) with the active
 * tab rendered as a solid white pill ([R.drawable.tab_pill]) carrying a near-black label. The active
 * tab is the focused one while the bar owns focus, or the selected section while focus is in page
 * content — so the pill always marks where you are. Unselected tabs stay near-white at ~90%, never
 * faded.
 *
 * This view owns the tab strip's focus state (reported through [onTabBarFocusChanged] so the
 * activity can drive the leave-off contract) and tab selection ([onTabSelected]).
 */
class HomeTabBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalGridView(context, attrs, defStyleAttr) {

    private val objectAdapter = ArrayObjectAdapter(TabPresenter())
    var onTabSelected: ((Int) -> Unit)? = null
    var onTabBarFocusChanged: ((Boolean) -> Unit)? = null
    /** Invoked when the user presses DOWN while the bar owns focus; hands focus into the page. */
    var onNavigateDown: (() -> Unit)? = null

    private var selectedIndex = 0

    init {
        adapter = ItemBridgeAdapter(objectAdapter)
        setNumRows(1)
        isFocusable = true
        // Capsule inset so the first/last tab pill doesn't sit flush against the rounded ends.
        val d = resources.displayMetrics.density
        setPadding((16 * d).toInt(), 0, (16 * d).toInt(), 0)
        clipToPadding = false
    }

    fun setTabs(titles: List<String>) {
        objectAdapter.clear()
        objectAdapter.addAll(0, titles.mapIndexed { i, name -> Tab(i, name) })
        resizeToContent(titles)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // DOWN while the bar owns focus: hand focus into the page content instead of leaving the
        // remote stuck on the tab bar. The page reports scroll, which collapses the bar.
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && hasFocus()) {
            onNavigateDown?.invoke()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onFocusChanged(
        gainFocus: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        // The pill follows the focused tab while the bar owns focus and the selected tab once
        // focus leaves. A child only repaints on its own focus change, so without this full
        // rebind the pill it wore when it lost focus stays white (two pills on screen).
        if (objectAdapter.size() > 0) {
            objectAdapter.notifyItemRangeChanged(0, objectAdapter.size())
        }
    }

    /**
     * Size the capsule to its content (capped at 90% of the screen width) so it floats centred.
     * When the labels don't fit, the grid scrolls internally instead of clipping the last tab.
     */
    private fun resizeToContent(titles: List<String>) {
        val d = resources.displayMetrics.density
        val probe = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_body))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tabPad = (18 * d).toInt() * 2
        val capsulePad = (16 * d).toInt() * 2
        var contentWidth = capsulePad
        for (title in titles) {
            contentWidth += probe.paint.measureText(title).toInt() + tabPad
        }
        val maxWidth = (resources.displayMetrics.widthPixels * 0.90f).toInt()
        val lp = layoutParams
        lp.width = minOf(contentWidth, maxWidth)
        layoutParams = lp
    }

    /** Marks [index] as the selected section; drives the persistent pill while focus is in content. */
    fun setSelectedIndex(index: Int) {
        if (index == selectedIndex) return
        val previous = selectedIndex
        selectedIndex = index
        objectAdapter.notifyArrayItemRangeChanged(previous, 1)
        objectAdapter.notifyArrayItemRangeChanged(index, 1)
    }

    private fun isActive(tab: Tab, focused: Boolean): Boolean =
        if (hasFocus()) focused else tab.index == selectedIndex

    private data class Tab(val index: Int, val name: String)

    private inner class TabPresenter : Presenter() {
        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            val d = resources.displayMetrics.density
            val tv = TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_body))
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                gravity = Gravity.CENTER
                isFocusable = true
                isFocusableInTouchMode = false
                // Fill the capsule height so the text (and the white pill) sits vertically centred.
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setPadding((18 * d).toInt(), 0, (18 * d).toInt(), 0)
                includeFontPadding = false
                setOnFocusChangeListener { v, hasFocus ->
                    paint(v as TextView, v.tag as Tab, hasFocus)
                    // Report the aggregate tab-bar focus state once this focus change settles, so a
                    // tab-to-tab move does not emit a spurious exit/enter pair.
                    v.post { onTabBarFocusChanged?.invoke(this@HomeTabBarView.hasFocus()) }
                }
            }
            return ViewHolder(tv)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
            val tab = item as Tab
            val tv = viewHolder.view as TextView
            tv.text = tab.name
            tv.tag = tab
            tv.setOnClickListener { onTabSelected?.invoke(tab.index) }
            paint(tv, tab, tv.hasFocus())
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder) = Unit
    }

    private fun paint(tv: TextView, tab: Tab, focused: Boolean) {
        val active = isActive(tab, focused)
        tv.background = if (active) context.getDrawable(R.drawable.tab_pill) else null
        val color = if (active) R.color.tab_text_active else R.color.tab_text_idle
        tv.setTextColor(ContextCompat.getColor(context, color))
    }
}
