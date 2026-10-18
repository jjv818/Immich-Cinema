package nl.giejay.android.tv.immich.home.nav

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.ItemBridgeAdapter
import androidx.leanback.widget.Presenter

/**
 * Top navigation bar: a single-row horizontal strip of section tabs.
 *
 * Minimal functional styling here — the capsule/pill treatment arrives in S3. This view owns the
 * tab strip's focus state (reported through [onTabBarFocusChanged] so the activity can drive the
 * leave-off contract) and tab selection ([onTabSelected]).
 */
class HomeTabBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalGridView(context, attrs, defStyleAttr) {

    private val objectAdapter = ArrayObjectAdapter(TabPresenter())
    var onTabSelected: ((Int) -> Unit)? = null
    var onTabBarFocusChanged: ((Boolean) -> Unit)? = null

    init {
        adapter = ItemBridgeAdapter(objectAdapter)
        setNumRows(1)
        isFocusable = true
    }

    fun setTabs(titles: List<String>) {
        objectAdapter.clear()
        objectAdapter.addAll(0, titles.mapIndexed { i, name -> Tab(i, name) })
    }

    private data class Tab(val index: Int, val name: String)

    private inner class TabPresenter : Presenter() {
        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            val d = resources.displayMetrics.density
            val tv = TextView(context).apply {
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                gravity = Gravity.CENTER
                isFocusable = true
                isFocusableInTouchMode = false
                setPadding((20 * d).toInt(), (12 * d).toInt(), (20 * d).toInt(), (12 * d).toInt())
                setTextColor(UNFOCUSED_COLOR)
                includeFontPadding = false
                setOnFocusChangeListener { v, hasFocus ->
                    (v as TextView).setTextColor(if (hasFocus) FOCUSED_COLOR else UNFOCUSED_COLOR)
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
            tv.setOnClickListener { onTabSelected?.invoke(tab.index) }
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder) = Unit
    }

    companion object {
        private const val FOCUSED_COLOR = 0xFFFFFFFF.toInt()
        private const val UNFOCUSED_COLOR = 0xB3FFFFFF.toInt()
    }
}
