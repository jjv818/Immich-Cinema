package nl.giejay.android.tv.immich.settings

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.widget.Presenter
import nl.giejay.android.tv.immich.R

/**
 * Text-first settings row: a [surface_raised] label with a white focus hairline. No icon tile.
 */
class SettingsTextPresenter(private val context: Context) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val d = context.resources.displayMetrics.density
        val tv = TextView(context).apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            isFocusableInTouchMode = false
            setPadding((28 * d).toInt(), (16 * d).toInt(), (28 * d).toInt(), (16 * d).toInt())
            includeFontPadding = false
            setTextColor(context.getColor(R.color.text_primary))
            setBackgroundResource(R.drawable.settings_row_background)
        }
        return ViewHolder(tv)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val card = item as SettingsCard
        val tv = viewHolder.view as TextView
        tv.text = card.title
        tv.setOnClickListener { card.onClick() }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) = Unit
}
